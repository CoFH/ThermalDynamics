package cofh.thermal.dynamics.common.network.data.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record AttachmentRedstoneControlPayload(BlockPos pos, Direction side, int threshold, byte mode) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(ID_THERMAL_DYNAMICS, "attachment_redstone_packet");

    public AttachmentRedstoneControlPayload(final FriendlyByteBuf buf) {

        this(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readInt(), buf.readByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {

        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeInt(threshold);
        buf.writeByte(mode);
    }

    @Override
    public ResourceLocation id() {

        return ID;
    }

}
