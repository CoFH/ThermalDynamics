package cofh.thermal.dynamics.common.network.packet.client;

import cofh.thermal.dynamics.client.DebugRenderer;
import cofh.thermal.dynamics.common.network.data.client.GridDebugPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;

import java.util.*;

public class GridDebugPacket {

    public static final GridDebugPacket INSTANCE = new GridDebugPacket();

    public static GridDebugPacket get() {

        return INSTANCE;
    }

    public void handle(final GridDebugPayload payload, final PlayPayloadContext context) {

        context.workHandler().submitAsync(() -> {

            Map<UUID, Map<BlockPos, List<BlockPos>>> grids = new HashMap<>();
            int numGrids = payload.buf().readVarInt();
            for (int g = 0; g < numGrids; g++) {
                Map<BlockPos, List<BlockPos>> newNodes = new HashMap<>();
                UUID uuid = payload.buf().readUUID();
                int numNodes = payload.buf().readVarInt();
                for (int i = 0; i < numNodes; ++i) {
                    BlockPos nodePos = payload.buf().readBlockPos();
                    int numEdges = payload.buf().readVarInt();
                    List<BlockPos> edges = new ArrayList<>(numEdges);
                    for (int j = 0; j < numEdges; j++) {
                        edges.add(payload.buf().readBlockPos());
                    }
                    newNodes.put(nodePos, edges);
                }
                grids.put(uuid, newNodes);
            }
            DebugRenderer.grids = grids;
        });
    }

    public static void sendToClients(FriendlyByteBuf buf) {

        if (buf == null) {
            return;
        }
        PacketDistributor.ALL.noArg().send(new GridDebugPayload(buf));
    }

}
