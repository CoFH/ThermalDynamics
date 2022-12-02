package cofh.thermal.dynamics.inventory.container.attachment;

import cofh.core.inventory.container.ContainerCoFH;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.attachment.IAttachment;
import cofh.thermal.dynamics.attachment.IPacketHandlerAttachment;
import cofh.thermal.dynamics.network.packet.client.AttachmentGuiPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayer;
import org.jetbrains.annotations.Nullable;

public abstract class AttachmentContainer extends ContainerCoFH {

    public final BlockEntity hostTile;
    public final IAttachment baseAttachment;

    public AttachmentContainer(@Nullable MenuType<?> type, int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {

        super(type, id, inventory, player);
        hostTile = world.getBlockEntity(pos);

        if (hostTile instanceof IGridHost<?, ?> host) {
            this.baseAttachment = host.getAttachment(side);
        } else {
            this.baseAttachment = null;
        }
    }

    @Override
    public void broadcastChanges() {

        super.broadcastChanges();

        if (baseAttachment instanceof IPacketHandlerAttachment handlerAttachment) {
            if (player instanceof ServerPlayer serverPlayer && (!(player instanceof FakePlayer))) {
                AttachmentGuiPacket.sendToClient(handlerAttachment, serverPlayer);
            }
        }
    }

    @Override
    public boolean stillValid(Player player) {

        return baseAttachment != null && hostTile != null && !hostTile.isRemoved();
    }

}
