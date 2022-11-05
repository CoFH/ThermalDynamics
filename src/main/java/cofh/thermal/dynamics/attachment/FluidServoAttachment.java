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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class FluidServoAttachment implements IFilterableAttachment, IRedstoneControllableAttachment {

    public static final Component DISPLAY_NAME = new TranslatableComponent("info.thermal.fluid_servo");

    protected IFilter filter = new BaseFluidFilter(FluidFilter.SIZE);
    protected RedstoneControlLogic rsControl = new RedstoneControlLogic();

    protected IGridHost<FluidGrid, FluidGridNode> host;
    protected Direction side;

    public FluidServoAttachment(IGridHost<FluidGrid, FluidGridNode> host, Direction side) {

        this.host = host;
        this.side = side;
    }

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

    // region IFilterableAttachment
    @Override
    public IFilter getFilter() {

        return filter;
    }
    // endregion

    // region IRedstoneControllableAttachment
    @Override
    public RedstoneControlLogic redstoneControl() {

        return rsControl;
    }
    // endregion
}
