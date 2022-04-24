package cofh.thermal.dynamics.grid.fluid;

import cofh.thermal.dynamics.api.grid.fluid.IFluidGrid;
import cofh.thermal.dynamics.api.grid.fluid.IFluidGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

/**
 * @author King Lemming
 */
public class FluidGridNode extends AbstractGridNode<IFluidGrid> implements IFluidGridNode {

    protected Direction[] distArray = new Direction[0];
    protected int distIndex = 0;

    protected FluidGridNode(IFluidGrid grid) {

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
                tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, distArray[i].getOpposite())
                        .ifPresent(e -> grid.drain(e.fill(grid.getFluid(), EXECUTE), EXECUTE));
            }
            for (int i = 0; i < distIndex; ++i) {
                TileEntity tile = world.getBlockEntity(pos.relative(distArray[i]));
                if (tile == null) {
                    continue; // Ignore non-tiles.
                }
                tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, distArray[i].getOpposite())
                        .ifPresent(e -> grid.drain(e.fill(grid.getFluid(), EXECUTE), EXECUTE));
            }
            ++distIndex;
            distIndex %= distArray.length;
        }
    }

}
