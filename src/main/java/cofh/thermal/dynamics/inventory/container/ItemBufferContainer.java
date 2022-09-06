package cofh.thermal.dynamics.inventory.container;

import cofh.core.inventory.container.TileContainer;
import cofh.lib.inventory.container.slot.SlotCoFH;
import cofh.lib.inventory.wrapper.InvWrapperCoFH;
import cofh.thermal.dynamics.block.entity.ItemBufferTile;
import cofh.thermal.dynamics.inventory.container.slot.SlotFalseBuffer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static cofh.thermal.dynamics.init.TDynReferences.ITEM_BUFFER_CONTAINER;

public class ItemBufferContainer extends TileContainer {

    public final ItemBufferTile tile;

    public ItemBufferContainer(int windowId, Level world, BlockPos pos, Inventory inventory, Player player) {

        super(ITEM_BUFFER_CONTAINER, windowId, world, pos, inventory, player);
        this.tile = (ItemBufferTile) world.getBlockEntity(pos);
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

}
