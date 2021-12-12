package cofh.thermal.dynamics.api.internal;

import cofh.thermal.dynamics.api.grid.Grid;
import cofh.thermal.dynamics.api.grid.GridHost;
import cofh.thermal.dynamics.api.grid.GridType;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * @author covers1624
 */
public interface GridHostInternal extends GridHost {

    void setGrid(Grid<?, ?> grid);

    Set<GridType<?>> getExposedTypes();

    @Nullable
    UUID getLastGrid();

}
