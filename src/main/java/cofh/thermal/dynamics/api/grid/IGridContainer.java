package cofh.thermal.dynamics.api.grid;

import cofh.thermal.dynamics.api.TDynApi;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;

import java.util.Optional;
import java.util.UUID;

/**
 * @author covers1624
 */
//TODO, remove this from the public api.
public interface IGridContainer {

    Optional<IGrid<?, ?>> getGrid(UUID id);

    void onGridHostPlaced(IGridHostInternal host);

    void onGridHostDestroyed(IGridHostInternal host);

    static Optional<IGridContainer> getCapability(IWorld _world) {

        if (!(_world instanceof World)) return Optional.empty();

        World world = (World) _world;
        return world.getCapability(TDynApi.GRID_CONTAINER_CAPABILITY).resolve();
    }

}
