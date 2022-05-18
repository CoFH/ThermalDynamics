package cofh.thermal.dynamics.api.grid.fluid;

import cofh.thermal.dynamics.api.grid.IGrid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

public interface IFluidGrid extends IGrid<IFluidGrid, IFluidGridNode>, IFluidHandler {

    int getCapacity();

    FluidStack getFluid();

    int getFluidAmount();

    void setBaseCapacity(int baseCapacity);

    void setCapacity(int capacity);

    void setFluid(FluidStack fluid);

}
