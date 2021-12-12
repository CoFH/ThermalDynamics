package cofh.thermal.dynamics.grid;

import cofh.thermal.dynamics.api.grid.*;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.api.internal.GridHostInternal;
import cofh.thermal.dynamics.api.internal.TickableGridNode;
import cofh.thermal.dynamics.handler.GridContainerImpl;
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

import static net.covers1624.quack.util.SneakyUtils.notPossible;
import static net.covers1624.quack.util.SneakyUtils.unsafeCast;

/**
 * Abstract base class for all {@link Grid} implementations.
 * <p>
 * @author covers1624
 */
@SuppressWarnings ("UnstableApiUsage")
public abstract class AbstractGrid<G extends Grid<?, ?>, N extends GridNode<?>> implements Grid<G, N>, INBTSerializable<CompoundNBT> {

    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG = AbstractGrid.class.desiredAssertionStatus();

    /**
     * The {@link ValueGraph} of nodes stored in this grid.
     * <p>
     * The value object being the path length.
     */
    public final MutableValueGraph<AbstractGridNode<?>, Integer> nodeGraph = ValueGraphBuilder
            .undirected()
            .nodeOrder(ElementOrder.unordered())
            .build();
    /**
     * The same list of nodes, indexed by BlockPos.
     */
    private final Map<BlockPos, AbstractGridNode<?>> nodes = new Object2ObjectRBTreeMap<>();
    /**
     * The same list of nodes, indexed by ChunkPos.
     */
    private final Long2ObjectMap<List<AbstractGridNode<?>>> nodesPerChunk = new Long2ObjectRBTreeMap<>();
    private final LongSet loadedChunks = new LongOpenHashSet();
    private final GridType<G> gridType;
    private final UUID id;
    private final World world;
    public boolean isLoaded;

    protected AbstractGrid(GridType<G> gridType, UUID id, World world) {
        this.gridType = gridType;
        this.id = id;
        this.world = world;
    }

    public void tick() {
        for (LongIterator iterator = loadedChunks.iterator(); iterator.hasNext(); ) {
            long loadedChunk = iterator.nextLong();
            List<AbstractGridNode<?>> nodes = nodesPerChunk.get(loadedChunk);
            if (nodes == null) continue;
            for (AbstractGridNode<?> node : nodes) {
                assert node.isLoaded();
                if (node instanceof TickableGridNode) {
                    ((TickableGridNode<?>) node).tick();
                }
            }
        }
    }

    // returns true if this grid changes its loaded state to true.
    public boolean onChunkLoad(IChunk chunk) {
        long pos = chunk.getPos().toLong();
        List<AbstractGridNode<?>> nodes = nodesPerChunk.get(pos);
        if (nodes == null || nodes.isEmpty()) return false;
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
        if (nodes == null || nodes.isEmpty()) return false;
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
            ListNBT edges = new ListNBT();

            for (AbstractGridNode<?> edge : nodeGraph.adjacentNodes(node)) {
                CompoundNBT edgeTag = new CompoundNBT();
                edgeTag.put("pos", NBTUtil.writeBlockPos(edge.getPos()));
                edgeTag.putInt("distance", nodeGraph.edgeValue(node, edge));
                edges.add(edgeTag);
            }

            nodeTag.put("edges", edges);
            nodeTag.merge(node.serializeNBT());
            nodes.add(nodeTag);
        }

