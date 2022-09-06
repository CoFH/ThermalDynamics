package cofh.thermal.dynamics.grid.fluid;

import cofh.thermal.dynamics.api.grid.fluid.IFluidGrid;
import cofh.thermal.dynamics.api.grid.fluid.IFluidGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;

import static net.minecraftforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE;

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
                tile.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY, distArray[i].getOpposite())
                        .ifPresent(e -> grid.drain(e.fill(grid.getFluid(), EXECUTE), EXECUTE));
            }
            for (int i = 0; i < distIndex; ++i) {
                BlockEntity tile = world.getBlockEntity(pos.relative(distArray[i]));
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
