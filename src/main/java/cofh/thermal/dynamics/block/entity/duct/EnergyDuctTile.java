package cofh.thermal.dynamics.block.entity.duct;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.grid.energy.EnergyGridNode;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static cofh.thermal.dynamics.init.TDynGrids.GRID_ENERGY;
import static cofh.thermal.dynamics.init.TDynTileEntities.DUCT_ENERGY_TILE;

public class EnergyDuctTile extends DuctTileBase<EnergyGrid, EnergyGridNode> {

    public EnergyDuctTile(BlockPos pos, BlockState state) {

        super(DUCT_ENERGY_TILE.get(), pos, state);
    }

    @Override
    protected boolean canConnectToBlock(Direction dir) {

        if (!connections[dir.ordinal()].allowBlockConnection()) {
            return false;
        }
        BlockEntity tile = level.getBlockEntity(getBlockPos().relative(dir));
        if (tile == null || GridHelper.getGridHost(tile) != null) {
            return false;
        }
        return tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), dir.getOpposite()).isPresent();
    }

    @Override
    public IGridType<EnergyGrid> getGridType() {

        return GRID_ENERGY.get();
    }

}
