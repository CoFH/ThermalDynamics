package cofh.thermal.dynamics.api.internal;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridNode;

/**
 * Represents a {@link IGridNode} capable of receiving world ticks.
 *
 * @author covers1624
 */
public interface ITickableGridNode<G extends IGrid<?, ?>> extends IGridNode<G> {

    void tick();

}
