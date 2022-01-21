package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.api.internal.ITickableGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

/**
 * @author covers1624
 */
public class EnergyGridNode extends AbstractGridNode<IEnergyGrid> implements IEnergyGridNode, ITickableGridNode<IEnergyGrid> {

    protected EnergyGridNode(IEnergyGrid grid) {

        super(grid);
    }

    @Override
    protected boolean isExternallyConnectable(Direction side) {

        TileEntity tile = getWorld().getBlockEntity(getPos().relative(side));
        if (tile == null) {
            return false;
        }
        if (GridHelper.getGridHost(tile).isPresent()) {
            return false; // We cannot externally connect to other grids.
        }
        if (tile.getCapability(CapabilityEnergy.ENERGY).isPresent()) {
            return true; // We can(not) connect to the inner face
        }
        if (tile.getCapability(CapabilityEnergy.ENERGY, side.getOpposite()).isPresent()) {
            return true; // We can connect to the face
        }
        return false; // nope
    }

    @Override
    public void tick() {

        World world = getWorld();
        BlockPos pos = getPos();

        //System.out.println("tick!");

        for (Direction dir : getExternalConnections()) {
            TileEntity tile = world.getBlockEntity(pos.relative(dir));
            if (tile == null) {
                continue; // Ignore non-tiles.
            }
            //System.out.println("tile!");

            LazyOptional<IEnergyStorage> innerCap = tile.getCapability(CapabilityEnergy.ENERGY);
            LazyOptional<IEnergyStorage> faceCap = tile.getCapability(CapabilityEnergy.ENERGY, dir.getOpposite());
            if (!innerCap.isPresent() && !faceCap.isPresent()) {
                continue;
            }
            if (GridHelper.getGridHost(tile).isPresent()) {
                continue; // Ignore other grids.
            }
            // TODO subtract from grid storage, determine proper amount to send
        }
    }

}
