package cofh.thermal.dynamics.inventory.container.attachment;

import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.attachment.EnergyLimiterAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static cofh.thermal.dynamics.init.TDynContainers.ENERGY_LIMITER_ATTACHMENT_CONTAINER;

public class EnergyLimiterAttachmentContainer extends AttachmentContainer {

    public final EnergyLimiterAttachment attachment;

    public EnergyLimiterAttachmentContainer(int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {

        super(ENERGY_LIMITER_ATTACHMENT_CONTAINER.get(), id, world, pos, side, inventory, player);

        if (hostTile instanceof IGridHost<?, ?> host && host.getAttachment(side) instanceof EnergyLimiterAttachment expectedAttachment) {
            this.attachment = expectedAttachment;
        } else {
            this.attachment = null;
        }
        bindPlayerInventory(inventory);
    }

    @Override
    protected int getMergeableSlotCount() {

        return 0;
    }

}
