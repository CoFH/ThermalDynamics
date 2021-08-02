package cofh.thermal.dynamics.inventory.container.slot;

import cofh.lib.inventory.container.slot.SlotFalseCopy;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

/**
 * Slot which copies an ItemStack when clicked on, does not decrement the ItemStack on the cursor.
 */
public class SlotFalseBuffer extends SlotFalseCopy {

    public SlotFalseBuffer(IInventory inventoryIn, int index, int xPosition, int yPosition) {

        super(inventoryIn, index, xPosition, yPosition);
    }

    @Override
    public void putStack(ItemStack stack) {

        if (!isItemValid(stack)) {
            return;
        }
        inventory.setInventorySlotContents(this.slotIndex, stack);
        onSlotChanged();
    }

}
