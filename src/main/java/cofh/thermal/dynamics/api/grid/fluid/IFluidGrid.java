package cofh.thermal.dynamics.api.grid.fluid;

import cofh.thermal.dynamics.api.grid.IGrid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/**
 * @author King Lemming
 */
public interface IFluidGrid extends IGrid<IFluidGrid, IFluidGridNode>, IFluidHandler {

    int getCapacity();

    FluidStack getFluid();

    void setCapacity(int capacity);

    void setFluid(FluidStack fluid);

}
