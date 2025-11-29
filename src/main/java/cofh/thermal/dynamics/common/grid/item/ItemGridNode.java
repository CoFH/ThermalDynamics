package cofh.thermal.dynamics.common.grid.item;

import cofh.core.util.filter.IFilter;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.ITickableGridNode;
import cofh.thermal.dynamics.common.attachment.IFilterableAttachment;
import cofh.thermal.dynamics.common.block.entity.duct.ItemDuctBlockEntity;
import cofh.thermal.dynamics.common.grid.GridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Grid node for item transport networks.
 * Handles pathfinding and item routing.
 */
public class ItemGridNode extends GridNode<ItemGrid> implements ITickableGridNode {

    // Routing weight constants for Dense/Vacuum duct priority
    private static final int DENSE_PENALTY = 1000;
    private static final int VACUUM_PRIORITY = -10000;

    public ItemGridNode(ItemGrid grid) {
        super(grid);
    }

    // Cache of connected item handlers
    private final Map<Direction, LazyOptional<IItemHandler>> connectedHandlers = new EnumMap<>(Direction.class);

    // Cache of valid destinations in the network
    private Map<BlockPos, PathInfo> destinationCache = new HashMap<>();

    /**
     * Topology version when cache was last updated.
     * Cache is invalidated when grid.getTopologyVersion() != cachedTopologyVersion.
     */
    private long cachedTopologyVersion = -1;

    private boolean connectionsCached = false;

    public void clearItemConnections() {
        connectedHandlers.clear();
        destinationCache.clear();
        connectionsCached = false;
        connections.clear();
    }
    
    private void cacheConnections() {
        connections.clear();
        for (Direction dir : Direction.values()) {
            if (grid.canConnectOnSide(pos.relative(dir), dir.getOpposite())) {
                connections.add(dir);
            }
        }
        connectionsCached = true;
    }

    public IDuct<?, ?> getDuct() {
        return gridHost();
    }
    
    @Override
    public void attachmentTick() {
        IDuct<?, ?> duct = getDuct();
        if (duct == null) {
            return;
        }

        for (Direction dir : Direction.values()) {
            var attachment = duct.getAttachment(dir);
            if (attachment != null && !(attachment instanceof cofh.thermal.dynamics.common.attachment.EmptyAttachment)) {
                attachment.tick();
            }
        }
    }
    
    @Override
    public void distributionTick() {
        if (!isLoaded()) return;

        // Tick attachments first
        attachmentTick();

        // Cache connections if needed
        if (!connectionsCached) {
            cacheConnections();
        }

        // Update destination cache only when topology changes (event-driven, not time-based)
        long currentTopologyVersion = grid.getTopologyVersion();
        if (cachedTopologyVersion != currentTopologyVersion) {
            updateDestinationCache();
            cachedTopologyVersion = currentTopologyVersion;
        }

        // Check for connected item handlers that might need servicing
        for (Direction dir : Direction.values()) {
            updateConnectedHandler(dir);
        }
    }

