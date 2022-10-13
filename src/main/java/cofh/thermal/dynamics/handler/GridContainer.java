package cofh.thermal.dynamics.handler;

import cofh.core.network.packet.client.ModelUpdatePacket;
import cofh.lib.util.helpers.BlockHelper;
import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.IGrid;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
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

import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.collection.ColUtils.only;
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

    private final Map<BlockPos, AbstractGrid<?, ?>> gridPosLookup = new HashMap<>();

    private final Map<UUID, AbstractGrid<?, ?>> grids = new HashMap<>();
    private final Map<UUID, AbstractGrid<?, ?>> loadedGrids = new HashMap<>();
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
        AbstractGrid<?, ?> grid = createAndAddGrid(nextUUID(), host.getExposedTypes().iterator().next(), true);
        host.setGrid(grid);
        grid.newNode(host.getHostPos());
        addGridLookup(grid, host.getHostPos());
        grid.onGridHostAdded(host);
        grid.onModified();
    }

    private void extendGrid(IGridHost host, IGridHost adjacent, Direction adjacentDir) {

        AbstractGridNode<?> adjacentNode = (AbstractGridNode<?>) adjacent.getNode();
        AbstractGrid<?, ?> adjacentGrid = (AbstractGrid<?, ?>) adjacent.getGrid();

        // Set the GridHost's grid.
        host.setGrid(adjacentGrid);

        AbstractGridNode<?> newNode = adjacentGrid.newNode(host.getHostPos());
        addGridLookup(adjacentGrid, newNode.getPos());
        if (adjacentNode == null) {
            // We are adding a duct next to another duct which does not have a node associated, we need to:
            // - Identify the 2 nodes at either end of this adjacent duct (a/b).
            // - Generate a new node at the adjacent position.
            // - Unlink the 'a/b' nodes from each other and re-link with the adjacent node.
            // - Add link to the node we just placed.
            AbstractGridNode<?> abMiddle = getNodeOrSplitEdgeAndInsertNode(adjacentGrid, adjacent.getHostPos());
            adjacentGrid.nodeGraph.putEdgeValue(abMiddle, newNode, new HashSet<>());

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
            Set<AbstractGridNode<?>> edgeNodes = adjacentGrid.nodeGraph.adjacentNodes(adjacentNode);

            AbstractGridNode<?> edge = onlyOrDefault(edgeNodes, null);
            if (edge != null && isOnSameAxis(newNode.getPos(), edge.getPos()) && !adjacentGrid.canConnectExternally(adjacentNode.getPos())) {
                Set<BlockPos> values = adjacentGrid.nodeGraph.edgeValueOrDefault(adjacentNode, edge, null);
                assert values != null;
                int oldLen = values.size();
                values.add(adjacentNode.getPos());
                if (DEBUG) {
                    LOGGER.info(
                            "Extending branch edge from {} to {} dist {}, new {}, newDist {}",
                            adjacentNode.getPos(),
                            edge.getPos(),
                            oldLen,
                            newNode.getPos(),
                            values.size()
                    );
                }
                adjacentGrid.removeNode(adjacentNode);
                adjacentGrid.nodeGraph.putEdgeValue(newNode, edge, values);
            } else {
                if (DEBUG) {
                    LOGGER.info("Adding new single node. adjacent {}, new {}", adjacentNode.getPos(), newNode.getPos());
                }
                adjacentGrid.nodeGraph.putEdgeValue(newNode, adjacentNode, new HashSet<>());
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
    private AbstractGridNode<?> getNodeOrSplitEdgeAndInsertNode(AbstractGrid<?, ?> grid, BlockPos pos) {
        AbstractGridNode<?> existing = (AbstractGridNode<?>) grid.getNodes().get(pos);
        if (existing != null) return existing;

        // We are adding a duct, next to an existing duct that does not have a node.
        // There is only one valid case for this, where there are 2 nodes directly attached.

        // Find the edge which pos lies on.
        EndpointPair<AbstractGridNode<?>> foundEdge = grid.findEdge(pos);
        assert foundEdge != null;

        AbstractGridNode<?> attachedA = foundEdge.nodeU();
        Set<BlockPos> attachedAValue = GridHelper.getPositionsBetween(pos, attachedA.getPos());
        AbstractGridNode<?> attachedB = foundEdge.nodeV();
        Set<BlockPos> attachedBValue = GridHelper.getPositionsBetween(pos, attachedB.getPos());

        // Make sure these 2 nodes are actually connected.
        assert grid.nodeGraph.edgeValueOrDefault(attachedA, attachedB, null) != null;

        // Link new node to 2 existing nodes, remove edge between existing nodes.
        AbstractGridNode<?> abMiddle = grid.newNode(pos);
        grid.nodeGraph.putEdgeValue(abMiddle, attachedA, attachedAValue);
        grid.nodeGraph.putEdgeValue(abMiddle, attachedB, attachedBValue);
        Set<BlockPos> abValue = grid.nodeGraph.removeEdge(attachedA, attachedB);
        if (DEBUG) {
            LOGGER.info("Node insertion. Node A: {}, Node B: {}, AB dist: {}, Middle: {}, NewA dist: {}, NewB dist: {}",
                    attachedA.getPos(),
                    attachedB.getPos(),
                    abValue.size(),
                    abMiddle.getPos(),
                    attachedAValue.size(),
                    attachedBValue.size()
            );
        }
        return abMiddle;
    }

    private void mergeGridBranches(IGridHost host, List<IGridHost> branches, boolean wasMerge) {

        assert branches.size() != 1;

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) branches.get(0).getGrid();

        host.setGrid(grid);
        // More than 2 branches, or the 2 adjacent branches aren't on the same axis as us. We must generate a node.
        AbstractGridNode<?> node = grid.newNode(host.getHostPos());
        addGridLookup(grid, node.getPos());
        for (IGridHost branch : branches) {
            AbstractGridNode<?> adj = (AbstractGridNode<?>) branch.getNode();
            if (adj == null) {
                // Adjacent isn't present, just generate node.
                AbstractGridNode<?> abMiddle = getNodeOrSplitEdgeAndInsertNode(grid, branch.getHostPos());
                if (DEBUG) {
                    LOGGER.info(" T intersection creation. New Node: {}, Adjacent: {}",
                            node.getPos(),
                            abMiddle.getPos()
                    );
                }
                grid.nodeGraph.putEdgeValue(abMiddle, node, new HashSet<>());
            } else {
                grid.nodeGraph.putEdgeValue(adj, node, new HashSet<>());
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

        Set<AbstractGrid<?, ?>> grids = new HashSet<>();
        for (IGridHost branch : branches) {
            AbstractGrid<?, ?> abstractGrid = (AbstractGrid<?, ?>) branch.getGrid();

            grids.add(abstractGrid);
        }

        // Choose the largest grid as the 'main' grid.
        AbstractGrid<?, ?> main = ColUtils.maxBy(grids, e -> e.nodeGraph.nodes().size());
        grids.remove(main);
        for (AbstractGrid<?, ?> other : grids) {
            Set<AbstractGridNode<?>> toBeMoved = new HashSet<>(other.nodeGraph.nodes());
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
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid();
        BlockPos hostPos = host.getHostPos();

        AbstractGridNode<?> removing = (AbstractGridNode<?>) host.getNode();

        removeGridLookup(grid, host.getHostPos());
        if (removing != null) {
            // All we need to do is remove the node.

            List<AbstractGridNode<?>> adjacentNodes = new LinkedList<>();
            // We must copy this otherwise, we get Concurrent modification exception modifying the grid with 'splitEdgeAndInsertNode'
            Set<EndpointPair<AbstractGridNode<?>>> allEdges = ImmutableSet.copyOf(grid.nodeGraph.incidentEdges(removing));
            for (EndpointPair<AbstractGridNode<?>> edge : allEdges) {
                AbstractGridNode<?> other = edge.nodeU() == removing ? edge.nodeV() : edge.nodeU();
                adjacentNodes.add(getNodeOrSplitEdgeAndInsertNode(grid, stepTowards(hostPos, other.getPos())));
            }

            if (DEBUG) {
                LOGGER.info("Removing node: {}", removing.getPos());
            }
            grid.removeNode(removing);
            // Might be redundant, safe to do anyway.
            for (AbstractGridNode<?> adjacentNode : adjacentNodes) {
                simplifyNode(adjacentNode);
            }
        } else {
            // We are a host without a node, on an edge.
            EndpointPair<AbstractGridNode<?>> edge = grid.findEdge(hostPos);
            assert edge != null : "Block does not lie on an edge.";
            AbstractGridNode<?> a = getNodeOrSplitEdgeAndInsertNode(grid, stepTowards(hostPos, edge.nodeU().getPos()));
            AbstractGridNode<?> b = getNodeOrSplitEdgeAndInsertNode(grid, stepTowards(hostPos, edge.nodeV().getPos()));
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

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid();

        AbstractGridNode<?> node = (AbstractGridNode<?>) host.getNode();
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
        AbstractGrid<?, ?> aGrid = (AbstractGrid<?, ?>) host.getGrid();
        AbstractGrid<?, ?> bGrid = (AbstractGrid<?, ?>) other.getGrid();

        // If edge being updated is already part of the grid
        boolean connecting = !disconnect;
        boolean nodesConnected = aGrid == bGrid && aGrid.isConnectedTo(host.getHostPos(), other.getHostPos());
        if (connecting == nodesConnected) {
            return; // Nothing to do.
        }

        if (connecting && (!host.canConnectTo(other, changed) || !other.canConnectTo(host, changed.getOpposite()))) {
            return; // can't connect anyway
        }

        AbstractGridNode<?> a = getNodeOrSplitEdgeAndInsertNode(aGrid, host.getHostPos());
        AbstractGridNode<?> b = getNodeOrSplitEdgeAndInsertNode(bGrid, other.getHostPos());

        if (connecting) {
            // Connect
            if (DEBUG) {
                LOGGER.info("Connecting nodes due to connectability change. A {}, B {}.", a.getPos(), b.getPos());
            }

            // Perform grid merge.
            if (aGrid != bGrid) {
                mergeGrids(Arrays.asList(host, other));
                aGrid = (AbstractGrid<?, ?>) a.getGrid();
            }

            // Perform connection. (we know these are directly adjacent.)
            aGrid.nodeGraph.putEdgeValue(a, b, new HashSet<>());

            // Simplify all directly adjacent (in world) nodes of a (excluding b).
            for (AbstractGridNode<?> adj : aGrid.nodeGraph.adjacentNodes(a)) {
                if (adj == b) continue;
                if (aGrid.nodeGraph.edgeValue(a, adj).isPresent()) continue;
                simplifyNode(adj);
            }
            // Simplify all directly adjacent (in world) nodes of b (excluding a)
            for (AbstractGridNode<?> adj : aGrid.nodeGraph.adjacentNodes(b)) {
                if (adj == a) continue;
                if (aGrid.nodeGraph.edgeValue(b, adj).isPresent()) continue;
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

    private void simplifyNode(AbstractGridNode<?> node) {

        AbstractGrid<?, ?> grid = unsafeCast(node.getGrid());
        if (!grid.nodeGraph.nodes().contains(node)) return;

        // We can't simplify if we can connect to adjacent blocks.
        if (grid.canConnectExternally(node.getPos())) {
            return;
        }
        Set<AbstractGridNode<?>> edgesSet = grid.nodeGraph.adjacentNodes(node);

        // We can't simplify a node if there aren't exactly 2 edges.
        if (edgesSet.size() != 2) {
            return;
        }
        AbstractGridNode<?>[] edges = edgesSet.toArray(new AbstractGridNode[0]);

        // If both edges aren't on the same axis, then we can't simplify.
        if (!isOnSameAxis(edges[0].getPos(), edges[1].getPos())) {
            return;
        }
        HashSet<BlockPos> value = new HashSet<>();
        value.addAll(requireNonNull(grid.nodeGraph.edgeValueOrDefault(node, edges[0], null)));
        value.addAll(requireNonNull(grid.nodeGraph.edgeValueOrDefault(node, edges[1], null)));
        value.add(node.getPos());

        grid.removeNode(node);
        grid.nodeGraph.putEdgeValue(edges[0], edges[1], value);
        // grid.checkInvariant();
        if (DEBUG) {
            LOGGER.info(
                    "Simplifying grid node '{}' A {}, B {}, Len {}",
                    node.getPos(),
                    edges[0].getPos(),
                    edges[1].getPos(),
                    value.size()
            );
        }
    }

    private boolean separateGrids(AbstractGrid<?, ?> grid) {

        // Generate the grid nodes isolated from each other.
        List<Set<AbstractGridNode<?>>> splitGraphs = GraphHelper.separateGraphs(grid.nodeGraph);
        if (splitGraphs.size() <= 1) return false;

        if (DEBUG) {
            LOGGER.info("Splitting grid into {} segments.", splitGraphs.size());
        }

        List<AbstractGrid<?, ?>> newGrids = grid.splitInto(splitGraphs);
        for (AbstractGrid<?, ?> newGrid : newGrids) {
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
        for (AbstractGrid<?, ?> value : loadedGrids.values()) {
            value.tick();
        }
        if (DEBUG && tickCounter % 10 == 0 && !loadedGrids.isEmpty()) {
            FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
            buffer.writeVarInt(loadedGrids.size());
            for (AbstractGrid<?, ?> value : loadedGrids.values()) {
                Map<BlockPos, AbstractGridNode<?>> nodes = (Map<BlockPos, AbstractGridNode<?>>) value.getNodes();
                buffer.writeUUID(value.getId());
                buffer.writeVarInt(nodes.size());
                for (AbstractGridNode<?> node : nodes.values()) {
                    buffer.writeBlockPos(node.getPos());
                    Set<AbstractGridNode<?>> edges = value.nodeGraph.adjacentNodes(node);
                    buffer.writeVarInt(edges.size());
                    for (AbstractGridNode<?> edge : edges) {
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

        for (AbstractGrid<?, ?> grid : grids.values()) {
            if (grid.onChunkLoad(chunk)) {
                assert !loadedGrids.containsKey(grid.getId());
                loadedGrids.put(grid.getId(), grid);
            }
        }
    }

    public void onChunkUnload(ChunkAccess chunk) {

        Set<UUID> rem = new HashSet<>(2);
        for (AbstractGrid<?, ?> grid : loadedGrids.values()) {
            if (grid.onChunkUnload(chunk)) {
                rem.add(grid.getId());
            }
        }
        loadedGrids.keySet().removeAll(rem);
    }
    // endregion

    @Nullable
    @Override
    public IGrid<?, ?> getGrid(UUID id) {

        return grids.get(id);
    }

    @Nullable
    @Override
    public IGrid<?, ?> getGrid(BlockPos pos) {

        return gridPosLookup.get(pos);
    }

    @Override
    public ListTag serializeNBT() {

        ListTag grids = new ListTag();
        for (Map.Entry<UUID, AbstractGrid<?, ?>> entry : this.grids.entrySet()) {
            AbstractGrid<?, ?> grid = entry.getValue();
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
            AbstractGrid<?, ?> grid = createAndAddGrid(id, gridType, false);
            grid.deserializeNBT(tag);

            for (AbstractGridNode<?> node : grid.nodeGraph.nodes()) {
                addGridLookup(grid, node.getPos());
            }
            for (EndpointPair<AbstractGridNode<?>> edge : grid.nodeGraph.edges()) {
                addGridLookupEdge(grid, requireNonNull(grid.nodeGraph.edgeValueOrDefault(edge.nodeU(), edge.nodeV(), null)));
            }
        }
        if (DEBUG) {
            LOGGER.info("Loaded {} grids for {}.", grids.size(), world.dimension().location());
        }
    }

    // region HELPERS
    private void removeGridLookupEdge(AbstractGrid<?, ?> grid, Set<BlockPos> edge) {

        for (BlockPos pos : edge) {
            removeGridLookup(grid, pos);
        }
    }

    private void removeGridLookup(AbstractGrid<?, ?> grid, BlockPos node) {

        AbstractGrid<?, ?> removed = gridPosLookup.remove(node);
        assert removed == grid;
    }

    private void addGridLookupEdge(AbstractGrid<?, ?> grid, Set<BlockPos> edge) {

        for (BlockPos pos : edge) {
            addGridLookup(grid, pos);
        }
    }

    private void addGridLookup(AbstractGrid<?, ?> grid, BlockPos node) {

        AbstractGrid<?, ?> removed = gridPosLookup.put(node, grid);
        assert removed == null;
    }

    private void replaceGridLookupEdge(AbstractGrid<?, ?> newGrid, AbstractGrid<?, ?> oldGrid, Set<BlockPos> edge) {

        for (BlockPos pos : edge) {
            replaceGridLookup(newGrid, oldGrid, pos);
        }
    }

    private void replaceGridLookup(AbstractGrid<?, ?> newGrid, AbstractGrid<?, ?> oldGrid, BlockPos pos) {

        AbstractGrid<?, ?> removed = gridPosLookup.put(pos, newGrid);
        assert removed == oldGrid;
    }

    private void replaceGridLookup(AbstractGrid<?, ?> newGrid, AbstractGrid<?, ?> oldGrid, Set<AbstractGridNode<?>> nodes) {

        for (AbstractGridNode<?> node : nodes) {
            replaceGridLookup(newGrid, oldGrid, node.getPos());
        }
        for (EndpointPair<AbstractGridNode<?>> edge : newGrid.nodeGraph.edges()) {
            AbstractGridNode<?> u = edge.nodeU();
            AbstractGridNode<?> v = edge.nodeV();
            boolean containsU = nodes.contains(u);
            boolean containsV = nodes.contains(v);

            assert containsU == containsV; // Wat? Should be impossible. Don't update edges whilst merging/splitting.
            if (!containsU) {
                continue; // Skip.
            }
            replaceGridLookupEdge(newGrid, oldGrid, requireNonNull(newGrid.nodeGraph.edgeValueOrDefault(u, v, null)));
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

    public AbstractGrid<?, ?> createAndAddGrid(UUID uuid, IGridType<?> gridType, boolean load) {

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) gridType.createGrid(uuid, world);
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

    private static boolean isOnSameAxis(BlockPos a, BlockPos b) {

        boolean x = a.getX() == b.getX();
        boolean y = a.getY() == b.getY();
        boolean z = a.getZ() == b.getZ();
        if (x && y) return true; // Z axis
        if (x && z) return true; // Y axis
        if (y && z) return true; // X axis
        return x && y && z; // Handle no match, or same block.
    }

    private static BlockPos stepTowards(BlockPos from, BlockPos towards) {

        assert isOnSameAxis(from, towards) : "Not on the same axis";
        Direction dir = BlockHelper.getSide(towards.subtract(from));
        assert dir != null : "Not on the same axis??";

        return from.relative(dir);
    }
    // endregion

}
