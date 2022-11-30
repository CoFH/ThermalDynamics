package cofh.thermal.dynamics.attachment;

import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.IntSupplier;

import static cofh.lib.util.constants.NBTTags.TAG_AMOUNT_IN;
import static cofh.lib.util.constants.NBTTags.TAG_AMOUNT_OUT;

public class EnergyLimiterAttachment implements IAttachment, MenuProvider {

    public static final Component DISPLAY_NAME = new TranslatableComponent("info.thermal.energy_limiter");
    public static final int MAX_INPUT = 64000;
    public static final int MAX_OUTPUT = 64000;

    protected RedstoneControlLogic rsControl = new RedstoneControlLogic();

    protected int amountInput = MAX_INPUT;
    protected int amountOutput = MAX_OUTPUT;

    protected LazyOptional<IEnergyStorage> gridCap = LazyOptional.empty();
    protected LazyOptional<IEnergyStorage> tileCap = LazyOptional.empty();

    @Override
    public IAttachment read(CompoundTag nbt) {

        rsControl.read(nbt);

        amountInput = nbt.getInt(TAG_AMOUNT_IN);
        amountOutput = nbt.getInt(TAG_AMOUNT_OUT);

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        rsControl.write(nbt);

        nbt.putInt(TAG_AMOUNT_IN, amountInput);
        nbt.putInt(TAG_AMOUNT_OUT, amountOutput);

        return nbt;
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return null;
    }

    @Override
    public <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridCap) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {
            Optional<T> gridOpt = gridCap.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IEnergyStorage) {
                if (!this.gridCap.isPresent()) {
                    this.gridCap = LazyOptional.of(() -> new InternalWrapper((IEnergyStorage) gridOpt.get(), () -> rsControl.getState() ? amountInput : 0, () -> rsControl.getState() ? amountOutput : 0));
                    gridCap.addListener(e -> this.gridCap.invalidate());
                }
                return this.gridCap.cast();
            }
        }
        return gridCap;
    }

    @Override
    public <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> tileCap) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {
            Optional<T> tileOpt = tileCap.resolve();
            if (tileOpt.isPresent() && tileOpt.get() instanceof IEnergyStorage) {
                if (!this.tileCap.isPresent()) {
                    this.tileCap = LazyOptional.of(() -> new InternalWrapper((IEnergyStorage) tileOpt.get(), () -> rsControl.getState() ? amountInput : 0, () -> rsControl.getState() ? amountOutput : 0));
                    tileCap.addListener(e -> this.tileCap.invalidate());
                }
                return this.tileCap.cast();
            }
        }
        return tileCap;
    }

    // region WRAPPER CLASS
    private static class InternalWrapper implements IEnergyStorage {

        protected IEnergyStorage wrappedStorage;

        protected IntSupplier curReceive;
        protected IntSupplier curExtract;

        public InternalWrapper(IEnergyStorage wrappedStorage, IntSupplier curReceive, IntSupplier curExtract) {

            this.wrappedStorage = wrappedStorage;
            this.curReceive = curReceive;
            this.curExtract = curExtract;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {

            return wrappedStorage.receiveEnergy(Math.min(maxReceive, curReceive.getAsInt()), simulate);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {

            return wrappedStorage.extractEnergy(Math.min(maxExtract, curExtract.getAsInt()), simulate);
        }

        @Override
        public int getEnergyStored() {

            return wrappedStorage.getEnergyStored();
        }

        @Override
        public int getMaxEnergyStored() {

            return wrappedStorage.getEnergyStored();
        }

        @Override
        public boolean canExtract() {

            return wrappedStorage.canExtract();
        }

        @Override
        public boolean canReceive() {

            return wrappedStorage.canReceive();
        }

    }
    // endregion
}
