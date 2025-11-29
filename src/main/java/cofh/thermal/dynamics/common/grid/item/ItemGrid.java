package cofh.thermal.dynamics.common.grid.item;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.common.block.entity.duct.ItemDuctBlockEntity;
import cofh.thermal.dynamics.common.grid.Grid;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static cofh.thermal.dynamics.init.registries.TDynGrids.ITEM_GRID;

/**
 * Grid for managing item transport through ducts.
 * Items travel through the network with transit time rather than instant teleportation.
 */
public class ItemGrid extends Grid<ItemGrid, ItemGridNode> {

    // Track active grids for client-side rendering
    private static final Map<Level, Set<ItemGrid>> activeGrids = new HashMap<>();
    
    // Travel speed constant (blocks per tick)
    // Normal ducts: 0.05 = 1 block per second at 20 TPS
    private static final float NORMAL_ITEM_SPEED = 0.05f;

    // Epsilon for float comparison to handle precision issues after deserialization
    private static final float DISTANCE_EPSILON = 0.001f;

    // NBT tag constants
    private static final String TAG_ITEMS_IN_TRANSIT = "itemsInTransit";
    private static final String TAG_RETURNING_ITEMS = "returningItems";

    // Queue for items in transit (grid operations are single-threaded on server tick)
    private final Queue<ItemInTransit> itemsInTransit = new LinkedList<>();

    // Items that need to be returned due to backup
    private final Queue<ItemInTransit> returningItems = new LinkedList<>();

    // Track items in transit to destinations for capacity calculation
    private final Map<BlockPos, Map<Direction, Integer>> itemsInTransitToDestination = new HashMap<>();

    /**
     * Topology version when items were last checked for path validity.
     * When this differs from current topology, all items need path revalidation.
     */
    private long lastCheckedTopologyVersion = -1;

    public ItemGrid(UUID id, Level world) {
        super(ITEM_GRID.get(), id, world);
        
        // Register this grid for rendering lookups
        activeGrids.computeIfAbsent(world, k -> new HashSet<>()).add(this);
        // Grid registered for item transport
    }
    
    /**
     * Get all active ItemGrids in a level for client-side rendering
     */
    public static Collection<ItemGrid> getActiveGrids(Level level) {
        Set<ItemGrid> grids = activeGrids.get(level);
        return grids != null ? new ArrayList<>(grids) : Collections.emptyList();
    }
    
    /**
     * Clean up grid tracking when grid is destroyed
     */
    public void cleanup() {
        if (world != null) {
            Set<ItemGrid> grids = activeGrids.get(world);
            if (grids != null) {
                grids.remove(this);
                if (grids.isEmpty()) {
                    activeGrids.remove(world);
                }
            }
        }
    }
    
    @Override
    public ItemGridNode newNode() {
        return new ItemGridNode(this);
    }

    @Override
    public void tick() {
        super.tick();

        // Track if we HAD items at start of tick (to know if we need final render clear)
        boolean hadItemsAtStart = !itemsInTransit.isEmpty() || !returningItems.isEmpty();

        if (hadItemsAtStart) {
            // Check if topology changed - need to validate/backflow items
            long currentTopology = getTopologyVersion();
            if (lastCheckedTopologyVersion != currentTopology) {
                handleTopologyChange();
                lastCheckedTopologyVersion = currentTopology;
            }

            // Process items in transit
            processItemsInTransit();

            // Process returning items
            processReturningItems();

            // Update render data for windowed ducts
            // IMPORTANT: We must update even if queues are now empty to clear stale renders
            updateRenderData();

            // If all items were just removed this tick, do one final render update to clear
            boolean hasItemsNow = !itemsInTransit.isEmpty() || !returningItems.isEmpty();
            if (!hasItemsNow) {
                // Force a final update to clear any lingering render data
                clearAllRenderData();
            }
        }
    }

    /**
     * Clear render data from all windowed ducts when no items remain in transit.
     */
    private void clearAllRenderData() {
        if (world.isClientSide) return;

        for (ItemGridNode node : getNodes().values()) {
            try {
                if (node.getDuct() instanceof cofh.thermal.dynamics.common.block.entity.duct.ItemDuctBlockEntity ductEntity) {
                    if (ductEntity.isWindowed() && ductEntity.hasLevel()) {
                        ductEntity.update();
                    }
                }
            } catch (Exception e) {
                // Silent failure
            }
        }
    }
    
