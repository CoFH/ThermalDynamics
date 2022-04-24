package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author covers1624
 */
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

        World world = getWorld();
        BlockPos pos = getPos();

        if (!cached) {
            cacheConnections();
        }
        if (distArray.length > 0) {
            for (int i = distIndex; i < distArray.length; ++i) {
                TileEntity tile = world.getBlockEntity(pos.relative(distArray[i]));
                if (tile == null) {
                    continue; // Ignore non-tiles.
                }
                tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), distArray[i].getOpposite())
                        .ifPresent(e -> grid.extractEnergy(e.receiveEnergy(grid.getEnergyStored(), false), false));
            }
            for (int i = 0; i < distIndex; ++i) {
                TileEntity tile = world.getBlockEntity(pos.relative(distArray[i]));
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
