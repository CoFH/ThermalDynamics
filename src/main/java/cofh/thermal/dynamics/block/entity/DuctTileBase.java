package cofh.thermal.dynamics.block.entity;

import cofh.core.network.packet.client.TileStatePacket;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.lib.api.block.entity.ITileLocation;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import cofh.thermal.dynamics.grid.Grid;
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

public abstract class DuctTileBase extends BlockEntity implements ITileLocation, IGridHost, IPacketHandlerTile {

    // Only available server side.
    @Nullable
    protected Grid<?, ?> grid = null;

    protected ConnectionType[] connections = {ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED};

    protected final DuctModelData modelData = new DuctModelData();
    protected boolean modelUpdate;

    public DuctTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);
    }

    public boolean attemptConnect(Direction dir) {

        Optional<IGridHost> adjacentOpt = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacentOpt.isPresent() && adjacentOpt.get() instanceof DuctTileBase other) {
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

        Optional<IGridHost> adjacentOpt = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacentOpt.isPresent() && adjacentOpt.get() instanceof DuctTileBase other) {
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
                Optional<IGridHost> adjacentOpt = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
                if (adjacentOpt.isPresent()) {
                    IGridHost adjacent = adjacentOpt.get();
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
    public Level getHostWorld() {

        return getLevel();
    }

    @Override
    public BlockPos getHostPos() {

        return getBlockPos();
    }

    @Override
    public Grid<?, ?> getGrid() {

        if (level.isClientSide) {
            throw new UnsupportedOperationException("No grid representation on client.");
        }
        if (grid == null) {
            IGridContainer gridContainer = IGridContainer.getCapability(level)
                    .orElseThrow(notPossible());
            grid = gridContainer.getGrid(getBlockPos());
        }
        assert grid != null;
        return grid;
    }

    @Override
    public void setGrid(Grid<?, ?> grid) {

        if (level.isClientSide) {
            throw new UnsupportedOperationException("No grid representation on client.");
        }
        this.grid = grid;
    }

    @Override
    public boolean canConnectTo(IGridHost other, Direction dir) {

        return connections[dir.ordinal()].allowDuctConnection() && getExposedTypes().equals(other.getExposedTypes());
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
