package cofh.thermal.dynamics.grid;

import cofh.thermal.dynamics.api.grid.*;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.api.internal.IGridHostInternal;
import cofh.thermal.dynamics.api.internal.IUpdateableGridHostInternal;
import cofh.thermal.dynamics.handler.GridContainer;
import com.google.common.graph.*;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectRBTreeMap;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.util.INBTSerializable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.LongFunction;

import static net.covers1624.quack.util.SneakyUtils.notPossible;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Abstract base class for all {@link IGrid} implementations.
 * <p>
 *
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractGrid<G extends IGrid<?, ?>, N extends IGridNode<?>> implements IGrid<G, N>, INBTSerializable<CompoundNBT> {

    protected static final Logger LOGGER = LogManager.getLogger();
    protected static final boolean DEBUG = AbstractGrid.class.desiredAssertionStatus();

    /**
     * The {@link ValueGraph} of nodes stored in this grid.
     * <p>
     * The value object being the path length.
     */
    public final MutableValueGraph<AbstractGridNode<?>, Set<BlockPos>> nodeGraph = ValueGraphBuilder
            .undirected()
            .nodeOrder(ElementOrder.unordered())
            .build();
    /**
     * The same list of nodes, indexed by BlockPos.
     */
    protected final Map<BlockPos, AbstractGridNode<?>> nodes = new Object2ObjectRBTreeMap<>();
    /**
     * The same list of nodes, indexed by ChunkPos.
     */
    protected final Long2ObjectMap<List<AbstractGridNode<?>>> nodesPerChunk = new Long2ObjectRBTreeMap<>();
    protected final LongSet loadedChunks = new LongOpenHashSet();
    protected final Set<BlockPos> updatableHosts = new HashSet<>();
    protected final IGridType<G> gridType;
    protected final UUID id;
    protected final World world;
    public boolean isLoaded;

    protected AbstractGrid(IGridType<G> gridType, UUID id, World world) {

        this.gridType = gridType;
        this.id = id;
        this.world = world;
    }

    public void tick() {

        for (LongIterator iterator = loadedChunks.iterator(); iterator.hasNext(); ) {
            long loadedChunk = iterator.nextLong();
            List<AbstractGridNode<?>> nodes = nodesPerChunk.get(loadedChunk);
            if (nodes == null) {
                continue;
            }
            for (AbstractGridNode<?> node : nodes) {
                assert node.isLoaded();
                if (node instanceof ITickableGridNode) {
                    ((ITickableGridNode<?>) node).tick();
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
        for (EndpointPair<AbstractGridNode<?>> edge : nodeGraph.edges()) {
            AbstractGridNode<?> u = edge.nodeU();
            AbstractGridNode<?> v = edge.nodeV();
            Set<BlockPos> between = GridHelper.getPositionsBetween(u.getPos(), v.getPos());
            Set<BlockPos> value = nodeGraph.edgeValue(u, v);
            assert value.size() == between.size();
            assert value.containsAll(between);
            for (BlockPos pos : between) {
                checkPos(pos, gridContainer);
            }
        }

        // Check nodes.
        for (AbstractGridNode<?> node : nodeGraph.nodes()) {

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
            assert gridHostOpt.get() instanceof IUpdateableGridHostInternal;
        }
    }

    private void checkPos(BlockPos pos, GridContainer gridContainer) {

        if (!world.isLoaded(pos)) {
            return;
        }
        Optional<IGridHost> gridHostOpt = GridHelper.getGridHost(world, pos);
        assert gridHostOpt.isPresent();
        IGridHost gridHost = gridHostOpt.get();
        assert gridHost.getGrid().isPresent();
        assert gridHost.getGrid().get() == this;
        assert !(gridHost instanceof IUpdateableGridHostInternal) || updatableHosts.contains(gridHost.getHostPos());

        assert gridContainer.getGrid(pos) == this;
    }

    // returns true if this grid changes its loaded state to true.
    public boolean onChunkLoad(IChunk chunk) {

        long pos = chunk.getPos().toLong();
        List<AbstractGridNode<?>> nodes = nodesPerChunk.get(pos);
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        assert !loadedChunks.contains(pos);
        loadedChunks.add(pos);

        for (AbstractGridNode<?> node : nodes) {
            node.setLoaded(true);
        }
        boolean wasLoaded = !isLoaded;
        isLoaded = true;
        return wasLoaded;
    }

    // returns true if this grid changes its loaded state to false.
    public boolean onChunkUnload(IChunk chunk) {

        long pos = chunk.getPos().toLong();
        List<AbstractGridNode<?>> nodes = nodesPerChunk.get(pos);
        if (nodes == null || nodes.isEmpty()) {
            return false;
        }
        assert loadedChunks.contains(pos);
        boolean wasLoaded = loadedChunks.size() == 1;
        loadedChunks.remove(pos);
        for (AbstractGridNode<?> node : nodes) {
            node.setLoaded(false);
        }
        assert !wasLoaded || this.nodes.values().stream().noneMatch(AbstractGridNode::isLoaded);
        isLoaded = !wasLoaded;
        return wasLoaded;
    }

    @Override
    public CompoundNBT serializeNBT() {

        CompoundNBT tag = new CompoundNBT();
        ListNBT nodes = new ListNBT();
        for (AbstractGridNode<?> node : nodeGraph.nodes()) {
            CompoundNBT nodeTag = new CompoundNBT();
            nodeTag.put("pos", NBTUtil.writeBlockPos(node.getPos()));
            nodeTag.merge(node.serializeNBT());
            nodes.add(nodeTag);
        }
        tag.put("nodes", nodes);

        ListNBT edges = new ListNBT();
        for (EndpointPair<AbstractGridNode<?>> edge : nodeGraph.edges()) {
            CompoundNBT edgeTag = new CompoundNBT();
            edgeTag.put("U", NBTUtil.writeBlockPos(edge.nodeU().getPos()));
            edgeTag.put("V", NBTUtil.writeBlockPos(edge.nodeV().getPos()));
            ListNBT valueTag = new ListNBT();
            for (BlockPos pos : nodeGraph.edgeValue(edge.nodeU(), edge.nodeV())) {
                valueTag.add(NBTUtil.writeBlockPos(pos));
            }
            edgeTag.put("value", valueTag);
            edges.add(edgeTag);
        }
        tag.put("edges", edges);

        ListNBT updateable = new ListNBT();
        for (BlockPos pos : updatableHosts) {
            CompoundNBT updateTag = new CompoundNBT();
            updateTag.put("pos", NBTUtil.writeBlockPos(pos));
            updateable.add(updateTag);
        }
        tag.put("updateable", updateable);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {

        ListNBT nodes = nbt.getList("nodes", 10);

        for (int i = 0; i < nodes.size(); ++i) {
            CompoundNBT nodeTag = nodes.getCompound(i);
            BlockPos pos = NBTUtil.readBlockPos(nodeTag.getCompound("pos"));
            AbstractGridNode<?> node = newNode(pos, false);
            node.deserializeNBT(nodeTag);
        }

        ListNBT edges = nbt.getList("edges", 10);
        for (int i = 0; i < edges.size(); ++i) {
            CompoundNBT edgeTag = edges.getCompound(i);
            BlockPos uPos = NBTUtil.readBlockPos(edgeTag.getCompound("U"));
            BlockPos vPos = NBTUtil.readBlockPos(edgeTag.getCompound("V"));
            Set<BlockPos> value = new HashSet<>();
            ListNBT valueTag = edgeTag.getList("value", 10);
            for (int j = 0; j < valueTag.size(); ++j) {
                value.add(NBTUtil.readBlockPos(valueTag.getCompound(j)));
            }
            nodeGraph.putEdgeValue(this.nodes.get(uPos), this.nodes.get(vPos), value);
        }
        ListNBT updateable = nbt.getList("updateable", 10);
        for (int i = 0; i < updateable.size(); ++i) {
            CompoundNBT updateTag = updateable.getCompound(i);
            BlockPos pos = NBTUtil.readBlockPos(updateTag.getCompound("pos"));
            updatableHosts.add(pos);
        }
        // Make sure no Node positions are identity match to BlockPos.ZERO
        assert !DEBUG || this.nodes.values().stream().noneMatch(e -> e.getPos() == BlockPos.ZERO);
    }

    public abstract AbstractGridNode<G> newNode();

    public final AbstractGridNode<?> newNode(BlockPos pos) {

        return newNode(pos, true);
    }

    public final AbstractGridNode<?> newNode(BlockPos pos, boolean load) {
        // We should never be adding a node for a position that already exists.
        assert !nodes.containsKey(pos);

        // Generate new node for position, set node internal pos, add to nodes map.
        AbstractGridNode<?> node = newNode();
        node.setPos(pos);
        AbstractGridNode<?> existing = nodes.put(pos, node);
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

    public final void removeNode(AbstractGridNode<?> toRemove) {

        boolean ret;

        ret = nodeGraph.removeNode(toRemove);
        assert ret;

        ret = getNodesForChunk(toRemove.getPos()).remove(toRemove);
        assert ret;

        ret = nodes.remove(toRemove.getPos(), toRemove);
        assert ret;
    }

    public final void insertExistingNode(AbstractGridNode<?> toAdd) {

        AbstractGridNode<?> existingNode = nodes.get(toAdd.getPos());
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

    public final void mergeFrom(AbstractGrid<?, ?> other) {

        assert gridType == other.gridType;
        if (DEBUG) {
            LOGGER.info("Merging {} nodes from grid {} into {}.", other.nodeGraph.nodes().size(), other.id, id);
        }
        // Loop all edges in other grid.
        PositionCollector positionCollector = new PositionCollector(world);
        for (EndpointPair<AbstractGridNode<?>> edge : other.nodeGraph.edges()) {
            AbstractGridNode<?> a = edge.nodeU();
            AbstractGridNode<?> b = edge.nodeV();

            // Collect all positions between node edges. 'betweenClosed' returns a and b.
            for (BlockPos pos : BlockPos.betweenClosed(a.getPos(), b.getPos())) {
                positionCollector.collectPosition(pos.immutable());
            }
            // Insert edge and value.
            nodeGraph.putEdgeValue(a, b, other.nodeGraph.edgeValue(a, b));
        }
        other.nodeGraph.nodes().forEach(e -> positionCollector.collectPosition(e.getPos()));

        // Iterate all in-world nodes and update the tracked grid.
        updateGridHosts(world, positionCollector.getChunkPositions(), this);

        // Insert all nodes into the Grid's lookup maps and update the node about the grid change.
        for (AbstractGridNode<?> node : other.nodeGraph.nodes()) {
            insertExistingNode(node);
            nodeGraph.addNode(node);
            node.setGrid(unsafeCast(this));
            node.onGridChange(unsafeCast(other));
        }
        onMerge(unsafeCast(other));
        updatableHosts.addAll(other.updatableHosts);
    }

    // Called to split the current grid into the specified partitions.
    public final List<AbstractGrid<?, ?>> splitInto(List<Set<AbstractGridNode<?>>> splitGraphs) {

        GridContainer gridContainer = ((GridContainer) IGridContainer.getCapability(world)
                .orElseThrow(notPossible()));
        List<AbstractGrid<?, ?>> newGrids = new LinkedList<>();

        for (Set<AbstractGridNode<?>> splitGraph : splitGraphs) {
            PositionCollector positionCollector = new PositionCollector(world);

            // Create new grid.
            AbstractGrid<?, ?> newGrid = gridContainer.createAndAddGrid(gridContainer.nextUUID(), gridType, true);
            for (AbstractGridNode<?> node : splitGraph) {
                positionCollector.collectPosition(node.getPos());
                newGrid.insertExistingNode(node);

                // Iterate all edges connected to this node.
                for (AbstractGridNode<?> adj : nodeGraph.adjacentNodes(node)) {
                    newGrid.insertExistingNode(adj);

                    for (BlockPos pos : BlockPos.betweenClosed(node.getPos(), adj.getPos())) {
                        positionCollector.collectPosition(pos.immutable());
                    }
                    // Put edge value in new grid.
                    newGrid.nodeGraph.putEdgeValue(node, adj, nodeGraph.edgeValue(node, adj));
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
            for (AbstractGridNode<?> node : splitGraph) {
                node.setGrid(unsafeCast(newGrid));
                node.onGridChange(unsafeCast(this));
            }
            newGrids.add(newGrid);
        }

        // Notify the current grid it has been split.
        onSplit(unsafeCast(newGrids));
        return newGrids;
    }

    private static void updateGridHosts(World world, Long2ObjectMap<Set<BlockPos>> posMap, AbstractGrid<?, ?> grid) {

        for (Long2ObjectMap.Entry<Set<BlockPos>> entry : posMap.long2ObjectEntrySet()) {
            long chunkPos = entry.getLongKey();
            Chunk chunk = world.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
            for (BlockPos pos : entry.getValue()) {
                Optional<IGridHost> gridHostOpt = GridHelper.getGridHost(chunk.getBlockEntity(pos));
                if (!gridHostOpt.isPresent()) {
                    LOGGER.error("Node not connected to grid! Chunk modified externally. {}", pos);
                    continue;
                }
                IGridHostInternal gridHost = (IGridHostInternal) gridHostOpt.get();
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

    public void onGridHostAdded(IGridHostInternal host) {

        if (host instanceof IUpdateableGridHostInternal) {
            updatableHosts.add(host.getHostPos());
            ((IUpdateableGridHostInternal) host).update();
        }
    }

    public void onGridHostRemoved(IGridHost host) {

        if (host instanceof IUpdateableGridHostInternal) {
            updatableHosts.remove(host.getHostPos());
        }
    }

    //@formatter:off
    @Override public final UUID getId() { return id; }
    @Override public final World getWorld() { return world; }
    @Override public IGridType<G> getGridType() { return gridType; }
    @Override public final Map<BlockPos, N> getNodes() { return unsafeCast(nodes); }
    //@formatter:on

    private List<AbstractGridNode<?>> getNodesForChunk(BlockPos pos) {

        return nodesPerChunk.computeIfAbsent(asChunkLong(pos), e -> new LinkedList<>());
    }

    private static long asChunkLong(BlockPos pos) {

        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static class PositionCollector {

        private static final LongFunction<Set<BlockPos>> FACTORY = e -> new HashSet<>();

        private final World world;
        private final Long2ObjectMap<Set<BlockPos>> chunkPositions = new Long2ObjectOpenHashMap<>();

        private final LongSet loadedChunks = new LongOpenHashSet();
        private final LongSet unloadedChunks = new LongOpenHashSet();

        private PositionCollector(World world) {

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