    /**
     * Handle topology changes by validating all items' paths.
     * Items with invalid forward paths are reversed back to origin IF return path is valid.
     * If both paths are broken, item is dropped as entity.
     */
    private void handleTopologyChange() {
        Iterator<ItemInTransit> iterator = itemsInTransit.iterator();
        while (iterator.hasNext()) {
            ItemInTransit item = iterator.next();

            // Check if the item's forward path (to destination) is still valid
            boolean forwardPathValid = isPathValid(item);

            if (forwardPathValid) {
                // Forward path is fine - item continues to destination
                continue;
            }

            // Forward path is broken - check if we can return to origin
            boolean returnPathValid = canReturnToOrigin(item);

            if (!returnPathValid) {
                // Both paths broken - drop item as entity at current position
                dropItemAsEntity(item);
                removeItemFromTransitTracking(item);
                iterator.remove();
                continue;
            }

            // Return path is valid - initiate backflow to origin
            // reverse() now preserves position - don't reset distanceTraveled after
            item.reverse();

            // After reverse: item.destination is now the servo duct position
            // Double-check it still exists (should be true since canReturnToOrigin passed)
            if (!getNodes().containsKey(item.destination)) {
                dropItemAsEntity(item);
                removeItemFromTransitTracking(item);
                iterator.remove();
                continue;
            }

            // CRITICAL: Filter the path to only include positions that still exist as nodes
            // After reverse, the path may contain the broken duct's position
            // Also remove duplicates and ensure path is valid
            List<BlockPos> validPath = new ArrayList<>();
            BlockPos lastPos = null;
            for (BlockPos pathPos : item.path) {
                // Only add if it's a valid node AND not a duplicate of the previous position
                if (getNodes().containsKey(pathPos) && !pathPos.equals(lastPos)) {
                    validPath.add(pathPos);
                    lastPos = pathPos;
                }
            }
            item.path = validPath;

            removeItemFromTransitTracking(item);
            returningItems.add(item);
            iterator.remove();

            // Notify the origin servo about the backflow
            notifyServoOfBackflow(item);
        }
    }

    /**
     * Check if an item's FORWARD path is still valid after a topology change.
     *
     * IMPORTANT: We only check if the path AHEAD of the item is valid.
     * If a duct BEHIND the item breaks (between origin and current position),
     * the item should continue forward to destination.
     * If a duct AHEAD of the item breaks (between current position and destination),
     * THEN the item should backflow.
     */
    private boolean isPathValid(ItemInTransit item) {
        // Check if destination still exists and has item handler
        BlockEntity destTile = world.getBlockEntity(item.destination);
        if (destTile == null) {
            return false;
        }

        // Check if the destination can still accept items
        if (!destTile.getCapability(ForgeCapabilities.ITEM_HANDLER, item.destinationSide).isPresent()) {
            return false;
        }

        // Calculate which path nodes are AHEAD of the item (not yet passed)
        // We don't care about path nodes behind the item - if they're broken, item continues forward
        double totalDistance = item.getTotalDistance();
        double progress = totalDistance > 0 ? item.distanceTraveled / totalDistance : 0;

        // Build full path: origin -> path nodes -> destination
        List<BlockPos> fullPath = new ArrayList<>();
        fullPath.add(item.origin);
        fullPath.addAll(item.path);
        fullPath.add(item.destination);

        // Calculate which segment the item is on
        double accumulatedDistance = 0;
        int currentSegmentIndex = 0;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            double segmentDist = Math.sqrt(fullPath.get(i).distSqr(fullPath.get(i + 1)));
            if (item.distanceTraveled <= accumulatedDistance + segmentDist) {
                currentSegmentIndex = i;
                break;
            }
            accumulatedDistance += segmentDist;
            currentSegmentIndex = i + 1;
        }

        // Only check nodes AHEAD of current position (from currentSegmentIndex+1 to end)
        // We skip the current segment's start node since item has passed it
        for (int i = currentSegmentIndex + 1; i < fullPath.size() - 1; i++) {
            BlockPos pathPos = fullPath.get(i);
            if (!getNodes().containsKey(pathPos)) {
                return false;
            }
        }

