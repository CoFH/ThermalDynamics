package cofh.thermal.dynamics.block.entity;

import cofh.lib.block.entity.ITileLocation;
import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
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
import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.util.SneakyUtils.notPossible;

public abstract class DuctTileBase extends BlockEntity implements ITileLocation, IGridHost {

    // Only available server side.
    @Nullable
    protected IGrid<?, ?> grid = null;

    protected ConnectionType[] connections = {ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED, ALLOWED};

    protected final DuctModelData modelData = new DuctModelData();
    protected boolean modelUpdate;

    public DuctTileBase(BlockEntityType<?> type, BlockPos pos, BlockState state) {

        super(type, pos, state);
    }

    public boolean attemptConnect(Direction dir) {

        System.out.println("Attempt to connect to: " + dir);
        return false;
    }

    public boolean attemptDisconnect(Direction dir) {

        Optional<IGridHost> adjacentOpt = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
        if (adjacentOpt.isPresent()) {
            System.out.println("Attempt to sever DUCT connection on: " + dir);
        } else {
            System.out.println("Attempt to sever BLOCK connection on: " + dir);
        }
        return false;
    }

    protected void attemptConnectInternal(Direction dir) {

    }

    protected void attemptDisconnectInternal(Direction dir) {

    }

    protected abstract boolean canConnect(Direction dir);

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
                    modelData.setInternalConnection(dir, canConnectTo(adjacent));
                } else {
                    modelData.setInternalConnection(dir, false);
                }
                modelData.setExternalConnection(dir, canConnect(dir));
            }
            modelUpdate = false;
        }
        return modelData;
    }

    // region NBT
    @Override
    public void load(CompoundTag tag) {

        super.load(tag);

        byte[] bConn = serializeNBT().getByteArray(TAG_SIDES);
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
    public IGrid<?, ?> getGrid() {

        if (level.isClientSide) {
            throw new UnsupportedOperationException("No grid representation on client.");
        }
        if (grid == null) {
            IGridContainer gridContainer = IGridContainer.getCapability(level)
                    .orElseThrow(notPossible());
            grid = requireNonNull(gridContainer.getGrid(getBlockPos()));
        }
        return grid;
    }

    @Override
    public void setGrid(IGrid<?, ?> grid) {

        if (level.isClientSide) throw new UnsupportedOperationException("No grid representation on client.");
        this.grid = grid;
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
}
