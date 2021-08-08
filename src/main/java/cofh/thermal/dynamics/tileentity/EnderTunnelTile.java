package cofh.thermal.dynamics.tileentity;

import cofh.core.tileentity.TileCoFH;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.BlockHelper;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.text.IFormattableTextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

import static cofh.lib.util.constants.Constants.*;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.dynamics.init.TDynReferences.ENDER_TUNNEL_TILE;

public class EnderTunnelTile extends TileCoFH {

    private static final Object2ObjectOpenHashMap<UUID, EnderTunnelTile> tunnelMap = new Object2ObjectOpenHashMap<>();

    protected Direction facing;

    protected UUID myId = UUID.randomUUID();
    protected UUID targetId = EMPTY_UUID;

    protected Set<LazyOptional<?>> adjCapabilities = new ObjectOpenHashSet<>();

    private static final char[] CHARS = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p'};

    public static long SEP_MASK_HIGH = 0x00000000_f000f000L;
    public static long SEP_MASK_LOW = 0xf000f000_00000000L;

    public static String toCharMap(long value, long sepMask) {

        StringBuilder o = new StringBuilder();
        long msk = 0xf0000000_00000000L, sft = Long.numberOfTrailingZeros(msk);
        do {
            if ((msk & sepMask) == msk) {
                o.append('-');
            }
            o.append(CHARS[(int) ((value & msk) >>> sft)]);
            msk >>>= 4;
            sft -= 4;
        } while (msk > 0);
        return o.toString();
    }

    public EnderTunnelTile() {

        super(ENDER_TUNNEL_TILE);
    }

    @Override
    public boolean onActivatedDelegate(World world, BlockPos pos, BlockState state, PlayerEntity player, Hand hand, BlockRayTraceResult result) {

        IFormattableTextComponent myAddress = new StringTextComponent("My address: ").append(new StringTextComponent(toCharMap(myId.getMostSignificantBits(), SEP_MASK_HIGH) + toCharMap(myId.getLeastSignificantBits(), SEP_MASK_LOW)).mergeStyle(ENDER_STYLE));
        IFormattableTextComponent targetAddress = targetId.equals(EMPTY_UUID) ? new TranslationTextComponent("info.cofh.none") : new StringTextComponent("Target address: ").append(new StringTextComponent(toCharMap(targetId.getMostSignificantBits(), SEP_MASK_HIGH) + toCharMap(targetId.getLeastSignificantBits(), SEP_MASK_LOW)).mergeStyle(ENDER_STYLE));

        ChatHelper.sendIndexedChatMessagesToPlayer(player, Lists.newArrayList(myAddress, targetAddress));
        return super.onActivatedDelegate(world, pos, state, player, hand, result);
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
            nbt.putUniqueId(TAG_ENDER_ADDRESS, targetId);
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
        targetId = nbt.getUniqueId(TAG_ENDER_ADDRESS);
    }

    @Override
    public CompoundNBT write(CompoundNBT nbt) {

        super.write(nbt);

        nbt.putUniqueId(TAG_UUID, myId);
        nbt.putUniqueId(TAG_ENDER_ADDRESS, targetId);

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

        if (tag.contains(TAG_ENDER_ADDRESS)) {
            UUID newTarget = tag.getUniqueId(TAG_ENDER_ADDRESS);
            if (!myId.equals(newTarget)) {
                targetId = newTarget;
            }
        }
    }

    public void writeConveyableData(PlayerEntity player, CompoundNBT tag) {

        tag.putUniqueId(TAG_ENDER_ADDRESS, myId);
    }
    // endregion
}