        // Path ahead is valid - item can continue to destination
        return true;
    }

    /**
     * Notify a servo that an item is being backflowed to it.
     */
    private void notifyServoOfBackflow(ItemInTransit item) {
        // The item's new destination (after reverse) is the original origin
        BlockPos originPos = item.destination;
        Direction originSide = item.destinationSide;

        ItemGridNode originNode = getNodes().get(originPos);
        if (originNode != null && originNode.getDuct() != null) {
            if (originNode.getDuct().getAttachment(originSide) instanceof cofh.thermal.dynamics.common.attachment.ItemServoAttachment servo) {
                servo.notifyBackflow();
            }
        }
    }

    /**
     * Check if the return path for a returning item is still valid.
     * For returning items:
     * - origin is the external block (chest) - NOT a grid node
     * - destination is the servo duct - IS a grid node
     * - path contains intermediate duct nodes (already filtered)
     *
     * Returns false only if the destination (servo) no longer exists.
     * The path has already been filtered during backflow initiation,
     * so we just need to verify the destination is still reachable.
     */
    private boolean isReturnPathValid(ItemInTransit item) {
        // Check if destination (the servo duct we're returning to) still exists in grid
        if (!getNodes().containsKey(item.destination)) {
            return false;
        }

        // The path was already filtered when backflow was initiated
        // We trust that the item can reach the destination through whatever path remains
        // If a path node was removed while item is returning, we'll handle it dynamically

        return true;
    }

    /**
     * Check if an item can return to its origin from its current position.
     * This is used to determine if backflow is possible when forward path is broken.
     *
     * IMPORTANT: We check if the path BEHIND the item (from current position to origin) is valid.
     * If the origin or path behind is broken, we cannot backflow.
     */
    private boolean canReturnToOrigin(ItemInTransit item) {
        // First check if origin (servo duct) still exists in our grid
        if (!getNodes().containsKey(item.origin)) {
            return false;
        }

        // Calculate which segment the item is on
        double totalDistance = item.getTotalDistance();
        double progress = totalDistance > 0 ? item.distanceTraveled / totalDistance : 0;

        // Build full path: origin -> path nodes -> destination
        List<BlockPos> fullPath = new ArrayList<>();
        fullPath.add(item.origin);
        fullPath.addAll(item.path);
        fullPath.add(item.destination);

        // Calculate which segment the item is on
        double accumulatedDistance = 0;
        int currentSegmentIndex = 0;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            double segmentDist = Math.sqrt(fullPath.get(i).distSqr(fullPath.get(i + 1)));
            if (item.distanceTraveled <= accumulatedDistance + segmentDist) {
                currentSegmentIndex = i;
                break;
            }
            accumulatedDistance += segmentDist;
            currentSegmentIndex = i + 1;
        }

        // Check nodes BEHIND current position (from origin to current segment start)
        // These are nodes at indices 1 to currentSegmentIndex (excluding origin at 0 and destination at end)
        for (int i = 1; i <= currentSegmentIndex && i < fullPath.size() - 1; i++) {
            BlockPos pathPos = fullPath.get(i);
            if (!getNodes().containsKey(pathPos)) {
                return false;
            }
        }

        // Return path is valid - item can backflow to origin
        return true;
    }

    /**
     * Drop an item as an entity at its current position when both paths are broken.
     */
    private void dropItemAsEntity(ItemInTransit item) {
        if (world == null || world.isClientSide) return;

        // Calculate current world position based on progress through path
        net.minecraft.world.phys.Vec3 pos = calculateItemWorldPosition(item);

        net.minecraft.world.entity.item.ItemEntity entity = new net.minecraft.world.entity.item.ItemEntity(
            world, pos.x, pos.y, pos.z, item.stack
        );
        // Set no velocity so item doesn't fly away
        entity.setDeltaMovement(0, 0, 0);
        // Prevent pickup delay so player can grab it immediately
        entity.setPickUpDelay(10);
        world.addFreshEntity(entity);
    }

    /**
     * Calculate the world position of an item based on its progress through the path.
     */
    private net.minecraft.world.phys.Vec3 calculateItemWorldPosition(ItemInTransit item) {
        double totalDistance = item.getTotalDistance();
        if (totalDistance <= 0) {
            // Fallback: use origin center
            return new net.minecraft.world.phys.Vec3(
                item.origin.getX() + 0.5,
                item.origin.getY() + 0.5,
                item.origin.getZ() + 0.5
            );
        }

        double progress = Math.min(item.distanceTraveled / totalDistance, 1.0);

        // Build ordered position list: origin -> path nodes -> destination
        List<BlockPos> positions = new ArrayList<>();
        positions.add(item.origin);
        positions.addAll(item.path);
        positions.add(item.destination);

        // Calculate cumulative distances
        List<Double> cumulativeDistances = new ArrayList<>();
        cumulativeDistances.add(0.0);
        double cumDist = 0;
        for (int i = 1; i < positions.size(); i++) {
            cumDist += Math.sqrt(positions.get(i - 1).distSqr(positions.get(i)));
            cumulativeDistances.add(cumDist);
        }

        // Find which segment we're in
        double targetDistance = progress * totalDistance;
        for (int i = 1; i < cumulativeDistances.size(); i++) {
            if (targetDistance <= cumulativeDistances.get(i)) {
                // We're in segment i-1 to i
                BlockPos from = positions.get(i - 1);
                BlockPos to = positions.get(i);
                double segmentStart = cumulativeDistances.get(i - 1);
                double segmentEnd = cumulativeDistances.get(i);
                double segmentLength = segmentEnd - segmentStart;

                double segmentProgress = segmentLength > 0 ? (targetDistance - segmentStart) / segmentLength : 0;

                return new net.minecraft.world.phys.Vec3(
                    from.getX() + 0.5 + (to.getX() - from.getX()) * segmentProgress,
                    from.getY() + 0.5 + (to.getY() - from.getY()) * segmentProgress,
                    from.getZ() + 0.5 + (to.getZ() - from.getZ()) * segmentProgress
                );
            }
        }

        // Fallback: at destination
        return new net.minecraft.world.phys.Vec3(
            item.destination.getX() + 0.5,
            item.destination.getY() + 0.5,
            item.destination.getZ() + 0.5
        );
    }

    private void updateRenderData() {
        // Skip client-side or if no items to render (check BOTH queues!)
        if (world.isClientSide || (itemsInTransit.isEmpty() && returningItems.isEmpty())) {
            return;
        }

        // Update only windowed duct entities with current item transit data
        for (ItemGridNode node : getNodes().values()) {
            try {
                if (node.getDuct() instanceof cofh.thermal.dynamics.common.block.entity.duct.ItemDuctBlockEntity ductEntity) {
                    if (ductEntity.isWindowed() && ductEntity.hasLevel()) {
                        ductEntity.update();
                    }
                }
            } catch (Exception e) {
                // Don't let render updates break the grid - silent failure
            }
        }
    }

    /**
     * Calculate which duct the item is currently traveling through.
     * Used to determine the appropriate travel speed.
     * @param item The item in transit
     * @return BlockPos of the duct segment the item is currently on
     */
    private BlockPos getCurrentDuctPosition(ItemInTransit item) {
        double totalDistance = item.getTotalDistance();
        if (totalDistance <= 0) {
            return item.origin;
        }

        // Build full path: origin -> path nodes -> destination
        List<BlockPos> fullPath = new ArrayList<>();
        fullPath.add(item.origin);
        fullPath.addAll(item.path);
        // Note: destination is external block, not a duct - don't include for speed lookup

        if (fullPath.isEmpty()) {
            return item.origin;
        }

        // Find which segment we're on
        double accumulatedDistance = 0;
        BlockPos lastDuctPos = item.origin;

        for (int i = 0; i < fullPath.size() - 1; i++) {
            BlockPos segmentStart = fullPath.get(i);
            BlockPos segmentEnd = fullPath.get(i + 1);
            double segmentDist = Math.sqrt(segmentStart.distSqr(segmentEnd));

            if (item.distanceTraveled <= accumulatedDistance + segmentDist) {
                // Item is on this segment - return the segment start (duct position)
                return segmentStart;
            }
            accumulatedDistance += segmentDist;
            lastDuctPos = segmentEnd;
        }

        // Item is on final segment (last duct to destination)
        return lastDuctPos;
    }

    /**
     * Get the item travel speed at a specific duct position.
     * @param pos The BlockPos to check
     * @return NORMAL_ITEM_SPEED for all duct types
     */
    private float getSpeedAtPosition(BlockPos pos) {
        return NORMAL_ITEM_SPEED;
    }

    private void processItemsInTransit() {
        Iterator<ItemInTransit> iterator = itemsInTransit.iterator();
        while (iterator.hasNext()) {
            ItemInTransit item = iterator.next();

            // Note: Per-tick rerouting removed - path validation now handled by handleTopologyChange()
            // This significantly improves performance by avoiding O(N) scans every tick

            // Calculate speed based on current duct type (impulse ducts are 4x faster)
            BlockPos currentDuct = getCurrentDuctPosition(item);
            float speed = getSpeedAtPosition(currentDuct);

            // Update item position based on current segment's speed
            item.distanceTraveled += speed;
            double totalDistance = item.getTotalDistance();

            // Check if item reached destination (use epsilon for float precision tolerance)
            if (item.distanceTraveled >= totalDistance - DISTANCE_EPSILON) {
                // Try to insert into destination
                if (tryInsertItem(item)) {
                    // Successfully inserted - remove from transit tracking
                    removeItemFromTransitTracking(item);
                    iterator.remove();
                } else {
                    // Backup occurred - reverse path and add to returning queue
                    // Note: reverse() preserves position, so item will be at destination end
                    // and travel back. Since we're at the destination, distanceTraveled should
                    // be close to totalDistance, so after reverse it will be near 0.
                    item.reverse();
                    removeItemFromTransitTracking(item);
                    returningItems.add(item);
                    iterator.remove();
                }
            }
        }
    }

    private void processReturningItems() {
        Iterator<ItemInTransit> iterator = returningItems.iterator();
        while (iterator.hasNext()) {
            ItemInTransit item = iterator.next();

            // Check if return path is still valid (both paths broken case)
            boolean returnPathValid = isReturnPathValid(item);
            if (!returnPathValid) {
                // Both destination AND return path broken - drop item as entity
                dropItemAsEntity(item);
                iterator.remove();
                continue;
            }

            double totalDistance = item.getTotalDistance();

            // Calculate speed based on current duct type
            BlockPos currentDuct = getCurrentDuctPosition(item);
            float speed = getSpeedAtPosition(currentDuct);

            // Update item position based on current segment's speed
            item.distanceTraveled += speed;

            // Check if item reached origin (use epsilon for float precision tolerance)
            if (item.distanceTraveled >= totalDistance - DISTANCE_EPSILON) {
                // Store in original servo's overflow (infinite storage)
                handleReturnedItem(item);
                iterator.remove();
            }
        }
    }

    private boolean tryInsertItem(ItemInTransit item) {
        BlockPos destPos = item.destination;
        Direction destSide = item.destinationSide;
        
        BlockEntity tile = world.getBlockEntity(destPos);
        if (tile == null) return false;
        
        LazyOptional<IItemHandler> cap = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, destSide);
        if (!cap.isPresent()) return false;
        
        IItemHandler handler = cap.orElse(null);
        if (handler == null) return false;
        
        // Try to insert the item
        ItemStack remaining = item.stack.copy();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            remaining = handler.insertItem(slot, remaining, false);
            if (remaining.isEmpty()) {
                return true;
            }
        }
        
        // Update stack with remaining
        item.stack = remaining;
        return false;
    }

    private void handleReturnedItem(ItemInTransit item) {
        // For returning items (after reverse()):
        // - item.origin = external block (chest) where item came from
        // - item.destination = servo duct position where servo is attached
        // - item.destinationSide = the side of the duct where servo is attached
        BlockPos servoPos = item.destination;
        Direction servoSide = item.destinationSide;
        Level world = getLevel();

        if (world != null && !world.isClientSide) {
            // Find the original servo that extracted this item
            ItemGridNode servoNode = getNodes().get(servoPos);
            if (servoNode != null && servoNode.getDuct() != null) {
                var attachment = servoNode.getDuct().getAttachment(servoSide);

                if (attachment instanceof cofh.thermal.dynamics.common.attachment.ItemServoAttachment servo) {
                    // Store in original servo's overflow (infinite storage)
                    servo.storeOverflowItem(item.stack);
                    return;
                }
            }

            // If original servo not found, drop item at calculated position
            dropItemAsEntity(item);
        }
    }

    public void insertItem(ItemStack stack, BlockPos origin, Direction originSide, BlockPos destination, Direction destinationSide, List<BlockPos> path) {
        if (stack.isEmpty()) {
            return;
        }

        ItemInTransit item = new ItemInTransit(stack.copy(), origin, originSide, destination, destinationSide, path);
        itemsInTransit.add(item);

        // Track this item as in transit to destination
        addItemToTransitTracking(item);

        // IMPORTANT: Sync lastCheckedTopologyVersion with current topology to prevent
        // immediate path validation on first tick after insertion. This avoids false
        // positives where a newly inserted item incorrectly triggers backflow.
        lastCheckedTopologyVersion = getTopologyVersion();
    }
    
    private void addItemToTransitTracking(ItemInTransit item) {
        itemsInTransitToDestination.computeIfAbsent(item.destination, k -> new HashMap<>())
            .merge(item.destinationSide, item.stack.getCount(), Integer::sum);
    }
    
    private void removeItemFromTransitTracking(ItemInTransit item) {
        Map<Direction, Integer> destMap = itemsInTransitToDestination.get(item.destination);
        if (destMap != null) {
            destMap.compute(item.destinationSide, (dir, count) -> {
                if (count == null) return null;
                int newCount = count - item.stack.getCount();
                return newCount <= 0 ? null : newCount;
            });
            if (destMap.isEmpty()) {
                itemsInTransitToDestination.remove(item.destination);
            }
        }
    }

    public Collection<ItemInTransit> getItemsInTransit() {
        return Collections.unmodifiableCollection(itemsInTransit);
    }

    /**
     * Get all items that are returning to their origin servo (backflowing).
     */
    public Collection<ItemInTransit> getReturningItems() {
        return Collections.unmodifiableCollection(returningItems);
    }

    /**
     * Get available capacity at destination considering items in transit
     */
    public int getAvailableCapacity(BlockPos destination, Direction side, ItemStack stack) {
        BlockEntity tile = world.getBlockEntity(destination);
        if (tile == null) return 0;
        
        LazyOptional<IItemHandler> cap = tile.getCapability(ForgeCapabilities.ITEM_HANDLER, side);
        if (!cap.isPresent()) return 0;
        
        IItemHandler handler = cap.orElse(null);
        if (handler == null) return 0;
        
        // Calculate total capacity
        int totalCapacity = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack existing = handler.getStackInSlot(slot);
            int slotLimit = handler.getSlotLimit(slot);
            
            if (existing.isEmpty()) {
                if (handler.isItemValid(slot, stack)) {
                    totalCapacity += Math.min(stack.getMaxStackSize(), slotLimit);
                }
            } else if (ItemStack.isSameItemSameTags(existing, stack)) {
                totalCapacity += Math.min(stack.getMaxStackSize(), slotLimit) - existing.getCount();
            }
        }
        
        // Subtract items in transit to this destination
        Map<Direction, Integer> transitMap = itemsInTransitToDestination.get(destination);
        if (transitMap != null) {
            Integer inTransit = transitMap.get(side);
            if (inTransit != null) {
                totalCapacity -= inTransit;
            }
        }
        
        return Math.max(0, totalCapacity);
    }

    @Override
    public boolean canConnectOnSide(BlockEntity tile, @Nullable Direction dir) {
        if (GridHelper.getGridHost(tile) != null) {
            return false;
        }

        if (dir != null) {
            return tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir).isPresent();
        }
        return false;
    }

    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap) {
        return LazyOptional.empty();
    }

    @Override
    public void onMerge(ItemGrid from) {
        // Transfer items in transit
        itemsInTransit.addAll(from.itemsInTransit);
        returningItems.addAll(from.returningItems);
        
        // Merge transit tracking
        for (Map.Entry<BlockPos, Map<Direction, Integer>> entry : from.itemsInTransitToDestination.entrySet()) {
            Map<Direction, Integer> ourMap = itemsInTransitToDestination.computeIfAbsent(entry.getKey(), k -> new HashMap<>());
            for (Map.Entry<Direction, Integer> dirEntry : entry.getValue().entrySet()) {
                ourMap.merge(dirEntry.getKey(), dirEntry.getValue(), Integer::sum);
            }
        }
    }

    @Override
    public void onSplit(List<ItemGrid> others) {
        // If no new grids (all ducts removed), drop all items
        if (others.isEmpty()) {
            for (ItemInTransit item : itemsInTransit) {
                dropItemAsEntity(item);
            }
            for (ItemInTransit item : returningItems) {
                dropItemAsEntity(item);
            }
            itemsInTransit.clear();
            returningItems.clear();
            itemsInTransitToDestination.clear();
            cleanup();
            return;
        }

        // Build a map of node positions to their new grids
        Map<BlockPos, ItemGrid> nodeToGrid = new HashMap<>();
        for (ItemGrid newGrid : others) {
            Map<BlockPos, ItemGridNode> newGridNodes = newGrid.getNodes();
            for (BlockPos pos : newGridNodes.keySet()) {
                nodeToGrid.put(pos, newGrid);
            }
        }

        // Process items in transit - decide based on which endpoints are reachable
        for (ItemInTransit item : itemsInTransit) {
            // Find which grid(s) contain our endpoints
            ItemGrid originGrid = nodeToGrid.get(item.origin);

            // Check if destination has an item handler (external block capability)
            BlockEntity destTile = world.getBlockEntity(item.destination);
            boolean destHasHandler = destTile != null &&
                destTile.getCapability(ForgeCapabilities.ITEM_HANDLER, item.destinationSide).isPresent();

            // Find which grid connects to the destination
            // We need to check if any grid has a path node that can reach the destination
            ItemGrid destGrid = null;
            for (ItemGrid newGrid : others) {
                // Check if any node in this grid is adjacent to the destination
                for (BlockPos nodePos : newGrid.getNodes().keySet()) {
                    // Check if nodePos is adjacent to destination
                    for (Direction dir : Direction.values()) {
                        if (nodePos.relative(dir).equals(item.destination)) {
                            destGrid = newGrid;
                            break;
                        }
                    }
                    if (destGrid != null) break;
                }
                if (destGrid != null) break;
            }

            // Decision logic:
            // 1. If destination is reachable AND item can reach it -> continue forward
            // 2. If destination NOT reachable BUT origin is -> backflow
            // 3. If neither reachable -> drop as entity

            if (destHasHandler && destGrid != null) {
                // Destination is reachable - check if item can actually reach it
                // We need to find the item's CURRENT position and see if there's a path to dest

                // Calculate item's current position based on distance traveled
                net.minecraft.world.phys.Vec3 currentPos = calculateItemWorldPosition(item);
                BlockPos currentBlockPos = BlockPos.containing(currentPos);

                // Find which grid the item's current position is in
                // Check if current position is a node in any grid
                ItemGrid itemCurrentGrid = nodeToGrid.get(currentBlockPos);

                // If not directly on a node, check adjacent positions
                if (itemCurrentGrid == null) {
                    for (Direction dir : Direction.values()) {
                        BlockPos adjacent = currentBlockPos.relative(dir);
                        itemCurrentGrid = nodeToGrid.get(adjacent);
                        if (itemCurrentGrid != null) {
                            break;
                        }
                    }
                }

                // Fallback to origin grid if still not found
                if (itemCurrentGrid == null) {
                    itemCurrentGrid = originGrid;
                }

                if (itemCurrentGrid == destGrid) {
                    // Item is in the grid that can reach destination - continue forward!
                    // Filter path to only include valid nodes in this grid
                    List<BlockPos> validPath = new ArrayList<>();
                    for (BlockPos pathPos : item.path) {
                        if (destGrid.getNodes().containsKey(pathPos)) {
                            validPath.add(pathPos);
                        }
                    }
                    item.path = validPath;
                    destGrid.itemsInTransit.add(item);
                    continue;
                }
            }

            // Destination not reachable from item's position - try backflow to origin
            if (originGrid != null) {
                // Can return to origin - reverse and send back
                item.reverse();

                // Filter the path to only include positions that exist in the origin grid
                List<BlockPos> validPath = new ArrayList<>();
                for (BlockPos pathPos : item.path) {
                    if (originGrid.getNodes().containsKey(pathPos)) {
                        validPath.add(pathPos);
                    }
                }
                item.path = validPath;

                originGrid.returningItems.add(item);

                // Notify the servo about the backflow
                ItemGridNode originNode = originGrid.getNodes().get(item.destination);
                if (originNode != null && originNode.getDuct() != null) {
                    if (originNode.getDuct().getAttachment(item.destinationSide) instanceof cofh.thermal.dynamics.common.attachment.ItemServoAttachment servo) {
                        servo.notifyBackflow();
                    }
                }
            } else {
                // Neither destination nor origin reachable - drop as entity
                dropItemAsEntity(item);
            }
        }

        // Process returning items - transfer to grid containing their destination (original origin)
        for (ItemInTransit item : returningItems) {
            ItemGrid destGrid = nodeToGrid.get(item.destination);

            // If destination not found, check path positions
            if (destGrid == null && item.path != null) {
                for (BlockPos pathPos : item.path) {
                    destGrid = nodeToGrid.get(pathPos);
                    if (destGrid != null) {
                        break;
                    }
                }
            }

            if (destGrid != null && destGrid.getNodes().containsKey(item.destination)) {
                // Transfer item to the grid containing its destination
                destGrid.returningItems.add(item);
            } else {
                // Destination is completely disconnected - drop as entity
                dropItemAsEntity(item);
            }
        }

        // Clear our queues (items have been transferred to new grids)
        itemsInTransit.clear();
        returningItems.clear();
        itemsInTransitToDestination.clear();

        // IMPORTANT: Clear render data from all ducts in the new grids
        // This prevents "ghost" items from appearing frozen in the old positions
        // The old grid's nodes are already transferred, so we clear from the new grids
        for (ItemGrid newGrid : others) {
            newGrid.clearAllRenderData();
        }

        // Cleanup grid tracking
        cleanup();
    }

    @Override
    public void refreshCapabilities() {
        // No capabilities to refresh
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = super.serializeNBT();
        
        // Serialize items in transit
        ListTag transitList = new ListTag();
        for (ItemInTransit item : itemsInTransit) {
            transitList.add(item.serializeNBT());
        }
        tag.put(TAG_ITEMS_IN_TRANSIT, transitList);

        ListTag returningList = new ListTag();
        for (ItemInTransit item : returningItems) {
            returningList.add(item.serializeNBT());
        }
        tag.put(TAG_RETURNING_ITEMS, returningList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        super.deserializeNBT(nbt);
        
        // Deserialize items in transit
        itemsInTransit.clear();
        itemsInTransitToDestination.clear();
        
        ListTag transitList = nbt.getList(TAG_ITEMS_IN_TRANSIT, 10);
        for (int i = 0; i < transitList.size(); i++) {
            ItemInTransit item = new ItemInTransit();
            item.deserializeNBT(transitList.getCompound(i));
            itemsInTransit.add(item);
            // Rebuild transit tracking
            addItemToTransitTracking(item);
        }

        returningItems.clear();
        ListTag returningList = nbt.getList(TAG_RETURNING_ITEMS, 10);
        for (int i = 0; i < returningList.size(); i++) {
            ItemInTransit item = new ItemInTransit();
            item.deserializeNBT(returningList.getCompound(i));
            returningItems.add(item);
        }

        // After deserialization, sync lastCheckedTopologyVersion with the current topology
        // to prevent immediate path validation (which would reverse items incorrectly
        // since destinations may not be loaded yet)
        lastCheckedTopologyVersion = getTopologyVersion();
    }
    
    // Note: Per-tick rerouting methods removed (checkAndRerouteItem, calculateCurrentPosition,
    // findNearestNode, calculatePathDistance) - path validation now handled by handleTopologyChange()

    /**
     * Represents an item traveling through the duct network
     */
    public static class ItemInTransit {
        public ItemStack stack;
        public BlockPos origin;
        public Direction originSide;
        public BlockPos destination;
        public Direction destinationSide;
        public List<BlockPos> path;
        public int currentPathIndex;
        public float distanceTraveled;
        public boolean returning;

        public ItemInTransit() {
            // For deserialization
        }

        public ItemInTransit(ItemStack stack, BlockPos origin, Direction originSide, BlockPos destination, Direction destinationSide, List<BlockPos> path) {
            this.stack = stack;
            this.origin = origin;
            this.originSide = originSide;
            this.destination = destination;
            this.destinationSide = destinationSide;
            this.path = new ArrayList<>(path);
            this.currentPathIndex = 0;
            this.distanceTraveled = 0;
            this.returning = false;
        }
        
        /**
         * Calculate the total physical distance the item needs to travel
         */
        public double getTotalDistance() {
            if (path.size() <= 1) {
                // Direct connection - distance from origin to destination
                return Math.sqrt(origin.distSqr(destination));
            }
            
            double totalDistance = 0;
            BlockPos currentPos = origin;
            
            // Add distance from origin to first path node
            if (!path.isEmpty()) {
                totalDistance += Math.sqrt(currentPos.distSqr(path.get(0)));
                currentPos = path.get(0);
            }
            
            // Add distance between path nodes
            for (int i = 1; i < path.size(); i++) {
                totalDistance += Math.sqrt(currentPos.distSqr(path.get(i)));
                currentPos = path.get(i);
            }
            
            // Add distance from last path node to destination
            totalDistance += Math.sqrt(currentPos.distSqr(destination));
            
            return totalDistance;
        }

        public void reverse() {
            // Calculate the remaining distance before swapping - this is how far the item
            // still needs to travel. After reversing, this becomes the distance traveled.
            double totalDistance = getTotalDistance();
            double remainingDistance = Math.max(0, totalDistance - distanceTraveled);

            // Swap origin and destination
            BlockPos tempPos = origin;
            Direction tempSide = originSide;
            origin = destination;
            originSide = destinationSide;
            destination = tempPos;
            destinationSide = tempSide;

            // Reverse path
            Collections.reverse(path);
            currentPathIndex = 0;

            // CRITICAL: The distance traveled after reversal is the remaining distance
            // that the item WOULD have traveled. This preserves the item's visual position
            // so it continues from where it was, not from the end of the path.
            distanceTraveled = (float) remainingDistance;
            returning = true;
        }

        public BlockPos getCurrentPosition() {
            if (currentPathIndex >= path.size() - 1) {
                return path.get(path.size() - 1);
            }
            return path.get(currentPathIndex);
        }

        public BlockPos getNextPosition() {
            if (currentPathIndex + 1 >= path.size()) {
                return path.get(path.size() - 1);
            }
            return path.get(currentPathIndex + 1);
        }

        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            tag.put("stack", stack.serializeNBT());
            tag.putLong("origin", origin.asLong());
            tag.putInt("originSide", originSide.ordinal());
            tag.putLong("destination", destination.asLong());
            tag.putInt("destSide", destinationSide.ordinal());
            tag.putInt("pathIndex", currentPathIndex);
            tag.putFloat("distance", distanceTraveled);
            tag.putBoolean("returning", returning);
            
            ListTag pathList = new ListTag();
            for (BlockPos pos : path) {
                CompoundTag posTag = new CompoundTag();
                posTag.putLong("pos", pos.asLong());
                pathList.add(posTag);
            }
            tag.put("path", pathList);
            
            return tag;
        }

        public void deserializeNBT(CompoundTag tag) {
            stack = ItemStack.of(tag.getCompound("stack"));
            origin = BlockPos.of(tag.getLong("origin"));
            originSide = Direction.values()[tag.getInt("originSide")];
            destination = BlockPos.of(tag.getLong("destination"));
            destinationSide = Direction.values()[tag.getInt("destSide")];
            currentPathIndex = tag.getInt("pathIndex");
            distanceTraveled = tag.getFloat("distance");
            returning = tag.getBoolean("returning");
            
            path = new ArrayList<>();
            ListTag pathList = tag.getList("path", 10);
            for (int i = 0; i < pathList.size(); i++) {
                path.add(BlockPos.of(pathList.getCompound(i).getLong("pos")));
            }
        }
    }
}