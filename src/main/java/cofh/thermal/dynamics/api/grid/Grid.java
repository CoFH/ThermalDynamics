package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.grid.AbstractGrid;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a Grid of nodes in a World.
 * <p>
 * This may not be a complete grid as sub-sections may be unloaded/loaded
 * as chunks load/unload.
 * In these cases, it acts as if these unloaded sections of the grid do not exist.
 * <p>
 * Every grid implementor is expected to extend {@link AbstractGrid}, this interface
 * exists for those who want to consume the Grid system. Modifications of the Grid system
 * outside well-defined methods on this interface are forbidden. Any such modifications may
 * cause the Grid to explode.
 * <p>
 *
 * @author covers1624
 */
public interface Grid<G extends Grid<?, ?>, N extends GridNode<?>> {

    /**
     * Gets the UUID that represents this Grid.
     * <p>
     * The primary use for this is to uniquely identify this grid for client <-> server communication.
     * <p>
     * Guaranteed to be unique over the lifetime of the grid object.
     * Guaranteed to be unique over a single play-session.
     * No other guarantees are made.
     *
     * @return The Grid id.
     */
    UUID getId();

    /**
     * Gets the world this grid is a prt of.
     *
     * @return The world.
     */
    World getWorld();

    /**
     * Gets the type of this grid.
     * <p>
     * {@link GridType} can be considered similar to {@link TileEntityType} where
     * it is a unique key and Factory.
     *
     * @return The type of this grid.
     * @see GridType
     */
    GridType<G> getGridType();

    /**
     * Returns a Map of all nodes in the Grid.
     *
     * @return The nodes.
     */
    Map<BlockPos, N> getNodes();

    default boolean canConnectToAdjacent(BlockPos pos) {

        for (Direction dir : Direction.values()) {
            if (canConnectExternally(pos.relative(dir), dir)) {
                return true;
            }
            if (canConnectExternally(pos.relative(dir), null)) {
                return true;
            }
        }
        return false;
    }

    default boolean canConnectExternally(BlockPos pos, @Nullable Direction dir) {

        TileEntity tile = getWorld().getBlockEntity(pos);
        if (tile == null) return false;

        return canConnectExternally(tile, dir);
    }

    boolean canConnectExternally(TileEntity tile, @Nullable Direction dir);

}