        tag.put("nodes", nodes);
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundNBT nbt) {
        ListNBT nodes = nbt.getList("nodes", 10);

        for (int i = 0; i < nodes.size(); i++) {
            CompoundNBT nodeTag = nodes.getCompound(i);
            BlockPos pos = NBTUtil.readBlockPos(nodeTag.getCompound("pos"));
            AbstractGridNode<?> node = this.nodes.computeIfAbsent(pos, e -> newNode());
            node.setPos(pos);
            nodeGraph.addNode(node);
            getNodesForChunk(pos).add(node);

            ListNBT edges = nodeTag.getList("edges", 10);
            for (int i1 = 0; i1 < edges.size(); i1++) {
                CompoundNBT edgeTag = edges.getCompound(i1);
                BlockPos edgePos = NBTUtil.readBlockPos(edgeTag.getCompound("pos"));
                AbstractGridNode<?> edgeNode = this.nodes.computeIfAbsent(edgePos, e -> newNode());
                nodeGraph.putEdgeValue(node, edgeNode, edgeTag.getInt("distance"));
            }
            node.deserializeNBT(nodeTag);
        }

        // Make sure no Node positions are identity match to BlockPos.ZERO
        assert this.nodes.values().stream().noneMatch(e -> e.getPos() == BlockPos.ZERO);
    }

    public abstract AbstractGridNode<G> newNode();

    public final AbstractGridNode<?> newNode(BlockPos pos) {
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

        // Set node as loaded, add chunk to loaded chunks.
        node.setLoaded(true);
        loadedChunks.add(asChunkLong(pos));
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
        onMerge(unsafeCast(other));

        // Loop all edges in other grid.
        Long2ObjectMap<Set<BlockPos>> posMap = new Long2ObjectArrayMap<>();
        for (EndpointPair<AbstractGridNode<?>> edge : other.nodeGraph.edges()) {
            AbstractGridNode<?> a = edge.nodeU();
            AbstractGridNode<?> b = edge.nodeV();

            // Collect all positions between node edges.
            collectPosition(posMap, a.getPos());
            collectPosition(posMap, b.getPos());
            for (BlockPos pos : BlockPos.betweenClosed(a.getPos(), b.getPos())) {
                collectPosition(posMap, pos.immutable());
            }
            // Insert edge and value.
            nodeGraph.putEdgeValue(a, b, other.nodeGraph.edgeValue(a, b));
        }
        other.nodeGraph.nodes().forEach(e -> collectPosition(posMap, e.getPos()));

        // Iterate all in-world nodes and update the tracked grid.
        updateGridHosts(world, posMap, this);

        // Insert all nodes into the Grid's lookup maps and update the node about the grid change.
        for (AbstractGridNode<?> node : other.nodeGraph.nodes()) {
            insertExistingNode(node);
            nodeGraph.addNode(node);
            node.setGrid(unsafeCast(this));
            node.onGridChange(unsafeCast(other));
        }
    }

    // Called to split the current grid into the specified partitions.
    public final void splitInto(List<Set<AbstractGridNode<?>>> splitGraphs) {
        GridContainerImpl gridContainer = ((GridContainerImpl) GridContainer.getCapability(world)
                .orElseThrow(notPossible()));
        List<AbstractGrid<?, ?>> otherGrids = new LinkedList<>();

        for (Set<AbstractGridNode<?>> splitGraph : splitGraphs) {
            Long2ObjectMap<Set<BlockPos>> posMap = new Long2ObjectArrayMap<>();

            // Create new grid.
            AbstractGrid<?, ?> newGrid = gridContainer.createAndAddGrid(gridContainer.nextUUID(), gridType, true);
            for (AbstractGridNode<?> node : splitGraph) {
                collectPosition(posMap, node.getPos());
                newGrid.insertExistingNode(node);

                // Iterate all edges connected to this node.
                for (AbstractGridNode<?> adj : nodeGraph.adjacentNodes(node)) {
                    newGrid.insertExistingNode(adj);

                    collectPosition(posMap, adj.getPos());
                    for (BlockPos pos : BlockPos.betweenClosed(node.getPos(), adj.getPos())) {
                        collectPosition(posMap, pos.immutable());
                    }
                    // Put edge value in new grid.
                    newGrid.nodeGraph.putEdgeValue(node, adj, nodeGraph.edgeValue(node, adj));
                }
            }
            // Update all hosts within the new grid of the change.
            updateGridHosts(world, posMap, newGrid);

            // Insert all nodes into the Grid's lookup maps and update the node about the grid change.
            for (AbstractGridNode<?> node : splitGraph) {
                node.setGrid(unsafeCast(newGrid));
                node.onGridChange(unsafeCast(this));
            }

            otherGrids.add(newGrid);
        }

        // Notify the current grid it has been split.
        onSplit(unsafeCast(otherGrids));
    }

    private static void updateGridHosts(World world, Long2ObjectMap<Set<BlockPos>> posMap, AbstractGrid<?, ?> grid) {
        for (Long2ObjectMap.Entry<Set<BlockPos>> entry : posMap.long2ObjectEntrySet()) {
            long chunkPos = entry.getLongKey();
            Chunk chunk = world.getChunk(ChunkPos.getX(chunkPos), ChunkPos.getZ(chunkPos));
            for (BlockPos pos : entry.getValue()) {
                Optional<GridHost> gridHostOpt = GridHelper.getGridHost(chunk.getBlockEntity(pos));
                if (!gridHostOpt.isPresent()) {
                    LOGGER.error("Node not connected to grid! Chunk modified externally. {}", pos);
                    continue;
                }
                GridHostInternal gridHost = (GridHostInternal) gridHostOpt.get();
                gridHost.setGrid(grid);
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

    //@formatter:off
    @Override public final UUID getId() { return id; }
    @Override public final World getWorld() { return world; }
    @Override public GridType<G> getGridType() { return gridType; }
    @Override public final Map<BlockPos, N> getNodes() { return unsafeCast(nodes); }
    //@formatter:on

    private List<AbstractGridNode<?>> getNodesForChunk(BlockPos pos) {
        return nodesPerChunk.computeIfAbsent(asChunkLong(pos), e -> new LinkedList<>());
    }

    private static long asChunkLong(BlockPos pos) {
        return ChunkPos.asLong(pos.getX() >> 4, pos.getZ() >> 4);
    }

    private static void collectPosition(Long2ObjectMap<Set<BlockPos>> chunkPositions, BlockPos toCollect) {
        chunkPositions.computeIfAbsent(asChunkLong(toCollect), e -> new HashSet<>())
                .add(toCollect);
    }
}
