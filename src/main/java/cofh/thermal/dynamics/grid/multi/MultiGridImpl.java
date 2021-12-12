package cofh.thermal.dynamics.grid.multi;

import cofh.thermal.dynamics.api.grid.multi.MultiGrid;
import cofh.thermal.dynamics.api.grid.multi.MultiGridNode;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @author covers1624
 */
public class MultiGridImpl extends AbstractGrid<MultiGrid, MultiGridNode> implements MultiGrid {

    public MultiGridImpl(UUID id, World world) {

        super(TDynReferences.MULTI_GRID, id, world);
    }

    @Override
    public AbstractGridNode<MultiGrid> newNode() {

        return new MultiGridNodeImpl(this);
    }

    @Override
    public void onMerge(MultiGrid from) {

    }

    @Override
    public void onSplit(List<MultiGrid> others) {

    }

    @Override
    public boolean canConnectExternally(TileEntity tile, @Nullable Direction dir) {

        return false;
    }

}
