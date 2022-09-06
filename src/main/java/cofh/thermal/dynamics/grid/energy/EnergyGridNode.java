package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class EnergyGridNode extends AbstractGridNode<IEnergyGrid> implements IEnergyGridNode {

    protected Direction[] distArray = new Direction[0];
    protected int distIndex = 0;

    protected EnergyGridNode(IEnergyGrid grid) {

        super(grid);
    }

    protected void cacheConnections() {

        for (Direction dir : Direction.values()) {
            if (grid.canConnectOnSide(pos.relative(dir), dir.getOpposite())) {
                connections.add(dir);
            }
        }
        distArray = connections.toArray(new Direction[0]);
    }

    @Override
    public void tick() {

        Level world = getWorld();
        BlockPos pos = getPos();

        if (!cached) {
            cacheConnections();
        }
        if (distArray.length > 0) {
            for (int i = distIndex; i < distArray.length; ++i) {
                BlockEntity tile = world.getBlockEntity(pos.relative(distArray[i]));
                if (tile == null) {
                    continue; // Ignore non-tiles.
                }
                tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), distArray[i].getOpposite())
                        .ifPresent(e -> grid.extractEnergy(e.receiveEnergy(grid.getEnergyStored(), false), false));
            }
            for (int i = 0; i < distIndex; ++i) {
                BlockEntity tile = world.getBlockEntity(pos.relative(distArray[i]));
                if (tile == null) {
                    continue; // Ignore non-tiles.
                }
                tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), distArray[i].getOpposite())
                        .ifPresent(e -> grid.extractEnergy(e.receiveEnergy(grid.getEnergyStored(), false), false));
            }
            ++distIndex;
            distIndex %= distArray.length;
        }
    }

}
