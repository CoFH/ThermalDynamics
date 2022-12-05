package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.attachment.IAttachment;
import cofh.thermal.dynamics.grid.Grid;
import cofh.thermal.dynamics.grid.GridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;

/**
 * Represents an object capable of hosting a {@link Grid}.
 * <p>
 * These are usually {@link BlockEntity} instances.
 * <p>
 * This interface may be implemented directly on the {@link BlockEntity},
 * or exposed via a {@link Capability}.
 *
 * @author covers1624
 * @see TDynApi#GRID_HOST_CAPABILITY
 */
public interface IDuct<G extends Grid<G, N>, N extends GridNode<G>> {

    Level getHostWorld();

    BlockPos getHostPos();

    /**
     * Checks if this duct has a grid attached.
     *
     * @return If a grid is attached.
     */
    boolean hasGrid();

    /**
     * Gets the raw {@link Grid} hosted by this {@link IDuct}.
     *
     * @return The raw grid.
     */
    G getGrid();

    /**
     * Set the {@link Grid}.
     *
     * @param grid The gird to set.
     */
    void setGrid(G grid);

    IGridType<G> getGridType();

    /**
     * Gets the {@link GridNode} hosted by this grid host.
     *
     * @return The {@link GridNode}.
     */
    @Nullable
    default N getNode() {

        return getGrid().getNodes().get(getHostPos());
    }

    default void neighborChanged(Block blockIn, BlockPos fromPos) {

    }

    void onAttachmentUpdate();

    @Nonnull
    IAttachment getAttachment(Direction dir);

    /**
     * Checks if {@code other} can connect to this duct.
     * <p>
     * These 2 ducts are guaranteed to be adjacent to each other.
     * <p>
     * This method should be called bidirectionally whenever used, e.g.
     * host.canConnectTo(other, dir) && other.canConnectTo(host, dir.getOpposite())
     * <p>
     * If this method is called with the current host, it
     * should always return true.
     * <p>
     * NOTE: {@code other} and {@code this} might not be attached to the same underlying grid.
     *
     * @param other The other host.
     * @param dir   The direction from this host, to the other host.
     * @return If they can connect.
     */
    default boolean canConnectTo(IDuct<?, ?> other, Direction dir) {

        return getGridType() == other.getGridType();
    }

    /**
     * Get the {@link ConnectionType} for the duct.
     *
     * @param dir The side.
     * @return The {@link ConnectionType}.
     */
    ConnectionType getConnectionType(Direction dir);

    /**
     * Set the {@link ConnectionType} for the duct.
     *
     * @param dir  The side.
     * @param type The {@link ConnectionType}.
     */
    void setConnectionType(Direction dir, ConnectionType type);

    // region CONNECTION TYPE
    enum ConnectionType implements StringRepresentable {

        ALLOWED("allowed", true, true),
        DISABLED("disabled", false, false),
        FORCED("forced", false, true);

        public static final ConnectionType[] VALUES = values();

        private final String name;
        private final boolean allowDuctConnection;
        private final boolean allowBlockConnection;

        ConnectionType(String name, boolean allowDuctConnection, boolean allowBlockConnection) {

            this.name = name;
            this.allowDuctConnection = allowDuctConnection;
            this.allowBlockConnection = allowBlockConnection;
        }

        public boolean allowDuctConnection() {

            return allowDuctConnection;
        }

        public boolean allowBlockConnection() {

            return allowBlockConnection;
        }

        @Override
        public String toString() {

            return this.getSerializedName();
        }

        @Override
        public String getSerializedName() {

            return this.name;
        }
    }
    // endregion
}
