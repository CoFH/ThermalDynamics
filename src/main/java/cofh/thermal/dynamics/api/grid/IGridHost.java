package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.grid.multi.IMultiGrid;
import cofh.thermal.dynamics.api.grid.multi.IMultiGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

/**
 * @author covers1624
 */
public interface IGridHost {

    World getHostWorld();

    BlockPos getHostPos();

    /**
     * Gets the raw {@link IGrid} hosted by this {@link IGridHost}.
     *
     * @return The raw grid.
     */
    Optional<IGrid<?, ?>> getGrid();

    /**
     * Gets the {@link IGridNode} hosted by this grid host.
     *
     * @return The {@link IGridNode}.
     */
    default Optional<IGridNode<?>> getNode() {

        Optional<IGrid<?, ?>> gridOpt = getGrid();
        return gridOpt.map(grid -> grid.getNodes().get(getHostPos()));
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
    default <G extends IGrid<?, ?>> Optional<IGridNode<G>> getNode(IGridType<G> gridType) {

        Optional<IGridNode<?>> nodeOpt = getNode();
        if (!nodeOpt.isPresent()) return Optional.empty();
        IGridNode<?> node = nodeOpt.get();
        if (node.getGrid().getGridType() == TDynReferences.MULTI_GRID) {
            IMultiGridNode multiGridNode = (IMultiGridNode) node;
            return multiGridNode.getSubGrid(gridType);
        }
        if (node.getGrid().getGridType() != gridType) return Optional.empty();

        //noinspection unchecked
        return Optional.of((IGridNode<G>) node);
    }

}
