package cofh.thermal.dynamics.block;

import cofh.lib.block.IDismantleable;
import cofh.lib.util.Utils;
import cofh.lib.util.raytracer.IndexedVoxelShape;
import cofh.lib.util.raytracer.MultiIndexedVoxelShape;
import cofh.lib.util.raytracer.VoxelShapeBlockHitResult;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.block.entity.DuctTileBase;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
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
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public class TileBlockDuct extends Block implements EntityBlock, SimpleWaterloggedBlock, IDismantleable {

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

    protected final Supplier<BlockEntityType<? extends DuctTileBase>> blockEntityType;

    public TileBlockDuct(Properties builder, Supplier<BlockEntityType<? extends DuctTileBase>> blockEntityType) {

        super(builder);
        this.blockEntityType = blockEntityType;
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

        if (Utils.isServerWorld(worldIn) && Utils.isWrench(player.getItemInHand(handIn)) && hit instanceof VoxelShapeBlockHitResult advHit && worldIn.getBlockEntity(pos) instanceof DuctTileBase duct) {

            if (advHit.subHit == 0) {
                duct.attemptConnect(advHit.getDirection());
            } else if (advHit.subHit < 7) {
                duct.attemptDisconnect(Direction.values()[advHit.subHit - 1]);
            } else if (advHit.subHit < 13) {
                duct.attemptDisconnect(Direction.values()[advHit.subHit - 7]);
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, Level world, BlockPos pos, Block block, BlockPos adj, boolean isMoving) {

        if (world.isClientSide()) {
            return;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHost host) {
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(world);
            gridContainer.ifPresent(e -> e.onGridHostNeighborChanged(host));
        }
    }

    @Override
    public void onPlace(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {

        if (worldIn.isClientSide()) {
            return;
        }
        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof IGridHost host) {
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(worldIn);
            gridContainer.ifPresent(e -> e.onGridHostPlaced(host));
        }
    }

    @Override
    public void onRemove(BlockState state, Level worldIn, BlockPos pos, BlockState newState, boolean isMoving) {

        if (state.getBlock() != newState.getBlock()) {
            BlockEntity tile = worldIn.getBlockEntity(pos);
            if (tile instanceof IGridHost host) {
                Optional<IGridContainer> gridContainer = IGridContainer.getCapability(worldIn);
                gridContainer.ifPresent(e -> e.onGridHostRemoved(host));
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter worldIn, BlockPos pos, CollisionContext context) {

        BlockEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof DuctTileBase) {
            return getConnectionShape(((DuctModelData) (tile.getModelData())).getConnectionState());
        }
        return super.getShape(state, worldIn, pos, context);
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
            if (tile instanceof DuctTileBase) {
                tile.requestModelDataUpdate();
            }
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

}