package cofh.thermal.dynamics.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.FluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.grid.fluid.FluidGrid;
import cofh.thermal.dynamics.grid.fluid.FluidGridNode;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.Optional;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static cofh.lib.util.constants.NBTTags.TAG_MODE;

public class FluidFilterAttachment implements IFilterableAttachment, MenuProvider {

    public static final Component DISPLAY_NAME = new TranslatableComponent("info.thermal.fluid_filter");

    protected IFilter filter = new BaseFluidFilter(FluidFilter.SIZE);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic();

    protected IGridHost<FluidGrid, FluidGridNode> host;
    protected Direction side;

    protected boolean allowReverseFlow = true;

    protected LazyOptional<IFluidHandler> gridCap = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> tileCap = LazyOptional.empty();

    public FluidFilterAttachment(IGridHost<FluidGrid, FluidGridNode> host, Direction side) {

        this.host = host;
        this.side = side;
    }

    @Override
    public IAttachment read(CompoundTag nbt) {

        filter.read(nbt);
        rsControl.read(nbt);

        allowReverseFlow = nbt.getBoolean(TAG_MODE);

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        filter.write(nbt);
        rsControl.write(nbt);

        nbt.putBoolean(TAG_MODE, allowReverseFlow);

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

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            Optional<T> gridOpt = gridCap.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IFluidHandler) {
                if (!this.gridCap.isPresent()) {
                    this.gridCap = LazyOptional.of(() -> new InternalWrapper((IFluidHandler) gridOpt.get(), e -> !rsControl.getState() || filter.valid(e), () -> allowReverseFlow));
                    gridCap.addListener(e -> this.gridCap.invalidate());
                }
                return this.gridCap.cast();
            }
        }
        return gridCap;
    }

    @Override
    public <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> tileCap) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            Optional<T> tileOpt = tileCap.resolve();
            if (tileOpt.isPresent() && tileOpt.get() instanceof IFluidHandler) {
                if (!this.tileCap.isPresent()) {
                    this.tileCap = LazyOptional.of(() -> new InternalWrapper((IFluidHandler) tileOpt.get(), e -> !rsControl.getState() || filter.valid(e), () -> allowReverseFlow));
                    tileCap.addListener(e -> this.tileCap.invalidate());
                }
                return this.tileCap.cast();
            }
        }
        return tileCap;
    }

    // region IFilterableAttachment
    @Override
    public IFilter getFilter() {

        return filter;
    }
    // endregion

    // region WRAPPER CLASS
    private static class InternalWrapper implements IFluidHandler {

        protected IFluidHandler wrappedHandler;

        protected Predicate<FluidStack> validator;
        protected BooleanSupplier allowReverseFlow;

        public InternalWrapper(IFluidHandler wrappedHandler, Predicate<FluidStack> validator, BooleanSupplier allowReverseFlow) {

            this.wrappedHandler = wrappedHandler;
            this.validator = validator;
            this.allowReverseFlow = allowReverseFlow;
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

            return allowReverseFlow.getAsBoolean() ? wrappedHandler.drain(resource, action) : FluidStack.EMPTY;
        }

        @NotNull
        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {

            return allowReverseFlow.getAsBoolean() ? wrappedHandler.drain(maxDrain, action) : FluidStack.EMPTY;
        }

    }
    // endregion
}
