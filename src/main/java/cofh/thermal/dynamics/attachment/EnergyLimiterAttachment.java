package cofh.thermal.dynamics.attachment;

import cofh.lib.api.IConveyableData;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.inventory.container.attachment.EnergyLimiterAttachmentContainer;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.IntSupplier;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC;
import static cofh.thermal.dynamics.client.TDynTextures.ENERGY_LIMITER_ATTACHMENT_LOC;
import static cofh.thermal.dynamics.init.TDynIDs.ENERGY_LIMITER;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENERGY_LIMITER_ATTACHMENT;

public class EnergyLimiterAttachment implements IAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public static final IAttachmentFactory<IAttachment> FACTORY = (nbt, duct, side) -> new EnergyLimiterAttachment(duct, side).read(nbt);

    public static final Component DISPLAY_NAME = new TranslatableComponent("attachment.thermal.energy_limiter");

    public final int MAX_TRANSFER = 64000;

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    public int amountInput = MAX_TRANSFER / 2;
    public int amountOutput = MAX_TRANSFER / 2;

    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);
    protected LazyOptional<IEnergyStorage> gridCap = LazyOptional.empty();
    protected LazyOptional<IEnergyStorage> externalCap = LazyOptional.empty();

    public EnergyLimiterAttachment(IDuct<?, ?> duct, Direction side) {

        this.duct = duct;
        this.side = side;
    }

    public int getMaxTransfer() {

        return MAX_TRANSFER;
    }

    @Override
    public IDuct<?, ?> duct() {

        return duct;
    }

    @Override
    public Direction side() {

        return side;
    }

    @Override
    public IAttachment read(CompoundTag nbt) {

        if (nbt.isEmpty()) {
            return this;
        }
        rsControl.read(nbt);

        amountInput = nbt.getInt(TAG_AMOUNT_IN);
        amountOutput = nbt.getInt(TAG_AMOUNT_OUT);

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        nbt.putString(TAG_TYPE, ENERGY_LIMITER);

        rsControl.write(nbt);

        nbt.putInt(TAG_AMOUNT_IN, amountInput);
        nbt.putInt(TAG_AMOUNT_OUT, amountOutput);

        return nbt;
    }

    @Override
    public ItemStack getItem() {

        return new ItemStack(ITEMS.get(ID_ENERGY_LIMITER_ATTACHMENT));
    }

    @Override
    public ResourceLocation getTexture() {

        return rsControl.getState() ? ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC : ENERGY_LIMITER_ATTACHMENT_LOC;
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new EnergyLimiterAttachmentContainer(i, player.getLevel(), pos(), side, inventory, player);
    }

    @Override
    public <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridLazOpt) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {
            if (gridCap.isPresent()) {
                return gridCap.cast();
            }
            Optional<T> gridOpt = gridLazOpt.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IEnergyStorage storage) {
                gridCap = LazyOptional.of(() -> new WrappedEnergyStorage(storage, () -> rsControl.getState() ? amountInput : 0, () -> rsControl.getState() ? amountOutput : 0));
                gridLazOpt.addListener(e -> gridCap.invalidate());
                return gridCap.cast();
            }
        }
        return gridLazOpt;
    }

    @Override
    public <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> extLazOpt) {

        if (cap == ThermalEnergyHelper.getBaseEnergySystem()) {
            if (externalCap.isPresent()) {
                return externalCap.cast();
            }
            Optional<T> extOpt = extLazOpt.resolve();
            if (extOpt.isPresent() && extOpt.get() instanceof IEnergyStorage storage) {
                externalCap = LazyOptional.of(() -> new WrappedEnergyStorage(storage, () -> rsControl.getState() ? amountOutput : 0, () -> rsControl.getState() ? amountInput : 0));
                extLazOpt.addListener(e -> externalCap.invalidate());
                return externalCap.cast();
            }
        }
        return extLazOpt;
    }

    // region IPacketHandlerAttachment
    @Override
    public FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        buffer.writeInt(amountInput);
        buffer.writeInt(amountOutput);

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        amountInput = MathHelper.clamp(buffer.readInt(), 0, getMaxTransfer());
        amountOutput = MathHelper.clamp(buffer.readInt(), 0, getMaxTransfer());
    }

    @Override
    public FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {

        rsControl.writeToBuffer(buffer);

        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

        rsControl.readFromBuffer(buffer);
    }
    // endregion

    // region IRedstoneControllableAttachment
    @Override
    public RedstoneControlLogic redstoneControl() {

        return rsControl;
    }
    // endregion

    // region IConveyableData
    @Override
    public void readConveyableData(Player player, CompoundTag tag) {

        rsControl.readSettings(tag);
    }

    @Override
    public void writeConveyableData(Player player, CompoundTag tag) {

        rsControl.writeSettings(tag);
    }
    // endregion

    // region WRAPPER CLASS
    private static class WrappedEnergyStorage implements IEnergyStorage {

        protected IEnergyStorage wrappedStorage;

        protected IntSupplier curReceive;
        protected IntSupplier curExtract;

        public WrappedEnergyStorage(IEnergyStorage wrappedStorage, IntSupplier curReceive, IntSupplier curExtract) {

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
