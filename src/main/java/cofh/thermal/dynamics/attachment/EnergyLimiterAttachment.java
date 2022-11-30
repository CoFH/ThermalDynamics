package cofh.thermal.dynamics.attachment;

import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nonnull;
import java.util.Optional;

public class EnergyLimiterAttachment implements IAttachment {

    public static final EnergyLimiterAttachment INSTANCE = new EnergyLimiterAttachment();

    @Override
    public IAttachment read(CompoundTag nbt) {

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        return nbt;
    }

    @Override
    public <T> LazyOptional<T> wrapInputCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridCap) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {

        }
        return gridCap;
    }

    @Override
    public <T> LazyOptional<T> wrapOutputCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> tileCap) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {
            Optional<T> tileOpt = tileCap.resolve();
            if (tileOpt.isPresent() && tileOpt.get() instanceof IEnergyStorage) {
                return LazyOptional.of(() -> new LimiterCap((IEnergyStorage) tileOpt.get())).cast();
            }
        }
        return tileCap;
    }

    static class LimiterCap implements IEnergyStorage {

        IEnergyStorage wrappedCap;

        public LimiterCap(IEnergyStorage wrappedCap) {

            this.wrappedCap = wrappedCap;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {

            return wrappedCap.receiveEnergy(1, simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {

            return wrappedCap.extractEnergy(1, simulate);
        }

        @Override
        public int getEnergyStored() {

            return wrappedCap.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {

            return wrappedCap.getEnergyStored();
        }

        @Override
        public boolean canExtract() {

            return wrappedCap.canExtract();
        }

        @Override
        public boolean canReceive() {

            return wrappedCap.canReceive();
        }

    }

}
