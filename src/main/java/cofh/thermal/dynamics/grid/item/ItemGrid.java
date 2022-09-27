//package cofh.thermal.dynamics.grid.item;
//
//import cofh.thermal.dynamics.api.grid.item.IItemGrid;
//import cofh.thermal.dynamics.api.grid.item.IItemGridNode;
//import cofh.thermal.dynamics.api.helper.GridHelper;
//import cofh.thermal.dynamics.grid.AbstractGrid;
//import cofh.thermal.dynamics.grid.AbstractGridNode;
//import net.minecraft.core.Direction;
//import net.minecraft.world.level.Level;
//import net.minecraft.world.level.block.entity.BlockEntity;
//import net.minecraftforge.items.CapabilityItemHandler;
//
//import javax.annotation.Nullable;
//import java.util.List;
//import java.util.UUID;
//
//import static cofh.thermal.dynamics.init.TDynReferences.ITEM_GRID;
//
//public class ItemGrid extends AbstractGrid<IItemGrid, IItemGridNode> implements IItemGrid {
//
//    public ItemGrid(UUID id, Level world) {
//
//        super(GRID_ITEM.get(), id, world);
//    }
//
//    @Override
//    public AbstractGridNode<IItemGrid> newNode() {
//
//        return new ItemGridNode(this);
//    }
//
//    @Override
//    public void onMerge(IItemGrid from) {
//
//    }
//
//    @Override
//    public void onSplit(List<IItemGrid> others) {
//
//    }
//
//    @Override
//    public boolean canConnectOnSide(BlockEntity tile, @Nullable Direction dir) {
//
//        if (GridHelper.getGridHost(tile).isPresent()) {
//            return false; // We cannot externally connect to other grids.
//        }
//        if (dir != null) {
//            return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, dir).isPresent();
//        }
//        return false;
//        // return tile.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).isPresent();
//    }
//
//    @Override
//    public void refreshCapabilities() {
//
//    }
//
//}
