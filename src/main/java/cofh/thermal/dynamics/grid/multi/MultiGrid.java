package cofh.thermal.dynamics.grid.multi;

import cofh.thermal.dynamics.api.grid.multi.IMultiGrid;
import cofh.thermal.dynamics.api.grid.multi.IMultiGridNode;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @author covers1624
 */
public class MultiGrid extends AbstractGrid<IMultiGrid, IMultiGridNode> implements IMultiGrid {

    public MultiGrid(UUID id, Level world) {

        super(TDynReferences.MULTI_GRID, id, world);
    }

    @Override
    public AbstractGridNode<IMultiGrid> newNode() {

        return new MultiGridNode(this);
    }

    @Override
    public void onMerge(IMultiGrid from) {

    }

    @Override
    public void onSplit(List<IMultiGrid> others) {

    }

    @Override
    public boolean canConnectOnSide(BlockEntity tile, @Nullable Direction dir) {

        return false;
    }

}
