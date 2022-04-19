package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.grid.energy.EnergyGrid;

import static cofh.thermal.dynamics.ThermalDynamics.GRIDS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENERGY_GRID;

public class TDynGrids {

    private TDynGrids() {

    }

    public static void register() {

        registerGridsTypes();
    }

    // region HELPERS
    private static void registerGridsTypes() {

        GRIDS.register(ID_ENERGY_GRID, () -> IGridType.of(IEnergyGrid.class, EnergyGrid::new));

        //        GRIDS.register(ID_GRID_FLUID, () -> GridType.of(FluidGrid.class, FluidGridImpl::new));

        //        GRIDS.register(ID_GRID_ITEM, () -> GridType.of(ItemGrid.class, ItemGridImpl::new));

        //        GRIDS.register(ID_GRID_MULTI, () -> GridType.of(MultiGrid.class, MultiGridImpl::new));
    }
    // endregion
}
