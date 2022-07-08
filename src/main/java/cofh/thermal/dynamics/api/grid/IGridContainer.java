package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

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

    static Optional<IGridContainer> getCapability(IWorld _world) {

        if (!(_world instanceof World)) {
            return Optional.empty();
        }
        World world = (World) _world;
        return world.getCapability(TDynApi.GRID_CONTAINER_CAPABILITY).resolve();
    }

}
