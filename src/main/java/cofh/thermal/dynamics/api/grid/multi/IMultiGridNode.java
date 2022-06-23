package cofh.thermal.dynamics.api.grid.multi;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridNode;
import cofh.thermal.dynamics.api.grid.IGridType;

import javax.annotation.Nullable;

/**
 * @author covers1624
 */
public interface IMultiGridNode extends IGridNode<IMultiGrid> {

    @Nullable
    <G extends IGrid<?, ?>> IGridNode<G> getSubGrid(IGridType<G> type);

}
