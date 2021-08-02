package cofh.thermal.dynamics.block;

import cofh.core.block.TileBlock6Way;
import cofh.core.tileentity.TileCoFH;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;
import java.util.function.Supplier;

import static cofh.lib.util.constants.Constants.FACING_ALL;

public class TileBlockEnderTunnel extends TileBlock6Way {

    private static final VoxelShape[] OPENING_SHAPE = new VoxelShape[]{
            Block.makeCuboidShape(1, 0, 1, 15, 4, 15),
            Block.makeCuboidShape(1, 12, 1, 15, 16, 15),
            Block.makeCuboidShape(1, 1, 0, 15, 15, 4),
            Block.makeCuboidShape(1, 1, 12, 15, 15, 16),
            Block.makeCuboidShape(0, 1, 1, 4, 15, 15),
            Block.makeCuboidShape(12, 1, 1, 16, 15, 15)
    };

    private static final VoxelShape[] CENTRAL_SHAPE = new VoxelShape[]{
            Block.makeCuboidShape(4, 4, 4, 12, 8, 12),
            Block.makeCuboidShape(4, 8, 4, 12, 12, 12),
            Block.makeCuboidShape(4, 4, 4, 12, 12, 8),
            Block.makeCuboidShape(4, 4, 8, 12, 12, 12),
            Block.makeCuboidShape(4, 4, 4, 8, 12, 12),
            Block.makeCuboidShape(8, 4, 4, 12, 12, 12)
    };

    private static final VoxelShape[] TUNNEL_SHAPE = new VoxelShape[]{
            VoxelShapes.or(OPENING_SHAPE[0], CENTRAL_SHAPE[0]),
            VoxelShapes.or(OPENING_SHAPE[1], CENTRAL_SHAPE[1]),
            VoxelShapes.or(OPENING_SHAPE[2], CENTRAL_SHAPE[2]),
            VoxelShapes.or(OPENING_SHAPE[3], CENTRAL_SHAPE[3]),
            VoxelShapes.or(OPENING_SHAPE[4], CENTRAL_SHAPE[4]),
            VoxelShapes.or(OPENING_SHAPE[5], CENTRAL_SHAPE[5])
    };

    public TileBlockEnderTunnel(Properties builder, Supplier<? extends TileCoFH> supplier) {

        super(builder, supplier);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {

        return this.getDefaultState().with(FACING_ALL, context.getNearestLookingDirection());
    }

    @Override
    public ItemStack getPickBlock(BlockState state, RayTraceResult target, IBlockReader world, BlockPos pos, PlayerEntity player) {

        // Ensures that creative mode doesn't copy UUID of the block!
        return new ItemStack(this);
    }

    @Override
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {

        return TUNNEL_SHAPE[state.get(FACING_ALL).ordinal()];
    }

}
