package cofh.thermal.dynamics.grid;

import cofh.thermal.dynamics.api.grid.*;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.handler.GridContainer;
import com.google.common.graph.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.LongFunction;

import static cofh.thermal.dynamics.api.helper.GridHelper.numBetween;
import static java.util.Objects.requireNonNull;
import static net.covers1624.quack.util.SneakyUtils.notPossible;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Represents a Grid of nodes in a World.
 * <p>
 * This may not be a complete grid as subsections may be unloaded/loaded
 * as chunks load/unload.
 * In these cases, it acts as if these unloaded sections of the grid do not exist.
 * <p>
 * Modifications of the Grid system outside well-defined methods on this interface or {@link GridContainer} are forbidden.
 * Any such modifications may cause the Grid to explode.
 * <p>
 *
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public abstract class Grid<G extends Grid<G, N>, N extends GridNode<G>> implements INBTSerializable<CompoundTag> {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final boolean DEBUG = Grid.class.desiredAssertionStatus();

    /**
     * The {@link ValueGraph} of nodes stored in this grid.
     * <p>
     * The value object being the path length.
     */
    public final MutableGraph<N> nodeGraph = GraphBuilder
            .undirected()
            .nodeOrder(ElementOrder.unordered())
            .build();
    /**
     * The same list of nodes, indexed by BlockPos.
     */
    protected final Map<BlockPos, N> nodes = new Object2ObjectRBTreeMap<>();
    /**
     * The same list of nodes, indexed by ChunkPos.
     */
    protected final Long2ObjectMap<List<N>> nodesPerChunk = new Long2ObjectRBTreeMap<>();
    protected final LongSet loadedChunks = new LongOpenHashSet();
    protected final Set<BlockPos> updatableHosts = new HashSet<>();
    protected final IGridType<G> gridType;
    protected final UUID id;
    protected final Level world;
    public boolean isLoaded;

    protected Grid(IGridType<G> gridType, UUID id, Level world) {

        this.gridType = gridType;
        this.id = id;
        this.world = world;
    }

    public void tick() {

        for (LongIterator iterator = loadedChunks.iterator(); iterator.hasNext(); ) {
            long loadedChunk = iterator.nextLong();
            List<N> nodes = nodesPerChunk.get(loadedChunk);
            if (nodes == null) {
                continue;
            }
            for (GridNode<?> node : nodes) {
                assert node.isLoaded();
                if (node instanceof ITickableGridNode) {
                    ((ITickableGridNode) node).tick();
                }
            }
        }
    }

    public void checkInvariant() {

        // Save some CPU when assertions are disabled.
        if (!DEBUG) {
            return;
        }
        GridContainer gridContainer = ((GridContainer) IGridContainer.getCapability(world)
                .orElseThrow(notPossible()));

        // Check all blocks on edges.
        for (EndpointPair<N> edge : nodeGraph.edges()) {
            GridNode<?> u = edge.nodeU();
            GridNode<?> v = edge.nodeV();
            for (BlockPos pos : GridHelper.positionsBetween(u.getPos(), v.getPos())) {
                checkPos(pos, gridContainer);
            }
        }

        // Check nodes.
        for (GridNode<?> node : nodeGraph.nodes()) {

            long chunkPos = asChunkLong(node.getPos());
            checkPos(node.getPos(), gridContainer);
            assert node.grid == this;
            assert nodes.get(node.getPos()) == node;
            assert nodesPerChunk.get(chunkPos) != null;
            assert nodesPerChunk.get(chunkPos).contains(node);
            assert node.isLoaded() == loadedChunks.contains(chunkPos);
        }

        for (BlockPos pos : updatableHosts) {
            if (!world.isLoaded(pos)) {
                continue;
            }
            Optional<IGridHost> gridHostOpt = GridHelper.getGridHost(world, pos);
            assert gridHostOpt.isPresent();
            assert gridHostOpt.get() instanceof IGridHostUpdateable;
        }
    }

    private void checkPos(BlockPos pos, GridContainer gridContainer) {

        if (!world.isLoaded(pos)) {
            return;
        }
        Optional<IGridHost> gridHostOpt = GridHelper.getGridHost(world, pos);
        assert gridHostOpt.isPresent();
        IGridHost gridHost = gridHostOpt.get();
        assert gridHost.getGrid() == this;
        assert !(gridHost instanceof IGridHostUpdateable) || updatableHosts.contains(gridHost.getHostPos());

        assert gridContainer.getGrid(pos) == this;
    }

    // returns true if this grid changes its loaded state to true.
    public boolean onChunkLoad(ChunkAccess chunk) {

        long pos = chunk.getPos().toLong();
        List<N> nodes = nodesPerChunk.get(pos);
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        assert !loadedChunks.contains(pos);
        loadedChunks.add(pos);

        for (N node : nodes) {
            node.setLoaded(true);
        }
        boolean wasLoaded = !isLoaded;
        isLoaded = true;
        return wasLoaded;
    }

    // returns true if this grid changes its loaded state to false.
    public boolean onChunkUnload(ChunkAccess chunk) {

        long pos = chunk.getPos().toLong();
        List<N> nodes = nodesPerChunk.get(pos);
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        assert loadedChunks.contains(pos);
        boolean wasLoaded = loadedChunks.size() == 1;
        loadedChunks.remove(pos);
        for (N node : nodes) {
            node.setLoaded(false);
        }
        assert !wasLoaded || this.nodes.values().stream().noneMatch(GridNode::isLoaded);
        isLoaded = !wasLoaded;
        return wasLoaded;
    }

    @Override
    public CompoundTag serializeNBT() {

        CompoundTag tag = new CompoundTag();
        ListTag nodes = new ListTag();
        for (N node : nodeGraph.nodes()) {
            CompoundTag nodeTag = new CompoundTag();
            nodeTag.put("pos", NbtUtils.writeBlockPos(node.getPos()));
            nodeTag.merge(node.serializeNBT());
            nodes.add(nodeTag);
        }
        tag.put("nodes", nodes);

        ListTag edges = new ListTag();
        for (EndpointPair<N> edge : nodeGraph.edges()) {
            CompoundTag edgeTag = new CompoundTag();
            edgeTag.put("U", NbtUtils.writeBlockPos(edge.nodeU().getPos()));
            edgeTag.put("V", NbtUtils.writeBlockPos(edge.nodeV().getPos()));
            edges.add(edgeTag);
        }
        tag.put("edges", edges);

        ListTag updateable = new ListTag();
        for (BlockPos pos : updatableHosts) {
            CompoundTag updateTag = new CompoundTag();
            updateTag.put("pos", NbtUtils.writeBlockPos(pos));
            updateable.add(updateTag);
        }
        tag.put("updateable", updateable);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {

        ListTag nodes = nbt.getList("nodes", 10);

        for (int i = 0; i < nodes.size(); ++i) {
            CompoundTag nodeTag = nodes.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(nodeTag.getCompound("pos"));
            GridNode<?> node = newNode(pos, false);
            node.deserializeNBT(nodeTag);
        }

        ListTag edges = nbt.getList("edges", 10);
        for (int i = 0; i < edges.size(); ++i) {
            CompoundTag edgeTag = edges.getCompound(i);
            BlockPos uPos = NbtUtils.readBlockPos(edgeTag.getCompound("U"));
            BlockPos vPos = NbtUtils.readBlockPos(edgeTag.getCompound("V"));
            nodeGraph.putEdge(this.nodes.get(uPos), this.nodes.get(vPos));
        }
        ListTag updateable = nbt.getList("updateable", 10);
        for (int i = 0; i < updateable.size(); ++i) {
            CompoundTag updateTag = updateable.getCompound(i);
            BlockPos pos = NbtUtils.readBlockPos(updateTag.getCompound("pos"));
            updatableHosts.add(pos);
        }
        // Make sure no Node positions are identity match to BlockPos.ZERO
        assert !DEBUG || this.nodes.values().stream().noneMatch(e -> e.getPos() == BlockPos.ZERO);
    }

    public abstract N newNode();

    public final N newNode(BlockPos pos) {

        return newNode(pos, true);
    }

    public final N newNode(BlockPos pos, boolean load) {
        // We should never be adding a node for a position that already exists.
        assert !nodes.containsKey(pos);

        // Generate new node for position, set node internal pos, add to nodes map.
        N node = newNode();
        node.setPos(pos);
        N existing = nodes.put(pos, node);
        assert existing == null; // double check.

        // Add node to graph and chunk map.
        nodeGraph.addNode(node);
        getNodesForChunk(pos).add(node);

        if (load) {
            // Set node as loaded, add chunk to loaded chunks.
            node.setLoaded(true);
            loadedChunks.add(asChunkLong(pos));
        }
        return node;
    }

    public final void removeNode(N toRemove) {

        boolean ret;

        ret = nodeGraph.removeNode(toRemove);
        assert ret;

        ret = getNodesForChunk(toRemove.getPos()).remove(toRemove);
        assert ret;

        ret = nodes.remove(toRemove.getPos(), toRemove);
        assert ret;
    }

    public final void insertExistingNode(N toAdd) {

        N existingNode = nodes.get(toAdd.getPos());
        if (existingNode == toAdd) return;

        assert existingNode == null;

        nodeGraph.addNode(toAdd);
        getNodesForChunk(toAdd.getPos()).add(toAdd);
        nodes.put(toAdd.getPos(), toAdd);
        long chunkPos = asChunkLong(toAdd.getPos());
        if (world.isLoaded(toAdd.getPos()) && !loadedChunks.contains(chunkPos)) {
            loadedChunks.add(chunkPos);
        }
    }

    public final void mergeFrom(G other) {

        assert gridType == other.gridType;
        if (DEBUG) {
            LOGGER.info("Merging {} nodes from grid {} into {}.", other.nodeGraph.nodes().size(), other.id, id);
        }
        // Loop all edges in other grid.
        PositionCollector positionCollector = new PositionCollector(world);
        for (EndpointPair<N> edge : other.nodeGraph.edges()) {
            N a = edge.nodeU();
            N b = edge.nodeV();

            // Collect all positions between node edges. 'betweenClosed' returns a and b.
            for (BlockPos pos : BlockPos.betweenClosed(a.getPos(), b.getPos())) {
                positionCollector.collectPosition(pos.immutable());
            }
            // Insert edge and value.
            nodeGraph.putEdge(a, b);
        }
        other.nodeGraph.nodes().forEach(e -> positionCollector.collectPosition(e.getPos()));

        // Iterate all in-world nodes and update the tracked grid.
        updateGridHosts(world, positionCollector.getChunkPositions(), unsafeCast(this));

        // Insert all nodes into the Grid's lookup maps and update the node about the grid change.
        for (N node : other.nodeGraph.nodes()) {
            insertExistingNode(node);
            nodeGraph.addNode(node);
            node.setGrid(unsafeCast(this));
            node.onGridChange(unsafeCast(other));
        }
        updatableHosts.addAll(other.updatableHosts);
        onMerge(unsafeCast(other));
    }

    // Called to split the current grid into the specified partitions.
    public final List<G> splitInto(List<Set<N>> splitGraphs) {

        GridContainer gridContainer = ((GridContainer) IGridContainer.getCapability(world)
                .orElseThrow(notPossible()));
        List<G> newGrids = new LinkedList<>();

        for (Set<N> splitGraph : splitGraphs) {
            PositionCollector positionCollector = new PositionCollector(world);

            // Create new grid.
            G newGrid = gridContainer.createAndAddGrid(gridContainer.nextUUID(), gridType, true);
            for (N node : splitGraph) {
                positionCollector.collectPosition(node.getPos());
                newGrid.insertExistingNode(node);

                // Iterate all edges connected to this node.
                for (N adj : nodeGraph.adjacentNodes(node)) {
                    newGrid.insertExistingNode(adj);

                    for (BlockPos pos : BlockPos.betweenClosed(node.getPos(), adj.getPos())) {
                        positionCollector.collectPosition(pos.immutable());
                    }
                    // Put edge value in new grid.
                    newGrid.nodeGraph.putEdge(node, adj);
                }
            }
            // Update all hosts within the new grid of the change.
            updateGridHosts(world, positionCollector.getChunkPositions(), newGrid);
            for (Set<BlockPos> positions : positionCollector.getChunkPositions().values()) {
                for (BlockPos position : positions) {
                    if (updatableHosts.remove(position)) {
                        newGrid.updatableHosts.add(position);
                    }
                }
            }

            // Insert all nodes into the Grid's lookup maps and update the node about the grid change.
            for (GridNode<?> node : splitGraph) {
                node.setGrid(unsafeCast(newGrid));
                node.onGridChange(unsafeCast(this));
            }
            newGrids.add(newGrid);
        }

        // Notify the current grid it has been split.
        onSplit(unsafeCast(newGrids));
        return newGrids;
    }

    /**
     * Inserts a node in the middle of an edge.
     * <p>
     * If a node already exists at {@code pos}, it will be returned instead.
     *
     * @param pos  The position in the grid to generate the node.
     * @return The existing node, or the new node at the position.
     */
    public final N getNodeOrSplitEdgeAndInsertNode(BlockPos pos) {

        N existing = getNodes().get(pos);
        if (existing != null) return existing;

        // We are adding a duct, next to an existing duct that does not have a node.
        // There is only one valid case for this, where there are 2 nodes directly attached.

        // Find the edge which pos lies on.
        EndpointPair<N> foundEdge = findEdge(pos);
        assert foundEdge != null;

        N a = foundEdge.nodeU();
        N b = foundEdge.nodeV();

        // Make sure these 2 nodes are actually connected.
        assert nodeGraph.hasEdgeConnecting(a, b);

        // Link new node to 2 existing nodes, remove edge between existing nodes.
        N abMiddle = newNode(pos);
        nodeGraph.putEdge(abMiddle, a);
        nodeGraph.putEdge(abMiddle, b);
        nodeGraph.removeEdge(a, b);
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

    private void updateGridHosts(Level world, Long2ObjectMap<Set<BlockPos>> posMap, G grid) {

        for (Long2ObjectMap.Entry<Set<BlockPos>> entry : posMap.long2ObjectEntrySet()) {
            long chunkPos = entry.getLongKey();
            LevelChunk chunk = world.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
            for (BlockPos pos : entry.getValue()) {
                Optional<IGridHost> gridHostOpt = GridHelper.getGridHost(chunk.getBlockEntity(pos));
                if (!gridHostOpt.isPresent()) {
                    LOGGER.error("Node not connected to grid! Chunk modified externally. {}", pos);
                    continue;
                }
                IGridHost gridHost = gridHostOpt.get();
                gridHost.setGrid(grid);
            }
        }
    }

    /**
     * Called when the internal grid structure is modified such
     * as nodes being added/removed, or edges being rewritten.
     */
    public void onModified() {

        if (DEBUG) {
            checkInvariant();
        }
    }

    protected void updateHosts() {

        for (BlockPos pos : updatableHosts) {
            if (world.isLoaded(pos) && world.getBlockEntity(pos) instanceof IGridHostUpdateable host) {
                host.update();
            }
        }
    }

    /**
     * Called when the specified grid is merged into us.
     * <p>
     * This is provided for Grid implementations to merge the internal state
     * of the provided grid into this one.
     *
     * @param from The other grid.
     */
    public abstract void onMerge(G from);

    /**
     * Called when this grid is split into the provided other
     * grids.
     * <p>
     * This grid is now invalid, and will be removed from the world.
     * <p>
     * This is provided for Grid implementations to split their internal
     * storage between the provided grids.
     *
     * @param others The other grids.
     */
    public abstract void onSplit(List<G> others);

    public void onGridHostAdded(IGridHost host) {

        if (host instanceof IGridHostUpdateable uHost) {
            updatableHosts.add(host.getHostPos());
            uHost.update();
        }
    }

    public void onGridHostRemoved(IGridHost host) {

        if (host instanceof IGridHostUpdateable) {
            updatableHosts.remove(host.getHostPos());
        }
    }

    @Nullable
    public final EndpointPair<N> findEdge(BlockPos pos) {

        for (EndpointPair<N> edge : nodeGraph.edges()) {
            if (isOnEdge(pos, edge)) {
                return edge;
            }
        }
        return null;
    }

    public final boolean isConnectedTo(BlockPos a, BlockPos b) {

        N aNode = nodes.get(a);
        N bNode = nodes.get(b);
        if (aNode != null && bNode != null) {
            return nodeGraph.hasEdgeConnecting(aNode, bNode);
        }
        if (aNode == null && bNode == null) {
            return isOnEdge(b, requireNonNull(findEdge(a)));
        }
        N node = aNode != null ? aNode : bNode;
        BlockPos pos = aNode != null ? b : a;
        for (EndpointPair<N> edge : nodeGraph.incidentEdges(node)) {
            if (isOnEdge(pos, edge)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the UUID that represents this Grid.
     * <p>
     * The primary use for this is to uniquely identify this grid for client <-> server communication.
     * <p>
     * Guaranteed to be unique over the lifetime of the grid object.
     * Guaranteed to be unique over a single play-session.
     * No other guarantees are made.
     *
     * @return The Grid id.
     */
    public final UUID getId() {
        return id;
    }

    /**
     * Gets the level this grid is a prt of.
     *
     * @return The world.
     */
    public final Level getLevel() {
        return world;
    }

    /**
     * Gets the type of this grid.
     * <p>
     * {@link IGridType} can be considered similar to {@link BlockEntityType} where
     * it is a unique key and Factory.
     *
     * @return The type of this grid.
     * @see IGridType
     */
    public IGridType<G> getGridType() {
        return gridType;
    }

    /**
     * Returns a Map of all nodes in the Grid.
     *
     * @return The nodes.
     */
    public final Map<BlockPos, N> getNodes() {
        return nodes;
    }

    /**
     * Checks if this {@link Grid} can externally connect to
     * any adjacent blocks at the given position.
     *
     * @param pos The position.
     * @return If the grid can connect to any adjacent blocks.
     */
    public boolean canConnectExternally(BlockPos pos) {

        for (Direction dir : Direction.values()) {
            if (canConnectOnSide(pos.relative(dir), dir.getOpposite())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if this {@link Grid} can externally connect to the given {@link BlockEntity}
     * on the given face of the {@link BlockEntity}.
     *
     * @param tile The {@link BlockEntity}.
     * @param dir  The face, <code>null</code> for the 'center' face.
     * @return If the {@link Grid} can externally connect.
     */
    public abstract boolean canConnectOnSide(BlockEntity tile, @javax.annotation.Nullable Direction dir);

    /**
     * Checks if this {@link Grid} can externally connect to the given {@link BlockEntity}
     * at the given {@link BlockPos} on the given face of the {@link BlockEntity}.
     *
     * @param pos The {@link BlockPos}.
     * @param dir The face, <code>null</code> for the 'center' face.
     * @return If the {@link Grid} can externally connect.
     */
    public boolean canConnectOnSide(BlockPos pos, @javax.annotation.Nullable Direction dir) {

        BlockEntity tile = getLevel().getBlockEntity(pos);
        if (tile == null) {
            return false;
        }
        return canConnectOnSide(tile, dir);
    }

    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {

        return LazyOptional.empty();
    }

    public final void debugWriteToPacket(FriendlyByteBuf buffer) {
        buffer.writeUUID(getId());
        buffer.writeVarInt(nodes.size());
        for (N node : nodes.values()) {
            buffer.writeBlockPos(node.getPos());
            Set<N> edges = nodeGraph.adjacentNodes(node);
            buffer.writeVarInt(edges.size());
            for (N edge : edges) {
                buffer.writeBlockPos(edge.getPos());
            }
        }
    }

    public abstract void refreshCapabilities();

    private boolean isOnEdge(BlockPos pos, EndpointPair<N> edge) {

        return GridHelper.isOnEdge(pos, edge.nodeU().getPos(), edge.nodeV().getPos());
    }

    private List<N> getNodesForChunk(BlockPos pos) {

        return nodesPerChunk.computeIfAbsent(asChunkLong(pos), e -> new LinkedList<>());
    }

    private static long asChunkLong(BlockPos pos) {

        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static class PositionCollector {

        private static final LongFunction<Set<BlockPos>> FACTORY = e -> new HashSet<>();

        private final Level world;
        private final Long2ObjectMap<Set<BlockPos>> chunkPositions = new Long2ObjectOpenHashMap<>();

        private final LongSet loadedChunks = new LongOpenHashSet();
        private final LongSet unloadedChunks = new LongOpenHashSet();

        private PositionCollector(Level world) {

            this.world = world;
        }

        public void collectPosition(BlockPos pos) {

            long chunkLong = asChunkLong(pos);

            if (unloadedChunks.contains(chunkLong)) {
                return; // Skip
            }

            if (!loadedChunks.contains(chunkLong)) {
                if (!world.isLoaded(pos)) {
                    unloadedChunks.add(chunkLong);
                    return; // Skip.
                }
                loadedChunks.add(chunkLong);
            }
            chunkPositions.computeIfAbsent(chunkLong, FACTORY).add(pos);
        }

        public Long2ObjectMap<Set<BlockPos>> getChunkPositions() {

            return chunkPositions;
        }

    }

}
