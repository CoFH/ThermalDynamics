package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.grid.Grid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author covers1624
 */
//TODO, remove this from the public api.
public interface IGridContainer {

    @Nullable
    Grid<?, ?> getGrid(UUID id);

    @Nullable
    <G extends Grid<G, ?>> G getGrid(IGridType<G> gridType, BlockPos pos);

    void onGridHostPlaced(IGridHost<?, ?> host, @Nullable Direction connectionPreference);

    void onGridHostRemoved(IGridHost<?, ?> host);

    void onGridHostNeighborChanged(IGridHost<?, ?> host);

    /**
     * Connect the grid host side.
     * <p>
     * Requires that any modifications to exposed connection state happen before this call.
     *
     * @param host The host to connect.
     * @param side The side to connect.
     * @return If the connection was successful.
     */
    boolean onGridHostSideConnected(IGridHost<?, ?> host, Direction side);

    /**
     * Disconnect the grid host side.
     * <p>
     * Requires that any modifications to exposed connection state happens after this call.
     *
     * @param host The host to disconnect.
     * @param side The side to disconnect.
     */
    void onGridHostSideDisconnecting(IGridHost<?, ?> host, Direction side);

    @Nullable
    static IGridContainer getCapability(LevelAccessor la) {

        if (!(la instanceof Level level)) return null;
        return level.getCapability(TDynApi.GRID_CONTAINER_CAPABILITY).resolve().orElse(null);
    }

}
