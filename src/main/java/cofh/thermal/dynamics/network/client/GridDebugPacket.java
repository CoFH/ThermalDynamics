package cofh.thermal.dynamics.network.client;

import cofh.lib.network.packet.IPacketClient;
import cofh.lib.network.packet.PacketBase;
import cofh.thermal.dynamics.client.DebugRenderer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import javax.annotation.Nullable;
import java.util.*;

import static cofh.thermal.dynamics.ThermalDynamics.PACKET_HANDLER;
import static cofh.thermal.dynamics.util.TDynConstants.PACKET_GRID_DEBUG;

public class GridDebugPacket extends PacketBase implements IPacketClient {

    @Nullable
    private PacketBuffer data;

    public GridDebugPacket(PacketBuffer data) {

        this();
        this.data = data;
    }

    public GridDebugPacket() {

        super(PACKET_GRID_DEBUG, PACKET_HANDLER);
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
    public void write(PacketBuffer buf) {

        assert data != null;
        buf.writeVarInt(data.readableBytes());
        buf.writeBytes(data);
    }

    @Override
    public void read(PacketBuffer buf) {

        data = new PacketBuffer(buf.readBytes(buf.readVarInt()));
    }

}
