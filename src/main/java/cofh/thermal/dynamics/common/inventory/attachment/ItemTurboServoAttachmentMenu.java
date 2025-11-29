package cofh.thermal.dynamics.common.inventory.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.attachment.ItemTurboServoAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static cofh.thermal.dynamics.init.registries.TDynContainers.ITEM_TURBO_SERVO_ATTACHMENT_CONTAINER;

public class ItemTurboServoAttachmentMenu extends ItemServoAttachmentMenu {

    public final ItemTurboServoAttachment turboAttachment;

    public ItemTurboServoAttachmentMenu(int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {
        super(ITEM_TURBO_SERVO_ATTACHMENT_CONTAINER.get(), id, world, pos, side, inventory, player);

        // Store turbo-specific reference for turbo-specific methods
        if (hostTile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof ItemTurboServoAttachment expectedAttachment) {
            this.turboAttachment = expectedAttachment;
        } else {
            this.turboAttachment = null;
        }
    }

    @Override
    public int getMaxTransferAmount() {
        return turboAttachment != null ? turboAttachment.getMaxTransfer() : 64;
    }
}
