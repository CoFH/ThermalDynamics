package cofh.thermal.dynamics.grid.item;

import cofh.thermal.dynamics.api.grid.item.IItemGrid;
import cofh.thermal.dynamics.api.grid.item.IItemGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.items.CapabilityItemHandler;

/**
 * @author covers1624
 */
public class ItemGridNode extends AbstractGridNode<IItemGrid> implements IItemGridNode {

    public ItemGridNode(ItemGrid grid) {

        super(grid);
    }

    @Override
    protected boolean isConnectable(Direction side) {

        TileEntity tile = getWorld().getBlockEntity(getPos().relative(side));
        if (tile == null) return false;
        if (GridHelper.getGridHost(tile).isPresent()) return false; // We cannot externally connect to other grids.
        if (tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent())
            return true; // We can connect to the inner face
        if (tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()).isPresent())
            return true; // We can connect to the face
        return false; // nope
    }

}
