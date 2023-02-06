package cofh.thermal.dynamics.block;

import cofh.core.network.packet.client.ModelUpdatePacket;
import cofh.lib.api.block.IDismantleable;
import cofh.lib.util.Utils;
import cofh.lib.util.raytracer.IndexedVoxelShape;
import cofh.lib.util.raytracer.MultiIndexedVoxelShape;
import cofh.lib.util.raytracer.RayTracer;
import cofh.lib.util.raytracer.VoxelShapeBlockHitResult;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHostLuminous;
import cofh.thermal.dynamics.block.entity.duct.DuctBlockEntity;
import cofh.thermal.dynamics.item.AttachmentItem;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

import static cofh.core.util.helpers.ItemHelper.consumeItem;
import static cofh.lib.util.Constants.DIRECTIONS;

public class DuctBlock extends Block implements EntityBlock, SimpleWaterloggedBlock, IDismantleable {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    private static final Int2ObjectMap<MultiIndexedVoxelShape> SHAPE_CACHE = new Int2ObjectOpenHashMap<>(512);
    private static final IndexedVoxelShape BASE_SHAPE = new IndexedVoxelShape(Block.box(4.5, 4.5, 4.5, 11.5, 11.5, 11.5), 0);

    private static final IndexedVoxelShape[] INTERNAL_CONNECTION = new IndexedVoxelShape[]{
            new IndexedVoxelShape(Block.box(4.5, 0, 4.5, 11.5, 4.5, 11.5), 1),
            new IndexedVoxelShape(Block.box(4.5, 11.5, 4.5, 11.5, 16, 11.5), 2),
            new IndexedVoxelShape(Block.box(4.5, 4.5, 0, 11.5, 11.5, 4.5), 3),
            new IndexedVoxelShape(Block.box(4.5, 4.5, 11.5, 11.5, 11.5, 16), 4),
            new IndexedVoxelShape(Block.box(0, 4.5, 4.5, 4.5, 11.5, 11.5), 5),
            new IndexedVoxelShape(Block.box(11.5, 4.5, 4.5, 16, 11.5, 11.5), 6)
    };

    private static final IndexedVoxelShape[] EXTERNAL_CONNECTION = new IndexedVoxelShape[]{
            new IndexedVoxelShape(Block.box(3.5, 0, 3.5, 12.5, 4.5, 12.5), 7),
            new IndexedVoxelShape(Block.box(3.5, 11.5, 3.5, 12.5, 16, 12.5), 8),
            new IndexedVoxelShape(Block.box(3.5, 3.5, 0, 12.5, 12.5, 4.5), 9),
            new IndexedVoxelShape(Block.box(3.5, 3.5, 11.5, 12.5, 12.5, 16), 10),
            new IndexedVoxelShape(Block.box(0, 3.5, 3.5, 4.5, 12.5, 12.5), 11),
            new IndexedVoxelShape(Block.box(11.5, 3.5, 3.5, 16, 12.5, 12.5), 12)
    };
    protected final Supplier<BlockEntityType<?>> blockEntityType;

    public DuctBlock(Properties builder, Supplier<BlockEntityType<?>> blockEntityType) {

        super(builder);
        this.blockEntityType = blockEntityType;
        this.registerDefaultState(this.stateDefinition.any().setValue(WATERLOGGED, false));
    }

