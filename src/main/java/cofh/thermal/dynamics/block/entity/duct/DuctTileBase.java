package cofh.thermal.dynamics.block.entity.duct;

import cofh.core.network.packet.client.ModelUpdatePacket;
import cofh.core.network.packet.client.TileRedstonePacket;
import cofh.core.network.packet.client.TileStatePacket;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.lib.api.block.entity.ITileLocation;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.attachment.*;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import cofh.thermal.dynamics.grid.Grid;
import cofh.thermal.dynamics.grid.GridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static cofh.lib.util.Constants.DIRECTIONS;
import static cofh.lib.util.constants.NBTTags.*;
import static cofh.thermal.dynamics.api.grid.IDuct.ConnectionType.*;

public abstract class DuctTileBase<G extends Grid<G, N>, N extends GridNode<G>> extends BlockEntity implements IDuct<G, N>, ITileLocation, IPacketHandlerTile {

    // Only available server side.
    @Nullable
    protected G grid = null;

    protected ConnectionType[] connections = {ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED};
    protected IAttachment[] attachments = {EmptyAttachment.INSTANCE, EmptyAttachment.INSTANCE, EmptyAttachment.INSTANCE, EmptyAttachment.INSTANCE, EmptyAttachment.INSTANCE, EmptyAttachment.INSTANCE};

    protected final DuctModelData modelData = new DuctModelData();
    protected boolean modelUpdate;

    protected int redstonePower;

