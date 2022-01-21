package cofh.thermal.dynamics.tileentity;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import cofh.thermal.dynamics.client.DuctModelData;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.util.SneakyUtils.notPossible;

public abstract class DuctTileBase extends TileEntity implements IGridHostInternal {

    // Only available server side.
    private Optional<IGrid<?, ?>> grid = Optional.empty();

    private final DuctModelData modelData = new DuctModelData();

    public DuctTileBase() {

        super(TDynReferences.ENERGY_DUCT_TILE);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {

        return super.save(tag);
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {

        super.load(state, tag);
    }

    @Override
    public World getHostWorld() {

        return getLevel();
    }

    @Override
    public BlockPos getHostPos() {

        return getBlockPos();
    }

    @Override
    public Optional<IGrid<?, ?>> getGrid() {

        if (level.isClientSide) throw new UnsupportedOperationException("No grid representation on client.");
        if (!grid.isPresent()) {
            IGridContainer gridContainer = IGridContainer.getCapability(level)
                    .orElseThrow(notPossible());
            grid = Optional.of(requireNonNull(gridContainer.getGrid(getBlockPos())));
        }

        return grid;
    }

    @Nonnull
    @Override
    public IModelData getModelData() {

        for (Direction dir : Direction.values()) {
            Optional<IGridHost> gridHostOpt = GridHelper.getGridHost(getLevel(), getBlockPos().relative(dir));
            if (gridHostOpt.isPresent()) {
                IGridHostInternal gridHost = (IGridHostInternal) gridHostOpt.get();
                modelData.setInternalConnection(dir, gridHost.getExposedTypes().equals(getExposedTypes()));
            } else {
                modelData.setInternalConnection(dir, false);
            }
            modelData.setExternalConnection(dir, canConnectExternally(dir));
        }

        return modelData;
    }

    @Override
    public void setGrid(IGrid<?, ?> grid) {

        if (level.isClientSide) throw new UnsupportedOperationException("No grid representation on client.");
        this.grid = Optional.of(grid);
    }

    protected abstract boolean canConnectExternally(Direction dir);

}
