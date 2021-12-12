package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.grid.multi.MultiGrid;
import cofh.thermal.dynamics.api.grid.multi.MultiGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * @author covers1624
 */
public interface GridHost {

    World getHostWorld();

    BlockPos getHostPos();

    /**
     * Gets the raw {@link Grid} hosted by this {@link GridHost}.
     *
     * @return The raw grid.
     */
    Optional<Grid<?, ?>> getGrid();

    /**
     * Gets the {@link GridNode} hosted by this grid host.
     *
     * @return The {@link GridNode}.
     */
    default Optional<GridNode<?>> getNode() {

        Optional<Grid<?, ?>> gridOpt = getGrid();
        return gridOpt.map(grid -> grid.getNodes().get(getHostPos()));
    }

    /**
     * Tries to get the {@link GridNode} hosted by this {@link GridHost} of the given type.
     * <p>
     * If the hosted grid is a {@link MultiGrid}, the {@link MultiGridNode} will be queried
     * for a grid of the given type.
     *
     * @param gridType The {@link GridType}
     * @return The {@link GridNode}
     */
    default <G extends Grid<?, ?>> Optional<GridNode<G>> getNode(GridType<G> gridType) {

        Optional<GridNode<?>> nodeOpt = getNode();
        if (!nodeOpt.isPresent()) return Optional.empty();
        GridNode<?> node = nodeOpt.get();
        if (node.getGrid().getGridType() == TDynReferences.MULTI_GRID) {
            MultiGridNode multiGridNode = (MultiGridNode) node;
            return multiGridNode.getSubGrid(gridType);
        }
        if (node.getGrid().getGridType() != gridType) return Optional.empty();

        //noinspection unchecked
        return Optional.of((GridNode<G>) node);
    }

}
