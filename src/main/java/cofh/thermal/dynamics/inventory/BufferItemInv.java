package cofh.thermal.dynamics.inventory;

import cofh.lib.inventory.ItemStorageCoFH;
import cofh.lib.inventory.SimpleItemHandler;
import cofh.lib.inventory.SimpleItemInv;
import cofh.lib.util.IInventoryCallback;
import cofh.lib.util.StorageGroup;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

public class BufferItemInv extends SimpleItemInv {

    protected List<ItemStorageCoFH> bufferSlots = new ArrayList<>();
    protected List<ItemStorageCoFH> internalSlots = new ArrayList<>();

    protected BufferItemHandler bufferHandler;
    protected IItemHandler internalHandler;
    protected IItemHandler allHandler;

    public BufferItemInv(@Nullable IInventoryCallback tile) {

        super(tile);
    }

    public BufferItemInv(IInventoryCallback tile, String tag) {

        super(tile, tag);
    }

    public void addSlot(ItemStorageCoFH slot, StorageGroup group) {

        if (allHandler != null) {
            return;
        }
        slots.add(slot);
        switch (group) {
            case INTERNAL:
                internalSlots.add(slot);
                break;
            case ACCESSIBLE:
                bufferSlots.add(slot);
                break;
            default:
        }
    }

    public void setConditions(BooleanSupplier allowInsert, BooleanSupplier allowExtract) {

        bufferHandler.setConditions(allowInsert, allowExtract);
    }

    protected void optimize() {

        ((ArrayList<ItemStorageCoFH>) slots).trimToSize();
        ((ArrayList<ItemStorageCoFH>) bufferSlots).trimToSize();
        ((ArrayList<ItemStorageCoFH>) internalSlots).trimToSize();
    }

    public void initHandlers() {

        optimize();

        bufferHandler = new BufferItemHandler(tile, bufferSlots);
        internalHandler = new SimpleItemHandler(tile, internalSlots);
        allHandler = new SimpleItemHandler(tile, slots);
    }

    public boolean isBufferEmpty() {

        for (ItemStorageCoFH slot : bufferSlots) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isBufferFull() {

        for (ItemStorageCoFH slot : bufferSlots) {
            if (!slot.isFull()) {
                return false;
            }
        }
        return true;
    }

    public boolean isConfigEmpty() {

        for (ItemStorageCoFH slot : internalSlots) {
            if (!slot.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isConfigFull() {

        for (ItemStorageCoFH slot : internalSlots) {
            if (!slot.isFull()) {
                return false;
            }
        }
        return true;
    }

    public List<ItemStorageCoFH> getBufferSlots() {

        return bufferSlots;
    }

    public List<ItemStorageCoFH> getInternalSlots() {

        return internalSlots;
    }

    public IItemHandler getHandler(StorageGroup group) {

        if (allHandler == null) {
            initHandlers();
        }
        switch (group) {
            case ACCESSIBLE:
                return bufferHandler;
            case INTERNAL:
                return internalHandler;
            case ALL:
                return allHandler;
            default:
        }
        return EmptyHandler.INSTANCE;
    }

}
