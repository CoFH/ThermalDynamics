package cofh.thermal.dynamics.tileentity;

import cofh.thermal.dynamics.api.grid.GridType;
import cofh.thermal.dynamics.init.TDynReferences;

import java.util.Collections;
import java.util.Set;

public class DuctTileEnergy extends DuctTileBase {

    @Override
    public Set<GridType<?>> getExposedTypes() {

        return Collections.singleton(TDynReferences.ENERGY_GRID);
    }

}
