package cofh.thermal.dynamics.tileentity;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.util.SneakyUtils.notPossible;

public abstract class DuctTileBase extends TileEntity implements IGridHostInternal {

    private Optional<IGrid<?, ?>> grid = Optional.empty();

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

        if (!grid.isPresent()) {
            IGridContainer gridContainer = IGridContainer.getCapability(level)
                    .orElseThrow(notPossible());
            grid = Optional.of(requireNonNull(gridContainer.getGrid(getBlockPos())));
        }

        return grid;
    }

    @Override
    public void setGrid(IGrid<?, ?> grid) {

        this.grid = Optional.of(grid);
    }

}
