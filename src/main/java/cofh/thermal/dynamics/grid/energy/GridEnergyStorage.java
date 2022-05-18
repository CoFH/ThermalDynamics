package cofh.thermal.dynamics.grid.energy;

import cofh.lib.capability.IRedstoneFluxStorage;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

import static cofh.lib.util.constants.NBTTags.*;

public final class GridEnergyStorage implements IRedstoneFluxStorage, INBTSerializable<CompoundNBT> {

    private long baseCapacity;
    private long capacity;
    private long energy;

    private byte sampleTracker = 0;

    //    private final long[] samplesIn = new long[40];
    //    private long rollingIn = 0;
    //    private long averageIn = 0;

    private final long[] samplesOut = new long[40];
    private long rollingOut = 0;
    private long averageOut = 0;

    public GridEnergyStorage(long baseCapacity) {

        this.baseCapacity = baseCapacity;
    }

    public GridEnergyStorage setBaseCapacity(long baseCapacity) {

        this.baseCapacity = Math.max(0, baseCapacity);
        //        if (this.energy > this.capacity) {
        //            this.energy = this.capacity;
        //        }
        return this;
    }

    public GridEnergyStorage setCapacity(long capacity) {

        this.capacity = capacity;
        resetTrackers();
        return this;
    }

    public GridEnergyStorage setEnergy(long energy) {

        this.energy = Math.max(0, energy);
        //        if (this.energy > this.capacity) {
        //            this.energy = this.capacity;
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

    public long getBaseCapacity() {

        return baseCapacity;
    }

    public long getEnergy() {

        return energy;
    }

    public void tick() {

        samplesOut[sampleTracker] = energy;
    }

    public void postTick() {

        //        rollingIn += samplesIn[sampleTracker];
        //        averageIn = rollingIn / samplesIn.length;

        samplesOut[sampleTracker] -= energy;
        rollingOut += samplesOut[sampleTracker];
        averageOut = rollingOut / samplesOut.length;

        ++sampleTracker;
        if (sampleTracker >= samplesOut.length) {
            sampleTracker = 0;
            updateCapacity();

            //            System.out.println("Average attempted input (2 seconds): " + averageIn);
            //            System.out.println("Average realized output (2 seconds): " + averageOut);
            //            System.out.println("Dynamic capacity:" + capacity);
            //            System.out.println("Energy stored:" + energy);
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
    public GridEnergyStorage read(CompoundNBT nbt) {

        this.energy = nbt.getLong(TAG_ENERGY);
        this.baseCapacity = nbt.getLong(TAG_ENERGY_MAX);

        //        this.averageIn = nbt.getLong(TAG_TRACK_IN);
        this.averageOut = nbt.getLong(TAG_TRACK_OUT);

        updateCapacity();
        return this;
    }

    public CompoundNBT write(CompoundNBT nbt) {

        nbt.putLong(TAG_ENERGY, energy);
        nbt.putLong(TAG_ENERGY_MAX, baseCapacity);

        //        nbt.putLong(TAG_TRACK_IN, averageIn);
        nbt.putLong(TAG_TRACK_OUT, averageOut);

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

    // region IEnergyStorage
    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {

        //        if (!simulate) {
        //            samplesIn[sampleTracker] += maxReceive;
        //        }
        long energyReceived = Math.max(0, Math.min(capacity - energy, maxReceive));
        if (!simulate) {
            energy += energyReceived;
        }
        return (int) energyReceived;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {

        long energyExtracted = Math.min(energy, maxExtract);
        if (!simulate) {
            energy -= energyExtracted;
        }
        return (int) energyExtracted;
    }

    @Override
    public int getEnergyStored() {

        return energy > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) energy;
    }

    @Override
    public int getMaxEnergyStored() {

        return capacity > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    @Override
    public boolean canExtract() {

        return false;
    }

    @Override
    public boolean canReceive() {

        return true;
    }
    // endregion
}
