package cofh.thermal.dynamics.grid.energy;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.ITickableGridNode;
import cofh.thermal.dynamics.attachment.IAttachment;
import cofh.thermal.dynamics.grid.GridNode;
import cofh.thermal.lib.util.ThermalEnergyHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static cofh.lib.util.Constants.DIRECTIONS;
import static cofh.thermal.dynamics.api.grid.IDuct.ConnectionType.DISABLED;

public class EnergyGridNode extends GridNode<EnergyGrid> implements ITickableGridNode {

    protected Direction[] distArray = new Direction[0];
    protected int distIndex = 0;

    protected EnergyGridNode(EnergyGrid grid) {

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
    public void attachmentTick() {

        IDuct<?, ?> duct = gridHost();
        if (duct == null) {
            return;
        }
        for (Direction dir : DIRECTIONS) {
            duct.getAttachment(dir).tick();
        }
    }

    @Override
    public void distributionTick() {

        if (!cached) {
            cacheConnections();
        }
        IDuct<?, ?> duct = gridHost();

        if (duct != null && distArray.length > 0) {
            ++distIndex;
            distIndex %= distArray.length;
            Level world = getWorld();

            for (int i = distIndex; i < distArray.length; ++i) {
                tickDir(world, pos, duct, distArray[i]);
            }
            for (int i = 0; i < distIndex; ++i) {
                tickDir(world, pos, duct, distArray[i]);
            }
        }
    }

    private void tickDir(Level world, BlockPos pos, IDuct<?, ?> duct, Direction dir) {

        if (duct.getConnectionType(dir) == DISABLED) {
            return;
        }
        IAttachment attachment = duct.getAttachment(dir);
        BlockEntity tile = world.getBlockEntity(pos.relative(dir));
        if (tile == null) {
            return;
        }
        attachment.wrapExternalCapability(ThermalEnergyHelper.getBaseEnergySystem(),
                        tile.getCapability(ThermalEnergyHelper.getBaseEnergySystem(), dir.getOpposite()).cast())
                .ifPresent(e -> grid.extractEnergy(e.receiveEnergy(grid.getEnergyStored(), false), false));
    }

}
