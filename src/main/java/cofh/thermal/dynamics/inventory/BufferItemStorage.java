package cofh.thermal.dynamics.inventory;

import cofh.lib.inventory.IItemStackAccess;
import cofh.lib.inventory.ItemStorageCoFH;

import java.util.function.BooleanSupplier;

import static cofh.lib.util.constants.Constants.TRUE;
import static cofh.lib.util.helpers.ItemHelper.itemsEqual;
import static cofh.lib.util.helpers.ItemHelper.itemsEqualWithTags;

public class BufferItemStorage extends ItemStorageCoFH {

    protected final IItemStackAccess linkedStack;
    protected BooleanSupplier checkNBT = TRUE;

    public BufferItemStorage(IItemStackAccess linkedStack) {

        this.linkedStack = linkedStack;

        validator = (item) -> checkNBT.getAsBoolean() ? itemsEqualWithTags(item, linkedStack.getItemStack()) : itemsEqual(item, linkedStack.getItemStack());
    }

    public BufferItemStorage setCheckNBT(BooleanSupplier checkNBT) {

        this.checkNBT = checkNBT;
        return this;
    }

    @Override
    public int getSlotLimit(int slot) {

        return linkedStack.getCount();
    }

}
