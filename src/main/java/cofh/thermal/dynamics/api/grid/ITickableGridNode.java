package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.grid.GridNode;

/**
 * Represents a {@link GridNode} capable of receiving world ticks.
 *
 * @author covers1624
 */
public interface ITickableGridNode {

    void attachmentTick();

    void distributionTick();

}
