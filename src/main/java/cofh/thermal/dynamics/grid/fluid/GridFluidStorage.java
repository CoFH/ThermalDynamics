package cofh.thermal.dynamics.grid.fluid;

import cofh.lib.util.helpers.MathHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import javax.annotation.Nonnull;

import static cofh.lib.util.Constants.MAX_CAPACITY;
import static cofh.lib.util.constants.NBTTags.TAG_CAPACITY;
import static cofh.lib.util.constants.NBTTags.TAG_TRACK_OUT;

public final class GridFluidStorage implements IFluidHandler, INBTSerializable<CompoundTag> {

    private int baseCapacity;
    private int capacity;

    private FluidStack fluid = FluidStack.EMPTY;

    private byte sampleTracker = 0;

    //    private final int[] samplesIn = new int[40];
    //    private int rollingIn = 0;
    //    private int averageIn = 0;

    private final int[] samplesOut = new int[40];
    private int rollingOut = 0;
    private int averageOut = 0;

    public GridFluidStorage(int baseCapacity) {

        this.baseCapacity = baseCapacity;
    }

    public GridFluidStorage setBaseCapacity(int baseCapacity) {

        this.baseCapacity = MathHelper.clamp(baseCapacity, 0, MAX_CAPACITY);
        //        if (!this.fluid.isEmpty()) {
        //            this.fluid.setAmount(MathHelper.clamp(this.fluid.getAmount(), 0, baseCapacity));
        //        }
        return this;
    }

    public GridFluidStorage setCapacity(int capacity) {

        this.capacity = capacity;
        resetTrackers();
        return this;
    }

    public GridFluidStorage setFluid(FluidStack fluid) {

        this.fluid = fluid.copy();
        //        if (!this.fluid.isEmpty()) {
        //            this.fluid.setAmount(MathHelper.clamp(this.fluid.getAmount(), 0, baseCapacity));
        //        }
        return this;
    }

    public void resetTrackers() {

        sampleTracker = 0;

        //        rollingIn = 0;
        //        averageIn = 0;

        rollingOut = 0;
        averageOut = 0;
    }

    public int getBaseCapacity() {

        return baseCapacity;
    }

    public FluidStack getFluid() {

        return fluid;
    }

    public void tick() {

        samplesOut[sampleTracker] = fluid.getAmount();
    }

    public void postTick() {

        //        rollingIn += samplesIn[sampleTracker];
        //        averageIn = rollingIn / samplesIn.length;

        samplesOut[sampleTracker] -= fluid.getAmount();
        rollingOut += samplesOut[sampleTracker];
        averageOut = rollingOut / samplesOut.length;

        ++sampleTracker;
        if (sampleTracker >= samplesOut.length) {
            sampleTracker = 0;
            updateCapacity();

            //            System.out.println("Average attempted input (2 seconds): " + averageIn);
            //            System.out.println("Average realized output (2 seconds): " + averageOut);
            //            System.out.println("Dynamic capacity:" + capacity);
            //            System.out.println("Fluid stored:" + fluid.getAmount());
        }
        //        rollingIn -= samplesIn[sampleTracker];
        //        samplesIn[sampleTracker] = 0;
        rollingOut -= samplesOut[sampleTracker];
        samplesOut[sampleTracker] = 0;
    }

    private void updateCapacity() {

        this.capacity = Math.max(baseCapacity, 4 * averageOut);
    }

    // region NBT
    public GridFluidStorage read(CompoundTag nbt) {

        setFluid(FluidStack.loadFluidStackFromNBT(nbt));
        this.baseCapacity = nbt.getInt(TAG_CAPACITY);

        //        this.averageIn = nbt.getInt(TAG_TRACK_IN);
        this.averageOut = nbt.getInt(TAG_TRACK_OUT);

        updateCapacity();
        return this;
    }

    public CompoundTag write(CompoundTag nbt) {

        fluid.writeToNBT(nbt);
        nbt.putInt(TAG_CAPACITY, baseCapacity);

        //        nbt.putInt(TAG_TRACK_IN, averageIn);
        nbt.putInt(TAG_TRACK_OUT, averageOut);

        return nbt;
    }

    @Override
    public CompoundTag serializeNBT() {

        return write(new CompoundTag());
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

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
        if (fluid.getAmount() >= capacity) {
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
