package cofh.thermal.dynamics.common.attachment;

import cofh.core.util.filter.BaseItemFilter;
import cofh.core.util.filter.IFilter;
import cofh.lib.api.IConveyableData;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.inventory.attachment.ItemFilterAttachmentMenu;
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
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.FILTER;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_FILTER_ATTACHMENT;

public class ItemFilterAttachment implements IFilterableAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.item_filter");

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    protected BaseItemFilter filter = new BaseItemFilter(15);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);

    protected LazyOptional<IItemHandler> gridCap = LazyOptional.empty();
    protected LazyOptional<IItemHandler> externalCap = LazyOptional.empty();

    public ItemFilterAttachment(IDuct<?, ?> duct, Direction side) {
        this.duct = duct;
        this.side = side;
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
        gridCap.invalidate();
        externalCap.invalidate();
    }

    @Override
    public IAttachment read(CompoundTag nbt) {
        if (nbt.isEmpty()) {
            return this;
        }
        filter.read(nbt);
        rsControl.read(nbt);
        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        nbt.putString(TAG_TYPE, FILTER);
        filter.write(nbt);
        rsControl.write(nbt);
        return nbt;
    }

    @Override
    public void tick() {
        // Item filters are passive - they don't actively move items
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ITEMS.get(ID_FILTER_ATTACHMENT));
    }

    @Override
    public ResourceLocation getTexture() {
        boolean active = rsControl.getState();
        // Show overflow/blocked texture when redstone has disabled the filter
        if (!active) {
            return FILTER_ATTACHMENT_OVERFLOW_LOC;
        }
        return FILTER_ATTACHMENT_ACTIVE_LOC;
    }

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ItemFilterAttachmentMenu(i, player.level, pos(), side, inventory, player);
    }

    @Override
    public <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridLazOpt) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (gridCap.isPresent()) {
                return gridCap.cast();
            }
            Optional<T> gridOpt = gridLazOpt.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IItemHandler handler) {
                gridCap = LazyOptional.of(() -> new WrappedGridItemHandler(handler));
                gridLazOpt.addListener(e -> gridCap.invalidate());
                return gridCap.cast();
            }
        }
        return gridLazOpt;
    }

    @Override
    public <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> extLazOpt) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            if (externalCap.isPresent()) {
                return externalCap.cast();
            }
            Optional<T> extOpt = extLazOpt.resolve();
            if (extOpt.isPresent() && extOpt.get() instanceof IItemHandler handler) {
                externalCap = LazyOptional.of(() -> new WrappedExternalItemHandler(handler, e -> rsControl.getState() && filter.valid(e)));
                extLazOpt.addListener(e -> externalCap.invalidate());
                return externalCap.cast();
            }
        }
        return extLazOpt;
    }

    // region IFilterableAttachment
    @Override
    public IFilter getFilter() {
        return filter;
    }
    // endregion

    // region IPacketHandlerAttachment
    @Override
    public FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {
        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());
        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {
        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());
    }

    @Override
    public FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {
        rsControl.writeToBuffer(buffer);
        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());
        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {
        rsControl.readFromBuffer(buffer);
        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());
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
        filter.read(tag);
        onControlUpdate();
    }

    @Override
    public void writeConveyableData(Player player, CompoundTag tag) {
        rsControl.writeSettings(tag);
        filter.write(tag);
    }
    // endregion

    // region GRID WRAPPER CLASS
    private static class WrappedGridItemHandler implements IItemHandler {
        protected IItemHandler wrappedHandler;

        public WrappedGridItemHandler(IItemHandler wrappedHandler) {
            this.wrappedHandler = wrappedHandler;
        }

        @Override
        public int getSlots() {
            return wrappedHandler.getSlots();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return wrappedHandler.getStackInSlot(slot);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return wrappedHandler.insertItem(slot, stack, simulate);
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return wrappedHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return wrappedHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return wrappedHandler.isItemValid(slot, stack);
        }
    }
    // endregion

    // region EXTERNAL WRAPPER CLASS
    private static class WrappedExternalItemHandler implements IItemHandler {
        protected IItemHandler wrappedHandler;
        protected Predicate<ItemStack> validator;

        public WrappedExternalItemHandler(IItemHandler wrappedHandler, Predicate<ItemStack> validator) {
            this.wrappedHandler = wrappedHandler;
            this.validator = validator;
        }

        @Override
        public int getSlots() {
            return wrappedHandler.getSlots();
        }

        @NotNull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return wrappedHandler.getStackInSlot(slot);
        }

        @Override
        public int getSlotLimit(int slot) {
            return wrappedHandler.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return validator.test(stack) && wrappedHandler.isItemValid(slot, stack);
        }

        @NotNull
        @Override
        public ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            return validator.test(stack) ? wrappedHandler.insertItem(slot, stack, simulate) : stack;
        }

        @NotNull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            ItemStack extracted = wrappedHandler.extractItem(slot, amount, true);
            return validator.test(extracted) ? wrappedHandler.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    }
    // endregion
}