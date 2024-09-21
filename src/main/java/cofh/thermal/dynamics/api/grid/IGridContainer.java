package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.common.grid.Grid;
import cofh.thermal.dynamics.common.grid.GridContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @author covers1624
 */
// TODO, remove this from the public api.
public interface IGridContainer {

    @Nullable
    Grid<?, ?> getGrid(UUID id);

    @Nullable
    <G extends Grid<G, ?>> G getGrid(IGridType<G> gridType, BlockPos pos);

    void onDuctPlaced(IDuct<?, ?> duct, @Nullable Direction connectionPreference);

    void onDuctRemoved(IDuct<?, ?> duct);

    boolean onDuctNeighborChanged(IDuct<?, ?> duct);

    /**
     * Connect the grid host side.
     * <p>
     * Requires that any modifications to exposed connection state happen before this call.
     *
     * @param duct The duct to connect.
     * @param side The side to connect.
     * @return If the connection was successful.
     */
    boolean onDuctSideConnected(IDuct<?, ?> duct, Direction side);

    /**
     * Disconnect the grid host side.
     * <p>
     * Requires that any modifications to exposed connection state happens after this call.
     *
     * @param duct The duct to disconnect.
     * @param side The side to disconnect.
     */
    void onDuctSideDisconnecting(IDuct<?, ?> duct, Direction side);

    @Nullable
    static IGridContainer getGrid(LevelAccessor la) {

        if (!(la instanceof ServerLevel level)) return null;

        return GridContainer.getInstance(level);
    }

}
