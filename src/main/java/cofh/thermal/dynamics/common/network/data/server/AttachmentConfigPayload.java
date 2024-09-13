package cofh.thermal.dynamics.common.network.data.server;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record AttachmentConfigPayload(BlockPos pos, Direction side, FriendlyByteBuf buf) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(ID_THERMAL_DYNAMICS, "attachment_config_packet");

    public AttachmentConfigPayload(final FriendlyByteBuf buf) {

        this(buf.readBlockPos(), buf.readEnum(Direction.class), buf);
    }

    @Override
    public void write(FriendlyByteBuf buf) {

        buf.writeBlockPos(pos);
        buf.writeEnum(side);
        buf.writeBytes(this.buf);
    }

    @Override
    public ResourceLocation id() {

        return ID;
    }

}