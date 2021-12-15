package cofh.thermal.dynamics.grid.item;

import cofh.thermal.dynamics.api.grid.item.IItemGrid;
import cofh.thermal.dynamics.api.grid.item.IItemGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.init.TDynReferences;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.world.World;
import net.minecraftforge.items.CapabilityItemHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

/**
 * @author covers1624
 */
public class ItemGrid extends AbstractGrid<IItemGrid, IItemGridNode> implements IItemGrid {

    public ItemGrid(UUID id, World world) {

        super(TDynReferences.ITEM_GRID, id, world);
    }

    @Override
    public AbstractGridNode<IItemGrid> newNode() {

        return new ItemGridNode(this);
    }

    @Override
    public void onMerge(IItemGrid from) {

    }

    @Override
    public void onSplit(List<IItemGrid> others) {

    }

    @Override
    public boolean canConnectExternally(TileEntity tile, @Nullable Direction dir) {

        if (GridHelper.getGridHost(tile).isPresent()) return false; // We cannot externally connect to other grids.
        if (dir != null) {
            return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir.getOpposite()).isPresent();
        }
        return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent();
    }

}
