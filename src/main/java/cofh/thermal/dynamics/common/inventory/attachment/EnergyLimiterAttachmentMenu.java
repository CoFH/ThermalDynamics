package cofh.thermal.dynamics.common.inventory.attachment;

import cofh.core.common.network.packet.client.ContainerGuiPacket;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.attachment.EnergyLimiterAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import static cofh.thermal.dynamics.init.registries.TDynContainers.ENERGY_LIMITER_ATTACHMENT_CONTAINER;

public class EnergyLimiterAttachmentMenu extends AttachmentMenu {

    public final EnergyLimiterAttachment attachment;

    public EnergyLimiterAttachmentMenu(int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {

        super(ENERGY_LIMITER_ATTACHMENT_CONTAINER.get(), id, world, pos, side, inventory, player);

        if (hostTile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof EnergyLimiterAttachment expectedAttachment) {
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

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();

        ContainerGuiPacket.sendToClient(this, player);
    }

    // region NETWORK
    @Override
    public FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        buffer.writeInt(attachment.amountInput);
        buffer.writeInt(attachment.amountOutput);

        return buffer;
    }

    @Override
    public void handleGuiPacket(FriendlyByteBuf buffer) {

        attachment.amountInput = buffer.readInt();
        attachment.amountOutput = buffer.readInt();
    }
    // endregion
}
