package cofh.thermal.dynamics.handler;

import cofh.core.network.packet.client.ModelUpdatePacket;
import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.Grid;
import cofh.thermal.dynamics.grid.GridNode;
import cofh.thermal.dynamics.network.client.GridDebugPacket;
import com.google.common.collect.ImmutableSet;
import com.google.common.graph.EndpointPair;
import io.netty.buffer.Unpooled;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.StreamableIterable;
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

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static cofh.thermal.dynamics.api.helper.GridHelper.*;
import static net.covers1624.quack.collection.ColUtils.onlyOrDefault;
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

    @Override
    public void onGridHostPlaced(IGridHost host) {

        EnumMap<Direction, IGridHost> adjacentGrids = getAdjacentGrids(host);
        // We aren't adjacent to anything else, new grid.
        if (adjacentGrids.isEmpty()) {
            constructNewGrid(host);
            return;
        }
        if (adjacentGrids.size() == 1) {
            Map.Entry<Direction, IGridHost> adjacent = ColUtils.only(adjacentGrids.entrySet());
            extendGrid(host, adjacent.getValue(), adjacent.getKey());
        } else {
            List<IGridHost> branches = new ArrayList<>(adjacentGrids.values());
            // Merge grids!

            // Check if we would be merging 2 isolated grids.
            boolean sameGrid = StreamableIterable.of(branches)
                    .map(IGridHost::getGrid)
                    .distinct()
                    .count() == 1;
            if (!sameGrid) {
                // Merge separate grids together.
                mergeGrids(branches);
            }
            // Grid only has branches between itself.
            mergeGridBranches(host, branches, !sameGrid);
        }
    }

    private void constructNewGrid(IGridHost host) {

        if (DEBUG) {
            LOGGER.info("Constructing new grid for {}", host.getHostPos());
        }
        assert host.getExposedTypes().size() == 1; // TODO, multi grids.
        Grid<?, ?> grid = createAndAddGrid(nextUUID(), host.getExposedTypes().iterator().next(), true);
        host.setGrid(grid);
        grid.newNode(host.getHostPos());
        addGridLookup(grid, host.getHostPos());
        grid.onGridHostAdded(host);
        grid.onModified();
    }

    private void extendGrid(IGridHost host, IGridHost adjacent, Direction adjacentDir) {

        GridNode<?> adjacentNode = adjacent.getNode();
        Grid<?, ?> adjacentGrid = adjacent.getGrid();

        // Set the GridHost's grid.
        host.setGrid(adjacentGrid);

        GridNode<?> newNode = adjacentGrid.newNode(host.getHostPos());
        addGridLookup(adjacentGrid, newNode.getPos());
        if (adjacentNode == null) {
            // We are adding a duct next to another duct which does not have a node associated, we need to:
            // - Identify the 2 nodes at either end of this adjacent duct (a/b).
            // - Generate a new node at the adjacent position.
            // - Unlink the 'a/b' nodes from each other and re-link with the adjacent node.
            // - Add link to the node we just placed.
            GridNode<?> abMiddle = getNodeOrSplitEdgeAndInsertNode(adjacentGrid, adjacent.getHostPos());
            adjacentGrid.nodeGraph.putEdge(abMiddle, newNode);

            if (DEBUG) {
                LOGGER.info(" T intersection creation. New Node: {}, Adjacent: {}",
                        newNode.getPos(),
                        abMiddle.getPos()
                );
            }
        } else {
            // We are appending to the end of a line of ducts, we need to:
            // - Check for any edges from the duct we are adding from, this could already be an intersection or a single duct grid.
            // - If there is a single edge to the node we are adjacent to and that edge is on the same axis as the new node:
            //   - Remove the edge, remove the node and re-link the edge to the new node and increment edge length by one.
            // - If there is 0, more than one edge, or the edge is not on the same axis, we can just link to the new node.
            Set<GridNode<?>> edgeNodes = adjacentGrid.nodeGraph.adjacentNodes(adjacentNode);

            GridNode<?> edge = onlyOrDefault(edgeNodes, null);
            if (edge != null && isOnSameAxis(newNode.getPos(), edge.getPos()) && !adjacentGrid.canConnectExternally(adjacentNode.getPos())) {
                if (DEBUG) {
                    LOGGER.info(
                            "Extending branch edge from {} to {} dist {}, new {}, newDist {}",
                            adjacentNode.getPos(),
                            edge.getPos(),
                            numBetween(adjacentNode.getPos(), edge.getPos()),
                            newNode.getPos(),
                            numBetween(newNode.getPos(), edge.getPos())
                    );
                }
                adjacentGrid.removeNode(adjacentNode);
                adjacentGrid.nodeGraph.putEdge(newNode, edge);
            } else {
                if (DEBUG) {
                    LOGGER.info("Adding new single node. adjacent {}, new {}", adjacentNode.getPos(), newNode.getPos());
                }
                adjacentGrid.nodeGraph.putEdge(newNode, adjacentNode);
            }
        }
        adjacentGrid.onGridHostAdded(host);
        adjacentGrid.onModified();
    }

    /**
     * Inserts a node in the middle of an edge.
     * <p>
     * If a node already exists at {@code pos}, it will be returned instead.
     *
     * @param grid The grid to add the node to.
     * @param pos  The position in the grid to generate the node.
     * @return The existing node, or the new node at the position.
     */
    private GridNode<?> getNodeOrSplitEdgeAndInsertNode(Grid<?, ?> grid, BlockPos pos) {

        GridNode<?> existing = grid.getNodes().get(pos);
        if (existing != null) return existing;

        // We are adding a duct, next to an existing duct that does not have a node.
        // There is only one valid case for this, where there are 2 nodes directly attached.

        // Find the edge which pos lies on.
        EndpointPair<GridNode<?>> foundEdge = grid.findEdge(pos);
        assert foundEdge != null;

        GridNode<?> a = foundEdge.nodeU();
        GridNode<?> b = foundEdge.nodeV();

        // Make sure these 2 nodes are actually connected.
        assert grid.nodeGraph.hasEdgeConnecting(a, b);

        // Link new node to 2 existing nodes, remove edge between existing nodes.
        GridNode<?> abMiddle = grid.newNode(pos);
        grid.nodeGraph.putEdge(abMiddle, a);
        grid.nodeGraph.putEdge(abMiddle, b);
        grid.nodeGraph.removeEdge(a, b);
        if (DEBUG) {
            LOGGER.info("Node insertion. Node A: {}, Node B: {}, AB dist: {}, Middle: {}, NewA dist: {}, NewB dist: {}",
                    a.getPos(),
                    b.getPos(),
                    numBetween(a.getPos(), b.getPos()),
                    abMiddle.getPos(),
                    numBetween(pos, a.getPos()),
                    numBetween(pos, b.getPos())
            );
        }
        return abMiddle;
    }

    private void mergeGridBranches(IGridHost host, List<IGridHost> branches, boolean wasMerge) {

        assert branches.size() != 1;

        Grid<?, ?> grid = branches.get(0).getGrid();

        host.setGrid(grid);
        // More than 2 branches, or the 2 adjacent branches aren't on the same axis as us. We must generate a node.
        GridNode<?> node = grid.newNode(host.getHostPos());
        addGridLookup(grid, node.getPos());
        for (IGridHost branch : branches) {
            GridNode<?> adj = branch.getNode();
            if (adj == null) {
                // Adjacent isn't present, just generate node.
                GridNode<?> abMiddle = getNodeOrSplitEdgeAndInsertNode(grid, branch.getHostPos());
                if (DEBUG) {
                    LOGGER.info(" T intersection creation. New Node: {}, Adjacent: {}",
                            node.getPos(),
                            abMiddle.getPos()
                    );
                }
                grid.nodeGraph.putEdge(abMiddle, node);
            } else {
                grid.nodeGraph.putEdge(adj, node);
                simplifyNode(adj);
                if (DEBUG) {
                    LOGGER.info("Adding edge. {}, {}", adj.getPos(), node.getPos());
                }
            }
        }
        simplifyNode(node);
        grid.onGridHostAdded(host);
        grid.onModified();
    }

    private void mergeGrids(List<IGridHost> branches) {

        Set<Grid<?, ?>> grids = new HashSet<>();
        for (IGridHost branch : branches) {
            Grid<?, ?> abstractGrid = branch.getGrid();

            grids.add(abstractGrid);
        }

        // Choose the largest grid as the 'main' grid.
        Grid<?, ?> main = ColUtils.maxBy(grids, e -> e.nodeGraph.nodes().size());
        grids.remove(main);
        for (Grid<?, ?> other : grids) {
            Set<GridNode<?>> toBeMoved = new HashSet<>(other.nodeGraph.nodes());
            main.mergeFrom(other);
            replaceGridLookup(main, other, toBeMoved);
            this.grids.remove(other.getId());
            this.loadedGrids.remove(other.getId());
        }
        main.onModified();
    }

    @Override
    public void onGridHostRemoved(IGridHost host) {

        // - Disconnect any edges.
        // - Remove current node.
        // - Try and split the grid if nodes exist.
        // - Delete grid if no more nodes exist.
        Grid<?, ?> grid = host.getGrid();
        BlockPos hostPos = host.getHostPos();

        GridNode<?> removing = host.getNode();

        removeGridLookup(grid, host.getHostPos());
        if (removing != null) {
            // All we need to do is remove the node.

            List<GridNode<?>> adjacentNodes = new LinkedList<>();
            // We must copy this otherwise, we get Concurrent modification exception modifying the grid with 'splitEdgeAndInsertNode'
            Set<EndpointPair<GridNode<?>>> allEdges = ImmutableSet.copyOf(grid.nodeGraph.incidentEdges(removing));
            for (EndpointPair<GridNode<?>> edge : allEdges) {
                GridNode<?> other = edge.nodeU() == removing ? edge.nodeV() : edge.nodeU();
                adjacentNodes.add(getNodeOrSplitEdgeAndInsertNode(grid, stepTowards(hostPos, other.getPos())));
            }

            if (DEBUG) {
                LOGGER.info("Removing node: {}", removing.getPos());
            }
            grid.removeNode(removing);
            // Might be redundant, safe to do anyway.
            for (GridNode<?> adjacentNode : adjacentNodes) {
                simplifyNode(adjacentNode);
            }
        } else {
            // We are a host without a node, on an edge.
            EndpointPair<GridNode<?>> edge = grid.findEdge(hostPos);
            assert edge != null : "Block does not lie on an edge.";
            GridNode<?> a = getNodeOrSplitEdgeAndInsertNode(grid, stepTowards(hostPos, edge.nodeU().getPos()));
            GridNode<?> b = getNodeOrSplitEdgeAndInsertNode(grid, stepTowards(hostPos, edge.nodeV().getPos()));
            grid.nodeGraph.removeEdge(a, b); // Yeet edge.
            if (DEBUG) {
                LOGGER.info("Removing edge between: {} and {}", a.getPos(), b.getPos());
            }

            // Might be redundant, safe to do anyway.
            simplifyNode(a);
            simplifyNode(b);
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
    public void onGridHostNeighborChanged(IGridHost host) {

        Grid<?, ?> grid = host.getGrid();

        GridNode<?> node = host.getNode();
        boolean canExternallyConnect = grid.canConnectExternally(host.getHostPos());
        if (node != null) {
            if (!canExternallyConnect) {
                simplifyNode(node);
            }
            node.clearConnections();
            ModelUpdatePacket.sendToClient(host.getHostWorld(), host.getHostPos());
        } else {
            if (canExternallyConnect) {
                getNodeOrSplitEdgeAndInsertNode(grid, host.getHostPos());
                ModelUpdatePacket.sendToClient(host.getHostWorld(), host.getHostPos());
            }
        }
    }

    @Override
    public void onGridHostSideConnected(IGridHost host, Direction side) {

        onSideConnectionChanged(host, side, false);
    }

    @Override
    public void onGridHostSideDisconnecting(IGridHost host, Direction side) {

        onSideConnectionChanged(host, side, true);
    }

    private void onSideConnectionChanged(IGridHost host, Direction changed, boolean disconnect) {
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
        IGridHost other = getAllAdjacentGrids(host).get(changed);
        if (other == null) return;

        // Grab grid and nodes either side of the split/join.
        // If nodes don't exist, insert temporary nodes, these will be cleaned up later.
        Grid<?, ?> aGrid = host.getGrid();
        Grid<?, ?> bGrid = other.getGrid();

        // If edge being updated is already part of the grid
        boolean connecting = !disconnect;
        boolean nodesConnected = aGrid == bGrid && aGrid.isConnectedTo(host.getHostPos(), other.getHostPos());
        if (connecting == nodesConnected) {
            return; // Nothing to do.
        }

        if (connecting && (!host.canConnectTo(other, changed) || !other.canConnectTo(host, changed.getOpposite()))) {
            return; // can't connect anyway
        }

        GridNode<?> a = getNodeOrSplitEdgeAndInsertNode(aGrid, host.getHostPos());
        GridNode<?> b = getNodeOrSplitEdgeAndInsertNode(bGrid, other.getHostPos());

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
            for (GridNode<?> adj : aGrid.nodeGraph.adjacentNodes(a)) {
                if (adj == b) continue;
                if (aGrid.nodeGraph.hasEdgeConnecting(a, adj)) continue;
                simplifyNode(adj);
            }
            // Simplify all directly adjacent (in world) nodes of b (excluding a)
            for (GridNode<?> adj : aGrid.nodeGraph.adjacentNodes(b)) {
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
    }

    private void simplifyNode(GridNode<?> node) {

        Grid<?, ?> grid = unsafeCast(node.getGrid());
        if (!grid.nodeGraph.nodes().contains(node)) return;

        // We can't simplify if we can connect to adjacent blocks.
        if (grid.canConnectExternally(node.getPos())) {
            return;
        }
        Set<GridNode<?>> edgesSet = grid.nodeGraph.adjacentNodes(node);

        // We can't simplify a node if there aren't exactly 2 edges.
        if (edgesSet.size() != 2) {
            return;
        }
        GridNode<?>[] edges = edgesSet.toArray(new GridNode[0]);

        // If both edges aren't on the same axis, then we can't simplify.
        if (!isOnSameAxis(edges[0].getPos(), edges[1].getPos())) {
            return;
        }

        grid.removeNode(node);
        grid.nodeGraph.putEdge(edges[0], edges[1]);
        // grid.checkInvariant();
        if (DEBUG) {
            LOGGER.info(
                    "Simplifying grid node '{}' A {}, B {}, Len {}",
                    node.getPos(),
                    edges[0].getPos(),
                    edges[1].getPos(),
                    numBetween(edges[0].getPos(), edges[1].getPos())
            );
        }
    }

    private boolean separateGrids(Grid<?, ?> grid) {

        // Generate the grid nodes isolated from each other.
        List<Set<GridNode<?>>> splitGraphs = GraphHelper.separateGraphs(grid.nodeGraph);
        if (splitGraphs.size() <= 1) return false;

        if (DEBUG) {
            LOGGER.info("Splitting grid into {} segments.", splitGraphs.size());
        }

        List<Grid<?, ?>> newGrids = grid.splitInto(splitGraphs);
        for (Grid<?, ?> newGrid : newGrids) {
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
        for (Grid<?, ?> value : loadedGrids.values()) {
            value.tick();
        }
        if (DEBUG && tickCounter % 10 == 0 && !loadedGrids.isEmpty()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(loadedGrids.size());
            for (Grid<?, ?> value : loadedGrids.values()) {

                Map<BlockPos, GridNode<?>> nodes = unsafeCast(value.getNodes());
                buffer.writeUUID(value.getId());
                buffer.writeVarInt(nodes.size());
                for (GridNode<?> node : nodes.values()) {
                    buffer.writeBlockPos(node.getPos());
                    Set<GridNode<?>> edges = value.nodeGraph.adjacentNodes(node);
                    buffer.writeVarInt(edges.size());
                    for (GridNode<?> edge : edges) {
                        buffer.writeBlockPos(edge.getPos());
                    }
                }
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

    public void onChunkUnload(ChunkAccess chunk) {

        Set<UUID> rem = new HashSet<>(2);
        for (Grid<?, ?> grid : loadedGrids.values()) {
            if (grid.onChunkUnload(chunk)) {
                rem.add(grid.getId());
            }
        }
        loadedGrids.keySet().removeAll(rem);
    }
    // endregion

    @Nullable
    @Override
    public Grid<?, ?> getGrid(UUID id) {

        return grids.get(id);
    }

    @Nullable
    @Override
    public Grid<?, ?> getGrid(BlockPos pos) {

        return gridPosLookup.get(pos);
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
            Grid<?, ?> grid = createAndAddGrid(id, gridType, false);
            grid.deserializeNBT(tag);

            for (GridNode<?> node : grid.nodeGraph.nodes()) {
                addGridLookup(grid, node.getPos());
            }
            for (EndpointPair<GridNode<?>> edge : grid.nodeGraph.edges()) {
                addGridLookupEdge(grid, positionsBetween(edge.nodeU().getPos(), edge.nodeV().getPos()));
            }
        }
        if (DEBUG) {
            LOGGER.info("Loaded {} grids for {}.", grids.size(), world.dimension().location());
        }
    }

    // region HELPERS
    private void removeGridLookupEdge(Grid<?, ?> grid, Set<BlockPos> edge) {

        for (BlockPos pos : edge) {
            removeGridLookup(grid, pos);
        }
    }

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

    private void replaceGridLookup(Grid<?, ?> newGrid, Grid<?, ?> oldGrid, Set<GridNode<?>> nodes) {

        for (GridNode<?> node : nodes) {
            replaceGridLookup(newGrid, oldGrid, node.getPos());
        }
        for (EndpointPair<GridNode<?>> edge : newGrid.nodeGraph.edges()) {
            GridNode<?> u = edge.nodeU();
            GridNode<?> v = edge.nodeV();
            boolean containsU = nodes.contains(u);
            boolean containsV = nodes.contains(v);

            assert containsU == containsV; // Wat? Should be impossible. Don't update edges whilst merging/splitting.
            if (!containsU) {
                continue; // Skip.
            }
            replaceGridLookupEdge(newGrid, oldGrid, positionsBetween(u.getPos(), v.getPos()));
        }
    }

    private EnumMap<Direction, IGridHost> getAdjacentGrids(IGridHost host) {

        EnumMap<Direction, IGridHost> adjacentGrids = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            Optional<IGridHost> otherOpt = GridHelper.getGridHost(world, host.getHostPos().relative(dir));
            if (otherOpt.isPresent()) {
                IGridHost other = otherOpt.get();
                // Ignore grids which don't expose any of our types.
                if (host.canConnectTo(other, dir) && other.canConnectTo(host, dir.getOpposite())) {
                    adjacentGrids.put(dir, other);
                }
            }
        }
        return adjacentGrids;
    }

    private EnumMap<Direction, IGridHost> getAllAdjacentGrids(IGridHost host) {

        EnumMap<Direction, IGridHost> adjacentGrids = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            Optional<IGridHost> otherOpt = GridHelper.getGridHost(world, host.getHostPos().relative(dir));
            if (otherOpt.isPresent()) {
                adjacentGrids.put(dir, otherOpt.get());
            }
        }
        return adjacentGrids;
    }

    public Grid<?, ?> createAndAddGrid(UUID uuid, IGridType<?> gridType, boolean load) {

        Grid<?, ?> grid = gridType.createGrid(uuid, world);
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
