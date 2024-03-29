//package cofh.thermal.dynamics.block.entity;
//
//import cofh.lib.api.block.entity.ITickableTile;
//import cofh.lib.energy.EnergyStorageCoFH;
//import cofh.thermal.dynamics.inventory.container.EnergyDistributorContainer;
//import cofh.thermal.lib.tileentity.ThermalTileAugmentable;
//import net.minecraft.core.BlockPos;
//import net.minecraft.world.entity.player.Inventory;
//import net.minecraft.world.entity.player.Player;
//import net.minecraft.world.inventory.AbstractContainerMenu;
//import net.minecraft.world.level.block.state.BlockState;
//import net.minecraftforge.energy.IEnergyStorage;
//
//import javax.annotation.Nullable;
//import java.util.ArrayList;
//import java.util.List;
//
//import static cofh.thermal.core.config.ThermalCoreConfig.storageAugments;
//import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_DISTRIBUTOR_TILE;
//
//public class EnergyDistributorTile extends ThermalTileAugmentable implements ITickableTile.IServerTickable {
//
//    public static final int BASE_CAPACITY = 100000;
//    public static final int BASE_XFER = 1000;
//
//    // protected ItemStorageCoFH chargeSlot = new ItemStorageCoFH(1, ThermalEnergyHelper::hasEnergyHandlerCap);
//
//    protected List<IEnergyStorage> distributionSet = new ArrayList<>(8);
//    protected int distributionTracker;
//
//    public EnergyDistributorTile(BlockPos pos, BlockState state) {
//
//        super(ENERGY_DISTRIBUTOR_TILE.get(), pos, state);
//
//        energyStorage = new EnergyStorageCoFH(BASE_CAPACITY, BASE_XFER);
//
//        // inventory.addSlot(chargeSlot, INTERNAL);
//
//        addAugmentSlots(storageAugments);
//        initHandlers();
//    }
//
//    @Override
//    public void tickServer() {
//
//        if (redstoneControl.getState()) {
//
//        }
//    }
//
//    @Nullable
//    @Override
//    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
//
//        return new EnergyDistributorContainer(i, level, worldPosition, inventory, player);
//    }
//
//}
