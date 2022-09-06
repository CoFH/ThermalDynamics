package cofh.thermal.dynamics.block.entity;

import cofh.core.block.entity.TileCoFH;
import cofh.core.network.packet.server.TileConfigPacket;
import cofh.lib.inventory.IOItemInv;
import cofh.lib.inventory.ItemStorageCoFH;
import cofh.lib.inventory.StackValidatedItemStorage;
import cofh.thermal.dynamics.inventory.container.ItemBufferContainer;
import cofh.thermal.lib.tileentity.ThermalTileSecurable;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cofh.lib.util.StorageGroup.*;
import static cofh.lib.util.constants.Constants.FACING_ALL;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.dynamics.init.TDynReferences.ITEM_BUFFER_TILE;

public class ItemBufferTile extends ThermalTileSecurable implements INamedContainerProvider {

    protected IOItemInv inventory = new IOItemInv(this, TAG_ITEM_INV);

    protected boolean latchMode = false;
    protected boolean checkNBT = true;

    protected boolean inputLock;
    protected boolean outputLock;

    public ItemBufferTile() {

        super(ITEM_BUFFER_TILE);

        StackValidatedItemStorage[] accessible = new StackValidatedItemStorage[9];
        ItemStorageCoFH[] internal = new ItemStorageCoFH[9];

        for (int i = 0; i < 9; ++i) {
            internal[i] = new ItemStorageCoFH();
            accessible[i] = new StackValidatedItemStorage(internal[i]).setCheckNBT(() -> checkNBT);
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
    public TileCoFH worldContext(BlockState state, IBlockReader world) {

        updateHandlers();

        return this;
    }

    @Override
    public void clearCache() {

        super.clearCache();
        updateHandlers();
    }

    @Override
    public int invSize() {

        return inventory.getSlots();
    }

    public IOItemInv getItemInv() {

        return inventory;
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory inventory, Player player) {

        return new ItemBufferContainer(i, level, worldPosition, inventory, player);
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
    public void load(BlockState state, CompoundTag nbt) {

        super.load(state, nbt);

        inventory.read(nbt);

        latchMode = nbt.getBoolean(TAG_MODE);
        checkNBT = nbt.getBoolean(TAG_FILTER_OPT_NBT);

        updateHandlers();
    }

    @Override
    public CompoundTag save(CompoundTag nbt) {

        super.save(nbt);

        inventory.write(nbt);

        nbt.putBoolean(TAG_MODE, latchMode);
        nbt.putBoolean(TAG_FILTER_OPT_NBT, checkNBT);

        return nbt;
    }
    // endregion

    // region INamedContainerProvider
    @Override
    public ITextComponent getDisplayName() {

        return new TranslationTextComponent(this.getBlockState().getBlock().getDescriptionId());
    }
    // endregion

    // region ITileCallback
    public void onInventoryChanged(int slot) {

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
    protected LazyOptional<?> inputCap = LazyOptional.empty();
    protected LazyOptional<?> outputCap = LazyOptional.empty();

    protected void updateHandlers() {

        LazyOptional<?> prevInputCap = inputCap;
        LazyOptional<?> prevOutputCap = outputCap;

        IItemHandler inputHandler = inventory.getHandler(INPUT);
        IItemHandler outputHandler = inventory.getHandler(OUTPUT);

        inputCap = LazyOptional.of(() -> inputHandler);
        outputCap = LazyOptional.of(() -> outputHandler);

        prevInputCap.invalidate();
        prevOutputCap.invalidate();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return getItemHandlerCapability(side);
        }
        return super.getCapability(cap, side);
    }

    protected <T> LazyOptional<T> getItemHandlerCapability(@Nullable Direction side) {

        if (side == getBlockState().getValue(FACING_ALL)) {
            return outputCap.cast();
        }
        return inputCap.cast();
    }
    // endregion
}
