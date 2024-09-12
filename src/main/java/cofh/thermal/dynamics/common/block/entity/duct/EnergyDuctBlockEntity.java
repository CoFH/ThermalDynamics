package cofh.thermal.dynamics.common.block.entity.duct;

import cofh.core.util.helpers.EnergyHelper;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.common.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.common.grid.energy.EnergyGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static cofh.thermal.dynamics.init.registries.TDynBlockEntities.ENERGY_DUCT_BLOCK_ENTITY;
import static cofh.thermal.dynamics.init.registries.TDynGrids.ENERGY_GRID;

public class EnergyDuctBlockEntity extends DuctBlockEntity<EnergyGrid, EnergyGridNode> {

    public EnergyDuctBlockEntity(BlockPos pos, BlockState state) {

        super(ENERGY_DUCT_BLOCK_ENTITY.get(), pos, state);
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
        return EnergyHelper.hasEnergyHandlerCap(tile, dir.getOpposite());
    }

    @Override
    public IGridType<EnergyGrid> getGridType() {

        return ENERGY_GRID.get();
    }

}
