package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.grid.multi.IMultiGrid;
import cofh.thermal.dynamics.api.grid.multi.IMultiGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;

import javax.annotation.Nullable;
import java.util.Set;

/**
 * Represents an object capable of hosting a {@link IGrid}.
 * <p>
 * These are usually {@link BlockEntity} instances.
 * <p>
 * This interface may be implemented directly on the {@link BlockEntity},
 * or exposed via a {@link Capability}.
 *
 * @author covers1624
 * @see TDynApi#GRID_HOST_CAPABILITY
 */
public interface IGridHost {

    Level getHostWorld();

    BlockPos getHostPos();

    /**
     * Gets the raw {@link IGrid} hosted by this {@link IGridHost}.
     *
     * @return The raw grid.
     */
    @Nullable
    IGrid<?, ?> getGrid();

    void setGrid(IGrid<?, ?> grid);

    Set<IGridType<?>> getExposedTypes();

    /**
     * Gets the {@link IGridNode} hosted by this grid host.
     *
     * @return The {@link IGridNode}.
     */
    @Nullable
    default IGridNode<?> getNode() {

        IGrid<?, ?> grid = getGrid();
        if (grid == null) return null;
        return grid.getNodes().get(getHostPos());
    }

    /**
     * Tries to get the {@link IGridNode} hosted by this {@link IGridHost} of the given type.
     * <p>
     * If the hosted grid is a {@link IMultiGrid}, the {@link IMultiGridNode} will be queried
     * for a grid of the given type.
     *
     * @param gridType The {@link IGridType}
     * @return The {@link IGridNode}
     */
    @Nullable
    default <G extends IGrid<?, ?>> IGridNode<G> getNode(IGridType<G> gridType) {

        IGridNode<?> node = getNode();
        if (node == null) return null;

        if (node.getGrid().getGridType() == TDynReferences.MULTI_GRID) {
            IMultiGridNode multiGridNode = (IMultiGridNode) node;
            return multiGridNode.getSubGrid(gridType);
        }
        if (node.getGrid().getGridType() != gridType) return null;

        //noinspection unchecked
        return (IGridNode<G>) node;
    }

    /**
     * Checks if {@code other} can connect to this grid host.
     * <p>
     * Standard {@code equals} semantics apply, reversing the inputs
     * should result in the same output.
     * <p>
     * If this method is called with the current host, it
     * should always return true.
     * <p>
     * NOTE: {@code other} may not be connected to the same grid!
     *
     * @param other The other host.
     * @return If they can connect.
     */
    default boolean canConnectTo(IGridHost other) {

        return getExposedTypes().equals(other.getExposedTypes());
    }

}
