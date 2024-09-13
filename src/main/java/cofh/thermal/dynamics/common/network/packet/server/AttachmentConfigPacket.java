package cofh.thermal.dynamics.common.network.packet.server;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.attachment.IPacketHandlerAttachment;
import cofh.thermal.dynamics.common.network.data.server.AttachmentConfigPayload;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.Optional;

public class AttachmentConfigPacket {

    public static final AttachmentConfigPacket INSTANCE = new AttachmentConfigPacket();

    public static AttachmentConfigPacket get() {

        return INSTANCE;
    }

    public void handle(final AttachmentConfigPayload payload, final PlayPayloadContext context) {

        context.workHandler().submitAsync(() -> {

            Optional<Player> senderOptional = context.player();
            if (senderOptional.isEmpty()) {
                return;
            }
            Player player = senderOptional.get();

            Level world = player.level;
            if (!world.isLoaded(payload.pos())) {
                return;
            }
            BlockEntity tile = world.getBlockEntity(payload.pos());
            if (tile instanceof IDuct<?, ?> duct && duct.getAttachment(payload.side()) instanceof IPacketHandlerAttachment attachment) {
                attachment.handleConfigPacket(payload.buf());
            }
        });
    }

    public static void sendToServer(IPacketHandlerAttachment attachment) {

        if (attachment == null) {
            return;
        }
        PacketDistributor.SERVER.noArg().send(new AttachmentConfigPayload(attachment.pos(), attachment.side(), attachment.getConfigPacket(new FriendlyByteBuf(Unpooled.buffer()))));
    }

}
