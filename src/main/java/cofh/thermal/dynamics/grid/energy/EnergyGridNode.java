package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.api.internal.ITickableGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * @author covers1624
 */
public class EnergyGridNode extends AbstractGridNode<IEnergyGrid> implements IEnergyGridNode, ITickableGridNode<IEnergyGrid> {

    protected EnergyGridNode(IEnergyGrid grid) {

        super(grid);
    }

    protected void cacheConnections() {

        for (Direction dir : Direction.values()) {
            if (grid.canConnectOnSide(pos.relative(dir), dir.getOpposite())) {
                connections.add(dir);
            }
        }
    }

    @Override
    public void tick() {

        World world = getWorld();
        BlockPos pos = getPos();

        if (!cached) {
            cacheConnections();
        }
        for (Direction dir : getConnections()) {
            TileEntity tile = world.getBlockEntity(pos.relative(dir));
            if (tile == null) {
                continue; // Ignore non-tiles.
            }
            int maxTransfer = grid.getEnergyStored();
            tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), dir.getOpposite())
                    .ifPresent(e -> grid.extractEnergy(e.receiveEnergy(maxTransfer, false), false));
        }
    }

}
