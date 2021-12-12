package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.api.grid.GridType;
import cofh.thermal.dynamics.api.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.grid.energy.EnergyGridImpl;

import static cofh.thermal.dynamics.ThermalDynamics.GRIDS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_GRID_ENERGY;

public class TDynGrids {

    private TDynGrids() {

    }

    public static void register() {
        registerGridsTypes();
    }

    //region HELPERS
    private static void registerGridsTypes() {

        GRIDS.register(ID_GRID_ENERGY, () -> GridType.of(EnergyGrid.class, EnergyGridImpl::new));

//        GRIDS.register(ID_GRID_FLUID, () -> GridType.of(FluidGrid.class, FluidGridImpl::new));

//        GRIDS.register(ID_GRID_ITEM, () -> GridType.of(ItemGrid.class, ItemGridImpl::new));

//        GRIDS.register(ID_GRID_MULTI, () -> GridType.of(MultiGrid.class, MultiGridImpl::new));
    }
    //endregion
}
