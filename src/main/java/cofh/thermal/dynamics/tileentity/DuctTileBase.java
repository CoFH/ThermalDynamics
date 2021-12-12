package cofh.thermal.dynamics.tileentity;

import cofh.thermal.dynamics.api.grid.Grid;
import cofh.thermal.dynamics.api.internal.GridHostInternal;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.block.BlockState;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

public abstract class DuctTileBase extends TileEntity implements GridHostInternal {

    private Optional<Grid<?, ?>> grid = Optional.empty();
    @Nullable
    private UUID lastGrid = null;

    public DuctTileBase() {
        super(TDynReferences.ENERGY_DUCT_TILE);
    }

    @Override
    public CompoundNBT save(CompoundNBT tag) {
        if (lastGrid != null) {
            tag.putUUID("last_grid", lastGrid);
        }
        return super.save(tag);
    }

    @Override
    public void load(BlockState state, CompoundNBT tag) {
        super.load(state, tag);
        if (tag.hasUUID("last_grid")) {
            lastGrid = tag.getUUID("last_grid");
        }
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
    public Optional<Grid<?, ?>> getGrid() {
        return grid;
    }

    @Override
    public void setGrid(Grid<?, ?> grid) {
        lastGrid = grid.getId();
        this.grid = Optional.of(grid);
    }

    @Nullable
    @Override
    public UUID getLastGrid() {
        return lastGrid;
    }
}