    private static VoxelShape getConnectionShape(int connectionState) {

        if (SHAPE_CACHE.containsKey(connectionState)) {
            return SHAPE_CACHE.get(connectionState);
        }
        ImmutableSet.Builder<IndexedVoxelShape> cuboids = ImmutableSet.builder();
        cuboids.add(BASE_SHAPE);
        for (int i = 0; i < 6; ++i) {
            if ((connectionState & (1 << i + 6)) > 0) {
                cuboids.add(EXTERNAL_CONNECTION[i]);
            } else if ((connectionState & (1 << i)) > 0) {
                cuboids.add(INTERNAL_CONNECTION[i]);
            }
        }
        MultiIndexedVoxelShape retShape = new MultiIndexedVoxelShape(cuboids.build());
        SHAPE_CACHE.put(connectionState, retShape);
        return retShape;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {

        super.createBlockStateDefinition(builder);
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {

        return blockEntityType.get().create(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level worldIn, BlockPos pos, Player player, InteractionHand handIn, BlockHitResult hit) {

        if (worldIn.getBlockEntity(pos) instanceof DuctBlockEntity<?, ?> duct) {
            duct.calcDuctModelDataServer();
            HitResult rawHit = RayTracer.retrace(player, ClipContext.Fluid.NONE);
            if (rawHit instanceof VoxelShapeBlockHitResult advHit) {
                ItemStack heldStack = player.getItemInHand(handIn);
                if (Utils.isWrench(heldStack)) {
                    if (Utils.isClientWorld(worldIn)) {
                        return InteractionResult.SUCCESS;
                    }
                    if (advHit.subHit == 0) {
                        duct.attemptConnect(advHit.getDirection());
                    } else if (advHit.subHit < 7) {
                        duct.attemptDisconnect(DIRECTIONS[advHit.subHit - 1], player);
                    } else if (advHit.subHit < 13) {
                        duct.attemptDisconnect(DIRECTIONS[advHit.subHit - 7], player);
                    }
                    return InteractionResult.CONSUME;
                } else if (heldStack.isEmpty()) {
                    if (Utils.isClientWorld(worldIn)) {
                        return InteractionResult.SUCCESS;
                    }
                    if (advHit.subHit >= 7) {
                        if (duct.openAttachmentGui(DIRECTIONS[advHit.subHit - 7], player)) {
                            return InteractionResult.SUCCESS;
                        }
                    } else {
                        if (duct.openDuctGui(player)) {
                            return InteractionResult.SUCCESS;
                        }
                    }
                    return InteractionResult.CONSUME;
                } else if (heldStack.getItem() instanceof AttachmentItem attachmentItem) {
                    if (Utils.isClientWorld(worldIn)) {
                        return InteractionResult.SUCCESS;
                    }
                    if (advHit.subHit == 0) {
                        if (duct.attemptAttachmentInstall(advHit.getDirection(), attachmentItem.getAttachmentType(heldStack))) {
                            if (!player.getAbilities().instabuild) {
                                player.setItemInHand(handIn, consumeItem(heldStack, 1));
                            }
                        } else {
                            duct.openDuctGui(player);
                        }
                        return InteractionResult.SUCCESS;
                    } else if (advHit.subHit >= 7) {
                        if (duct.attemptAttachmentInstall(DIRECTIONS[advHit.subHit - 7], attachmentItem.getAttachmentType(heldStack))) {
                            if (!player.getAbilities().instabuild) {
                                player.setItemInHand(handIn, consumeItem(heldStack, 1));
                            }
                        } else {
                            duct.openAttachmentGui(DIRECTIONS[advHit.subHit - 7], player);
                        }
                        return InteractionResult.SUCCESS;
                    }
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {

        ModelUpdatePacket.sendToClient(level, pos);
    }

    @Override
    public void neighborChanged(BlockState state, Level worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {

        if (worldIn.isClientSide()) {
            return;
        }
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof IDuct<?, ?> host) {
            host.neighborChanged(blockIn, fromPos);
            IGridContainer gridContainer = IGridContainer.getCapability(worldIn);
            if (gridContainer != null && gridContainer.onDuctNeighborChanged(host) || worldIn.getBlockEntity(fromPos) instanceof IDuct<?, ?>) {
                worldIn.scheduleTick(pos, this, 1);
            }
        }
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {

        if (worldIn.isClientSide()) {
            return;
        }
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof IDuct<?, ?> host && !host.hasGrid()) {
            IGridContainer gridContainer = IGridContainer.getCapability(worldIn);
            if (gridContainer != null) {
                gridContainer.onDuctPlaced(host, null);
            }
        }
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {

        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tile = worldIn.getBlockEntity(pos);
            if (tile instanceof DuctBlockEntity<?, ?> host) {
                host.dropAttachments();
                IGridContainer gridContainer = IGridContainer.getCapability(worldIn);
                if (gridContainer != null) {
                    gridContainer.onDuctRemoved(host);
                }
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public int getLightEmission(BlockState state, BlockGetter world, BlockPos pos) {

        if (state.getLightEmission() > 0) {
            return state.getLightEmission();
        }
        if (world.getBlockEntity(pos) instanceof IGridHostLuminous tile) {
            return tile.getLightValue();
        }
        return state.getLightEmission();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {

        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof DuctBlockEntity<?, ?> duct) {
            return getConnectionShape(duct.getDuctModelData().getConnectionState());
        }
        return BASE_SHAPE;
    }

    @Override
    public FluidState getFluidState(BlockState state) {

        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {

        boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return super.getStateForPlacement(context).setValue(WATERLOGGED, flag);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, LevelAccessor worldIn, BlockPos currentPos, BlockPos facingPos) {

        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
        if (worldIn.isClientSide()) {
            BlockEntity tile = worldIn.getBlockEntity(currentPos);
            if (tile instanceof DuctBlockEntity<?, ?>) {
                tile.requestModelDataUpdate();
            }
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    // region IDismantleable
    @Override
    public void dismantleBlock(Level world, BlockPos pos, BlockState state, HitResult target, Player player, boolean returnDrops) {

        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof DuctBlockEntity<?, ?> duct) {
            duct.dismantleAttachments(player, returnDrops);
        }
        ItemStack dropBlock = this.getCloneItemStack(state, target, world, pos, player);
        world.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL_IMMEDIATE);
        if (!returnDrops || player == null || !player.addItem(dropBlock)) {
            Utils.dropDismantleStackIntoWorld(dropBlock, world, pos);
        }
    }
    // endregion
}
