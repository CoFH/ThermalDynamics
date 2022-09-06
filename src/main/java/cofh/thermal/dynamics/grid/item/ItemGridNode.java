package cofh.thermal.dynamics.grid.item;

import cofh.thermal.dynamics.api.grid.item.IItemGrid;
import cofh.thermal.dynamics.api.grid.item.IItemGridNode;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import net.minecraft.core.Direction;

public class ItemGridNode extends AbstractGridNode<IItemGrid> implements IItemGridNode {

    public ItemGridNode(ItemGrid grid) {

        super(grid);
    }

    protected void cacheConnections() {

        for (Direction dir : Direction.values()) {
            if (grid.canConnectOnSide(pos.relative(dir), dir.getOpposite())) {
                connections.add(dir);
            }
        }
    }

}
