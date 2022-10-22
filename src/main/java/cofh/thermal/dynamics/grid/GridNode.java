package cofh.thermal.dynamics.grid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.EnumSet;

/**
 * Represents a Node on a {@link Grid} at a given position.
 * <p>
 *
 * @author covers1624
 */
public abstract class GridNode<G extends Grid<G, ?>> implements INBTSerializable<CompoundTag> {

    //    protected final EnumMap<Direction, INodeAttachment> attachments = new EnumMap<>(Direction.class);
    protected final EnumSet<Direction> connections = EnumSet.noneOf(Direction.class);
    protected G grid;
    protected BlockPos pos = BlockPos.ZERO;
    protected boolean loaded;
    protected boolean cached;

    protected GridNode(G grid) {

        this.grid = grid;
    }

    @Override
    public CompoundTag serializeNBT() {

        return new CompoundTag();
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

    }

    public void onGridChange(G oldGrid) {

    }

    public final void clearConnections() {

        connections.clear();
        cached = false;
    }

    /**
     * Gets the grid which this node belongs to.
     *
     * @return The grid.
     */
    public final G getGrid() {

        return grid;
    }

    /**
     * Gets the position in world this {@link GridNode} exists in.
     *
     * @return The node's position.
     */
    public final BlockPos getPos() {

        return pos;
    }

    /**
     * Flag returning if the node is loaded.
     *
     * @return The node's loaded state.
     */
    public final boolean isLoaded() {

        return loaded;
    }

    /**
     * The external connections this Node has.
     *
     * @return The directions this node has external connections to.
     */
    public final EnumSet<Direction> getConnections() {

        return connections;
    }

    //    /**
    //     * The attachments this node provides for each face.
    //     *
    //     * @return The attachments.
    //     */
    //    public final EnumMap<Direction, INodeAttachment> getAttachments() {
    //        return attachments;
    //    }

    //@formatter:off
    public final Level getWorld() { return grid.getLevel(); }
    public void setPos(BlockPos pos) { this.pos = pos; }
    public void setGrid(G grid) { this.grid = grid; }
    public void setLoaded(boolean loaded) { this.loaded = loaded; }
    //@formatter:on
}
