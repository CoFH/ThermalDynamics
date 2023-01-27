package cofh.thermal.dynamics.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.FluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.inventory.container.attachment.FluidServoAttachmentContainer;
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
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.FLUID_SERVO_ATTACHMENT_ACTIVE_LOC;
import static cofh.thermal.dynamics.client.TDynTextures.FLUID_SERVO_ATTACHMENT_LOC;
import static cofh.thermal.dynamics.init.TDynIDs.ID_SERVO_ATTACHMENT;
import static cofh.thermal.dynamics.init.TDynIDs.SERVO;

public class FluidServoAttachment implements IFilterableAttachment, IRedstoneControllableAttachment, MenuProvider {

    public static final Component DISPLAY_NAME = new TranslatableComponent("attachment.thermal.fluid_servo");
    protected final IDuct<?, ?> duct;
    protected final Direction side;
    protected BaseFluidFilter filter = new BaseFluidFilter(FluidFilter.SIZE);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic(this);
    protected LazyOptional<IFluidHandler> gridCap = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> externalCap = LazyOptional.empty();

    public FluidServoAttachment(IDuct<?, ?> duct, Direction side) {

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

        nbt.putString(TAG_TYPE, SERVO);

        filter.write(nbt);
        rsControl.write(nbt);

        return nbt;
    }

    @Override
    public void tick() {

    }

    @Override
    public ItemStack getItem() {

        return new ItemStack(ITEMS.get(ID_SERVO_ATTACHMENT));
    }

    @Override
    public ResourceLocation getTexture() {

        return rsControl.getState() ? FLUID_SERVO_ATTACHMENT_ACTIVE_LOC : FLUID_SERVO_ATTACHMENT_LOC;
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new FluidServoAttachmentContainer(i, player.getLevel(), pos(), side, inventory, player);
    }

    @Override
    public <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridLazOpt) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            if (gridCap.isPresent()) {
                return gridCap.cast();
            }
            Optional<T> gridOpt = gridLazOpt.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IFluidHandler handler) {
                gridCap = LazyOptional.of(() -> new WrappedGridFluidHandler(handler));
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
                externalCap = LazyOptional.of(() -> new WrappedExternalFluidHandler(handler, e -> rsControl.getState() && filter.valid(e)));
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

        return buffer;
    }

    @Override
    public void handleControlPacket(FriendlyByteBuf buffer) {

        rsControl.readFromBuffer(buffer);
    }

    @Override
    public boolean hasGuiPacket() {

        return false;
    }
    // endregion

    // region IRedstoneControllableAttachment
    @Override
    public RedstoneControlLogic redstoneControl() {

        return rsControl;
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

            return validator.test(resource) ? wrappedHandler.fill(resource, action) : 0;
        }

        @NotNull
        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {

            return wrappedHandler.drain(resource, action);
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            return wrappedHandler.drain(maxDrain, action);
        }

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
}
