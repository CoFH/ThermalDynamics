package cofh.thermal.dynamics.network.packet.server;

import cofh.lib.api.control.IRedstoneControllable.ControlMode;
import cofh.lib.network.packet.IPacketServer;
import cofh.lib.network.packet.PacketBase;
import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.attachment.IRedstoneControllableAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import static cofh.core.network.packet.PacketIDs.PACKET_REDSTONE_CONTROL;

public class AttachmentRedstoneControlPacket extends PacketBase implements IPacketServer {

    protected BlockPos pos;
    protected Direction side;
    protected int threshold;
    protected byte mode;

    public AttachmentRedstoneControlPacket() {

        super(PACKET_REDSTONE_CONTROL, ThermalDynamics.PACKET_HANDLER);
    }

    @Override
    public void handleServer(ServerPlayer player) {

        Level world = player.level;
        if (!world.isLoaded(pos)) {
            return;
        }
        BlockEntity tile = world.getBlockEntity(pos);
        if (tile instanceof IGridHost<?, ?> host && host.getAttachment(side) instanceof IRedstoneControllableAttachment attachment) {
            attachment.setControl(threshold, ControlMode.VALUES[mode]);
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {

        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeInt(threshold);
        buf.writeByte(mode);
    }

    @Override
    public void read(FriendlyByteBuf buf) {

        pos = buf.readBlockPos();
        side = buf.readEnum(Direction.class);
        threshold = buf.readInt();
        mode = buf.readByte();
    }

    public static void sendToServer(IRedstoneControllableAttachment attachment) {

        AttachmentRedstoneControlPacket packet = new AttachmentRedstoneControlPacket();
        packet.pos = attachment.pos();
        packet.side = attachment.side();
        packet.threshold = attachment.redstoneControl().getThreshold();
        packet.mode = (byte) attachment.redstoneControl().getMode().ordinal();
        packet.sendToServer();
    }

}
