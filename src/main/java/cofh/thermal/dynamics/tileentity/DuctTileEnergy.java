package cofh.thermal.dynamics.tileentity;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Set;

public class DuctTileEnergy extends DuctTileBase {

    @Override
    public Set<IGridType<?>> getExposedTypes() {

        return Collections.singleton(TDynReferences.ENERGY_GRID);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        return getGrid().isPresent() ? getGrid().get().getCapability(cap) : LazyOptional.empty();
    }

    @Override
    protected boolean canConnectExternally(Direction dir) {

        TileEntity tile = level.getBlockEntity(getBlockPos().relative(dir));
        if (tile == null || GridHelper.getGridHost(tile).isPresent()) return false;

        return tile.getCapability(CapabilityEnergy.ENERGY, dir.getOpposite()).isPresent();
    }
}
