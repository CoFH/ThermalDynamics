package cofh.thermal.dynamics.grid.fluid;

import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.ITickableGridNode;
import cofh.thermal.dynamics.attachment.IAttachment;
import cofh.thermal.dynamics.grid.GridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import static cofh.thermal.dynamics.api.grid.IGridHost.ConnectionType.DISABLED;
import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

public class FluidGridNode extends GridNode<FluidGrid> implements ITickableGridNode {

    protected Direction[] distArray = new Direction[0];
    protected int distIndex = 0;

    protected FluidGridNode(FluidGrid grid) {

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

        if (!cached) {
            cacheConnections();
        }
        IGridHost<?, ?> host = gridHost();

        if (host != null && distArray.length > 0) {
            ++distIndex;
            distIndex %= distArray.length;
            Level world = getWorld();

            for (int i = distIndex; i < distArray.length; ++i) {
                tickDir(world, pos, host, distArray[i]);
            }
            for (int i = 0; i < distIndex; ++i) {
                tickDir(world, pos, host, distArray[i]);
            }
        }
    }

    private void tickDir(Level world, BlockPos pos, IGridHost<?, ?> host, Direction dir) {

        if (host.getConnectionType(dir) == DISABLED) {
            return;
        }
        IAttachment attachment = host.getAttachment(dir);
        attachment.tick();

        BlockEntity tile = world.getBlockEntity(pos.relative(dir));
        if (tile == null) {
            return;
        }
        attachment.wrapExternalCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY,
                        tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, dir.getOpposite()))
                .ifPresent(e -> grid.drain(e.fill(grid.getFluid(), EXECUTE), EXECUTE));
    }

}
