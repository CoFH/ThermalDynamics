package cofh.thermal.dynamics.common.network.packet.client;

import cofh.core.CoFHCore;
import cofh.core.util.ProxyUtils;
import cofh.lib.common.network.packet.IPacketClient;
import cofh.lib.common.network.packet.PacketBase;
import cofh.lib.util.Utils;
import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.attachment.IPacketHandlerAttachment;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static cofh.core.common.network.packet.PacketIDs.PACKET_CONTROL;
import static cofh.lib.util.Constants.NETWORK_UPDATE_DISTANCE;

public class AttachmentControlPacket extends PacketBase implements IPacketClient {

    protected BlockPos pos;
    protected Direction side;
    protected FriendlyByteBuf buffer;

    public AttachmentControlPacket() {

        super(PACKET_CONTROL, ThermalDynamics.PACKET_HANDLER);
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
            attachment.handleControlPacket(buffer);
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

    public static void sendToClient(IPacketHandlerAttachment attachment) {

        if (attachment.world() == null || Utils.isClientWorld(attachment.world()) || !attachment.hasControlPacket()) {
            return;
        }
        AttachmentControlPacket packet = new AttachmentControlPacket();
        packet.pos = attachment.pos();
        packet.side = attachment.side();
        packet.buffer = attachment.getControlPacket(new FriendlyByteBuf(Unpooled.buffer()));
        packet.sendToAllAround(packet.pos, NETWORK_UPDATE_DISTANCE, attachment.world().dimension());
    }

}
