package cofh.thermal.dynamics.api.grid.multi;

import cofh.thermal.dynamics.api.grid.Grid;
import cofh.thermal.dynamics.api.grid.GridNode;
import cofh.thermal.dynamics.api.grid.GridType;

import java.util.Optional;

/**
 * @author covers1624
 */
public interface MultiGridNode extends GridNode<MultiGrid> {

    <G extends Grid<?, ?>> Optional<GridNode<G>> getSubGrid(GridType<G> type);

}
