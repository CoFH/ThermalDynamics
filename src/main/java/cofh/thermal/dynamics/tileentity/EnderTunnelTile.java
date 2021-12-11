package cofh.thermal.dynamics.tileentity;

import cofh.core.tileentity.TileCoFH;
import cofh.core.util.helpers.ChatHelper;
import cofh.lib.capability.CapabilityRedstoneFlux;
import cofh.lib.util.Utils;
import cofh.lib.util.helpers.BlockHelper;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
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
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

import static cofh.lib.util.constants.Constants.*;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.dynamics.init.TDynReferences.ENDER_TUNNEL_TILE;

public class EnderTunnelTile extends TileCoFH {

    private static final Object2ObjectOpenHashMap<UUID, EnderTunnelTile> TUNNEL_MAP = new Object2ObjectOpenHashMap<>();
    private static final Set<Capability> VALID_CAPABILIIES = new ObjectOpenHashSet<>(4);

    protected Direction facing;

    protected UUID myId = UUID.randomUUID();
    protected UUID targetId = EMPTY_UUID;

    protected Set<LazyOptional<?>> adjCapabilities = new ObjectOpenHashSet<>();

    private static final char[] CHARS = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p'};

    // These are actual UUID separations.
    //    public static final long SEP_MASK_HIGH = 0x00000000_f000f000L;
    //    public static final long SEP_MASK_LOW = 0xf000f000_00000000L;

    public static final long SEP_MASK_HIGH = 0x00000000_f0000000L;
    public static final long SEP_MASK_LOW = 0xf0000000_f0000000L;

    public static void initializeValidCapabilities() {

        VALID_CAPABILIIES.add(CapabilityEnergy.ENERGY);
        VALID_CAPABILIIES.add(CapabilityRedstoneFlux.RF_ENERGY);
        VALID_CAPABILIIES.add(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY);
        VALID_CAPABILIIES.add(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY);
    }

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

        if (super.onActivatedDelegate(world, pos, state, player, hand, result)) {
            return true;
        }
        IFormattableTextComponent myAddress = new StringTextComponent("My address: ").append(new StringTextComponent(toCharMap(myId.getMostSignificantBits(), SEP_MASK_HIGH) + toCharMap(myId.getLeastSignificantBits(), SEP_MASK_LOW)).withStyle(ENDER_STYLE));
        IFormattableTextComponent targetAddress = new StringTextComponent("Target address: ").append(targetId.equals(EMPTY_UUID) ? new TranslationTextComponent("info.cofh.none") : new StringTextComponent(toCharMap(targetId.getMostSignificantBits(), SEP_MASK_HIGH) + toCharMap(targetId.getLeastSignificantBits(), SEP_MASK_LOW)).withStyle(ENDER_STYLE));

        ChatHelper.sendIndexedChatMessagesToPlayer(player, Lists.newArrayList(myAddress, targetAddress));

        return true;
    }

    @Override
    public TileCoFH worldContext(BlockState state, IBlockReader world) {

        facing = state.getValue(FACING_ALL);
        return this;
    }

    @Override
    public void onLoad() {

        super.onLoad();
        if (level != null && Utils.isServerWorld(level)) {
            TUNNEL_MAP.put(myId, this);
        }
    }

    @Override
    public void setRemoved() {

        super.setRemoved();
        TUNNEL_MAP.remove(myId);

        for (LazyOptional<?> opt : adjCapabilities) {
            opt.invalidate();
        }
        adjCapabilities.clear();
    }

    @Override
    public void clearCache() {

        super.clearCache();
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
        Direction curFacing = getBlockState().getValue(FACING_ALL);

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

        CompoundNBT nbt = stack.getOrCreateTagElement(TAG_BLOCK_ENTITY);

        nbt.putUUID(TAG_UUID, myId);
        if (targetId != EMPTY_UUID) {
            nbt.putUUID(TAG_ENDER_ADDRESS, targetId);
        }
        if (!nbt.isEmpty()) {
            stack.addTagElement(TAG_BLOCK_ENTITY, nbt);
        }
        return super.createItemStackTag(stack);
    }

    // region NBT
    @Override
    public void load(BlockState state, CompoundNBT nbt) {

        super.load(state, nbt);

        myId = nbt.getUUID(TAG_UUID);
        targetId = nbt.getUUID(TAG_ENDER_ADDRESS);
    }

    @Override
    public CompoundNBT save(CompoundNBT nbt) {

        super.save(nbt);

        nbt.putUUID(TAG_UUID, myId);
        nbt.putUUID(TAG_ENDER_ADDRESS, targetId);

        return nbt;
    }
    // endregion

    private boolean retrievingCapability = false;

    protected <T> LazyOptional<T> getRemoteCapability(@Nonnull Capability<T> cap) {

        // Whitelist + recursion check.
        if (retrievingCapability || !VALID_CAPABILIIES.contains(cap)) {
            return LazyOptional.empty();
        }
        TileEntity adjTile = BlockHelper.getAdjacentTileEntity(this, getFacing());
        if (adjTile != null && !(adjTile instanceof EnderTunnelTile)) {
            retrievingCapability = true;
            LazyOptional<T> adjCap = adjTile.getCapability(cap, getFacing().getOpposite());
            adjCapabilities.add(adjCap);
            retrievingCapability = false;
            return adjCap;
        }
        return LazyOptional.empty();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        // Whitelist + recursion check.
        if (retrievingCapability || !VALID_CAPABILIIES.contains(cap)) {
            return LazyOptional.empty();
        }
        if (side != null && side.equals(getFacing())) {
            EnderTunnelTile targetTile = TUNNEL_MAP.get(targetId);
            if (targetTile != null && !targetTile.isRemoved()) {
                retrievingCapability = true;
                LazyOptional<T> remCap = targetTile.getRemoteCapability(cap);
                retrievingCapability = false;
                return remCap;
            }
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onPlacedBy(World worldIn, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {

        // This will only happen if pickBlock is used or an ItemStack is otherwise copied directly.
        if (TUNNEL_MAP.containsKey(myId)) {
            myId = UUID.randomUUID();
            if (placer instanceof PlayerEntity) {
                IFormattableTextComponent myAddress = new StringTextComponent("Causality violation detected, new local address established: ").append(new StringTextComponent(toCharMap(myId.getMostSignificantBits(), SEP_MASK_HIGH) + toCharMap(myId.getLeastSignificantBits(), SEP_MASK_LOW)).withStyle(ENDER_STYLE));
                ChatHelper.sendIndexedChatMessagesToPlayer((PlayerEntity) placer, Lists.newArrayList(myAddress));
            }
        }
    }

    // region IConveyableData
    public void readConveyableData(PlayerEntity player, CompoundNBT tag) {

        if (tag.contains(TAG_ENDER_ADDRESS)) {
            UUID newTarget = tag.getUUID(TAG_ENDER_ADDRESS);
            if (!myId.equals(newTarget)) {
                targetId = newTarget;
            }
        }
    }

    public void writeConveyableData(PlayerEntity player, CompoundNBT tag) {

        tag.putUUID(TAG_ENDER_ADDRESS, myId);
    }
    // endregion
}
