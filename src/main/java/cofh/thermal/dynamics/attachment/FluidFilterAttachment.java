package cofh.thermal.dynamics.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.lib.api.IConveyableData;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.inventory.container.attachment.FluidFilterAttachmentContainer;
import cofh.thermal.dynamics.network.packet.server.AttachmentConfigPacket;
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
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.TAG_MODE;
import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.*;
import static cofh.thermal.dynamics.init.TDynIDs.FILTER;
import static cofh.thermal.dynamics.init.TDynIDs.ID_FILTER_ATTACHMENT;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class FluidFilterAttachment implements IFilterableAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public enum FilterMode {
        BIDIRECTIONAL, TO_EXTERNAL_ONLY, TO_GRID_ONLY;

        public static final FilterMode[] VALUES = values();
    }

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.filter");

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    protected FilterMode mode = FilterMode.BIDIRECTIONAL;

    protected BaseFluidFilter filter = new BaseFluidFilter(15);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);

    protected LazyOptional<IFluidHandler> gridCap = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> externalCap = LazyOptional.empty();

    public FluidFilterAttachment(IDuct<?, ?> duct, Direction side) {

        this.duct = duct;
        this.side = side;
    }

    public FilterMode getFilterMode() {

        return mode;
    }

    public void setFilterMode(FilterMode mode) {

        this.mode = mode;
        AttachmentConfigPacket.sendToServer(this);
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
        mode = FilterMode.VALUES[nbt.getByte(TAG_MODE)];

        filter.read(nbt);
        rsControl.read(nbt);

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        nbt.putString(TAG_TYPE, FILTER);
        nbt.putByte(TAG_MODE, (byte) mode.ordinal());

        filter.write(nbt);
        rsControl.write(nbt);

        return nbt;
    }

    @Override
    public ItemStack getItem() {

        return new ItemStack(ITEMS.get(ID_FILTER_ATTACHMENT));
    }

    @Override
    public ResourceLocation getTexture() {

        switch (mode) {
            case TO_EXTERNAL_ONLY -> {
                return rsControl.getState() ? FILTER_ATTACHMENT_TO_EXTERNAL_ACTIVE_LOC : FILTER_ATTACHMENT_TO_EXTERNAL_LOC;
            }
            case TO_GRID_ONLY -> {
                return rsControl.getState() ? FILTER_ATTACHMENT_TO_GRID_ACTIVE_LOC : FILTER_ATTACHMENT_TO_GRID_LOC;
            }
            default -> {
                return rsControl.getState() ? FILTER_ATTACHMENT_ACTIVE_LOC : FILTER_ATTACHMENT_LOC;
            }
        }
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new FluidFilterAttachmentContainer(i, player.getLevel(), pos(), side, inventory, player);
    }

    @Override
    public <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridLazOpt) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (gridCap.isPresent()) {
                return gridCap.cast();
            }
            Optional<T> gridOpt = gridLazOpt.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IFluidHandler handler) {
                gridCap = LazyOptional.of(() -> new WrappedGridFluidHandler(handler, e -> rsControl.getState() && filter.valid(e) || !rsControl.getState()));
                gridLazOpt.addListener(e -> gridCap.invalidate());
                return gridCap.cast();
            }
        }
        return gridLazOpt;
    }

    @Override
    public <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> extLazOpt) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (externalCap.isPresent()) {
                return externalCap.cast();
            }
            Optional<T> extOpt = extLazOpt.resolve();
            if (extOpt.isPresent() && extOpt.get() instanceof IFluidHandler handler) {
                externalCap = LazyOptional.of(() -> new WrappedExternalFluidHandler(handler, e -> rsControl.getState() && filter.valid(e) || !rsControl.getState()));
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

        buffer.writeByte(mode.ordinal());

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        FilterMode prevMode = mode;
        mode = FilterMode.VALUES[buffer.readByte()];

        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());

        if (mode != prevMode) {
            onControlUpdate();
        }
    }

    @Override
    public FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {

        buffer.writeByte(mode.ordinal());
        rsControl.writeToBuffer(buffer);

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

        mode = FilterMode.VALUES[buffer.readByte()];
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

        mode = FilterMode.VALUES[tag.getByte("FilterAttachmentMode")];
        rsControl.readSettings(tag);
        filter.read(tag);

        onControlUpdate();
    }

    @Override
    public void writeConveyableData(Player player, CompoundTag tag) {

        tag.putByte("FilterAttachmentMode", (byte) mode.ordinal());
        rsControl.writeSettings(tag);
        filter.write(tag);
    }
    // endregion

    // region GRID WRAPPER CLASS
    private class WrappedGridFluidHandler implements IFluidHandler {

        protected IFluidHandler wrappedHandler;

        protected Predicate<FluidStack> validator;

        public WrappedGridFluidHandler(IFluidHandler wrappedHandler, Predicate<FluidStack> validator) {

            this.wrappedHandler = wrappedHandler;
            this.validator = validator;
        }

        @Override
        public int getTanks() {

            return wrappedHandler.getTanks();
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {

            return wrappedHandler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {

            return wrappedHandler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return false;
            }
            return validator.test(stack) && wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return 0;
            }
            return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(resource) ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(wrappedHandler.drain(maxDrain, SIMULATE)) ? wrappedHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }

    }
    // endregion

    // region EXTERNAL WRAPPER CLASS
    private class WrappedExternalFluidHandler implements IFluidHandler {

        protected IFluidHandler wrappedHandler;

        protected Predicate<FluidStack> validator;

        public WrappedExternalFluidHandler(IFluidHandler wrappedHandler, Predicate<FluidStack> validator) {

            this.wrappedHandler = wrappedHandler;
            this.validator = validator;
        }

        @Override
        public int getTanks() {

            return wrappedHandler.getTanks();
        }

        @NotNull
        @Override
        public FluidStack getFluidInTank(int tank) {

            return wrappedHandler.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank) {

            return wrappedHandler.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @NotNull FluidStack stack) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return false;
            }
            return validator.test(stack) && wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_GRID_ONLY) {
                return 0;
            }
            return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(resource) ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            if (mode == FilterMode.TO_EXTERNAL_ONLY) {
                return FluidStack.EMPTY;
            }
            return validator.test(wrappedHandler.drain(maxDrain, SIMULATE)) ? wrappedHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }

    }
    // endregion
}
