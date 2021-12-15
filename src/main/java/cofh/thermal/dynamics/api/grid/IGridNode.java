package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.grid.node.INodeAttachment;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Represents a Node on a {@link IGrid} at a given position.
 * <p>
 *
 * @author covers1624
 */
public interface IGridNode<G extends IGrid<?, ?>> {

    /**
     * Gets the grid which this node belongs to.
     *
     * @return The grid.
     */
    G getGrid();

    /**
     * Gets the position in world this {@link IGridNode} exists in.
     *
     * @return The node's position.
     */
    BlockPos getPos();

    /**
     * The connections this node has to other blocks within the grid.
     * <p>
     * These may not be grid nodes.
     *
     * @return The directions this node has internal connections to.
     */
    EnumSet<Direction> getInternalConnections();

    /**
     * The external connections this Node has.
     *
     * @return The directions this node has external connections to.
     */
    EnumSet<Direction> getExternalConnections();

    /**
     * The attachments this node provides for each face.
     *
     * @return The attachments.
     */
    EnumMap<Direction, INodeAttachment> getAttachments();

}
