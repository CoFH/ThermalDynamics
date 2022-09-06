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

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.util.SneakyUtils.notPossible;

public abstract class DuctTileBase extends BlockEntity implements ITileLocation, IGridHost {

    // Only available server side.
    @Nullable
    protected IGrid<?, ?> grid = null;

    protected final DuctModelData modelData = new DuctModelData();
    protected boolean modelUpdate;

    public DuctTileBase(BlockEntityType<?> type) {

        super(type);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {

        return super.save(tag);
    }

    @Override
    public void load(BlockState state, CompoundTag tag) {

        super.load(state, tag);
    }

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

        if (level.isClientSide) throw new UnsupportedOperationException("No grid representation on client.");
        if (grid == null) {
            IGridContainer gridContainer = IGridContainer.getCapability(level)
                    .orElseThrow(notPossible());
            grid = requireNonNull(gridContainer.getGrid(getBlockPos()));
        }
        return grid;
    }

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

    @Override
    public void setGrid(IGrid<?, ?> grid) {

        if (level.isClientSide) throw new UnsupportedOperationException("No grid representation on client.");
        this.grid = grid;
    }

    protected abstract boolean canConnect(Direction dir);

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
