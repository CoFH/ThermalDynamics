package cofh.thermal.dynamics.attachment;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.FluidFilter;
import cofh.core.util.filter.IFilter;
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
import java.util.function.Predicate;

public class FluidFilterAttachment implements IFilterableAttachment, MenuProvider {

    public static final Component DISPLAY_NAME = new TranslatableComponent("info.thermal.fluid_filter");

    protected IFilter filter = new BaseFluidFilter(FluidFilter.SIZE);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic();

    protected LazyOptional<IFluidHandler> gridCap = LazyOptional.empty();
    protected LazyOptional<IFluidHandler> externalCap = LazyOptional.empty();

    @Override
    public IAttachment read(CompoundTag nbt) {

        filter.read(nbt);
        rsControl.read(nbt);

        return this;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        filter.write(nbt);
        rsControl.write(nbt);

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
    public <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridLazOpt) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            Optional<T> gridOpt = gridLazOpt.resolve();
            if (gridOpt.isPresent() && gridOpt.get() instanceof IFluidHandler) {
                if (!gridCap.isPresent()) {
                    gridCap = LazyOptional.of(() -> new CapabilityWrapper((IFluidHandler) gridOpt.get(), e -> !rsControl.getState() || filter.valid(e)));
                    gridLazOpt.addListener(e -> gridCap.invalidate());
                }
                return gridCap.cast();
            }
        }
        return gridLazOpt;
    }

    @Override
    public <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> extLazOpt) {

        if (cap == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            Optional<T> extOpt = extLazOpt.resolve();
            if (extOpt.isPresent() && extOpt.get() instanceof IFluidHandler) {
                if (!externalCap.isPresent()) {
                    externalCap = LazyOptional.of(() -> new CapabilityWrapper((IFluidHandler) extOpt.get(), e -> !rsControl.getState() || filter.valid(e)));
                    extLazOpt.addListener(e -> externalCap.invalidate());
                }
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

    // region WRAPPER CLASS
    private static class CapabilityWrapper implements IFluidHandler {

        protected IFluidHandler wrappedHandler;

        protected Predicate<FluidStack> validator;

        public CapabilityWrapper(IFluidHandler wrappedHandler, Predicate<FluidStack> validator) {

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
}
