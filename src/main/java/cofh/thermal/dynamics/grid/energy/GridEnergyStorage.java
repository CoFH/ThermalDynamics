package cofh.thermal.dynamics.grid.energy;

import cofh.lib.capability.IRedstoneFluxStorage;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;

import static cofh.lib.util.constants.NBTTags.*;

public final class GridEnergyStorage implements IRedstoneFluxStorage, INBTSerializable<CompoundNBT> {

    private long capacity;
    private long energy;

    //    private long[] samples = new long[100];
    //    private byte sampleTracker = 0;
    //    private long rollingTotal = 0;
    //    private long rollingAverage = 0;

    public GridEnergyStorage(long capacity) {

        this.capacity = capacity;
    }

    public GridEnergyStorage setCapacity(long capacity) {

        this.capacity = Math.max(0, capacity);
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
        return this;
    }

    public GridEnergyStorage setEnergy(long energy) {

        this.energy = Math.max(0, energy);
        if (this.energy > this.capacity) {
            this.energy = this.capacity;
        }
        return this;
    }

    public long getCapacity() {

        return capacity;
    }

    public long getEnergy() {

        return energy;
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
        //
        //        System.out.println(rollingAverage);
    }

    // region NBT
    public GridEnergyStorage read(CompoundNBT nbt) {

        this.capacity = nbt.getLong(TAG_CAPACITY);
        this.energy = nbt.getLong(TAG_ENERGY);
        return this;
    }

    public CompoundNBT write(CompoundNBT nbt) {

        if (this.capacity <= 0) {
            return nbt;
        }
        nbt.putLong(TAG_ENERGY, energy);
        nbt.putLong(TAG_ENERGY_MAX, capacity);
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

        // samples[sampleTracker] += maxReceive;
        long energyReceived = Math.min(capacity - energy, maxReceive);
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
