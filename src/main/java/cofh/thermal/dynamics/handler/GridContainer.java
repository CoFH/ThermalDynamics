package cofh.thermal.dynamics.handler;

import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.IDuct.ConnectionType;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.Grid;
import cofh.thermal.dynamics.grid.GridNode;
import cofh.thermal.dynamics.network.packet.client.GridDebugPacket;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.graph.EndpointPair;
import io.netty.buffer.Unpooled;
import net.covers1624.quack.collection.ColUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.TickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cofh.lib.util.Constants.DIRECTIONS;
import static cofh.thermal.core.ThermalCore.LOG;
import static cofh.thermal.dynamics.api.helper.GridHelper.*;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public class GridContainer implements IGridContainer, INBTSerializable<ListTag> {

    private static final boolean DEBUG = GridContainer.class.desiredAssertionStatus();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<UUID> USED_UUIDS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<BlockPos, Grid<?, ?>> gridPosLookup = new HashMap<>();

    private final Map<UUID, Grid<?, ?>> grids = new HashMap<>();
    private final Map<UUID, Grid<?, ?>> loadedGrids = new HashMap<>();
    private final Level world;

    private int tickCounter;

    public GridContainer(Level world) {

        this.world = world;
    }

    private static boolean canConnectTo(IDuct<?, ?> from, IDuct<?, ?> to, Direction dir) {

        return from.canConnectTo(to, dir) && to.canConnectTo(from, dir.getOpposite());
    }

    @Override
    public void onDuctPlaced(IDuct<?, ?> duct, @Nullable Direction connectionPreference) {

        gridHostPlaced(duct, connectionPreference);
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> void gridHostPlaced(IDuct<G, N> host, @Nullable Direction connectionPreference) {

        // New Grid.
        constructNewGrid(host);
        EnumMap<Direction, IDuct<G, N>> adjacentGrids = getAdjacentGrids(host);
        if (adjacentGrids.isEmpty()) {
            return;
        }

        List<Direction> dirs = Lists.newArrayList(DIRECTIONS);
        if (connectionPreference != null) {
            dirs.remove(connectionPreference);
            dirs.add(0, connectionPreference);
        }

        for (Direction dir : dirs) {
            IDuct<?, ?> other = GridHelper.getGridHost(world, host.getHostPos().relative(dir));
            if (other == null) continue; // No host
            if (!canConnectTo(host, other, dir)) {
                if (host.getConnectionType(dir).allowDuctConnection()) {
                    host.setConnectionType(dir, ConnectionType.DISABLED);
                }
                if (other.getConnectionType(dir.getOpposite()).allowDuctConnection()) {
                    other.setConnectionType(dir.getOpposite(), ConnectionType.DISABLED);
                }
                continue; // Not allowed to connect.
            }

            IDuct<G, N> otherHost = unsafeCast(other); // Guaranteed safe by canConnectTo

            if (other.getGrid() != host.getGrid()) {
                // Merge into the other grid.
                mergeGrids(List.of(otherHost, host));
            }

            G grid = otherHost.getGrid();
            N otherNode = grid.getNodeOrSplitEdgeAndInsertNode(otherHost.getHostPos());
            N thisNode = grid.getNodeOrSplitEdgeAndInsertNode(host.getHostPos());
            grid.nodeGraph.putEdge(otherNode, thisNode);
            simplifyNode(otherNode);
            simplifyNode(thisNode);
        }
        host.getGrid().onModified();
    }

    private <G extends Grid<G, ?>> void constructNewGrid(IDuct<G, ?> host) {

        if (DEBUG) {
            LOGGER.info("Constructing new grid for {}", host.getHostPos());
        }
        G grid = createAndAddGrid(nextUUID(), host.getGridType(), true);
        host.setGrid(grid);
        grid.newNode(host.getHostPos());
        addGridLookup(grid, host.getHostPos());
        grid.onGridHostAdded(host);
        grid.onModified();
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> void mergeGrids(List<IDuct<G, N>> branches) {

        Set<G> grids = new HashSet<>();
        for (IDuct<G, N> branch : branches) {
            grids.add(branch.getGrid());
        }

        // Choose the largest grid as the 'main' grid.
        G main = ColUtils.maxBy(grids, e -> e.nodeGraph.nodes().size());
        assert main != null;

        grids.remove(main);
        for (G other : grids) {
            Set<N> toBeMoved = new HashSet<>(other.nodeGraph.nodes());
            main.mergeFrom(other);
            replaceGridLookup(main, other, toBeMoved);
            this.grids.remove(other.getId());
            this.loadedGrids.remove(other.getId());
        }
        main.onModified();
    }

    @Override
    public void onDuctRemoved(IDuct<?, ?> duct) {

        removeGridHost(duct);
    }

    public <G extends Grid<G, N>, N extends GridNode<G>> void removeGridHost(IDuct<G, N> host) {
        // - Disconnect any edges.
        // - Remove current node.
        // - Try and split the grid if nodes exist.
        // - Delete grid if no more nodes exist.
        G grid = host.getGrid();
        N removing = host.getNode();
        BlockPos hostPos = host.getHostPos();

        removeGridLookup(grid, host.getHostPos());
        if (removing != null) {
            // All we need to do is remove the node.

            List<N> adjacentNodes = new LinkedList<>();
            // We must copy this otherwise, we get Concurrent modification exception modifying the grid with 'splitEdgeAndInsertNode'
            Set<EndpointPair<N>> allEdges = ImmutableSet.copyOf(grid.nodeGraph.incidentEdges(removing));
            for (EndpointPair<N> edge : allEdges) {
                GridNode<?> other = edge.nodeU() == removing ? edge.nodeV() : edge.nodeU();
                adjacentNodes.add(grid.getNodeOrSplitEdgeAndInsertNode(stepTowards(hostPos, other.getPos())));
            }

            if (DEBUG) {
                LOGGER.info("Removing node: {}", removing.getPos());
            }
            grid.removeNode(removing);
            // Might be redundant, safe to do anyway.
            for (N adjacentNode : adjacentNodes) {
                simplifyNode(adjacentNode);
            }
        } else {
            // We are a host without a node, on an edge.
            EndpointPair<N> edge = grid.findEdge(hostPos);
            assert edge != null : "Block does not lie on an edge.";
            N a = grid.getNodeOrSplitEdgeAndInsertNode(stepTowards(hostPos, edge.nodeU().getPos()));
            N b = grid.getNodeOrSplitEdgeAndInsertNode(stepTowards(hostPos, edge.nodeV().getPos()));
            grid.nodeGraph.removeEdge(a, b); // Yeet edge.
            if (DEBUG) {
                LOGGER.info("Removing edge between: {} and {}", a.getPos(), b.getPos());
            }

            // Might be redundant, safe to do anyway.
            simplifyNode(a);
            simplifyNode(b);
        }

        // Clear masks set by placement when duct is removed.
        for (Direction dir : DIRECTIONS) {
            IDuct<?, ?> adjacent = GridHelper.getGridHost(host.getHostWorld(), host.getHostPos().relative(dir));
            if (adjacent == null) continue;

            if (adjacent.getConnectionType(dir.getOpposite()) == ConnectionType.DISABLED) {
                adjacent.setConnectionType(dir.getOpposite(), ConnectionType.ALLOWED);
            }
        }

        if (!grid.getNodes().isEmpty()) {
            grid.onGridHostRemoved(host);
            grid.onModified();
            separateGrids(grid);
        } else {
            if (DEBUG) {
                LOGGER.info("Removing grid for {}", host.getHostPos());
            }

            grids.remove(grid.getId());
            loadedGrids.remove(grid.getId());
        }
    }

    @Override
    public boolean onDuctNeighborChanged(IDuct<?, ?> duct) {

        return gridNeighborChanged(duct);
    }

    public <G extends Grid<G, N>, N extends GridNode<G>> boolean gridNeighborChanged(IDuct<G, N> duct) {

        G grid = duct.getGrid();
        N node = duct.getNode();

        boolean canExternallyConnect = grid.canConnectExternally(duct.getHostPos());
        if (node != null) {
            if (!canExternallyConnect) {
                simplifyNode(node);
            }
            node.clearConnections();
            return true;
        } else {
            if (canExternallyConnect) {
                grid.getNodeOrSplitEdgeAndInsertNode(duct.getHostPos());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean onDuctSideConnected(IDuct<?, ?> duct, Direction side) {

        return onSideConnectionChanged(duct, side, false);
    }

    @Override
    public void onDuctSideDisconnecting(IDuct<?, ?> duct, Direction side) {

        onSideConnectionChanged(duct, side, true);
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> boolean onSideConnectionChanged(IDuct<G, N> host, Direction changed, boolean disconnect) {
        // Connection attempt:
        // - Already connected?
        //  - Yes? return.
        // - Can hosts connect?
        //  - No? return.
        // - Connect:
        //  - Split edges if required.
        //  - Merge grids if they are different.
        //  - Connect nodes.

        // Disconnection attempt:
        // - Already disconnected?
        //  - Yes? return.
        // - Disconnect:
        //  - Split edges if required.
        //  - Disconnect nodes.
        //  - Try splitting the grid.

        // Try and find adjacent grid host.
        IDuct<G, N> other = getAdjacentGrids(host).get(changed);
        if (other == null) return false;

        // Grab grid and nodes either side of the split/join.
        // If nodes don't exist, insert temporary nodes, these will be cleaned up later.
        G aGrid = host.getGrid();
        G bGrid = other.getGrid();

        // If edge being updated is already part of the grid
        boolean connecting = !disconnect;
        boolean nodesConnected = aGrid == bGrid && aGrid.isConnectedTo(host.getHostPos(), other.getHostPos());
        if (connecting == nodesConnected) {
            return true; // Nothing to do.
        }

        N a = aGrid.getNodeOrSplitEdgeAndInsertNode(host.getHostPos());
        N b = bGrid.getNodeOrSplitEdgeAndInsertNode(other.getHostPos());

        if (connecting) {
            // Connect
            if (DEBUG) {
                LOGGER.info("Connecting nodes due to connectability change. A {}, B {}.", a.getPos(), b.getPos());
            }

            // Perform grid merge.
            if (aGrid != bGrid) {
                mergeGrids(Arrays.asList(host, other));
                aGrid = a.getGrid();
            }

            // Perform connection. (we know these are directly adjacent.)
            aGrid.nodeGraph.putEdge(a, b);

            // Simplify all directly adjacent (in world) nodes of a (excluding b).
            for (N adj : aGrid.nodeGraph.adjacentNodes(a)) {
                if (adj == b) continue;
                if (aGrid.nodeGraph.hasEdgeConnecting(a, adj)) continue;
                simplifyNode(adj);
            }
            // Simplify all directly adjacent (in world) nodes of b (excluding a)
            for (N adj : aGrid.nodeGraph.adjacentNodes(b)) {
                if (adj == a) continue;
                if (aGrid.nodeGraph.hasEdgeConnecting(b, adj)) continue;
                simplifyNode(adj);
            }
            // Try and simplify A and B.
            simplifyNode(a);
            simplifyNode(b);
            aGrid.onModified();
        } else {
            // Disconnect
            if (DEBUG) {
                LOGGER.info("Disconnecting nodes due to connectability change. A {}, B {}.", a.getPos(), b.getPos());
            }
            aGrid.nodeGraph.removeEdge(a, b);
            // Try and simplify both nodes.
            simplifyNode(a);
            simplifyNode(b);
            // Try splitting.
            if (!separateGrids(aGrid)) {
                // We didn't split, must fire grid events.
                aGrid.onModified();
            }
        }
        return true;
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> void simplifyNode(N node) {

        G grid = node.getGrid();
        if (!grid.nodeGraph.nodes().contains(node)) return;

        // We can't simplify if we can connect to adjacent blocks.
        if (grid.canConnectExternally(node.getPos())) {
            return;
        }
        List<N> edges = new ArrayList<>(grid.nodeGraph.adjacentNodes(node));

        // We can't simplify a node if there aren't exactly 2 edges.
        if (edges.size() != 2) {
            return;
        }
        N a = edges.get(0);
        N b = edges.get(1);

        // If both edges aren't on the same axis, then we can't simplify.
        if (!isOnSameAxis(a.getPos(), b.getPos())) {
            return;
        }

        grid.removeNode(node);
        grid.nodeGraph.putEdge(a, b);
        // grid.checkInvariant();
        if (DEBUG) {
            LOGGER.info(
                    "Simplifying grid node '{}' A {}, B {}, Len {}",
                    node.getPos(),
                    a.getPos(),
                    b.getPos(),
                    numBetween(a.getPos(), b.getPos())
            );
        }
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> boolean separateGrids(G grid) {

        // Generate the grid nodes isolated from each other.
        List<Set<N>> splitGraphs = GraphHelper.separateGraphs(grid.nodeGraph);
        if (splitGraphs.size() <= 1) return false;

        if (DEBUG) {
            LOGGER.info("Splitting grid into {} segments.", splitGraphs.size());
        }

        List<G> newGrids = grid.splitInto(splitGraphs);
        for (G newGrid : newGrids) {
            replaceGridLookup(newGrid, grid, newGrid.nodeGraph.nodes());
            newGrid.onModified();
        }
        grids.remove(grid.getId());
        loadedGrids.remove(grid.getId());
        return true;
    }

    // region EVENT CALLBACKS
    public void onWorldTick(TickEvent.Phase phase) {
        // TODO do we want to pass this through to grids?
        if (phase != TickEvent.Phase.END) {
            return;
        }
        try {
            for (Grid<?, ?> value : loadedGrids.values()) {
                value.tick();
            }
        } catch (Exception e) {
            LOG.error("Thermal Dynamics encountered an error during grid ticking:", e);
        }
        if (DEBUG && tickCounter % 10 == 0 && !loadedGrids.isEmpty()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(loadedGrids.size());
            for (Grid<?, ?> value : loadedGrids.values()) {
                value.debugWriteToPacket(buffer);
            }
            GridDebugPacket debugPacket = new GridDebugPacket(buffer);
            debugPacket.sendToClients();
        }
        tickCounter++;
    }

    public void onChunkLoad(ChunkAccess chunk) {

        for (Grid<?, ?> grid : grids.values()) {
            if (grid.onChunkLoad(chunk)) {
                assert !loadedGrids.containsKey(grid.getId());
                loadedGrids.put(grid.getId(), grid);
            }
        }
    }
    // endregion

    public void onChunkUnload(ChunkAccess chunk) {

        Set<UUID> rem = new HashSet<>(2);
        for (Grid<?, ?> grid : loadedGrids.values()) {
            if (grid.onChunkUnload(chunk)) {
                rem.add(grid.getId());
            }
        }
        loadedGrids.keySet().removeAll(rem);
    }

    @Nullable
    @Override
    public Grid<?, ?> getGrid(UUID id) {

        return grids.get(id);
    }

    @Override
    public <G extends Grid<G, ?>> G getGrid(IGridType<G> type, BlockPos pos) {

        Grid<?, ?> grid = gridPosLookup.get(pos);
        if (grid.getGridType() != type) {
            throw new IllegalStateException("Grid at position " + pos + " is not of type " + type + ". Got: " + grid.getGridType());
        }
        return unsafeCast(grid);
    }

    @Override
    public ListTag serializeNBT() {

        ListTag grids = new ListTag();
        for (Map.Entry<UUID, Grid<?, ?>> entry : this.grids.entrySet()) {
            Grid<?, ?> grid = entry.getValue();
            CompoundTag tag = new CompoundTag();
            tag.putUUID("id", entry.getKey());
            tag.putString("type", grid.getGridType().getRegistryName().toString());
            tag.merge(grid.serializeNBT());
            grids.add(tag);
        }
        return grids;
    }

    @Override
    public void deserializeNBT(ListTag nbt) {

        assert grids.isEmpty();
        for (int i = 0; i < nbt.size(); ++i) {
            CompoundTag tag = nbt.getCompound(i);
            UUID id = tag.getUUID("id");
            assert !grids.containsKey(id) : "Duplicate grid found.";
            ResourceLocation gridTypeName = new ResourceLocation(tag.getString("type"));
            IGridType<?> gridType = ThermalDynamics.GRID_TYPE_REGISTRY.get().getValue(gridTypeName);
            if (gridType == null) {
                LOGGER.error("Failed to load Grid {} with type {} in world {}. GridType is no longer registered, it will be removed from the world.", id, gridTypeName, world.dimension().location());
                continue;
            }
            deserializeGrid(tag, id, unsafeCast(gridType));
        }
        if (DEBUG) {
            LOGGER.info("Loaded {} grids for {}.", grids.size(), world.dimension().location());
        }
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> void deserializeGrid(CompoundTag tag, UUID id, IGridType<G> gridType) {

        G grid = createAndAddGrid(id, gridType, false);
        grid.deserializeNBT(tag);

        for (N node : grid.nodeGraph.nodes()) {
            addGridLookup(grid, node.getPos());
        }
        for (EndpointPair<N> edge : grid.nodeGraph.edges()) {
            addGridLookupEdge(grid, positionsBetween(edge.nodeU().getPos(), edge.nodeV().getPos()));
        }
    }

    // region HELPERS
    private void removeGridLookup(Grid<?, ?> grid, BlockPos node) {

        Grid<?, ?> removed = gridPosLookup.remove(node);
        assert removed == grid;
    }

    private void addGridLookupEdge(Grid<?, ?> grid, Iterable<BlockPos> edge) {

        for (BlockPos pos : edge) {
            addGridLookup(grid, pos);
        }
    }

    private void addGridLookup(Grid<?, ?> grid, BlockPos node) {

        Grid<?, ?> removed = gridPosLookup.put(node, grid);
        assert removed == null;
    }

    private void replaceGridLookupEdge(Grid<?, ?> newGrid, Grid<?, ?> oldGrid, Iterable<BlockPos> edge) {

        for (BlockPos pos : edge) {
            replaceGridLookup(newGrid, oldGrid, pos);
        }
    }

    private void replaceGridLookup(Grid<?, ?> newGrid, Grid<?, ?> oldGrid, BlockPos pos) {

        Grid<?, ?> removed = gridPosLookup.put(pos, newGrid);
        assert removed == oldGrid;
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> void replaceGridLookup(G newGrid, G oldGrid, Set<N> nodes) {

        for (N node : nodes) {
            replaceGridLookup(newGrid, oldGrid, node.getPos());
        }
        for (EndpointPair<N> edge : newGrid.nodeGraph.edges()) {
            N u = edge.nodeU();
            N v = edge.nodeV();
            boolean containsU = nodes.contains(u);
            boolean containsV = nodes.contains(v);

            assert containsU == containsV; // Wat? Should be impossible. Don't update edges whilst merging/splitting.
            if (!containsU) {
                continue; // Skip.
            }
            replaceGridLookupEdge(newGrid, oldGrid, positionsBetween(u.getPos(), v.getPos()));
        }
    }

    private <G extends Grid<G, N>, N extends GridNode<G>> EnumMap<Direction, IDuct<G, N>> getAdjacentGrids(IDuct<G, N> host) {

        EnumMap<Direction, IDuct<G, N>> adjacentGrids = new EnumMap<>(Direction.class);
        for (Direction dir : DIRECTIONS) {
            IDuct<?, ?> other = GridHelper.getGridHost(world, host.getHostPos().relative(dir));
            if (other != null && canConnectTo(host, other, dir)) {
                adjacentGrids.put(dir, unsafeCast(other)); // canConnectTo asserts both grids are of the same type.
            }
        }
        return adjacentGrids;
    }

    public synchronized <G extends Grid<G, ?>> G createAndAddGrid(UUID uuid, IGridType<G> gridType, boolean load) {

        G grid = gridType.createGrid(uuid, world);
        grids.put(uuid, grid);
        if (load) {
            loadedGrids.put(uuid, grid);
        }
        return grid;
    }

    public UUID nextUUID() {

        while (true) {
            UUID uuid = UUID.randomUUID();
            if (USED_UUIDS.add(uuid) && !grids.containsKey(uuid)) {
                return uuid;
            }
        }
    }
    // endregion

}
