package cofh.thermal.dynamics.common.inventory;

import cofh.core.common.inventory.BlockEntityCoFHMenu;
import cofh.lib.common.inventory.SlotCoFH;
import cofh.lib.common.inventory.wrapper.InvWrapperCoFH;
import cofh.lib.util.helpers.MathHelper;
import cofh.thermal.dynamics.common.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.common.inventory.slot.SlotFalseBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import static cofh.thermal.dynamics.init.registries.TDynContainers.ITEM_BUFFER_CONTAINER;

public class ItemBufferMenu extends BlockEntityCoFHMenu {

    public final ItemBufferBlockEntity tile;

    public int wheelSlot = -1;
    public int wheelDir = 0;

    public ItemBufferMenu(int windowId, Level world, BlockPos pos, Inventory inventory, Player player) {

        super(ITEM_BUFFER_CONTAINER.get(), windowId, world, pos, inventory, player);
        this.tile = (ItemBufferBlockEntity) world.getBlockEntity(pos);
        InvWrapperCoFH tileInv = new InvWrapperCoFH(this.tile.getItemInv());

        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                addSlot(new SlotCoFH(tileInv, j + i * 3, 107 + j * 18, 23 + i * 18) {

                    @Override
                    public void setChanged() {

                        ((InvWrapperCoFH) container).onInventoryChange(slot);
                    }
                });
            }
        }
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                addSlot(new SlotFalseBuffer(tileInv, 9 + j + i * 3, 17 + j * 18, 23 + i * 18));
            }
        }
        bindPlayerInventory(inventory);
    }

    @Override
    protected int getPlayerInventoryVerticalOffset() {

        return 96;
    }

    // region NETWORK
    public FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        buffer.writeVarInt(wheelSlot);
        buffer.writeVarInt(wheelDir);

        return buffer;
    }

    public void handleConfigPacket(FriendlyByteBuf buffer) {

        wheelSlot = buffer.readVarInt();
        wheelDir = buffer.readVarInt();

        if (slots.get(wheelSlot).isActive() && slots.get(wheelSlot).hasItem()) {
            ItemStack stackInSlot = slots.get(wheelSlot).getItem();
            stackInSlot.setCount(MathHelper.clamp(stackInSlot.getCount() + wheelDir, 1, stackInSlot.getMaxStackSize()));
            slots.get(wheelSlot).set(stackInSlot);
        }
    }
    // endregion
}
