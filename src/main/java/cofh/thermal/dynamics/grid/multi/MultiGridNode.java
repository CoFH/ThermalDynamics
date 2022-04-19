package cofh.thermal.dynamics.grid.multi;

import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridNode;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.grid.multi.IMultiGrid;
import cofh.thermal.dynamics.api.grid.multi.IMultiGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.util.Direction;

import java.util.Optional;

/**
 * @author covers1624
 */
public class MultiGridNode extends AbstractGridNode<IMultiGrid> implements IMultiGridNode {

    public MultiGridNode(MultiGrid grid) {

        super(grid);
    }

    @Override
    protected boolean isConnectable(Direction side) {

        return false;
    }

    @Override
    public <G extends IGrid<?, ?>> Optional<IGridNode<G>> getSubGrid(IGridType<G> type) {

        return Optional.empty();
    }

}
