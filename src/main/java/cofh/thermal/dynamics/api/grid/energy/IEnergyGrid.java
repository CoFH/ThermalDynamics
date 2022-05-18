package cofh.thermal.dynamics.api.grid.energy;

import cofh.lib.capability.IRedstoneFluxStorage;
import cofh.thermal.dynamics.api.grid.IGrid;

public interface IEnergyGrid extends IGrid<IEnergyGrid, IEnergyGridNode>, IRedstoneFluxStorage {

    long getCapacity();

    long getEnergy();

    void setBaseCapacity(long baseCapacity);

    void setCapacity(long capacity);

    void setEnergy(long energy);

}
