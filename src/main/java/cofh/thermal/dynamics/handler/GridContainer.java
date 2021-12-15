package cofh.thermal.dynamics.handler;

import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.*;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.network.client.GridDebugPacket;
import com.google.common.graph.EndpointPair;
import io.netty.buffer.Unpooled;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.StreamableIterable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.TickEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static net.covers1624.quack.collection.ColUtils.only;
import static net.covers1624.quack.collection.ColUtils.onlyOrDefault;
import static net.covers1624.quack.util.SneakyUtils.notPossible;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public class GridContainer implements IGridContainer, INBTSerializable<ListNBT> {

    private static final boolean DEBUG = GridContainer.class.desiredAssertionStatus();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<UUID> USED_UUIDS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<BlockPos, AbstractGrid<?, ?>> gridPosLookup = new HashMap<>();

    private final Map<UUID, AbstractGrid<?, ?>> grids = new HashMap<>();
    private final Map<UUID, AbstractGrid<?, ?>> loadedGrids = new HashMap<>();
    private final World world;

    private int tickCounter;

    public GridContainer(World world) {

        this.world = world;
    }

    @Override
    public void onGridHostPlaced(IGridHostInternal host) {

        EnumMap<Direction, IGridHostInternal> adjacentGrids = getAdjacentGrids(host);
        // We aren't adjacent to anything else, new grid.
        if (adjacentGrids.isEmpty()) {
            constructNewGrid(host);
            return;
        }

        if (adjacentGrids.size() == 1) {
            Map.Entry<Direction, IGridHostInternal> adjacent = ColUtils.only(adjacentGrids.entrySet());
            extendGrid(host, adjacent.getValue(), adjacent.getKey());
        } else {
            List<IGridHostInternal> branches = new ArrayList<>(adjacentGrids.values());
            // Merge grids!

            // Check if we would be merging 2 isolated grids.
            boolean sameGrid = StreamableIterable.of(branches)
                    .map(e -> e.getGrid().orElseThrow(notPossible()))
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

    private void constructNewGrid(IGridHostInternal host) {

        if (DEBUG) {
            LOGGER.info("Constructing new grid for {}", host.getHostPos());
        }
        assert host.getExposedTypes().size() == 1; // TODO, multi grids.
        AbstractGrid<?, ?> grid = createAndAddGrid(nextUUID(), host.getExposedTypes().iterator().next(), true);
        host.setGrid(grid);
        grid.newNode(host.getHostPos());
        addGridLookup(grid, host.getHostPos());
        grid.checkInvariant();
        grid.onModified();
    }

    private void extendGrid(IGridHostInternal host, IGridHost adjacent, Direction adjacentDir) {

        Optional<IGridNode<?>> adjacentNodeOpt = adjacent.getNode();
        Optional<IGrid<?, ?>> adjacentGridOpt = adjacent.getGrid();

        assert adjacentGridOpt.isPresent(); // All adjacent nodes should have a Grid when this method is called.
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) adjacentGridOpt.get();

        // Set the GridHost's grid.
        host.setGrid(grid);

        AbstractGridNode<?> newNode = grid.newNode(host.getHostPos());
        addGridLookup(grid, newNode.getPos());
        if (!adjacentNodeOpt.isPresent()) {
            // We are adding a duct next to another duct which does not have a node associated, we need to:
            // - Identify the 2 nodes at either end of this adjacent duct (a/b).
            // - Generate a new node at the adjacent position.
            // - Unlink the 'a/b' nodes from each other and re-link with the adjacent node.
            // - Add link to the node we just placed.
            AbstractGridNode<?> abMiddle = insertNode(grid, adjacent.getHostPos(), host.getHostPos());
            grid.nodeGraph.putEdgeValue(abMiddle, newNode, new HashSet<>());
            grid.checkInvariant();

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
            AbstractGridNode<?> adjacentNode = (AbstractGridNode<?>) adjacentNodeOpt.get();
            Set<AbstractGridNode<?>> edgeNodes = grid.nodeGraph.adjacentNodes(adjacentNode);

            AbstractGridNode<?> edge = onlyOrDefault(edgeNodes, null);
            if (edge != null && isOnSameAxis(newNode.getPos(), edge.getPos()) && !grid.canConnectToAdjacent(adjacentNode.getPos())) {
                Set<BlockPos> values = grid.nodeGraph.edgeValue(adjacentNode, edge);
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
                grid.removeNode(adjacentNode);
                grid.nodeGraph.putEdgeValue(newNode, edge, values);
                grid.checkInvariant();
            } else {
                if (DEBUG) {
                    LOGGER.info("Adding new single node. adjacent {}, new {}", adjacentNode.getPos(), newNode.getPos());
                }
                grid.nodeGraph.putEdgeValue(newNode, adjacentNode, new HashSet<>());
                grid.checkInvariant();
            }
        }
        grid.onModified();
    }

    /**
     * Inserts a node in the middle of an edge.
     * <p>
     * This method assumes there is not already a node at <code>pos</code>.
     *
     * @param grid The grid to add the node to.
     * @param pos  The position in the grid to generate the node.
     * @param from The position next to <code>pos</code> which triggered this insertion.
     *             May be <code>pos</code> if no adjacent position exist. This position will
     *             be ignored when searching for adjacent grid hosts to build the new node.
     * @return The new node at the position.
     */
    private AbstractGridNode<?> insertNode(AbstractGrid<?, ?> grid, BlockPos pos, BlockPos from) {
        // We are adding a duct, next to an existing duct that does not have a node.
        // There is only one valid case for this, where there are 2 nodes directly attached.

        assert grid.getNodes().get(pos) == null;

        // Find the 2 aforementioned existing nodes.
        List<Pair<IGridNode<?>, Set<BlockPos>>> attached = GridHelper.locateAttachedNodes(world, pos, from);
        assert attached.size() == 2;

        Pair<IGridNode<?>, Set<BlockPos>> a = attached.get(0);
        Pair<IGridNode<?>, Set<BlockPos>> b = attached.get(1);
        AbstractGridNode<?> attachedA = (AbstractGridNode<?>) a.getLeft();
        Set<BlockPos> attachedAValue = a.getRight();
        AbstractGridNode<?> attachedB = (AbstractGridNode<?>) b.getLeft();
        Set<BlockPos> attachedBValue = b.getRight();

        // Make sure these 2 nodes are actually connected.
        assert grid.nodeGraph.edgeValueOrDefault(attachedA, attachedB, null) != null;

        // Link new node to 2 existing nodes, remove edge between existing nodes.
        AbstractGridNode<?> abMiddle = grid.newNode(pos);
        grid.nodeGraph.putEdgeValue(abMiddle, attachedA, attachedAValue);
        grid.nodeGraph.putEdgeValue(abMiddle, attachedB, attachedBValue);
        Set<BlockPos> abValue = grid.nodeGraph.removeEdge(attachedA, attachedB);
        grid.checkInvariant();
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

    private void mergeGridBranches(IGridHostInternal host, List<IGridHostInternal> branches, boolean wasMerge) {

        assert branches.size() != 1;

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) branches.get(0).getGrid().orElseThrow(notPossible());
        host.setGrid(grid);
        // More than 2 branches, or the 2 adjacent branches aren't on the same axis as us. We must generate a node.
        AbstractGridNode<?> node = grid.newNode(host.getHostPos());
        addGridLookup(grid, node.getPos());
        for (IGridHost branch : branches) {
            Optional<IGridNode<?>> adjOpt = branch.getNode();
            if (!adjOpt.isPresent()) {
                // Adjacent isn't present, just generate node.
                AbstractGridNode<?> abMiddle = insertNode(grid, branch.getHostPos(), node.getPos());
                if (DEBUG) {
                    LOGGER.info(" T intersection creation. New Node: {}, Adjacent: {}",
                            node.getPos(),
                            abMiddle.getPos()
                    );
                }
                grid.nodeGraph.putEdgeValue(abMiddle, node, new HashSet<>());
                grid.checkInvariant();
            } else {
                AbstractGridNode<?> adj = (AbstractGridNode<?>) adjOpt.get();
                grid.nodeGraph.putEdgeValue(adj, node, new HashSet<>());
                grid.checkInvariant();
                simplifyNode(adj);
                if (DEBUG) {
                    LOGGER.info("Adding edge. {}, {}", adj.getPos(), node.getPos());
                }
            }
        }
        simplifyNode(node);
        grid.checkInvariant();
        grid.onModified();
    }

    private void mergeGrids(List<IGridHostInternal> branches) {

        Set<AbstractGrid<?, ?>> grids = new HashSet<>();
        for (IGridHostInternal branch : branches) {
            AbstractGrid<?, ?> abstractGrid = (AbstractGrid<?, ?>) branch.getGrid().orElseThrow(notPossible());
            grids.add(abstractGrid);
        }

        // Choose the largest grid as the 'main' grid.
        AbstractGrid<?, ?> main = ColUtils.maxBy(grids, e -> e.nodeGraph.nodes().size());
        grids.remove(main);
        for (AbstractGrid<?, ?> other : grids) {
            Set<AbstractGridNode<?>> toBeMoved = new HashSet<>(other.nodeGraph.nodes());
            main.mergeFrom(other);
            replaceGridLookup(main, other, toBeMoved);
            main.checkInvariant();
            this.grids.remove(other.getId());
            this.loadedGrids.remove(other.getId());
        }
        main.onModified();
    }

    @Override
    public void onGridHostDestroyed(IGridHostInternal host) {

        EnumMap<Direction, IGridHostInternal> adjacentHosts = getAdjacentGrids(host);
        if (adjacentHosts.isEmpty()) {
            // No adjacent grids, just remove the grid.
            removeSingleGrid(host);
            return;
        }

        if (adjacentHosts.size() == 1) {
            shrinkGrid(host, only(adjacentHosts.values()));
        } else {
            removeNode(host, adjacentHosts);
            separateGrids(host);
        }
    }

    @Override
    public void onGridHostNeighborChanged(IGridHostInternal host) {

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid()
                .orElseThrow(notPossible());
        Optional<IGridNode<?>> nodeOpt = host.getNode();
        boolean canConnect = grid.canConnectToAdjacent(host.getHostPos());
        if (nodeOpt.isPresent()) {
            if (!canConnect) {
                simplifyNode((AbstractGridNode<?>) nodeOpt.get());
            }
        } else {
            if (canConnect) {
                insertNode(grid, host.getHostPos(), host.getHostPos());
            }
        }
    }

    private void removeSingleGrid(IGridHostInternal host) {

        if (DEBUG) {
            LOGGER.info("Removing grid for {}", host.getHostPos());
        }
        assert host.getExposedTypes().size() == 1; // TODO, multi grids.

        assert host.getGrid().isPresent();
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid().get();
        assert grid.getNodes().size() == 1;
        assert grid.getNodes().containsKey(host.getHostPos());
        grids.remove(grid.getId());
        loadedGrids.remove(grid.getId());
        removeGridLookup(grid, host.getHostPos());
    }

    private void shrinkGrid(IGridHostInternal host, IGridHost adjacent) {

        Optional<IGridNode<?>> adjacentNodeOpt = adjacent.getNode();
        Optional<IGrid<?, ?>> adjacentGridOpt = adjacent.getGrid();

        assert adjacentGridOpt.isPresent(); // All adjacent nodes should have a Grid when this method is called.
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) adjacentGridOpt.get();

        assert host.getGrid().isPresent() && grid == host.getGrid().get();

        AbstractGridNode<?> currNode = (AbstractGridNode<?>) host.getNode()
                .orElseThrow(notPossible());

        removeGridLookup(grid, currNode.getPos());
        if (!adjacentNodeOpt.isPresent()) {
            // We are removing a node which is not adjacent to another node.
            // We must do the following:
            // - Remove the current node.
            // - Create a new node for our adjacent with path length decremented by one.
            AbstractGridNode<?> adjacentNode = grid.newNode(adjacent.getHostPos());

            Set<AbstractGridNode<?>> edgeNodes = grid.nodeGraph.adjacentNodes(currNode);
            AbstractGridNode<?> edge = only(edgeNodes);
            Set<BlockPos> edgeValue = grid.nodeGraph.edgeValue(edge, currNode);
            int preLen = edgeValue.size();
            edgeValue.remove(adjacentNode.getPos());
            grid.nodeGraph.putEdgeValue(adjacentNode, edge, edgeValue);
            if (DEBUG) {
                LOGGER.info("Removing dead end: Curr node: {}, Adjacent: {}, Edge: {}, Len: {}, New len: {}",
                        currNode.getPos(),
                        adjacentNode.getPos(),
                        edge.getPos(),
                        preLen,
                        edgeValue.size()
                );
            }
            grid.removeNode(currNode);
            grid.checkInvariant();
        } else {
            AbstractGridNode<?> adjacentNode = (AbstractGridNode<?>) adjacentNodeOpt.get();
            // Nuke the current node
            grid.removeNode(currNode);
            grid.checkInvariant();

            // Attempt simplification on our single adjacent node.
            simplifyNode(adjacentNode);
            grid.checkInvariant();
        }
        grid.onModified();
    }

    private void removeNode(IGridHostInternal host, EnumMap<Direction, IGridHostInternal> adjacentHosts) {
        // We are removing a host which has a node, we need to:
        // - Disconnect all connected edges.
        // - Iterate all adjacent hosts
        //  - If adjacent host has a node
        //   - Check if the node can be removed simplifying the grid.
        //  - If the adjacent host does not have a node.
        //   - Always create a new node.
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid()
                .orElseThrow(notPossible());

        removeGridLookup(grid, host.getHostPos());
        if (host.getNode().isPresent()) {
            // All we need to do is remove the node.
            AbstractGridNode<?> removing = (AbstractGridNode<?>) host.getNode().get();
            grid.removeNode(removing);
            grid.checkInvariant();
            if (DEBUG) {
                LOGGER.info("Removing node: {}", removing.getPos());
            }
        } else {
            // We are a host without a node, between 2 nodes.
            List<Pair<IGridNode<?>, Set<BlockPos>>> attached = GridHelper.locateAttachedNodes(world, host.getHostPos(), host.getHostPos());
            assert attached.size() == 2;
            AbstractGridNode<?> a = (AbstractGridNode<?>) attached.get(0).getLeft();
            AbstractGridNode<?> b = (AbstractGridNode<?>) attached.get(1).getLeft();
            grid.nodeGraph.removeEdge(a, b); // Yeet edge.
            grid.checkInvariant();
            if (DEBUG) {
                LOGGER.info("Removing edge between: {} and {}", a.getPos(), b.getPos());
            }
        }

        // Create/delete adjacent nodes if required
        for (IGridHostInternal adjHost : adjacentHosts.values()) {
            if (!adjHost.getNode().isPresent()) {
                // We always need to create a new node here.
                Pair<IGridNode<?>, Set<BlockPos>> foundEdge = only(GridHelper.locateAttachedNodes(world, adjHost.getHostPos(), host.getHostPos()));
                assert foundEdge != null;
                AbstractGridNode<?> newNode = grid.newNode(adjHost.getHostPos());
                AbstractGridNode<?> foundNode = (AbstractGridNode<?>) foundEdge.getLeft();
                Set<BlockPos> foundNodeValue = foundEdge.getRight();
                grid.nodeGraph.putEdgeValue(newNode, foundNode, foundNodeValue);
                grid.checkInvariant();
                if (DEBUG) {
                    LOGGER.info("Generating new node at {} with edge {} len {}", newNode.getPos(), foundNode.getPos(), foundNodeValue);
                }
            } else {
                // Attempt to simplify the node.
                AbstractGridNode<?> adjNode = (AbstractGridNode<?>) adjHost.getNode().get();
                simplifyNode(adjNode);
                grid.checkInvariant();
            }
        }
        grid.onModified();
    }

    private void simplifyNode(AbstractGridNode<?> node) {

        AbstractGrid<?, ?> grid = unsafeCast(node.getGrid());

        // We can't simplify if we can connect to adjacent blocks.
        if (grid.canConnectToAdjacent(node.getPos())) return;

        Set<AbstractGridNode<?>> edgesSet = grid.nodeGraph.adjacentNodes(node);

        // We can't simplify a node if there aren't exactly 2 edges.
        if (edgesSet.size() != 2) return;

        AbstractGridNode<?>[] edges = edgesSet.toArray(new AbstractGridNode[0]);

        // If both edges aren't on the same axis, then we can't simplify.
        if (!isOnSameAxis(edges[0].getPos(), edges[1].getPos())) return;

        HashSet<BlockPos> value = new HashSet<>();
        value.addAll(grid.nodeGraph.edgeValue(node, edges[0]));
        value.addAll(grid.nodeGraph.edgeValue(node, edges[1]));
        value.add(node.getPos());

        grid.removeNode(node);
        grid.nodeGraph.putEdgeValue(edges[0], edges[1], value);
        grid.checkInvariant();
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

    private void separateGrids(IGridHostInternal host) {

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid()
                .orElseThrow(notPossible());

        // Check if the grid
        List<Set<AbstractGridNode<?>>> splitGraphs = GraphHelper.separateGraphs(grid.nodeGraph);
        if (splitGraphs.size() <= 1) return;

        if (DEBUG) {
            LOGGER.info("Splitting grid into {} segments.", splitGraphs.size());
        }

        List<AbstractGrid<?, ?>> newGrids = grid.splitInto(splitGraphs);
        for (AbstractGrid<?, ?> newGrid : newGrids) {
            replaceGridLookup(newGrid, grid, newGrid.nodeGraph.nodes());
            newGrid.checkInvariant();
            newGrid.onModified();
        }
        grids.remove(grid.getId());
        loadedGrids.remove(grid.getId());
    }

    // region EVENT CALLBACKS
    public void onWorldTick(TickEvent.Phase phase) {
        // TODO do we want to pass this through to grids?
        if (phase != TickEvent.Phase.END) return;

        for (AbstractGrid<?, ?> value : loadedGrids.values()) {
            value.tick();
        }

        if (DEBUG && tickCounter % 10 == 0 && !loadedGrids.isEmpty()) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
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

    public void onChunkLoad(IChunk chunk) {

        for (AbstractGrid<?, ?> grid : grids.values()) {
            if (grid.onChunkLoad(chunk)) {
                assert !loadedGrids.containsKey(grid.getId());
                loadedGrids.put(grid.getId(), grid);
            }
        }
    }

    public void onChunkUnload(IChunk chunk) {

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
    public ListNBT serializeNBT() {

        ListNBT grids = new ListNBT();
        for (Map.Entry<UUID, AbstractGrid<?, ?>> entry : this.grids.entrySet()) {
            AbstractGrid<?, ?> grid = entry.getValue();
            CompoundNBT tag = new CompoundNBT();
            tag.putUUID("id", entry.getKey());
            tag.putString("type", grid.getGridType().getRegistryName().toString());
            tag.merge(grid.serializeNBT());
            grids.add(tag);
        }
        return grids;
    }

    @Override
    public void deserializeNBT(ListNBT nbt) {

        assert grids.isEmpty();
        for (int i = 0; i < nbt.size(); i++) {
            CompoundNBT tag = nbt.getCompound(i);
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
                addGridLookupEdge(grid, grid.nodeGraph.edgeValue(edge.nodeU(), edge.nodeV()));
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
            replaceGridLookupEdge(newGrid, oldGrid, newGrid.nodeGraph.edgeValue(u, v));
        }
    }

    private EnumMap<Direction, IGridHostInternal> getAdjacentGrids(IGridHost host) {

        EnumMap<Direction, IGridHostInternal> adjacentGrids = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            GridHelper.getGridHost(world, host.getHostPos().relative(dir))
                    .ifPresent(gridHost -> adjacentGrids.put(dir, (IGridHostInternal) gridHost));
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
    // endregion

}