    public DuctTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);
    }

    public boolean attemptConnect(Direction dir) {

        IDuct<?, ?> adjacent = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacent instanceof DuctTileBase other) {
            IGridContainer gridContainer = IGridContainer.getCapability(level);
            if (gridContainer == null) {
                return false;
            }

            connections[dir.ordinal()] = ALLOWED;
            other.connections[dir.getOpposite().ordinal()] = ALLOWED;

            if (!gridContainer.onDuctSideConnected(this, dir)) {
                connections[dir.ordinal()] = DISABLED;
                other.connections[dir.getOpposite().ordinal()] = DISABLED;
                return false;
            }

            setChanged();
            other.setChanged();

            TileStatePacket.sendToClient(this);
            TileStatePacket.sendToClient(other);
        } else {
            connections[dir.ordinal()] = ALLOWED;
            setChanged();
            callNeighborStateChange();
            TileStatePacket.sendToClient(this);
        }
        return true;
    }

    public boolean attemptDisconnect(Direction dir) {

        // TODO Dismantle attachment
        //        if (false) {
        //            return true;
        //        }
        IDuct<?, ?> adjacent = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacent instanceof DuctTileBase<?, ?> other) { // TODO, This should be moved up to IGridHost as a common implementation for (eventual) multiparts.
            IGridContainer gridContainer = IGridContainer.getCapability(level);
            if (gridContainer == null) {
                return false;
            }
            gridContainer.onDuctSideDisconnecting(this, dir);

            connections[dir.ordinal()] = DISABLED;
            other.connections[dir.getOpposite().ordinal()] = DISABLED;

            setChanged();
            other.setChanged();

            TileStatePacket.sendToClient(this);
            TileStatePacket.sendToClient(other);
        } else {
            connections[dir.ordinal()] = DISABLED;
            setChanged();
            callNeighborStateChange();
            TileStatePacket.sendToClient(this);
        }
        return true;
    }

    public boolean attemptAttachmentInstall(Direction side, String type) {

        if (attachments[side.ordinal()] != EmptyAttachment.INSTANCE) {
            return false;
        }
        IAttachment attachment = AttachmentRegistry.getAttachment(type, new CompoundTag(), this, side);
        if (attachment == null || attachment == EmptyAttachment.INSTANCE) {
            return false;
        }
        attachments[side.ordinal()] = attachment;
        connections[side.ordinal()] = FORCED;
        setChanged();
        callNeighborStateChange();
        TileStatePacket.sendToClient(this);
        return true;
    }

    public boolean openDuctGui(Player player) {

        if (this instanceof MenuProvider provider) {
            NetworkHooks.openGui((ServerPlayer) player, provider, pos());
            return true;
        }
        return false;
    }

    public boolean openAttachmentGui(Direction side, Player player) {

        if (side != null && attachments[side.ordinal()] instanceof MenuProvider provider) {
            AttachmentHelper.openAttachmentScreen((ServerPlayer) player, provider, pos(), side);
            return true;
        }
        return false;
    }

    protected void callNeighborStateChange() {

        if (level == null) {
            return;
        }
        level.updateNeighborsAt(pos(), block());
    }

    protected abstract boolean canConnectToBlock(Direction dir);

    public void requestModelDataUpdate() {

        if (this.level != null && level.isClientSide) {
            modelUpdate = true;
            ModelDataManager.requestModelDataRefresh(this);
        }
    }

    @Nonnull
    @Override
    public IModelData getModelData() {

        if (modelUpdate) {
            for (Direction dir : DIRECTIONS) {
                IDuct<?, ?> adjacent = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
                modelData.setInternalConnection(dir, adjacent != null && canConnectTo(adjacent, dir) && adjacent.canConnectTo(this, dir.getOpposite()));
                modelData.setExternalConnection(dir, canConnectToBlock(dir) || connections[dir.ordinal()] == FORCED);
                modelData.setAttachment(dir, attachments[dir.ordinal()].getTexture());
            }
            modelUpdate = false;
        }
        return modelData;
    }

    // region NETWORK

    // REDSTONE
    @Override
    public FriendlyByteBuf getRedstonePacket(FriendlyByteBuf buffer) {

        buffer.writeInt(redstonePower);

        return buffer;
    }

    @Override
    public void handleRedstonePacket(FriendlyByteBuf buffer) {

        int power = buffer.readInt();
        for (IAttachment attachment : attachments) {
            if (attachment instanceof IRedstoneControllableAttachment redstoneControllableAttachment) {
                redstoneControllableAttachment.redstoneControl().setPower(power);
            }
        }
    }

    // STATE
    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {

        for (ConnectionType connection : connections) {
            buffer.writeByte(connection.ordinal());
        }
        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {

        for (int i = 0; i < 6; i++) {
            connections[i] = ConnectionType.VALUES[buffer.readByte()];
        }
        requestModelDataUpdate();
    }
    // endregion

    // region NBT
    @Override
    public CompoundTag getUpdateTag() {

        return saveWithoutMetadata();
    }

    @Override
    public void load(CompoundTag tag) {

        super.load(tag);

        redstonePower = tag.getInt(TAG_RS_POWER);

        byte[] bConn = tag.getByteArray(TAG_SIDES);
        if (bConn.length == 6) {
            for (int i = 0; i < 6; ++i) {
                connections[i] = ConnectionType.VALUES[bConn[i]];
            }
        }
        for (int i = 0; i < 6; ++i) {
            if (tag.contains("Attachment" + i)) {
                CompoundTag attachmentTag = tag.getCompound("Attachment" + i);
                attachments[i] = AttachmentRegistry.getAttachment(attachmentTag.getString(TAG_TYPE), attachmentTag, this, DIRECTIONS[i]);
            } else {
                attachments[i] = EmptyAttachment.INSTANCE;
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {

        super.saveAdditional(tag);

        tag.putInt(TAG_RS_POWER, redstonePower);

        byte[] bConn = new byte[6];
        for (int i = 0; i < 6; ++i) {
            bConn[i] = (byte) connections[i].ordinal();
        }
        tag.putByteArray(TAG_SIDES, bConn);

        for (int i = 0; i < 6; ++i) {
            CompoundTag attachmentTag = new CompoundTag();
            attachments[i].write(attachmentTag);
            if (!attachmentTag.isEmpty()) {
                tag.put("Attachment" + i, attachmentTag);
            }
        }
    }
    // endregion

    // region IGridHost
    @Override
    public final Level getHostWorld() {

        return world();
    }

    @Override
    public final BlockPos getHostPos() {

        return pos();
    }

    @Override
    public boolean hasGrid() {

        return grid != null;
    }

    @Override
    public final G getGrid() {

        if (level.isClientSide) {
            throw new UnsupportedOperationException("No grid representation on client.");
        }
        if (grid == null) {
            IGridContainer gridContainer = IGridContainer.getCapability(level);
            assert gridContainer != null;
            grid = gridContainer.getGrid(getGridType(), getBlockPos());
        }
        assert grid != null;
        return grid;
    }

    @Override
    public final void setGrid(G grid) {

        if (level.isClientSide) {
            throw new UnsupportedOperationException("No grid representation on client.");
        }
        this.grid = grid;
    }

    @Override
    public void neighborChanged(Block blockIn, BlockPos fromPos) {

        if (level != null) {
            redstonePower = level.getBestNeighborSignal(worldPosition);
            for (IAttachment attachment : attachments) {
                if (attachment instanceof IRedstoneControllableAttachment redstoneControllableAttachment) {
                    redstoneControllableAttachment.redstoneControl().setPower(redstonePower);
                }
            }
            TileRedstonePacket.sendToClient(this);
        }
    }

    @Override
    public void onAttachmentUpdate() {

        setChanged();
        callNeighborStateChange();
        ModelUpdatePacket.sendToClient(getLevel(), getBlockPos());
    }

    @Nonnull
    @Override
    public IAttachment getAttachment(Direction dir) {

        return attachments[dir.ordinal()];
    }

    @Override
    public boolean canConnectTo(IDuct<?, ?> other, Direction dir) {

        return IDuct.super.canConnectTo(other, dir) && connections[dir.ordinal()].allowDuctConnection();
    }

    @Override
    public ConnectionType getConnectionType(Direction dir) {

        return connections[dir.ordinal()];
    }

    @Override
    public void setConnectionType(Direction dir, ConnectionType type) {

        connections[dir.ordinal()] = type;
        setChanged();
        callNeighborStateChange();
        TileStatePacket.sendToClient(this);
    }
    // endregion

    // region ILocationAccess
    @Override
    public Block block() {

        return getBlockState().getBlock();
    }

    @Override
    public BlockState state() {

        return getBlockState();
    }

    @Override
    public BlockPos pos() {

        return worldPosition;
    }

    @Override
    public Level world() {

        return level;
    }
    // endregion

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction side) {

        if (side == null || level == null || level.isClientSide || connections[side.ordinal()] == DISABLED) {
            return LazyOptional.empty();
        }
        return attachments[side.ordinal()].wrapGridCapability(cap, getGrid().getCapability(cap));
    }

}
