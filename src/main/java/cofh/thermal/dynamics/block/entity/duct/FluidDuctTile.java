package cofh.thermal.dynamics.block.entity.duct;

import cofh.core.util.helpers.FluidHelper;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import cofh.thermal.dynamics.grid.fluid.FluidGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import static cofh.thermal.dynamics.init.TDynGrids.GRID_FLUID;
import static cofh.thermal.dynamics.init.TDynTileEntities.DUCT_FLUID_TILE;

public class FluidDuctTile extends DuctTileBase<FluidGrid, FluidGridNode> {

    public FluidDuctTile(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);
    }

    public FluidDuctTile(BlockPos pos, BlockState state) {

        super(DUCT_FLUID_TILE.get(), pos, state);
    }

    @Override
    protected boolean canConnectToBlock(Direction dir) {

        if (!connections[dir.ordinal()].allowBlockConnection()) {
            return false;
        }
        BlockEntity tile = level.getBlockEntity(getBlockPos().relative(dir));
        if (tile == null || GridHelper.getGridHost(tile) != null) {
            return false;
        }
        return tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite()).isPresent();
    }

    // region IGridHost
    @Override
    public IGridType<FluidGrid> getGridType() {

        return GRID_FLUID.get();
    }

    @Override
    public boolean canConnectTo(IGridHost<?, ?> other, Direction dir) {

        if (!getLevel().isClientSide && other.getGrid() instanceof FluidGrid otherGrid) {
            FluidStack myFluid = getGrid().getFluid();
            FluidStack otherFluid = otherGrid.getFluid();
            if (!myFluid.isEmpty() && !otherFluid.isEmpty() && !FluidHelper.fluidsEqual(myFluid, otherFluid)) {
                return false;
            }
        }
        return super.canConnectTo(other, dir);
    }
    // endregion
}