package cofh.thermal.dynamics.block.entity;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static cofh.thermal.dynamics.api.grid.IGridHost.ConnectionType.DISABLED;
import static cofh.thermal.dynamics.init.TDynGrids.GRID_FLUID;
import static cofh.thermal.dynamics.init.TDynTileEntities.DUCT_FLUID_TILE;

public class FluidDuctTile extends DuctTileBase {

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
        if (tile == null || GridHelper.getGridHost(tile).isPresent()) {
            return false;
        }
        return tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite()).isPresent();
    }

    // region IGridHost
    @Override
    public Set<IGridType<?>> getExposedTypes() {

        return Collections.singleton(GRID_FLUID.get());
    }

    //    @Override
    //    public boolean canConnectTo(IGridHost other, Direction dir) {
    //
    //        if (getGrid() instanceof FluidGrid myGrid && other.getGrid() instanceof FluidGrid otherGrid) {
    //            FluidStack myFluid = myGrid.getFluid();
    //            FluidStack otherFluid = otherGrid.getFluid();
    //            if (!FluidHelper.fluidsEqual(myFluid, otherFluid)) {
    //                return false;
    //            }
    //        }
    //        return connections[dir.ordinal()].allowDuctConnection() && getExposedTypes().equals(other.getExposedTypes());
    //    }
    // endregion

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        return level.isClientSide || side != null && connections[side.ordinal()] == DISABLED ? LazyOptional.empty() : getGrid().getCapability(cap);
    }

}
