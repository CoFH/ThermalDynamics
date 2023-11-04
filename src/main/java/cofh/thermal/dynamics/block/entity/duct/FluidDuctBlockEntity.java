package cofh.thermal.dynamics.block.entity.duct;

import cofh.core.util.helpers.FluidHelper;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import cofh.thermal.dynamics.grid.fluid.FluidGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;

import static cofh.thermal.dynamics.init.TDynBlockEntities.FLUID_DUCT_BLOCK_ENTITY;
import static cofh.thermal.dynamics.init.TDynGrids.FLUID_GRID;

public class FluidDuctBlockEntity extends DuctBlockEntity<FluidGrid, FluidGridNode> {

    public FluidDuctBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);
    }

    public FluidDuctBlockEntity(BlockPos pos, BlockState state) {

        super(FLUID_DUCT_BLOCK_ENTITY.get(), pos, state);
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
        return tile.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).isPresent();
    }

    // region IGridHost
    @Override
    public IGridType<FluidGrid> getGridType() {

        return FLUID_GRID.get();
    }

    @Override
    public boolean canConnectTo(IDuct<?, ?> other, Direction dir) {

        if (!level.isClientSide && other.getGrid() instanceof FluidGrid otherGrid) {
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
