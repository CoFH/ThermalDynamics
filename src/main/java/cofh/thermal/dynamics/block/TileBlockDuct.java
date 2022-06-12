package cofh.thermal.dynamics.block;

import cofh.lib.block.IDismantleable;
import cofh.lib.util.Utils;
import cofh.lib.util.raytracer.IndexedVoxelShape;
import cofh.lib.util.raytracer.MultiIndexedVoxelShape;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import cofh.thermal.dynamics.tileentity.DuctTileBase;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.IWaterLoggable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public class TileBlockDuct extends Block implements IWaterLoggable, IDismantleable {

    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final Int2ObjectMap<MultiIndexedVoxelShape> SHAPE_CACHE = new Int2ObjectOpenHashMap<>(512);
    private static final IndexedVoxelShape BASE_SHAPE = new IndexedVoxelShape(Block.box(4.5, 4.5, 4.5, 11.5, 11.5, 11.5), 0);

    protected final Supplier<? extends DuctTileBase> supplier;

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

    public TileBlockDuct(Properties properties, Supplier<? extends DuctTileBase> supplier) {

        super(properties);
        this.supplier = supplier;
    }

    @Override
    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {

        builder.add(WATERLOGGED);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {

        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {

        return supplier.get();
    }

    @Override
    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {

        if (Utils.isServerWorld(worldIn) && Utils.isWrench(player.getItemInHand(handIn).getItem())) {
            if (hit.subHit == 0) {
                // TODO: Attempt connection w/ adjacent duct OR block
            } else if (hit.subHit < 7) {
                // TODO: Sever connection w/ adjacent duct
            } else {
                // TODO: Sever connection w/ adjacent block
            }
        }
        return ActionResultType.PASS;
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos adj, boolean isMoving) {

        if (world.isClientSide()) {
            return;
        }
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHost) {
            IGridHost host = (IGridHost) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(world);
            gridContainer.ifPresent(e -> e.onGridHostNeighborChanged(host));
        }
    }

    @Override
    public void onPlace(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {

        if (worldIn.isClientSide()) {
            return;
        }
        TileEntity tile = worldIn.getBlockEntity(pos);
        if (tile instanceof IGridHost) {
            IGridHost host = (IGridHost) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(worldIn);
            gridContainer.ifPresent(e -> e.onGridHostPlaced(host));
        }
    }

    @Override
    public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {

        if (state.getBlock() != newState.getBlock()) {
            TileEntity tile = worldIn.getBlockEntity(pos);
            if (tile instanceof IGridHost) {
                IGridHost host = (IGridHost) tile;
                Optional<IGridContainer> gridContainer = IGridContainer.getCapability(worldIn);
                gridContainer.ifPresent(e -> e.onGridHostRemoved(host));
            }
            super.onRemove(state, worldIn, pos, newState, isMoving);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {

        TileEntity tile = worldIn.getBlockEntity(pos);
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
    public BlockState getStateForPlacement(BlockItemUseContext context) {

        boolean flag = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return this.defaultBlockState().setValue(WATERLOGGED, flag);
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {

        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
        if (worldIn.isClientSide()) {
            TileEntity tile = worldIn.getBlockEntity(currentPos);
            if (tile instanceof DuctTileBase) {
                tile.requestModelDataUpdate();
            }
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

}
