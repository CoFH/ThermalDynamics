package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.fluid.IFluidGrid;
import cofh.thermal.dynamics.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import net.minecraftforge.registries.RegistryObject;

import static cofh.thermal.dynamics.ThermalDynamics.GRIDS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENERGY_GRID;
import static cofh.thermal.dynamics.init.TDynIDs.ID_FLUID_GRID;

public class TDynGrids {

    private TDynGrids() {

    }

    public static void register() {

    }

    public static final RegistryObject<IGridType<IEnergyGrid>> GRID_ENERGY = GRIDS.register(ID_ENERGY_GRID, () -> IGridType.of(IEnergyGrid.class, EnergyGrid::new));
    public static final RegistryObject<IGridType<IFluidGrid>> GRID_FLUID = GRIDS.register(ID_FLUID_GRID, () -> IGridType.of(IFluidGrid.class, FluidGrid::new));

}
