package cofh.thermal.dynamics.api.grid.energy;

import cofh.lib.capability.IRedstoneFluxStorage;
import cofh.thermal.dynamics.api.grid.IGrid;

/**
 * @author covers1624
 */
public interface IEnergyGrid extends IGrid<IEnergyGrid, IEnergyGridNode>, IRedstoneFluxStorage {

    long getCapacity();

    long getEnergy();

    void setCapacity(long capacity);

    void setEnergy(long energy);

}