    private void updateConnectedHandler(Direction dir) {
        BlockEntity tile = grid.getLevel().getBlockEntity(pos.relative(dir));
        if (tile != null) {
            LazyOptional<IItemHandler> handler = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite());
            connectedHandlers.put(dir, handler);
        } else {
            connectedHandlers.remove(dir);
        }
    }

    private void updateDestinationCache() {
        destinationCache.clear();
        
        // Use BFS to find all reachable destinations
        Set<BlockPos> visited = new HashSet<>();
        Queue<PathNode> queue = new LinkedList<>();
        queue.add(new PathNode(pos, new ArrayList<>()));
        visited.add(pos);
        
        while (!queue.isEmpty()) {
            PathNode current = queue.poll();
            
            // Check for external connections from current position
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = current.pos.relative(dir);
                BlockEntity tile = grid.getLevel().getBlockEntity(neighborPos);
                
                // Skip if this is another duct (internal connection)
                if (grid.getNodes().containsKey(neighborPos)) {
                    continue;
                }
                
                // Check if this tile has item handler capability
                if (tile != null && tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent()) {
                    // Found an external destination - path contains only duct positions, not the destination
                    List<BlockPos> pathThroughDucts = new ArrayList<>(current.path);
                    // Add the current duct position to the path if it's not the starting position
                    if (!current.pos.equals(pos)) {
                        pathThroughDucts.add(current.pos);
                    }
                    destinationCache.put(neighborPos, new PathInfo(pathThroughDucts, dir.getOpposite()));
                }
            }
            
            // Explore grid neighbors
            ItemGridNode currentNode = grid.getNodes().get(current.pos);
            if (currentNode != null) {
                for (ItemGridNode neighbor : grid.nodeGraph.adjacentNodes(currentNode)) {
                    if (!visited.contains(neighbor.pos)) {
                        visited.add(neighbor.pos);
                        List<BlockPos> newPath = new ArrayList<>(current.path);
                        newPath.add(neighbor.pos);
                        queue.add(new PathNode(neighbor.pos, newPath));
                    }
                }
            }
        }
    }

    public boolean canExtractItem(Direction from) {
        LazyOptional<IItemHandler> handler = connectedHandlers.get(from);
        if (!handler.isPresent()) return false;
        
        IItemHandler itemHandler = handler.orElse(null);
        if (itemHandler == null) return false;
        
        // Check if any slot has items
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            if (!itemHandler.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        
        return false;
    }

    public ItemStack extractItem(Direction from, int amount, boolean simulate) {
        LazyOptional<IItemHandler> handler = connectedHandlers.get(from);
        if (!handler.isPresent()) return ItemStack.EMPTY;
        
        IItemHandler itemHandler = handler.orElse(null);
        if (itemHandler == null) return ItemStack.EMPTY;
        
        // Try to extract from any slot
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            ItemStack extracted = itemHandler.extractItem(slot, amount, simulate);
            if (!extracted.isEmpty()) {
                return extracted;
            }
        }
        
        return ItemStack.EMPTY;
    }

    public static class DestinationResult {
        public final BlockPos destination;
        public final PathInfo pathInfo;

        public DestinationResult(BlockPos destination, PathInfo pathInfo) {
            this.destination = destination;
            this.pathInfo = pathInfo;
        }
    }

    /**
     * Calculate the routing weight for a path.
     * - Vacuum ducts: Return very negative weight (highest priority, checked first)
     * - Dense ducts: Add +1000 per dense duct (lowest priority, overflow only)
     * - Normal ducts: Just count path length
     */
    private int calculatePathWeight(List<BlockPos> path) {
        int weight = path.size();  // Base weight = hop count
        boolean hasVacuum = false;
        int denseCount = 0;

        // For direct connections (empty path), check origin duct only
        if (path.isEmpty()) {
            IDuct<?, ?> originDuct = getDuct();
            if (originDuct instanceof ItemDuctBlockEntity itemDuct) {
                if (itemDuct.isVacuum()) return VACUUM_PRIORITY;
                if (itemDuct.isDense()) return DENSE_PENALTY;
            }
            return 0;  // Direct connection, normal duct
        }

        // Check each duct in the path
        for (BlockPos pathPos : path) {
            ItemGridNode node = grid.getNodes().get(pathPos);
            if (node != null && node.getDuct() instanceof ItemDuctBlockEntity itemDuct) {
                if (itemDuct.isVacuum()) {
                    hasVacuum = true;
                }
                if (itemDuct.isDense()) {
                    denseCount++;
                }
            }
        }

        // Also check origin duct (servo's duct)
        IDuct<?, ?> originDuct = getDuct();
        if (originDuct instanceof ItemDuctBlockEntity itemDuct) {
            if (itemDuct.isVacuum()) {
                hasVacuum = true;
            }
            if (itemDuct.isDense()) {
                denseCount++;
            }
        }

        if (hasVacuum) {
            return VACUUM_PRIORITY + weight;  // Very negative = highest priority
        }

        return weight + (denseCount * DENSE_PENALTY);  // Dense adds massive penalty
    }

    public DestinationResult findBestDestination(ItemStack stack) {
        // Sort destinations by weighted path calculation
        // Vacuum ducts have highest priority (negative weight), Dense ducts have lowest priority (+1000 per duct)
        List<Map.Entry<BlockPos, PathInfo>> sortedDestinations = new ArrayList<>(destinationCache.entrySet());
        sortedDestinations.sort(Comparator.comparingInt(entry -> calculatePathWeight(entry.getValue().path)));

        for (Map.Entry<BlockPos, PathInfo> entry : sortedDestinations) {
            BlockPos destPos = entry.getKey();
            PathInfo pathInfo = entry.getValue();

            BlockEntity tile = grid.getLevel().getBlockEntity(destPos);
            if (tile == null) {
                continue;
            }

            LazyOptional<IItemHandler> cap = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, pathInfo.side);
            if (!cap.isPresent()) {
                continue;
            }

            IItemHandler handler = cap.orElse(null);
            if (handler == null) {
                continue;
            }

            if (!canInsertItem(handler, stack)) {
                continue;
            }

            // Check if this destination has a servo that would extract from it
            // This prevents items from being routed to chests that servos are already extracting from
            boolean hasConflictingServo = false;

            // Determine the duct position that connects to this destination
            BlockPos connectingDuctPos;
            if (pathInfo.path.size() > 0) {
                // Path through other ducts - use the last duct in the path
                connectingDuctPos = pathInfo.path.get(pathInfo.path.size() - 1);
            } else {
                // Direct connection - this node connects directly to destination
                connectingDuctPos = pos;
            }

            ItemGridNode connectingDuctNode = grid.getNodes().get(connectingDuctPos);

            try {
                if (connectingDuctNode != null && connectingDuctNode.getDuct() != null) {
                    // Get the direction from the connecting duct to the destination
                    Direction dirToDestination = null;
                    BlockPos relative = destPos.subtract(connectingDuctPos);
                    for (Direction checkDir : Direction.values()) {
                        if (checkDir.getNormal().equals(relative)) {
                            dirToDestination = checkDir;
                            break;
                        }
                    }

                    if (dirToDestination != null) {
                        var attachment = connectingDuctNode.getDuct().getAttachment(dirToDestination);
                        if (attachment instanceof cofh.thermal.dynamics.common.attachment.ItemServoAttachment) {
                            hasConflictingServo = true;
                        }
                        // Check if attachment is a filter that would reject this item
                        if (attachment instanceof IFilterableAttachment filterAttachment) {
                            IFilter filter = filterAttachment.getFilter();
                            if (filter != null && !filter.valid(stack)) {
                                // Item doesn't match filter - skip this destination
                                continue;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                // If there's any error, allow the destination
                hasConflictingServo = false;
            }

            if (!hasConflictingServo) {
                return new DestinationResult(destPos, pathInfo);
            }
        }

        return null;
    }

    private boolean canInsertItem(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            remaining = handler.insertItem(slot, remaining, true);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        return remaining.getCount() < stack.getCount();
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        // No additional data to serialize
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        // No additional data to deserialize
    }

    private static class PathNode {
        final BlockPos pos;
        final List<BlockPos> path;

        PathNode(BlockPos pos, List<BlockPos> path) {
            this.pos = pos;
            this.path = path;
        }
    }

    public static class PathInfo {
        public final List<BlockPos> path;
        public final Direction side;

        public PathInfo(List<BlockPos> path, Direction side) {
            this.path = path;
            this.side = side;
        }
    }
}