package cofh.thermal.dynamics.block;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import net.minecraft.block.Block;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;

public abstract class TileBlockDuctBase extends Block {

    public TileBlockDuctBase(Properties properties) {

        super(properties);
    }

    @Override
    public boolean hasTileEntity(BlockState state) {

        return true;
    }

    @Override
    public BlockRenderType getRenderShape(BlockState p_149645_1_) {

        return BlockRenderType.INVISIBLE;
    }

    @Override
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {

        if (world.isClientSide()) return;

        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHostInternal) {
            IGridHostInternal host = (IGridHostInternal) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(world);
            gridContainer.ifPresent(e -> e.onGridHostDestroyed(host));
        }
        super.playerWillDestroy(world, pos, state, player);
    }

    @Override
    public void setPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {

        if (world.isClientSide()) return;

        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHostInternal) {
            IGridHostInternal host = (IGridHostInternal) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(world);
            gridContainer.ifPresent(e -> e.onGridHostPlaced(host));
        }
    }

    @Override
    public void neighborChanged(BlockState state, World level, BlockPos pos, Block block, BlockPos adj, boolean isMoving) {

        if (level.isClientSide()) return;

        TileEntity tile = level.getBlockEntity(pos);
        if (tile instanceof IGridHostInternal) {
            IGridHostInternal host = (IGridHostInternal) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(level);
            gridContainer.ifPresent(e -> e.onGridHostNeighborChanged(host));
        }
    }
}
