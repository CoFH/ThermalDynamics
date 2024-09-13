package cofh.thermal.dynamics.common.network.data.client;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public record GridDebugPayload(FriendlyByteBuf buf) implements CustomPacketPayload {

    public static final ResourceLocation ID = new ResourceLocation(ID_THERMAL_DYNAMICS, "grid_debug_packet");

    @Override
    public void write(FriendlyByteBuf buf) {

        buf.writeBytes(this.buf);
    }

    @Override
    public ResourceLocation id() {

        return null;
    }

}
