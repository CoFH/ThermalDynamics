package cofh.thermal.dynamics.inventory.container.attachment;

import cofh.core.network.packet.client.ContainerGuiPacket;
import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilterOptions;
import cofh.lib.inventory.container.slot.SlotFalseCopy;
import cofh.lib.inventory.wrapper.InvWrapperFluids;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.attachment.FluidOutputFilterAttachment;
import cofh.thermal.dynamics.network.packet.server.AttachmentConfigPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

import static cofh.thermal.dynamics.init.TDynContainers.FLUID_OUTPUT_FILTER_ATTACHMENT_CONTAINER;

public class FluidOutputFilterAttachmentContainer extends AttachmentContainer implements IFilterOptions {

    public final FluidOutputFilterAttachment attachment;

    protected BaseFluidFilter filter;
    protected InvWrapperFluids filterInventory;

    public FluidOutputFilterAttachmentContainer(int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {

        super(FLUID_OUTPUT_FILTER_ATTACHMENT_CONTAINER.get(), id, world, pos, side, inventory, player);

        if (hostTile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof FluidOutputFilterAttachment expectedAttachment) {
            this.attachment = expectedAttachment;
            this.filter = (BaseFluidFilter) attachment.getFilter();
        } else {
            this.attachment = null;
        }
        allowSwap = false;
        if (filter != null) {
            int slots = filter.size();
            filterInventory = new InvWrapperFluids(this, filter.getFluids(), slots);

            int rows = MathHelper.clamp(slots / 3, 1, 3);
            int rowSize = slots / rows;

            int xOffset = 62 - 9 * rowSize;
            int yOffset = 44 - 9 * rows;

            for (int i = 0; i < filter.size(); ++i) {
                addSlot(new SlotFalseCopy(filterInventory, i, xOffset + i % rowSize * 18, yOffset + i / rowSize * 18));
            }
        }
        bindPlayerInventory(inventory);
    }

    public int getFilterSize() {

        return filter.size();
    }

    public List<FluidStack> getFilterStacks() {

        return filterInventory.getStacks();
    }

    @Override
    protected int getMergeableSlotCount() {

        return filterInventory.getContainerSize();
    }

    @Override
    public void broadcastChanges() {

        // This seems strange when the Attachment already has a Gui Packet, but the attachment doesn't know about the filter inventory.
        super.broadcastChanges();
        ContainerGuiPacket.sendToClient(this, player);
    }

    @Override
    public void removed(Player playerIn) {

        filter.setFluids(filterInventory.getStacks());
        super.removed(playerIn);
    }

    // region NETWORK
    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        byte size = (byte) filter.getFluids().size();
        buffer.writeByte(size);
        for (int i = 0; i < size; ++i) {
            buffer.writeFluidStack(getFilterStacks().get(i));
        }
        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {

        byte size = buffer.readByte();
        List<FluidStack> fluidStacks = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            fluidStacks.add(buffer.readFluidStack());
        }
        filterInventory.readFromSource(fluidStacks);
    }
    // endregion

    // region IFilterOptions
    @Override
    public boolean getAllowList() {

        return filter.getAllowList();
    }

    @Override
    public boolean setAllowList(boolean allowList) {

        boolean ret = filter.setAllowList(allowList);
        AttachmentConfigPacket.sendToServer(attachment);
        return ret;
    }

    @Override
    public boolean getCheckNBT() {

        return filter.getCheckNBT();
    }

    @Override
    public boolean setCheckNBT(boolean checkNBT) {

        boolean ret = filter.setCheckNBT(checkNBT);
        AttachmentConfigPacket.sendToServer(attachment);
        return ret;
    }
    // endregion
}
