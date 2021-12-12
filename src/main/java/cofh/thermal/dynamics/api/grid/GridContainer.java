package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDApi;
import cofh.thermal.dynamics.api.internal.GridHostInternal;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * @author covers1624
 */
//TODO, remove this from the public api.
public interface GridContainer {

    Optional<Grid<?, ?>> getGrid(UUID id);

    void onGridHostPlaced(GridHostInternal host);

    void onGridHostDestroyed(GridHostInternal host);

    static Optional<GridContainer> getCapability(IWorld _world) {
        if (!(_world instanceof World)) return Optional.empty();

        World world = (World) _world;
        return world.getCapability(TDApi.GRID_CONTAINER_CAPABILITY).resolve();
    }
}
