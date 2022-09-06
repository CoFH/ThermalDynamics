package cofh.thermal.dynamics.inventory.container;

import cofh.core.inventory.container.TileContainer;
import cofh.lib.inventory.wrapper.InvWrapperCoFH;
import cofh.thermal.dynamics.block.entity.EnergyDistributorTile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_DISTRIBUTOR_CONTAINER;

public class EnergyDistributorContainer extends TileContainer {

    public final EnergyDistributorTile tile;

    public EnergyDistributorContainer(int windowId, Level world, BlockPos pos, Inventory inventory, Player player) {

        super(ENERGY_DISTRIBUTOR_CONTAINER, windowId, world, pos, inventory, player);
        this.tile = (EnergyDistributorTile) world.getBlockEntity(pos);
        InvWrapperCoFH tileInv = new InvWrapperCoFH(this.tile.getItemInv());

        // addSlot(new SlotCoFH(tileInv, 0, 8, 53));

        bindAugmentSlots(tileInv, 0, this.tile.augSize());
        bindPlayerInventory(inventory);
    }

}
