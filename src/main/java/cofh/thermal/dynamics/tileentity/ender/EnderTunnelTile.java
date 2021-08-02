package cofh.thermal.dynamics.tileentity.ender;

import cofh.core.tileentity.TileCoFH;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.BlockHelper;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.IBlockReader;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

import static cofh.lib.util.constants.Constants.EMPTY_UUID;
import static cofh.lib.util.constants.Constants.FACING_ALL;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.dynamics.init.TDynReferences.ENDER_TUNNEL_TILE;

public class EnderTunnelTile extends TileCoFH {

    private static final Object2ObjectOpenHashMap<UUID, EnderTunnelTile> tunnelMap = new Object2ObjectOpenHashMap<>();

    protected Direction facing;

    protected UUID myId = UUID.randomUUID();
    protected UUID targetId = EMPTY_UUID;

    protected Set<LazyOptional<?>> adjCapabilities = new ObjectOpenHashSet<>();

    public EnderTunnelTile() {

        super(ENDER_TUNNEL_TILE);
    }

    @Override
    public TileCoFH worldContext(BlockState state, IBlockReader world) {

        facing = state.get(FACING_ALL);
        return this;
    }

    @Override
    public void onLoad() {

        super.onLoad();
        if (world != null && Utils.isServerWorld(world)) {
            tunnelMap.put(myId, this);
        }
    }

    @Override
    public void remove() {

        super.remove();
        tunnelMap.remove(myId);

        for (LazyOptional<?> opt : adjCapabilities) {
            opt.invalidate();
        }
        adjCapabilities.clear();
    }

    @Override
    public void updateContainingBlockInfo() {

        super.updateContainingBlockInfo();
        updateFacing();
    }

    protected Direction getFacing() {

        if (facing == null) {
            updateFacing();
        }
        return facing;
    }

    protected void updateFacing() {

        Direction prevFacing = facing;
        Direction curFacing = getBlockState().get(FACING_ALL);

        facing = curFacing; // Facing must be updated before invalidation or some things may improperly reacquire.

        if (prevFacing != curFacing) {
            for (LazyOptional<?> opt : adjCapabilities) {
                opt.invalidate();
            }
            adjCapabilities.clear();
        }
    }

    @Override
    public ItemStack createItemStackTag(ItemStack stack) {

        CompoundNBT nbt = stack.getOrCreateChildTag(TAG_BLOCK_ENTITY);

        nbt.putUniqueId(TAG_UUID, myId);
        if (targetId != EMPTY_UUID) {
            nbt.putUniqueId(TAG_FACING, targetId);
        }
        if (!nbt.isEmpty()) {
            stack.setTagInfo(TAG_BLOCK_ENTITY, nbt);
        }
        return super.createItemStackTag(stack);
    }

    // region NBT
    @Override
    public void read(BlockState state, CompoundNBT nbt) {

        super.read(state, nbt);

        myId = nbt.getUniqueId(TAG_UUID);
        targetId = nbt.getUniqueId(TAG_ENDER_FREQUENCY);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {

        super.write(nbt);

        nbt.putUniqueId(TAG_UUID, myId);
        nbt.putUniqueId(TAG_ENDER_FREQUENCY, targetId);

        return nbt;
    }
    // endregion

    protected <T> LazyOptional<T> getRemoteCapability(@Nonnull Capability<T> cap) {

        TileEntity adjTile = BlockHelper.getAdjacentTileEntity(this, getFacing());
        if (adjTile != null && !(adjTile instanceof EnderTunnelTile)) {
            LazyOptional<T> adjCap = adjTile.getCapability(cap, getFacing().getOpposite());
            adjCapabilities.add(adjCap);
            return adjCap;
        }
        return LazyOptional.empty();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        if (side != null && side.equals(getFacing())) {
            EnderTunnelTile targetTile = tunnelMap.get(targetId);
            if (targetTile != null && !targetTile.isRemoved()) {
                LazyOptional<T> remCap = targetTile.getRemoteCapability(cap);
                return remCap;
            }
        }
        return super.getCapability(cap, side);
    }

    // region IConveyableData
    public void readConveyableData(PlayerEntity player, CompoundNBT tag) {

        if (tag.contains(TAG_ENDER_FREQUENCY)) {
            UUID newTarget = tag.getUniqueId(TAG_ENDER_FREQUENCY);
            if (!myId.equals(newTarget)) {
                targetId = newTarget;
            }
        }
    }

    public void writeConveyableData(PlayerEntity player, CompoundNBT tag) {

        tag.putUniqueId(TAG_ENDER_FREQUENCY, myId);
    }
    // endregion
}
