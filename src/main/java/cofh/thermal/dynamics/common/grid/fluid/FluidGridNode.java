package cofh.thermal.dynamics.common.grid.fluid;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.ITickableGridNode;
import cofh.thermal.dynamics.common.attachment.IAttachment;
import cofh.thermal.dynamics.common.grid.GridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import static cofh.lib.util.Constants.DIRECTIONS;
import static cofh.thermal.dynamics.api.grid.IDuct.ConnectionType.DISABLED;
import static net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

public class FluidGridNode extends GridNode<FluidGrid> implements ITickableGridNode {

    protected Direction[] distArray = new Direction[0];
    protected int distIndex = 0;

    protected FluidGridNode(FluidGrid grid) {

        super(grid);
    }

    protected void cacheConnections() {

        for (Direction dir : DIRECTIONS) {
            if (grid.canConnectOnSide(pos.relative(dir), dir.getOpposite())) {
                connections.add(dir);
            }
        }
        distArray = connections.toArray(new Direction[0]);
        cached = true;
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
        IFluidHandler handler = attachment.wrapExternalCapability(Capabilities.FluidHandler.BLOCK,
                world.getCapability(Capabilities.FluidHandler.BLOCK, tile.getBlockPos(), tile.getBlockState(), tile, dir.getOpposite()));
        if (handler != null) {
            grid.drain(handler.fill(grid.getFluid(), EXECUTE), EXECUTE);
        }
    }

}
