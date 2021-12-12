package cofh.thermal.dynamics.api.internal;

import cofh.thermal.dynamics.api.grid.Grid;
import cofh.thermal.dynamics.api.grid.GridNode;

/**
 * @author covers1624
 */
public interface TickableGridNode<G extends Grid<?, ?>> extends GridNode<G> {

    void tick();
}
