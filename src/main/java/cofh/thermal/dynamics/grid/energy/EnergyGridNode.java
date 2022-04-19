package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
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
            if (isConnectable(dir)) {
                connections.add(dir);
            }
        }
    }

    @Override
    protected boolean isConnectable(Direction side) {

        TileEntity tile = getWorld().getBlockEntity(getPos().relative(side));
        if (tile == null) {
            return false;
        }
        if (GridHelper.getGridHost(tile).isPresent()) {
            return false; // We cannot externally connect to other grids.
        }
        //        if (tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem()).isPresent()) {
        //            return true; // We can(not) connect to the inner face
        //        }
        if (tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), side.getOpposite()).isPresent()) {
            return true; // We can connect to the face
        }
        return false; // nope
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
            Direction opposite = dir.getOpposite();
            int maxTransfer = grid.getEnergyStored();
            tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), opposite)
                    .ifPresent(e -> grid.extractEnergy(e.receiveEnergy(maxTransfer, false), false));
        }
    }

}
