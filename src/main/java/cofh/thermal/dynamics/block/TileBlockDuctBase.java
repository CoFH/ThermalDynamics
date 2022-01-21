package cofh.thermal.dynamics.block;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import cofh.thermal.dynamics.tileentity.DuctTileBase;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
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
    public void playerWillDestroy(World world, BlockPos pos, BlockState state, PlayerEntity player) {

        if (world.isClientSide()) {
            return;
        }
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

        if (world.isClientSide()) {
            return;
        }
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHostInternal) {
            IGridHostInternal host = (IGridHostInternal) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(world);
            gridContainer.ifPresent(e -> e.onGridHostPlaced(host));
        }
    }

    @Override
    public void neighborChanged(BlockState state, World world, BlockPos pos, Block block, BlockPos adj, boolean isMoving) {

        if (world.isClientSide()) {
            return;
        }
        TileEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHostInternal) {
            IGridHostInternal host = (IGridHostInternal) tile;
            Optional<IGridContainer> gridContainer = IGridContainer.getCapability(world);
            gridContainer.ifPresent(e -> e.onGridHostNeighborChanged(host));
        }
    }

    @Override
    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {

        if (worldIn.isClientSide()) {
            TileEntity tile = worldIn.getBlockEntity(currentPos);
            if (tile instanceof DuctTileBase) {
                tile.requestModelDataUpdate();
            }
        }
        return super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

}
