package cofh.thermal.dynamics.handler;

import cofh.thermal.dynamics.ThermalDynamics;
import cofh.thermal.dynamics.api.grid.*;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.api.internal.GridHostInternal;
import cofh.thermal.dynamics.grid.AbstractGrid;
import cofh.thermal.dynamics.grid.AbstractGridNode;
import cofh.thermal.dynamics.network.client.GridDebugPacket;
import io.netty.buffer.Unpooled;
import net.covers1624.quack.collection.ColUtils;
import net.covers1624.quack.collection.StreamableIterable;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.event.TickEvent;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class GridContainerImpl implements GridContainer, INBTSerializable<ListNBT> {

    private static final boolean DEBUG = GridContainerImpl.class.desiredAssertionStatus();

    private static final Logger LOGGER = LogManager.getLogger();
    private static final Set<UUID> USED_UUIDS = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private final Map<UUID, AbstractGrid<?, ?>> grids = new HashMap<>();
    private final Map<UUID, AbstractGrid<?, ?>> loadedGrids = new HashMap<>();
    private final World world;

    private int tickCounter;

    public GridContainerImpl(World world) {
        this.world = world;
    }

    @Override
    public void onGridHostPlaced(GridHostInternal host) {
        EnumMap<Direction, GridHostInternal> adjacentGrids = getAdjacentGrids(host);
        // We aren't adjacent to anything else, new grid.
        if (adjacentGrids.isEmpty()) {
            constructNewGrid(host);
            return;
        }

        if (adjacentGrids.size() == 1) {
            Map.Entry<Direction, GridHostInternal> adjacent = ColUtils.only(adjacentGrids.entrySet());
            extendGrid(host, adjacent.getValue(), adjacent.getKey());
        } else {
            List<GridHostInternal> branches = new ArrayList<>(adjacentGrids.values());
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

    private void constructNewGrid(GridHostInternal host) {
        if (DEBUG) {
            LOGGER.info("Constructing new grid for {}", host.getHostPos());
        }
        assert host.getExposedTypes().size() == 1; // TODO, multi grids.
        AbstractGrid<?, ?> grid = createAndAddGrid(nextUUID(), host.getExposedTypes().iterator().next(), true);
        host.setGrid(grid);
        grid.newNode(host.getHostPos());
    }

    private void extendGrid(GridHostInternal host, GridHost adjacent, Direction adjacentDir) {
        Optional<GridNode<?>> adjacentNodeOpt = adjacent.getNode();
        Optional<Grid<?, ?>> adjacentGridOpt = adjacent.getGrid();

        assert adjacentGridOpt.isPresent(); // All adjacent nodes should have a Grid when this method is called.
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) adjacentGridOpt.get();

        // Set the GridHost's grid.
        host.setGrid(grid);

        AbstractGridNode<?> newNode = grid.newNode(host.getHostPos());
        if (!adjacentNodeOpt.isPresent()) {
            // We are adding a duct next to another duct which does not have a node associated, we need to:
            // - Identify the 2 nodes at either end of this adjacent duct (a/b).
            // - Generate a new node at the adjacent position.
            // - Unlink the 'a/b' nodes from each other and re-link with the adjacent node.
            // - Add link to the node we just placed.
            AbstractGridNode<?> abMiddle = generateIntersection(grid, adjacent.getHostPos(), host.getHostPos());
            grid.nodeGraph.putEdgeValue(abMiddle, newNode, 0);

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
                Integer distance = grid.nodeGraph.edgeValue(adjacentNode, edge);
                if (DEBUG) {
                    LOGGER.info(
                            "Extending branch edge from {} to {} dist {}, new {}, newDist {}",
                            adjacentNode.getPos(),
                            edge.getPos(),
                            distance,
                            newNode.getPos(),
                            distance + 1
                    );
                }
                grid.removeNode(adjacentNode);
                grid.nodeGraph.putEdgeValue(newNode, edge, distance + 1);
            } else {
                if (DEBUG) {
                    LOGGER.info("Adding new single node. adjacent {}, new {}", adjacentNode.getPos(), newNode.getPos());
                }
                grid.nodeGraph.putEdgeValue(newNode, adjacentNode, 0);
            }
        }
    }

    /**
     * Generates an intersection node at the specified position in the grid.
     * <p>
     * This method assumes there is not already a node at <code>pos</code>.
     *
     * @param grid The grid to add the node to.
     * @param pos  The position in the grid to add the intersection node.
     * @param from The position next to the intersection node which triggered
     *             the intersection generation (to be ignored when locating attached nodes).
     * @return The new node at the position.
     */
    private AbstractGridNode<?> generateIntersection(AbstractGrid<?, ?> grid, BlockPos pos, BlockPos from) {
        // We are adding a duct, next to an existing duct that does not have a node.
        // There is only one valid case for this, where there are 2 nodes directly attached.

        assert grid.getNodes().get(pos) == null;

        // Find the 2 aforementioned existing nodes.
        List<Pair<GridNode<?>, Integer>> attached = GridHelper.locateAttachedNodes(world, pos, from);
        assert attached.size() == 2;

        Pair<GridNode<?>, Integer> a = attached.get(0);
        Pair<GridNode<?>, Integer> b = attached.get(1);
        AbstractGridNode<?> attachedA = (AbstractGridNode<?>) a.getLeft();
        int attachedALen = a.getRight() - 1;
        AbstractGridNode<?> attachedB = (AbstractGridNode<?>) b.getLeft();
        int attachedBLen = b.getRight() - 1;

        // Make sure these 2 nodes are actually connected.
        assert grid.nodeGraph.edgeValueOrDefault(attachedA, attachedB, null) != null;

        // Link new node to 2 existing nodes, remove edge between existing nodes.
        AbstractGridNode<?> abMiddle = grid.newNode(pos);
        grid.nodeGraph.putEdgeValue(abMiddle, attachedA, attachedALen);
        grid.nodeGraph.putEdgeValue(abMiddle, attachedB, attachedBLen);
        Integer abLen = grid.nodeGraph.removeEdge(attachedA, attachedB);
        if (DEBUG) {
            LOGGER.info("Intersection creation. Node A: {}, Node B: {}, AB dist: {}, Middle: {}, NewA dist: {}, NewB dist: {}",
                    attachedA.getPos(),
                    attachedB.getPos(),
                    abLen,
                    abMiddle.getPos(),
                    attachedALen,
                    attachedBLen
            );
        }
        return abMiddle;
    }

    private void mergeGridBranches(GridHostInternal host, List<GridHostInternal> branches, boolean wasMerge) {
        assert branches.size() != 1;

        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) branches.get(0).getGrid().orElseThrow(notPossible());
        host.setGrid(grid);
        // More than 2 branches, or the 2 adjacent branches aren't on the same axis as us. We must generate a node.
        AbstractGridNode<?> node = grid.newNode(host.getHostPos());
        for (GridHost branch : branches) {
            Optional<GridNode<?>> adjOpt = branch.getNode();
            if (!adjOpt.isPresent()) {
                // Adjacent isn't present, just generate node.
                AbstractGridNode<?> abMiddle = generateIntersection(grid, branch.getHostPos(), node.getPos());
                if (DEBUG) {
                    LOGGER.info(" T intersection creation. New Node: {}, Adjacent: {}",
                            node.getPos(),
                            abMiddle.getPos()
                    );
                }
                grid.nodeGraph.putEdgeValue(abMiddle, node, 0);
            } else {
                AbstractGridNode<?> adj = (AbstractGridNode<?>) adjOpt.get();
                grid.nodeGraph.putEdgeValue(adj, node, 0);
                simplifyNode(adj);
                if (DEBUG) {
                    LOGGER.info("Adding edge. {}, {}", adj.getPos(), node.getPos());
                }
            }
        }
        simplifyNode(node);
    }

    private void mergeGrids(List<GridHostInternal> branches) {
        Set<AbstractGrid<?, ?>> grids = new HashSet<>();
        for (GridHostInternal branch : branches) {
            AbstractGrid<?, ?> abstractGrid = (AbstractGrid<?, ?>) branch.getGrid().orElseThrow(notPossible());
            grids.add(abstractGrid);
        }

        // Choose the largest grid as the 'main' grid.
        AbstractGrid<?, ?> main = ColUtils.maxBy(grids, e -> e.nodeGraph.nodes().size());
        grids.remove(main);
        for (AbstractGrid<?, ?> other : grids) {
            main.mergeFrom(other);
            this.grids.remove(other.getId());
            this.loadedGrids.remove(other.getId());
        }
    }

    @Override
    public void onGridHostDestroyed(GridHostInternal host) {
        EnumMap<Direction, GridHostInternal> adjacentHosts = getAdjacentGrids(host);
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

    private void removeSingleGrid(GridHostInternal host) {
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
    }

    private void shrinkGrid(GridHostInternal host, GridHost adjacent) {
        Optional<GridNode<?>> adjacentNodeOpt = adjacent.getNode();
        Optional<Grid<?, ?>> adjacentGridOpt = adjacent.getGrid();

        assert adjacentGridOpt.isPresent(); // All adjacent nodes should have a Grid when this method is called.
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) adjacentGridOpt.get();

        assert host.getGrid().isPresent() && grid == host.getGrid().get();

        AbstractGridNode<?> currNode = (AbstractGridNode<?>) host.getNode()
                .orElseThrow(notPossible());

        if (!adjacentNodeOpt.isPresent()) {
            // We are removing a node which is not adjacent to another node.
            // We must do the following:
            // - Remove the current node.
            // - Create a new node for our adjacent with path length decremented by one.
            AbstractGridNode<?> adjacentNode = grid.newNode(adjacent.getHostPos());

            Set<AbstractGridNode<?>> edgeNodes = grid.nodeGraph.adjacentNodes(currNode);
            AbstractGridNode<?> edge = only(edgeNodes);
            int edgeLen = grid.nodeGraph.edgeValue(edge, currNode);
            grid.nodeGraph.putEdgeValue(adjacentNode, edge, edgeLen - 1);
            if (DEBUG) {
                LOGGER.info("Removing dead end: Curr node: {}, Adjacent: {}, Edge: {}, Len: {}, New len: {}",
                        currNode.getPos(),
                        adjacentNode.getPos(),
                        edge.getPos(),
                        edgeLen,
                        edgeLen - 1
                );
            }
            grid.removeNode(currNode);
        } else {
            AbstractGridNode<?> adjacentNode = (AbstractGridNode<?>) adjacentNodeOpt.get();
            // Nuke the current node
            grid.removeNode(currNode);

            // Attempt simplification on our single adjacent node.
            simplifyNode(adjacentNode);
        }

    }

    private void removeNode(GridHostInternal host, EnumMap<Direction, GridHostInternal> adjacentHosts) {
        // We are removing a host which has a node, we need to:
        // - Disconnect all connected edges.
        // - Iterate all adjacent hosts
        //  - If adjacent host has a node
        //   - Check if the node can be removed simplifying the grid.
        //  - If the adjacent host does not have a node.
        //   - Always create a new node.
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid()
                .orElseThrow(notPossible());

        if (host.getNode().isPresent()) {
            // All we need to do is remove the node.
            AbstractGridNode<?> removing = (AbstractGridNode<?>) host.getNode().get();
            grid.removeNode(removing);
            if (DEBUG) {
                LOGGER.info("Removing node: {}", removing.getPos());
            }
        } else {
            // We are a host without a node, between 2 nodes.
            List<Pair<GridNode<?>, Integer>> attached = GridHelper.locateAttachedNodes(world, host.getHostPos(), host.getHostPos());
            assert attached.size() == 2;
            AbstractGridNode<?> a = (AbstractGridNode<?>) attached.get(0).getLeft();
            AbstractGridNode<?> b = (AbstractGridNode<?>) attached.get(1).getLeft();
            grid.nodeGraph.removeEdge(a, b); // Yeet edge.
            if (DEBUG) {
                LOGGER.info("Removing edge between: {} and {}", a.getPos(), b.getPos());
            }
        }

        // Create/delete adjacent nodes if required
        for (GridHostInternal adjHost : adjacentHosts.values()) {
            if (!adjHost.getNode().isPresent()) {
                // We always need to create a new node here.
                Pair<GridNode<?>, Integer> foundEdge = only(GridHelper.locateAttachedNodes(world, adjHost.getHostPos(), host.getHostPos()));
                assert foundEdge != null;
                AbstractGridNode<?> newNode = grid.newNode(adjHost.getHostPos());
                AbstractGridNode<?> foundNode = (AbstractGridNode<?>) foundEdge.getLeft();
                int foundNodeLen = foundEdge.getRight();
                grid.nodeGraph.putEdgeValue(newNode, foundNode, foundNodeLen);
                if (DEBUG) {
                    LOGGER.info("Generating new node at {} with edge {} len {}", newNode.getPos(), foundNode.getPos(), foundNodeLen);
                }
            } else {
                // Attempt to simplify the node.
                AbstractGridNode<?> adjNode = (AbstractGridNode<?>) adjHost.getNode().get();
                simplifyNode(adjNode);
            }
        }
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

        int aLen = grid.nodeGraph.edgeValue(node, edges[0]);
        int bLen = grid.nodeGraph.edgeValue(node, edges[1]);

        grid.removeNode(node);
        grid.nodeGraph.putEdgeValue(edges[0], edges[1], aLen + bLen + 1);
        if (DEBUG) {
            LOGGER.info(
                    "Simplifying grid node '{}' A {}, B {}, Len {}",
                    node.getPos(),
                    edges[0].getPos(),
                    edges[1].getPos(),
                    aLen + bLen + 1
            );
        }
    }

    private void separateGrids(GridHostInternal host) {
        AbstractGrid<?, ?> grid = (AbstractGrid<?, ?>) host.getGrid()
                .orElseThrow(notPossible());

        // Check if the grid
        List<Set<AbstractGridNode<?>>> splitGraphs = GraphHelper.separateGraphs(grid.nodeGraph);
        if (splitGraphs.size() <= 1) return;

        if (DEBUG) {
            LOGGER.info("Splitting grid into {} segments.", splitGraphs.size());
        }

        grid.splitInto(splitGraphs);
        grids.remove(grid.getId());
        loadedGrids.remove(grid.getId());
    }

    //region Event callbacks.
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
        // This should always be the cause, but whatever.
        if (chunk instanceof Chunk) {
            Chunk chonk = (Chunk) chunk;
            // TODO, we _may_ be able to use TileEntity.onLoad instead of this.
            // We need to attach each GridHost back to its grid when the chunk gets loaded.
            for (TileEntity tile : chonk.getBlockEntities().values()) {
                Optional<GridHost> gridHostOpt = GridHelper.getGridHost(tile);
                gridHostOpt.ifPresent(e -> {
                    GridHostInternal host = (GridHostInternal) e;
                    UUID lastGrid = host.getLastGrid();
                    if (lastGrid == null) return;

                    AbstractGrid<?, ?> grid = grids.get(lastGrid);
                    if (grid != null) {
                        if (DEBUG) {
                            LOGGER.info("Reattaching host {} to grid.", host.getHostPos());
                        }
                        host.setGrid(grid);
                    } else {
                        LOGGER.warn("Unable to re-attach grid to {}. Not found.", host.getHostPos());
                    }
                });
            }
        }

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
    //endregion

    @Override
    public Optional<Grid<?, ?>> getGrid(UUID id) {
        return Optional.ofNullable(grids.get(id));
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
            GridType<?> gridType = ThermalDynamics.GRID_TYPE_REGISTRY.get().getValue(gridTypeName);
            if (gridType == null) {
                LOGGER.error("Failed to load Grid {} with type {} in world {}. GridType is no longer registered, it will be removed from the world.", id, gridTypeName, world.dimension().location());
                continue;
            }

            AbstractGrid<?, ?> grid = createAndAddGrid(id, gridType, false);
            grid.deserializeNBT(tag);
        }
        if (DEBUG) {
            LOGGER.info("Loaded {} grids for {}.", grids.size(), world.dimension().location());
        }
    }

    //region helpers
    private EnumMap<Direction, GridHostInternal> getAdjacentGrids(GridHost host) {
        EnumMap<Direction, GridHostInternal> adjacentGrids = new EnumMap<>(Direction.class);
        for (Direction dir : Direction.values()) {
            GridHelper.getGridHost(world, host.getHostPos().relative(dir))
                    .ifPresent(gridHost -> adjacentGrids.put(dir, (GridHostInternal) gridHost));
        }
        return adjacentGrids;
    }

    public AbstractGrid<?, ?> createAndAddGrid(UUID uuid, GridType<?> gridType, boolean load) {
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
    //endregion

}
