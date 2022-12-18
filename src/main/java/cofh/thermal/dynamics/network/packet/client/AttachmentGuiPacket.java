package cofh.thermal.dynamics.network.packet.client;

import cofh.core.CoFHCore;
import cofh.core.util.ProxyUtils;
import cofh.lib.network.packet.IPacketClient;
import cofh.lib.network.packet.PacketBase;
import cofh.lib.util.Utils;
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

import static cofh.core.network.packet.PacketIDs.PACKET_GUI;

public class AttachmentGuiPacket extends PacketBase implements IPacketClient {

    protected BlockPos pos;
    protected Direction side;
    protected FriendlyByteBuf buffer;

    public AttachmentGuiPacket() {

        super(PACKET_GUI, ThermalDynamics.PACKET_HANDLER);
    }

    @Override
    public void handleClient() {

        Level world = ProxyUtils.getClientWorld();
        if (world == null) {
            CoFHCore.LOG.error("Client world is null! (Is this being called on the server?)");
            return;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IDuct<?, ?> duct && duct.getAttachment(side) instanceof IPacketHandlerAttachment attachment) {
            attachment.handleGuiPacket(buffer);
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

    public static void sendToClient(IPacketHandlerAttachment attachment, ServerPlayer player) {

        if (Utils.isClientWorld(player.getLevel()) || !attachment.hasGuiPacket()) {
            return;
        }
        AttachmentGuiPacket packet = new AttachmentGuiPacket();
        packet.pos = attachment.pos();
        packet.side = attachment.side();
        packet.buffer = attachment.getGuiPacket(new FriendlyByteBuf(Unpooled.buffer()));
        packet.sendToPlayer(player);
    }

}
