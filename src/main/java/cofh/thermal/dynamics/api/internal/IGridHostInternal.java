package cofh.thermal.dynamics.api.internal;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridType;

import java.util.Set;

/**
 * Represents internal, unsupported methods on a {@link IGridHost}.
 *
 * @author covers1624
 */
public interface IGridHostInternal extends IGridHost {

    void setGrid(IGrid<?, ?> grid);

    Set<IGridType<?>> getExposedTypes();

}
