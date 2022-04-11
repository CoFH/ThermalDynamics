package cofh.thermal.dynamics.inventory.container;

import cofh.core.inventory.container.TileContainer;
import cofh.lib.inventory.wrapper.InvWrapperCoFH;
import cofh.thermal.dynamics.tileentity.EnergyDistributorTile;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_DISTRIBUTOR_CONTAINER;

public class EnergyDistributorContainer extends TileContainer {

    public final EnergyDistributorTile tile;

    public EnergyDistributorContainer(int windowId, World world, BlockPos pos, PlayerInventory inventory, PlayerEntity player) {

        super(ENERGY_DISTRIBUTOR_CONTAINER, windowId, world, pos, inventory, player);
        this.tile = (EnergyDistributorTile) world.getBlockEntity(pos);
        InvWrapperCoFH tileInv = new InvWrapperCoFH(this.tile.getItemInv());

        // addSlot(new SlotCoFH(tileInv, 0, 8, 53));

        bindAugmentSlots(tileInv, 0, this.tile.augSize());
        bindPlayerInventory(inventory);
    }

}
