package cofh.thermal.dynamics.api.grid.energy;

import cofh.thermal.dynamics.api.grid.Grid;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * @author covers1624
 */
public interface EnergyGrid extends Grid<EnergyGrid, EnergyGridNode>, IEnergyStorage {
}
