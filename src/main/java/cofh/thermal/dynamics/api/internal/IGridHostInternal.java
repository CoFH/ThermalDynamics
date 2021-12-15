package cofh.thermal.dynamics.api.internal;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridType;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.UUID;

/**
 * @author covers1624
 */
public interface IGridHostInternal extends IGridHost {

    void setGrid(IGrid<?, ?> grid);

    Set<IGridType<?>> getExposedTypes();

    @Nullable
    UUID getLastGrid();

}
