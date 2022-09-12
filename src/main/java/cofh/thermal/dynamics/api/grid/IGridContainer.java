package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.UUID;

/**
 * @author covers1624
 */
//TODO, remove this from the public api.
public interface IGridContainer {

    @Nullable
    IGrid<?, ?> getGrid(UUID id);

    @Nullable
    IGrid<?, ?> getGrid(BlockPos pos);

    void onGridHostPlaced(IGridHost host);

    void onGridHostRemoved(IGridHost host);

    void onGridHostNeighborChanged(IGridHost host);

    void onGridHostConnectabilityChanged(IGridHost host);

    static Optional<IGridContainer> getCapability(LevelAccessor la) {

        if (!(la instanceof Level level)) return Optional.empty();
        return level.getCapability(TDynApi.GRID_CONTAINER_CAPABILITY).resolve();
    }

}
