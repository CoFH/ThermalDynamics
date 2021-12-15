package cofh.thermal.dynamics.api.grid.multi;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridNode;
import cofh.thermal.dynamics.api.grid.IGridType;

import java.util.Optional;

/**
 * @author covers1624
 */
public interface IMultiGridNode extends IGridNode<IMultiGrid> {

    <G extends IGrid<?, ?>> Optional<IGridNode<G>> getSubGrid(IGridType<G> type);

}
