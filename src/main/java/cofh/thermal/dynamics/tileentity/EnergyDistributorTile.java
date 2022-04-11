package cofh.thermal.dynamics.tileentity;

import cofh.lib.energy.EnergyStorageCoFH;
import cofh.thermal.dynamics.inventory.container.EnergyDistributorContainer;
import cofh.thermal.lib.tileentity.ThermalTileAugmentable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_DISTRIBUTOR_TILE;
import static cofh.thermal.lib.common.ThermalConfig.storageAugments;

public class EnergyDistributorTile extends ThermalTileAugmentable implements ITickableTileEntity {

    public static final int BASE_CAPACITY = 100000;
    public static final int BASE_XFER = 1000;

    // protected ItemStorageCoFH chargeSlot = new ItemStorageCoFH(1, ThermalEnergyHelper::hasEnergyHandlerCap);

    protected List<IEnergyStorage> distributionSet = new ArrayList<>(8);
    protected int distributionTracker;

    public EnergyDistributorTile() {

        super(ENERGY_DISTRIBUTOR_TILE);

        energyStorage = new EnergyStorageCoFH(BASE_CAPACITY, BASE_XFER);

        // inventory.addSlot(chargeSlot, INTERNAL);

        addAugmentSlots(storageAugments);
        initHandlers();
    }

    @Override
    public void tick() {

        if (redstoneControl.getState()) {

        }
    }

    @Nullable
    @Override
    public Container createMenu(int i, PlayerInventory inventory, PlayerEntity player) {

        return new EnergyDistributorContainer(i, level, worldPosition, inventory, player);
    }

}
