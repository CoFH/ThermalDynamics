package cofh.thermal.dynamics.grid.fluid;

import cofh.lib.util.helpers.MathHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

import static cofh.lib.util.constants.Constants.MAX_CAPACITY;
import static cofh.lib.util.constants.NBTTags.TAG_CAPACITY;

public final class GridFluidStorage implements IFluidHandler, INBTSerializable<CompoundNBT> {

    private int capacity;
    private FluidStack fluid = FluidStack.EMPTY;

    //    private int[] samples = new long[100];
    //    private byte sampleTracker = 0;
    //    private int rollingTotal = 0;
    //    private int rollingAverage = 0;

    public GridFluidStorage(int capacity) {

        this.capacity = capacity;
    }

    public GridFluidStorage setCapacity(int capacity) {

        this.capacity = MathHelper.clamp(capacity, 0, MAX_CAPACITY);
        if (!this.fluid.isEmpty()) {
            this.fluid.setAmount(MathHelper.clamp(this.fluid.getAmount(), 0, capacity));
        }
        return this;
    }

    public GridFluidStorage setFluid(FluidStack fluid) {

        this.fluid = fluid.copy();
        if (!this.fluid.isEmpty()) {
            this.fluid.setAmount(MathHelper.clamp(this.fluid.getAmount(), 0, capacity));
        }
        return this;
    }

    public int getCapacity() {

        return capacity;
    }

    public FluidStack getFluid() {

        return fluid;
    }

    public void tick() {

        //        rollingTotal += samples[sampleTracker];
        //        rollingAverage = rollingTotal / samples.length;
        //
        //        ++sampleTracker;
        //        if (sampleTracker >= samples.length) {
        //            sampleTracker = 0;
        //        }
        //        rollingTotal -= samples[sampleTracker];
        //        samples[sampleTracker] = 0;
    }

    // region NBT
    public GridFluidStorage read(CompoundNBT nbt) {

        FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
        setFluid(fluid);
        return this;
    }

    public CompoundNBT write(CompoundNBT nbt) {

        fluid.writeToNBT(nbt);
        nbt.putInt(TAG_CAPACITY, capacity);
        return nbt;
    }

    @Override
    public CompoundNBT serializeNBT() {

        return write(new CompoundNBT());
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

        read(nbt);
    }
    // endregion

    @Override
    public int getTanks() {

        return 1;
    }

    @Nonnull
    @Override
    public FluidStack getFluidInTank(int tank) {

        return fluid;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {

        if (resource.isEmpty() || !isFluidValid(0, resource)) {
            return 0;
        }
        if (action.simulate()) {
            if (fluid.isEmpty()) {
                return Math.min(capacity, resource.getAmount());
            }
            if (!fluid.isFluidEqual(resource)) {
                return 0;
            }
            return Math.min(capacity - fluid.getAmount(), resource.getAmount());
        }
        if (fluid.isEmpty()) {
            setFluid(new FluidStack(resource, Math.min(capacity, resource.getAmount())));
            return fluid.getAmount();
        }
        if (!fluid.isFluidEqual(resource)) {
            return 0;
        }
        int filled = capacity - fluid.getAmount();

        if (resource.getAmount() < filled) {
            fluid.grow(resource.getAmount());
            filled = resource.getAmount();
        } else {
            fluid.setAmount(capacity);
        }
        return filled;
    }

    @Nonnull
    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {

        if (resource.isEmpty() || !resource.isFluidEqual(fluid)) {
            return FluidStack.EMPTY;
        }
        return drain(resource.getAmount(), action);
    }

    @Nonnull
    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {

        if (maxDrain <= 0 || fluid.isEmpty()) {
            return FluidStack.EMPTY;
        }
        int drained = maxDrain;
        if (fluid.getAmount() < drained) {
            drained = fluid.getAmount();
        }
        FluidStack stack = new FluidStack(fluid, drained);
        if (action.execute()) {
            fluid.shrink(drained);
            if (fluid.isEmpty()) {
                setFluid(FluidStack.EMPTY);
            }
        }
        return stack;
    }

    @Override
    public int getTankCapacity(int tank) {

        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, @Nonnull FluidStack stack) {

        return true;
    }
    // endregion
}
