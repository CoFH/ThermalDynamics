package cofh.thermal.dynamics.common.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.lib.api.IConveyableData;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.inventory.attachment.FluidTurboServoAttachmentMenu;
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
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import static cofh.lib.util.Constants.BUCKET_VOLUME;
import static cofh.lib.util.Constants.TANK_MEDIUM;
import static cofh.lib.util.constants.NBTTags.TAG_AMOUNT;
import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.TURBO_SERVO_ATTACHMENT_ACTIVE_LOC;
import static cofh.thermal.dynamics.client.TDynTextures.TURBO_SERVO_ATTACHMENT_LOC;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_TURBO_SERVO_ATTACHMENT;
import static cofh.thermal.dynamics.init.registries.TDynIDs.TURBO_SERVO;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE;

public class FluidTurboServoAttachment implements IFilterableAttachment, IRedstoneControllableAttachment, IConveyableData, MenuProvider {

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.turbo_servo");

    public static final int MAX_TRANSFER = TANK_MEDIUM;

    protected final IDuct<?, ?> duct;
    protected final Direction side;

    public int amountTransfer = BUCKET_VOLUME;

    protected BaseFluidFilter filter = new BaseFluidFilter(1);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);

    protected IFluidHandler internalGridCap = null;
    protected IFluidHandler gridCap = null;
    protected IFluidHandler extCap = null;

    public FluidTurboServoAttachment(IDuct<?, ?> duct, Direction side) {

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

        internalGridCap = null;
        gridCap = null;
        extCap = null;
    }

    @Override
    public IAttachment read(CompoundTag nbt) {

        if (nbt.isEmpty()) {
            return this;
        }
        amountTransfer = nbt.getInt(TAG_AMOUNT);

        filter.read(nbt);
        rsControl.read(nbt);

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        nbt.putString(TAG_TYPE, TURBO_SERVO);
        nbt.putInt(TAG_AMOUNT, amountTransfer);

        filter.write(nbt);
        rsControl.write(nbt);

        return nbt;
    }

    @Override
    public void tick() {

        if (!rsControl.getState()) {
            return;
        }
        if (internalGridCap == null) {
            internalGridCap = duct.getGrid().getCapability(Capabilities.FluidHandler.BLOCK);
        }
        if (extCap != null && internalGridCap != null) {
            internalGridCap.fill(extCap.drain(internalGridCap.fill(extCap.drain(amountTransfer, SIMULATE), SIMULATE), EXECUTE), EXECUTE);
        }
    }

    @Override
    public ItemStack getItem() {

        return new ItemStack(ITEMS.get(ID_TURBO_SERVO_ATTACHMENT));
    }

    @Override
    public ResourceLocation getTexture() {

        return rsControl.getState() ? TURBO_SERVO_ATTACHMENT_ACTIVE_LOC : TURBO_SERVO_ATTACHMENT_LOC;
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new FluidTurboServoAttachmentMenu(i, player.level, pos(), side, inventory, player);
    }

    @Nullable
    @Override
    public <T, C> T wrapGridCapability(BlockCapability<T, C> capability, T gridCapIn) {

        if (capability == Capabilities.FluidHandler.BLOCK) {
            if (gridCap != null) {
                return (T) gridCap;
            }
            if (gridCapIn instanceof IFluidHandler handler) {
                gridCap = new WrappedGridFluidHandler(handler);
                return (T) gridCap;
            }
        }
        return gridCapIn;
    }

    @Nullable
    @Override
    public <T, C> T wrapExternalCapability(BlockCapability<T, C> capability, T extCapIn) {

        if (capability == Capabilities.FluidHandler.BLOCK) {
            if (extCap != null) {
                return (T) extCap;
            }
            if (extCapIn instanceof IFluidHandler handler) {
                extCap = new WrappedExternalFluidHandler(handler, e -> rsControl.getState() && filter.valid(e));
                return (T) extCap;
            }
        }
        return extCapIn;
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

        buffer.writeInt(amountTransfer);

        buffer.writeBoolean(filter.getAllowList());
        buffer.writeBoolean(filter.getCheckNBT());

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        amountTransfer = MathHelper.clamp(buffer.readInt(), 0, MAX_TRANSFER);

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
    private static class WrappedGridFluidHandler implements IFluidHandler {

        protected IFluidHandler wrappedHandler;

        public WrappedGridFluidHandler(IFluidHandler wrappedHandler) {

            this.wrappedHandler = wrappedHandler;
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

            return wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            return 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            return FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            return FluidStack.EMPTY;
        }

    }
    // endregion

    // region EXTERNAL WRAPPER CLASS
    private static class WrappedExternalFluidHandler implements IFluidHandler {

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

            return validator.test(stack) && wrappedHandler.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {

            return 0;
            // return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            return validator.test(resource) ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            return validator.test(wrappedHandler.drain(maxDrain, SIMULATE)) ? wrappedHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }

    }
    // endregion
}
