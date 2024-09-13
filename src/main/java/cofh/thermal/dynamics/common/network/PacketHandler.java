package cofh.thermal.dynamics.common.network;

import cofh.thermal.dynamics.common.network.data.client.AttachmentControlPayload;
import cofh.thermal.dynamics.common.network.data.client.GridDebugPayload;
import cofh.thermal.dynamics.common.network.data.server.AttachmentConfigPayload;
import cofh.thermal.dynamics.common.network.data.server.AttachmentRedstoneControlPayload;
import cofh.thermal.dynamics.common.network.packet.client.AttachmentControlPacket;
import cofh.thermal.dynamics.common.network.packet.client.GridDebugPacket;
import cofh.thermal.dynamics.common.network.packet.server.AttachmentConfigPacket;
import cofh.thermal.dynamics.common.network.packet.server.AttachmentRedstoneControlPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public class PacketHandler {

    public static void registerNetworking(final RegisterPayloadHandlerEvent event) {

        final IPayloadRegistrar registrar = event.registrar(ID_THERMAL_DYNAMICS);

        // SERVER
        registrar.play(AttachmentConfigPayload.ID, AttachmentConfigPayload::new, handler -> handler.server(AttachmentConfigPacket.get()::handle));
        registrar.play(AttachmentRedstoneControlPayload.ID, AttachmentRedstoneControlPayload::new, handler -> handler.server(AttachmentRedstoneControlPacket.get()::handle));

        // CLIENT
        registrar.play(AttachmentControlPayload.ID, AttachmentControlPayload::new, handler -> handler.client(AttachmentControlPacket.get()::handle));
        registrar.play(GridDebugPayload.ID, GridDebugPayload::new, handler -> handler.client(GridDebugPacket.get()::handle));
    }
}
