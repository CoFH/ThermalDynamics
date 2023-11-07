package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import net.minecraftforge.registries.RegistryObject;

import static cofh.thermal.dynamics.ThermalDynamics.GRIDS;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_ENERGY_GRID;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_FLUID_GRID;

public class TDynGrids {

    private TDynGrids() {

    }

    public static void register() {

    }

    public static final RegistryObject<IGridType<EnergyGrid>> ENERGY_GRID = GRIDS.register(ID_ENERGY_GRID, () -> IGridType.of(EnergyGrid::new));
    public static final RegistryObject<IGridType<FluidGrid>> FLUID_GRID = GRIDS.register(ID_FLUID_GRID, () -> IGridType.of(FluidGrid::new));

}
