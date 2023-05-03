package cofh.thermal.dynamics.inventory.container.slot;

import cofh.lib.inventory.container.slot.SlotFalseCopy;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import static cofh.core.util.helpers.ItemHelper.cloneStack;

/**
 * Slot which copies an ItemStack when clicked on, does not decrement the ItemStack on the cursor.
 */
public class SlotFalseBuffer extends SlotFalseCopy {

    public SlotFalseBuffer(Container inventoryIn, int index, int xPosition, int yPosition) {

        super(inventoryIn, index, xPosition, yPosition);
    }

    @Override
    public void set(ItemStack stack) {

        if (!mayPlace(stack)) {
            return;
        }
        container.setItem(this.slot, stack);
        setChanged();
    }

    public void incrementStack(int count) {

        ItemStack myStack = getItem();
        if (!myStack.isEmpty() && myStack.getCount() < myStack.getMaxStackSize()) {
            container.setItem(this.slot, cloneStack(myStack, Math.min(myStack.getCount() + count, myStack.getMaxStackSize())));
            setChanged();
        }
    }

    public void decrementStack(int count) {

        ItemStack myStack = getItem();
        if (!myStack.isEmpty() && myStack.getCount() > 1) {
            container.setItem(this.slot, cloneStack(myStack, Math.max(myStack.getCount() - count, 1)));
            setChanged();
        }
    }

}
