package cofh.thermal.dynamics.tileentity;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

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

}
