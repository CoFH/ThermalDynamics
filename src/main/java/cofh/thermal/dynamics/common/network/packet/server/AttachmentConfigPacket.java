package cofh.thermal.dynamics.common.network.packet.server;

import cofh.lib.common.network.packet.IPacketServer;
import cofh.lib.common.network.packet.PacketBase;
import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.attachment.IPacketHandlerAttachment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static cofh.core.common.network.packet.PacketIDs.PACKET_CONFIG;

public class AttachmentConfigPacket extends PacketBase implements IPacketServer {

    protected BlockPos pos;
    protected Direction side;
    protected FriendlyByteBuf buffer;

    public AttachmentConfigPacket() {

        super(PACKET_CONFIG, ThermalDynamics.PACKET_HANDLER);
    }

    @Override
    public void handleServer(ServerPlayer player) {

        Level world = player.level;
        if (!world.isLoaded(pos)) {
            return;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof IPacketHandlerAttachment attachment) {
            attachment.handleConfigPacket(buffer);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {

        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeBytes(buffer);
    }

    @Override
    public void read(FriendlyByteBuf buf) {

        buffer = buf;
        pos = buffer.readBlockPos();
        side = buffer.readEnum(Direction.class);
    }

    public static void sendToServer(IPacketHandlerAttachment attachment) {

        if (!attachment.hasConfigPacket()) {
            return;
        }
        AttachmentConfigPacket packet = new AttachmentConfigPacket();
        packet.pos = attachment.pos();
        packet.side = attachment.side();
        packet.buffer = attachment.getConfigPacket(new FriendlyByteBuf(Unpooled.buffer()));
        packet.sendToServer();
    }

}
