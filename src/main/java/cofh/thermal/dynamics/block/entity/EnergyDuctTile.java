package cofh.thermal.dynamics.block.entity;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_DUCT_TILE;
import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_GRID;

public class EnergyDuctTile extends DuctTileBase {

    public EnergyDuctTile(BlockPos pos, BlockState state) {

        super(ENERGY_DUCT_TILE, pos, state);
    }

    @Override
    public Set<IGridType<?>> getExposedTypes() {

        return Collections.singleton(ENERGY_GRID);
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
        return tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), dir.getOpposite()).isPresent();
    }

}
