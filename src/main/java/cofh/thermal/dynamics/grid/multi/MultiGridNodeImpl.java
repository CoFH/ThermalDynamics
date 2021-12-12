package cofh.thermal.dynamics.grid.multi;

import cofh.thermal.dynamics.api.grid.Grid;
import cofh.thermal.dynamics.api.grid.GridNode;
import cofh.thermal.dynamics.api.grid.GridType;
import cofh.thermal.dynamics.api.grid.multi.MultiGrid;
import cofh.thermal.dynamics.api.grid.multi.MultiGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.util.Direction;

import java.util.Optional;

/**
 * @author covers1624
 */
public class MultiGridNodeImpl extends AbstractGridNode<MultiGrid> implements MultiGridNode {

    public MultiGridNodeImpl(MultiGridImpl grid) {

        super(grid);
    }

    @Override
    protected boolean isExternallyConnectable(Direction side) {

        return false;
    }

    @Override
    public <G extends Grid<?, ?>> Optional<GridNode<G>> getSubGrid(GridType<G> type) {

        return Optional.empty();
    }

}
