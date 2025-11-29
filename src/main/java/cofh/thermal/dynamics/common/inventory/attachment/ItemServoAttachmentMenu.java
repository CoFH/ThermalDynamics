package cofh.thermal.dynamics.common.inventory.attachment;

import cofh.core.common.network.packet.client.ContainerGuiPacket;
import cofh.core.util.filter.BaseItemFilter;
import cofh.core.util.filter.IFilterOptions;
import cofh.lib.common.inventory.SlotFalseCopy;
import cofh.lib.common.inventory.wrapper.InvWrapperGeneric;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.attachment.ItemServoAttachment;
import cofh.thermal.dynamics.common.network.packet.server.AttachmentConfigPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.SlotItemHandler;

import net.minecraft.world.inventory.MenuType;

import java.util.ArrayList;
import java.util.List;

import static cofh.thermal.dynamics.init.registries.TDynContainers.ITEM_SERVO_ATTACHMENT_CONTAINER;

public class ItemServoAttachmentMenu extends AttachmentMenu implements IFilterOptions {

    public final ItemServoAttachment attachment;

    protected BaseItemFilter filter;
    protected InvWrapperGeneric filterInventory;

    public ItemServoAttachmentMenu(int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {
        this(ITEM_SERVO_ATTACHMENT_CONTAINER.get(), id, world, pos, side, inventory, player);
    }

    protected ItemServoAttachmentMenu(MenuType<?> type, int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {
        super(type, id, world, pos, side, inventory, player);

        if (hostTile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof ItemServoAttachment expectedAttachment) {
            this.attachment = expectedAttachment;
            this.filter = (BaseItemFilter) attachment.getFilter();
        } else {
            this.attachment = null;
        }
        allowSwap = false;

        if (filter != null) {
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

            // Filter slots
            for (int i = 0; i < filter.size(); ++i) {
                addSlot(new SlotFalseCopy(filterInventory, i, xOffset + i % rowSize * 18, yOffset + i / rowSize * 18));
            }

            // Overflow storage slots (3x3 grid below filter)
            if (attachment != null) {
                int overflowYOffset = yOffset + (rows * 18) + 20; // 20 pixels below filter
                for (int i = 0; i < 9; i++) {
                    int row = i / 3;
                    int col = i % 3;
                    addSlot(new SlotItemHandler(attachment.getOverflowStorage(), i, 
                            62 + col * 18, overflowYOffset + row * 18));
                }
            }
        }
        bindPlayerInventory(inventory);
    }

    public int getFilterSize() {
        return filter != null ? filter.size() : 0;
    }

    public List<ItemStack> getFilterStacks() {
        return filterInventory != null ? filterInventory.getStacks() : new ArrayList<>();
    }
    
    public int getTransferAmount() {
        return attachment != null ? attachment.getTransfer() : 1;
    }
    
    public void setTransferAmount(int amount) {
        if (attachment != null) {
            attachment.setTransfer(amount);
            AttachmentConfigPacket.sendToServer(attachment);
        }
    }
    
    public void incrementTransferAmount() {
        if (attachment != null) {
            attachment.incrementTransfer();
            AttachmentConfigPacket.sendToServer(attachment);
        }
    }
    
    public void decrementTransferAmount() {
        if (attachment != null) {
            attachment.decrementTransfer();
            AttachmentConfigPacket.sendToServer(attachment);
        }
    }
    
    public int getMinTransferAmount() {
        return attachment != null ? attachment.getMinTransfer() : 1;
    }
    
    public int getMaxTransferAmount() {
        return attachment != null ? attachment.getMaxTransfer() : 8;
    }

    @Override
    protected int getMergeableSlotCount() {
        return filterInventory != null ? filterInventory.getContainerSize() : 0;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        ContainerGuiPacket.sendToClient(this, player);
    }

    @Override
    public void removed(Player playerIn) {
        if (filter != null && filterInventory != null) {
            filter.setItems(filterInventory.getStacks());
        }
        super.removed(playerIn);
    }

    // region NETWORK
    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {
        if (filter != null && filterInventory != null) {
            byte size = (byte) filter.getItems().size();
            buffer.writeByte(size);
            for (int i = 0; i < size; ++i) {
                buffer.writeItem(getFilterStacks().get(i));
            }
        } else {
            buffer.writeByte(0);
        }
        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {
        byte size = buffer.readByte();
        List<ItemStack> itemStacks = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            itemStacks.add(buffer.readItem());
        }
        if (filterInventory != null) {
            filterInventory.readFromSource(itemStacks);
        }
    }
    // endregion

    // region IFilterOptions
    @Override
    public boolean getAllowList() {
        return filter != null ? filter.getAllowList() : false;
    }

    @Override
    public boolean setAllowList(boolean allowList) {
        if (filter != null && attachment != null) {
            boolean ret = filter.setAllowList(allowList);
            AttachmentConfigPacket.sendToServer(attachment);
            return ret;
        }
        return false;
    }

    @Override
    public boolean getCheckNBT() {
        return filter != null ? filter.getCheckNBT() : false;
    }

    @Override
    public boolean setCheckNBT(boolean checkNBT) {
        if (filter != null && attachment != null) {
            boolean ret = filter.setCheckNBT(checkNBT);
            AttachmentConfigPacket.sendToServer(attachment);
            return ret;
        }
        return false;
    }
    // endregion
}