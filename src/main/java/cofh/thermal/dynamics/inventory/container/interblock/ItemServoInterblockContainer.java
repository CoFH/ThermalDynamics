package cofh.thermal.dynamics.inventory.container.interblock;

import cofh.core.inventory.container.ContainerCoFH;
import cofh.core.network.packet.server.ContainerConfigPacket;
import cofh.core.util.filter.BaseItemFilter;
import cofh.core.util.filter.IFilterOptions;
import cofh.lib.inventory.container.slot.SlotFalseCopy;
import cofh.lib.inventory.wrapper.InvWrapperGeneric;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.interblock.ItemServoInterblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class ItemServoInterblockContainer extends ContainerCoFH implements IFilterOptions {

    public final ItemServoInterblock attachment;

    protected BaseItemFilter filter;
    protected InvWrapperGeneric filterInventory;

    public ItemServoInterblockContainer(int id, Level world, BlockPos pos, Direction side, Inventory inventory) {

        // TODO Container type
        super(null, id, inventory, inventory.player);

        this.attachment = null; // TODO Grab from map
        this.filter = (BaseItemFilter) attachment.getFilter();

        allowSwap = false;

        int slots = filter.size();
        filterInventory = new InvWrapperGeneric(this, filter.getItems(), slots) {
            @Override
            public void setChanged() {

                filter.setItems(filterInventory.getStacks());
            }
        };

        int rows = MathHelper.clamp(slots / 3, 1, 3);
        int rowSize = slots / rows;

        int xOffset = 53 - 9 * rowSize;
        int yOffset = 44 - 9 * rows;

        for (int i = 0; i < filter.size(); ++i) {
            addSlot(new SlotFalseCopy(filterInventory, i, xOffset + i % rowSize * 18, yOffset + i / rowSize * 18));
        }
        bindPlayerInventory(inventory);
    }

    public int getFilterSize() {

        return filter.size();
    }

    @Override
    protected int getMergeableSlotCount() {

        return filterInventory.getContainerSize();
    }

    @Override
    public boolean stillValid(Player player) {

        return false;
    }

    @Override
    public void removed(Player playerIn) {

        filter.setItems(filterInventory.getStacks());
        super.removed(playerIn);
    }

    // region NETWORK
    @Override
    public FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        buffer.writeBoolean(getAllowList());
        buffer.writeBoolean(getCheckNBT());

        return buffer;
    }

    @Override
    public void handleConfigPacket(FriendlyByteBuf buffer) {

        filter.setAllowList(buffer.readBoolean());
        filter.setCheckNBT(buffer.readBoolean());
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
        ContainerConfigPacket.sendToServer(this);
        return ret;
    }

    @Override
    public boolean getCheckNBT() {

        return filter.getCheckNBT();
    }

    @Override
    public boolean setCheckNBT(boolean checkNBT) {

        boolean ret = filter.setCheckNBT(checkNBT);
        ContainerConfigPacket.sendToServer(this);
        return ret;
    }
    // endregion
}
