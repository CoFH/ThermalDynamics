package cofh.thermal.dynamics.block.entity;

import cofh.core.network.packet.client.TileStatePacket;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.lib.api.block.entity.ITileLocation;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import cofh.thermal.dynamics.grid.Grid;
import cofh.thermal.dynamics.grid.GridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

import static cofh.lib.util.constants.NBTTags.TAG_SIDES;
import static cofh.thermal.dynamics.api.grid.IGridHost.ConnectionType.ALLOWED;
import static cofh.thermal.dynamics.api.grid.IGridHost.ConnectionType.DISABLED;
import static net.covers1624.quack.util.SneakyUtils.notPossible;

public abstract class DuctTileBase<G extends Grid<G, N>, N extends GridNode<G>> extends BlockEntity implements IGridHost<G, N>, ITileLocation, IPacketHandlerTile {

    // Only available server side.
    @Nullable
    protected G grid = null;

    protected ConnectionType[] connections = { ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED };

    protected final DuctModelData modelData = new DuctModelData();
    protected boolean modelUpdate;

    public DuctTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);
    }

    public boolean attemptConnect(Direction dir) {

        IGridHost<?, ?> adjacent = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacent instanceof DuctTileBase other) {
            connections[dir.ordinal()] = ALLOWED;
            other.connections[dir.getOpposite().ordinal()] = ALLOWED;

            IGridContainer.getCapability(level).ifPresent(e -> e.onGridHostSideConnected(this, dir));

            setChanged();
            other.setChanged();

            TileStatePacket.sendToClient(this);
            TileStatePacket.sendToClient(other);
        } else {
            System.out.println("Attempt to connect to BLOCK on: " + dir);
            connections[dir.ordinal()] = ALLOWED;
            setChanged();
            callNeighborStateChange();
            TileStatePacket.sendToClient(this);
        }
        return false;
    }

    public boolean attemptDisconnect(Direction dir) {

        IGridHost<?, ?> adjacent = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacent instanceof DuctTileBase<?, ?> other) { // TODO, This should be moved up to IGridHost as a common implementation for (eventual) multiparts.
            IGridContainer.getCapability(level).ifPresent(e -> e.onGridHostSideDisconnecting(this, dir));

            connections[dir.ordinal()] = DISABLED;
            other.connections[dir.getOpposite().ordinal()] = DISABLED;

            setChanged();
            other.setChanged();

            TileStatePacket.sendToClient(this);
            TileStatePacket.sendToClient(other);
        } else {
            System.out.println("Attempt to sever BLOCK connection on: " + dir);
            connections[dir.ordinal()] = DISABLED;
            setChanged();
            callNeighborStateChange();
            TileStatePacket.sendToClient(this);
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
            for (Direction dir : Direction.values()) {
                IGridHost<?, ?> adjacent = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
                if (adjacent != null) {
                    modelData.setInternalConnection(dir, canConnectTo(adjacent, dir) && adjacent.canConnectTo(this, dir.getOpposite()));
                } else {
                    modelData.setInternalConnection(dir, false);
                }
                modelData.setExternalConnection(dir, canConnectToBlock(dir));
            }
            modelUpdate = false;
        }
        return modelData;
    }

    // region NBT

    @Override
    public CompoundTag getUpdateTag() {

        return saveWithoutMetadata();
    }

    @Override
    public void load(CompoundTag tag) {

        super.load(tag);

        byte[] bConn = tag.getByteArray(TAG_SIDES);
        if (bConn.length == 6) {
            for (int i = 0; i < 6; ++i) {
                connections[i] = ConnectionType.VALUES[bConn[i]];
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {

        super.saveAdditional(tag);

        byte[] bConn = new byte[6];
        for (int i = 0; i < 6; ++i) {
            bConn[i] = (byte) connections[i].ordinal();
        }
        tag.putByteArray(TAG_SIDES, bConn);
    }
    // endregion

    // region IGridHost
    @Override
    public final Level getHostWorld() {

        return getLevel();
    }

    @Override
    public final BlockPos getHostPos() {

        return getBlockPos();
    }

    @Override
    public final G getGrid() {

        if (level.isClientSide) {
            throw new UnsupportedOperationException("No grid representation on client.");
        }
        if (grid == null) {
            IGridContainer gridContainer = IGridContainer.getCapability(level)
                    .orElseThrow(notPossible());
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
    public boolean canConnectTo(IGridHost<?, ?> other, Direction dir) {

        return IGridHost.super.canConnectTo(other, dir) && connections[dir.ordinal()].allowDuctConnection();
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

    // region STATE

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

}
