package cofh.thermal.dynamics.block.entity;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public class FluidDuctTile extends DuctTileBase {

    public FluidDuctTile(BlockEntityType<?> type) {

        super(type);
    }

    public FluidDuctTile() {

        super(TDynReferences.FLUID_DUCT_TILE);
    }

    @Override
    public Set<IGridType<?>> getExposedTypes() {

        return Collections.singleton(TDynReferences.FLUID_GRID);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        IGrid<?, ?> grid = getGrid();
        return grid != null ? grid.getCapability(cap) : LazyOptional.empty();
    }

    @Override
    protected boolean canConnect(Direction dir) {

        BlockEntity tile = level.getBlockEntity(getBlockPos().relative(dir));
        if (tile == null || GridHelper.getGridHost(tile).isPresent()) {
            return false;
        }
        return tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite()).isPresent();
    }

}
