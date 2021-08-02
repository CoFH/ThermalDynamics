package cofh.thermal.dynamics.tileentity.logistics;

import cofh.core.network.packet.server.TileConfigPacket;
import cofh.lib.inventory.ItemStorageCoFH;
import cofh.thermal.dynamics.inventory.BufferItemInv;
import cofh.thermal.dynamics.inventory.BufferItemStorage;
import cofh.thermal.dynamics.inventory.container.logistics.LogisticsItemBufferContainer;
import cofh.thermal.lib.tileentity.ThermalTileSecurable;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cofh.lib.util.StorageGroup.ACCESSIBLE;
import static cofh.lib.util.StorageGroup.INTERNAL;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.dynamics.init.TDynReferences.LOGISTICS_ITEM_BUFFER_TILE;

public class LogisticsItemBufferTile extends ThermalTileSecurable implements INamedContainerProvider {

    protected BufferItemInv inventory = new BufferItemInv(this, TAG_ITEM_INV);

    protected boolean latchMode;
    protected boolean checkNBT;

    protected boolean inputLock;
    protected boolean outputLock;

    public LogisticsItemBufferTile() {

        super(LOGISTICS_ITEM_BUFFER_TILE);

        BufferItemStorage[] accessible = new BufferItemStorage[9];
        ItemStorageCoFH[] internal = new ItemStorageCoFH[9];

        for (int i = 0; i < 9; ++i) {
            internal[i] = new ItemStorageCoFH();
            accessible[i] = new BufferItemStorage(internal[i]).setCheckNBT(() -> checkNBT);
        }
        for (int i = 0; i < 9; ++i) {
            inventory.addSlot(accessible[i], ACCESSIBLE);
        }
        for (int i = 0; i < 9; ++i) {
            inventory.addSlot(internal[i], INTERNAL);
        }
        inventory.initHandlers();
        inventory.setConditions(() -> !inputLock, () -> !outputLock);
    }

    @Override
    public int invSize() {

        return inventory.getSlots();
    }

    public BufferItemInv getItemInv() {

        return inventory;
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory inventory, PlayerEntity player) {

        return new LogisticsItemBufferContainer(i, world, pos, inventory, player);
    }

    public void setLatchMode(boolean latchMode) {

        boolean curLatch = this.latchMode;
        this.latchMode = latchMode;
        TileConfigPacket.sendToServer(this);
        this.latchMode = curLatch;
    }

    public boolean getLatchMode() {

        return latchMode;
    }

    public void setCheckNBT(boolean checkNBT) {

        boolean curCheckNBT = this.checkNBT;
        this.checkNBT = checkNBT;
        TileConfigPacket.sendToServer(this);
        this.checkNBT = curCheckNBT;
    }

    public boolean getCheckNBT() {

        return checkNBT;
    }

    // region NETWORK

    // CONFIG
    @Override
    public PacketBuffer getConfigPacket(PacketBuffer buffer) {

        super.getConfigPacket(buffer);

        buffer.writeBoolean(latchMode);
        buffer.writeBoolean(checkNBT);

        return buffer;
    }

    @Override
    public void handleConfigPacket(PacketBuffer buffer) {

        super.handleConfigPacket(buffer);

        latchMode = buffer.readBoolean();
        checkNBT = buffer.readBoolean();

        if (latchMode) {
            if (inventory.isConfigEmpty() || inventory.isBufferEmpty()) {
                inputLock = false;
                outputLock = true;
            } else if (outputLock) {
                if (inventory.isBufferFull()) {
                    inputLock = true;
                    outputLock = false;
                } else if (inventory.isBufferEmpty()) {
                    inputLock = false;
                    outputLock = true;
                }
            }
        } else {
            inputLock = outputLock = false;
        }
    }

    // GUI
    @Override
    public PacketBuffer getGuiPacket(PacketBuffer buffer) {

        super.getGuiPacket(buffer);

        buffer.writeBoolean(latchMode);
        buffer.writeBoolean(checkNBT);

        return buffer;
    }

    @Override
    public void handleGuiPacket(PacketBuffer buffer) {

        super.handleGuiPacket(buffer);

        latchMode = buffer.readBoolean();
        checkNBT = buffer.readBoolean();
    }
    // endregion

    // region NBT
    @Override
    public void read(BlockState state, CompoundNBT nbt) {

        super.read(state, nbt);

        inventory.read(nbt);

        latchMode = nbt.getBoolean(TAG_MODE);
        checkNBT = nbt.getBoolean(TAG_FILTER_OPT_NBT);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {

        super.write(nbt);

        inventory.write(nbt);

        nbt.putBoolean(TAG_MODE, latchMode);
        nbt.putBoolean(TAG_FILTER_OPT_NBT, checkNBT);

        return nbt;
    }
    // endregion

    // region INamedContainerProvider
    @Override
    public ITextComponent getDisplayName() {

        return new TranslationTextComponent(this.getBlockState().getBlock().getTranslationKey());
    }
    // endregion

    // region ITileCallback
    public void onInventoryChange(int slot) {

        if (latchMode) {
            if (inventory.isConfigEmpty() || inventory.isBufferEmpty()) {
                inputLock = false;
                outputLock = true;
            } else if (outputLock) {
                if (inventory.isBufferFull()) {
                    inputLock = true;
                    outputLock = false;
                } else if (inventory.isBufferEmpty()) {
                    inputLock = false;
                    outputLock = true;
                }
            }
        }
    }
    // endregion

    // region CAPABILITIES
    protected LazyOptional<?> itemCap = LazyOptional.empty();

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return getItemHandlerCapability(side);
        }
        return super.getCapability(cap, side);
    }

    protected <T> LazyOptional<T> getItemHandlerCapability(@Nullable Direction side) {

        if (!itemCap.isPresent()) {
            IItemHandler handler = inventory.getHandler(ACCESSIBLE);
            itemCap = LazyOptional.of(() -> handler);
        }
        return itemCap.cast();
    }
    // endregion
}
