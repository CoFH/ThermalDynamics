package cofh.thermal.dynamics.grid.item;

import cofh.thermal.dynamics.api.grid.item.ItemGrid;
import cofh.thermal.dynamics.api.grid.item.ItemGridNode;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraftforge.items.CapabilityItemHandler;

/**
 * @author covers1624
 */
public class ItemGridNodeImpl extends AbstractGridNode<ItemGrid> implements ItemGridNode {

    public ItemGridNodeImpl(ItemGridImpl grid) {

        super(grid);
    }

    @Override
    protected boolean isExternallyConnectable(Direction side) {

        TileEntity tile = getWorld().getBlockEntity(getPos().relative(side));
        if (GridHelper.getGridHost(tile).isPresent()) return false; // We cannot externally connect to other grids.
        if (tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent())
            return true; // We can connect to the inner face
        if (tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, side.getOpposite()).isPresent())
            return true; // We can connect to the face
        return false; // nope
    }

}
