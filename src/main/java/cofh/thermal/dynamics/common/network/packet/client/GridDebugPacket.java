package cofh.thermal.dynamics.common.network.packet.client;

import cofh.lib.common.network.packet.IPacketClient;
import cofh.lib.common.network.packet.PacketBase;
import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.client.DebugRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.*;

import static cofh.thermal.dynamics.util.TDynConstants.PACKET_GRID_DEBUG;

public class GridDebugPacket extends PacketBase implements IPacketClient {

    @Nullable
    private FriendlyByteBuf data;

    public GridDebugPacket(FriendlyByteBuf data) {

        this();
        this.data = data;
    }

    public GridDebugPacket() {

        super(PACKET_GRID_DEBUG, ThermalDynamics.PACKET_HANDLER);
    }

    @Override
    public void handleClient() {

        Map<UUID, Map<BlockPos, List<BlockPos>>> grids = new HashMap<>();
        int numGrids = data.readVarInt();
        for (int g = 0; g < numGrids; g++) {
            Map<BlockPos, List<BlockPos>> newNodes = new HashMap<>();
            UUID uuid = data.readUUID();
            int numNodes = data.readVarInt();
            for (int i = 0; i < numNodes; ++i) {
                BlockPos nodePos = data.readBlockPos();
                int numEdges = data.readVarInt();
                List<BlockPos> edges = new ArrayList<>(numEdges);
                for (int j = 0; j < numEdges; j++) {
                    edges.add(data.readBlockPos());
                }
                newNodes.put(nodePos, edges);
            }
            grids.put(uuid, newNodes);
        }
        DebugRenderer.grids = grids;
    }

    @Override
    public void write(FriendlyByteBuf buf) {

        assert data != null;
        buf.writeVarInt(data.readableBytes());
        buf.writeBytes(data);
    }

    @Override
    public void read(FriendlyByteBuf buf) {

        data = new FriendlyByteBuf(buf.readBytes(buf.readVarInt()));
    }

}
