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
    public Set<IGridType<?>> getExposedTypes() {

        return Collections.singleton(GRID_FLUID.get());
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        return level.isClientSide ? LazyOptional.empty() : getGrid().getCapability(cap);
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
