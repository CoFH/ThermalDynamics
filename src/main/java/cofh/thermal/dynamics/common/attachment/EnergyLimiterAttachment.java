package cofh.thermal.dynamics.common.attachment;

import cofh.lib.api.IConveyableData;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.inventory.attachment.EnergyLimiterAttachmentMenu;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import org.jetbrains.annotations.Nullable;

import java.util.function.IntSupplier;

import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC;
import static cofh.thermal.dynamics.client.TDynTextures.ENERGY_LIMITER_ATTACHMENT_LOC;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ENERGY_LIMITER;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_ENERGY_LIMITER_ATTACHMENT;

public class EnergyLimiterAttachment implements IAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public static final IAttachmentFactory<IAttachment> FACTORY = (nbt, duct, side) -> new EnergyLimiterAttachment(duct, side).read(nbt);

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.energy_limiter");

    public final int MAX_TRANSFER = 64000;

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    public int amountInput = MAX_TRANSFER / 2;
    public int amountOutput = MAX_TRANSFER / 2;

    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);

    protected IEnergyStorage gridCap = null;
    protected IEnergyStorage extCap = null;

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
    public void invalidate() {

        gridCap = null;
        extCap = null;
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

        return new EnergyLimiterAttachmentMenu(i, player.level, pos(), side, inventory, player);
    }

    @Nullable
    @Override
    @SuppressWarnings ("unchecked")
    public <T, C> T wrapGridCapability(BlockCapability<T, C> capability, T gridCapIn) {

        if (capability == Capabilities.EnergyStorage.BLOCK) {
            if (gridCap != null) {
                return (T) gridCap;
            }
            if (gridCapIn instanceof IEnergyStorage storage) {
                gridCap = new WrappedEnergyStorage(storage, () -> rsControl.getState() ? amountInput : 0, () -> rsControl.getState() ? amountOutput : 0);
                return (T) gridCap;
            }
        }
        return gridCapIn;
    }

    @Nullable
    @Override
    @SuppressWarnings ("unchecked")
    public <T, C> T wrapExternalCapability(BlockCapability<T, C> capability, T extCapIn) {

        if (capability == Capabilities.EnergyStorage.BLOCK) {
            if (extCap != null) {
                return (T) extCap;
            }
            if (extCapIn instanceof IEnergyStorage storage) {
                extCap = new WrappedEnergyStorage(storage, () -> rsControl.getState() ? amountOutput : 0, () -> rsControl.getState() ? amountInput : 0);
                return (T) extCap;
            }
        }
        return extCapIn;
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

        buffer.writeInt(amountInput);
        buffer.writeInt(amountOutput);

        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

        rsControl.readFromBuffer(buffer);

        amountInput = MathHelper.clamp(buffer.readInt(), 0, getMaxTransfer());
        amountOutput = MathHelper.clamp(buffer.readInt(), 0, getMaxTransfer());
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

        onControlUpdate();
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
