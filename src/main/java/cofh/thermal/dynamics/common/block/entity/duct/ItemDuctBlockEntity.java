package cofh.thermal.dynamics.common.block.entity.duct;

import cofh.core.common.network.packet.client.TileStatePacket;
import cofh.lib.api.block.entity.IPacketHandlerTile;
import cofh.thermal.dynamics.api.grid.IGridHostUpdateable;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.helper.GridHelper;
import cofh.thermal.dynamics.client.renderer.ItemTransportRenderer;
import cofh.thermal.dynamics.common.grid.item.ItemGrid;
import cofh.thermal.dynamics.common.grid.item.ItemGridNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static cofh.thermal.dynamics.init.registries.TDynGrids.ITEM_GRID;

public class ItemDuctBlockEntity extends DuctBlockEntity<ItemGrid, ItemGridNode> implements IGridHostUpdateable, IPacketHandlerTile {

    private static final Logger LOGGER = LogManager.getLogger();
    private final boolean windowed;

    // Client-side item transit data for rendering
    protected List<ItemTransitData> renderItemsInTransit = new ArrayList<>();

    public ItemDuctBlockEntity(BlockEntityType<?> blockEntityType, BlockPos pos, BlockState state, boolean windowed) {
        super(blockEntityType, pos, state);
        this.windowed = windowed;
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        // Register windowed ducts for efficient rendering
        if (level != null && level.isClientSide && windowed) {
            ItemTransportRenderer.registerWindowedDuct(this);
        }
    }
    
    @Override
    public void setRemoved() {
        super.setRemoved();
        // Unregister from renderer when removed
        if (level != null && level.isClientSide && windowed) {
            ItemTransportRenderer.unregisterWindowedDuct(getBlockPos());
        }
    }

    public boolean isWindowed() {
        return windowed;
    }

    /**
     * Check if this duct is a dense variant.
     * Dense ducts add +1000 to path weight, making them lowest priority (overflow only).
     */
    public boolean isDense() {
        var registryName = getBlockState().getBlock().builtInRegistryHolder().key().location();
        return registryName.getPath().contains("dense");
    }

    /**
     * Check if this duct is a vacuum variant.
     * Vacuum ducts have highest routing priority - paths through them are checked first.
     */
    public boolean isVacuum() {
        var registryName = getBlockState().getBlock().builtInRegistryHolder().key().location();
        return registryName.getPath().contains("vacuum");
    }

    @Override
    protected boolean canConnectToBlock(Direction dir) {
        if (!connections[dir.ordinal()].allowBlockConnection()) {
            return false;
        }
        BlockEntity tile = level.getBlockEntity(getBlockPos().relative(dir));
        if (tile == null || GridHelper.getGridHost(tile) != null) {
            return false;
        }
        
        return tile.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).isPresent();
    }

    @Override
    public IGridType<ItemGrid> getGridType() {
        return ITEM_GRID.get();
    }
    
    public List<ItemTransitData> getRenderItemsInTransit() {
        return renderItemsInTransit;
    }
    
    @Override
    public void update() {
        // Only send packets if we're properly initialized
        if (level != null && !level.isClientSide && hasLevel()) {
            // Update render items before sending packet
            if (windowed) {
                updateRenderItems();
            }
            TileStatePacket.sendToClient(this);
        }
    }
    
    public void updateRenderItems() {
        if (level == null || level.isClientSide) {
            return;
        }

        // Only update windowed ducts
        if (!windowed) {
            return;
        }

        ItemGrid grid = getGrid();

        // If grid is null/lost, clear render items to prevent frozen ghost items
        if (grid == null) {
            if (!renderItemsInTransit.isEmpty()) {
                renderItemsInTransit = new ArrayList<>();
            }
            return;
        }

        // Collect items in transit that should be rendered in this duct
        List<ItemTransitData> newRenderItems = new ArrayList<>();

        // Include forward-traveling items
        Collection<ItemGrid.ItemInTransit> itemsInTransit = grid.getItemsInTransit();
        for (ItemGrid.ItemInTransit item : itemsInTransit) {
            // Calculate render data with velocity and progress for smooth interpolation
            ItemTransitData renderData = calculateItemRenderDataForDuct(item);
            if (renderData != null) {
                newRenderItems.add(renderData);
            }
        }

        // Include returning (backflowing) items - they need to be rendered too!
        Collection<ItemGrid.ItemInTransit> returningItems = grid.getReturningItems();
        for (ItemGrid.ItemInTransit item : returningItems) {
            // Calculate render data for returning items (they travel in reverse)
            ItemTransitData renderData = calculateItemRenderDataForDuct(item);
            if (renderData != null) {
                newRenderItems.add(renderData);
            }
        }

        // Only update if render items changed to avoid unnecessary client updates
        if (!renderItemsEqual(renderItemsInTransit, newRenderItems)) {
            renderItemsInTransit = newRenderItems;
        }
    }
    
    /**
     * Mark render data as dirty for client-side caching
     */
    private void markRenderDataDirty() {
        // This will be called on client side when tile data is received
        if (level != null && level.isClientSide && windowed) {
            ItemTransportRenderer.markDuctDirty(getBlockPos());
        }
    }
    
    /**
     * Compare two render item lists for equality to avoid unnecessary updates
     */
    private boolean renderItemsEqual(List<ItemTransitData> list1, List<ItemTransitData> list2) {
        if (list1.size() != list2.size()) {
            return false;
        }
        
        for (int i = 0; i < list1.size(); i++) {
            ItemTransitData item1 = list1.get(i);
            ItemTransitData item2 = list2.get(i);
            
            if (!ItemStack.matches(item1.stack, item2.stack) || 
                !item1.position.equals(item2.position)) {
                return false;
            }
        }
        
        return true;
    }
    
    private Vec3 calculateItemPosition(ItemGrid.ItemInTransit item) {
        double totalDistance = item.getTotalDistance();
        double currentDistance = item.distanceTraveled;
        double progress = Math.min(1.0, currentDistance / totalDistance);

        // Use SAME logic for ALL items (forward and returning)
        // After reverse(): origin=chest, destination=servo, path=reversed
        // The math works identically - just different positions
        if (item.path.isEmpty()) {
            // Direct connection - interpolate between origin and destination
            Vec3 origin = Vec3.atCenterOf(item.origin);
            Vec3 destination = Vec3.atCenterOf(item.destination);
            return origin.lerp(destination, progress);
        }

        // Multi-segment path - calculate position along the full path
        return calculatePositionAlongPath(item, progress);
    }

    private Vec3 calculatePositionAlongPath(ItemGrid.ItemInTransit item, double progress) {
        // Calculate cumulative distances for each path segment
        double totalDistance = item.getTotalDistance();
        double targetDistance = progress * totalDistance;
        
        Vec3 currentPos = Vec3.atCenterOf(item.origin);
        double accumulatedDistance = 0;
        
        // Create list of all waypoints: origin -> path nodes -> destination
        List<Vec3> waypoints = new ArrayList<>();
        waypoints.add(Vec3.atCenterOf(item.origin));
        for (BlockPos pathPos : item.path) {
            waypoints.add(Vec3.atCenterOf(pathPos));
        }
        waypoints.add(Vec3.atCenterOf(item.destination));
        
        // Find which segment the item is currently on
        for (int i = 0; i < waypoints.size() - 1; i++) {
            Vec3 segmentStart = waypoints.get(i);
            Vec3 segmentEnd = waypoints.get(i + 1);
            double segmentDistance = segmentStart.distanceTo(segmentEnd);
            
            if (targetDistance <= accumulatedDistance + segmentDistance) {
                // Item is on this segment
                double segmentProgress = segmentDistance > 0 ? (targetDistance - accumulatedDistance) / segmentDistance : 0;
                return segmentStart.lerp(segmentEnd, Math.min(1.0, segmentProgress));
            }
            
            accumulatedDistance += segmentDistance;
        }
        
        // Fallback to destination
        return Vec3.atCenterOf(item.destination);
    }
    
    // Handle client packet updates - mark as dirty when receiving from server
    public void onClientDataReceived() {
        if (level != null && level.isClientSide && windowed) {
            markRenderDataDirty();
        }
    }
    
    // Client data handling - called when tile data is received from server
    public void triggerClientUpdate() {
        if (level != null && level.isClientSide && windowed) {
            onClientDataReceived();
        }
    }
    
    /**
     * Check if this duct should render the item.
     * Uses the item's calculated position to determine if it's within this duct's bounds.
     */
    private boolean isDuctOnItemPath(ItemGrid.ItemInTransit item) {
        // For returning items, origin is an external block (not a duct) and destination is the servo duct
        // For forward items, origin is servo duct and destination is external block
        // We check path nodes and destination for returning, or origin and path for forward

        // Check if this duct is explicitly in the path
        for (BlockPos pathPos : item.path) {
            if (worldPosition.equals(pathPos)) {
                return true;
            }
        }

        // Check destination (servo duct for returning items)
        if (worldPosition.equals(item.destination)) {
            return true;
        }

        // Check origin (servo duct for forward items)
        if (worldPosition.equals(item.origin)) {
            return true;
        }

        // If not explicitly in path, check if item's calculated position is within this duct
        // This handles cases where the path has been filtered (e.g., during backflow)
        // and the item is traveling through ducts that aren't explicit nodes in the path
        Vec3 itemPos = calculateItemPosition(item);
        BlockPos itemBlockPos = BlockPos.containing(itemPos);

        // Check if item is in this duct or an adjacent block (for items on boundaries)
        if (worldPosition.equals(itemBlockPos)) {
            return true;
        }

        // Also check if the item is within this duct's block bounds
        // This ensures we render items that are crossing between ducts
        Vec3 ductCenter = Vec3.atCenterOf(worldPosition);
        double distSq = itemPos.distanceToSqr(ductCenter);
        if (distSq < 0.75) { // ~0.87 blocks radius, covers items crossing duct boundaries
            return true;
        }

        return false;
    }

    /**
     * Calculate the velocity vector for an item (direction it's traveling)
     * Uses SAME logic for ALL items (forward and returning)
     */
    private Vec3 calculateItemVelocity(ItemGrid.ItemInTransit item) {
        double totalDistance = item.getTotalDistance();
        double currentDistance = item.distanceTraveled;
        double progress = Math.min(1.0, currentDistance / totalDistance);

        // Build waypoints list: origin -> path nodes -> destination
        List<Vec3> waypoints = new ArrayList<>();
        waypoints.add(Vec3.atCenterOf(item.origin));
        for (BlockPos pathPos : item.path) {
            waypoints.add(Vec3.atCenterOf(pathPos));
        }
        waypoints.add(Vec3.atCenterOf(item.destination));

        // Find current segment
        double targetDistance = progress * totalDistance;
        double accumulatedDistance = 0;

        for (int i = 0; i < waypoints.size() - 1; i++) {
            Vec3 segmentStart = waypoints.get(i);
            Vec3 segmentEnd = waypoints.get(i + 1);
            double segmentDistance = segmentStart.distanceTo(segmentEnd);

            if (targetDistance <= accumulatedDistance + segmentDistance) {
                // Item is on this segment - return normalized direction
                return segmentEnd.subtract(segmentStart).normalize();
            }

            accumulatedDistance += segmentDistance;
        }

        // Fallback - moving toward destination
        if (waypoints.size() >= 2) {
            return waypoints.get(waypoints.size() - 1).subtract(waypoints.get(waypoints.size() - 2)).normalize();
        }
        return Vec3.ZERO;
    }

    /**
     * Calculate where to render the item within this specific duct based on the item's progress
     * Returns ItemTransitData with position, velocity, and progress for smooth client interpolation
     */
    private ItemTransitData calculateItemRenderDataForDuct(ItemGrid.ItemInTransit item) {
        // Check if this duct is part of the item's journey
        if (isDuctOnItemPath(item)) {
            Vec3 globalPosition = calculateItemPosition(item);
            Vec3 velocity = calculateItemVelocity(item);
            float progress = (float) (item.distanceTraveled / item.getTotalDistance());
            return new ItemTransitData(item.stack, globalPosition, velocity, progress);
        }

        return null; // Don't render if item isn't related to this duct
    }

    /**
     * @deprecated Use calculateItemRenderDataForDuct instead
     */
    @Deprecated
    private Vec3 calculateItemRenderPositionForDuct(ItemGrid.ItemInTransit item) {
        // Check if this duct is part of the item's journey
        if (isDuctOnItemPath(item)) {
            Vec3 globalPosition = calculateItemPosition(item);
            return globalPosition;
        }

        return null; // Don't render if item isn't related to this duct
    }
    
    // region NETWORK
    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    // STATE
    @Override
    public FriendlyByteBuf getStatePacket(FriendlyByteBuf buffer) {
        // Send the current render items with velocity and progress for smooth client interpolation
        buffer.writeInt(renderItemsInTransit.size());
        for (ItemTransitData itemData : renderItemsInTransit) {
            buffer.writeItem(itemData.stack);
            buffer.writeDouble(itemData.position.x);
            buffer.writeDouble(itemData.position.y);
            buffer.writeDouble(itemData.position.z);
            buffer.writeDouble(itemData.velocity.x);
            buffer.writeDouble(itemData.velocity.y);
            buffer.writeDouble(itemData.velocity.z);
            buffer.writeFloat(itemData.progress);
        }

        super.getStatePacket(buffer);
        return buffer;
    }

    @Override
    public void handleStatePacket(FriendlyByteBuf buffer) {
        renderItemsInTransit.clear();
        int count = buffer.readInt();

        for (int i = 0; i < count; i++) {
            ItemStack stack = buffer.readItem();
            double x = buffer.readDouble();
            double y = buffer.readDouble();
            double z = buffer.readDouble();
            double vx = buffer.readDouble();
            double vy = buffer.readDouble();
            double vz = buffer.readDouble();
            float progress = buffer.readFloat();
            ItemTransitData transitData = new ItemTransitData(stack, new Vec3(x, y, z), new Vec3(vx, vy, vz), progress);
            renderItemsInTransit.add(transitData);
        }

        super.handleStatePacket(buffer);

        // Trigger client-side render update after receiving packet data
        triggerClientUpdate();
    }
    // endregion
    
    /**
     * Data class for client-side item rendering with interpolation support
     */
    public static class ItemTransitData {
        public final ItemStack stack;
        public final Vec3 position;
        public final Vec3 velocity;  // Direction and speed for client-side interpolation
        public final float progress; // 0-1 progress for rotation calculation
        public final long timestamp; // When this data was received (client-side)

        public ItemTransitData(ItemStack stack, Vec3 position) {
            this(stack, position, Vec3.ZERO, 0f);
        }

        public ItemTransitData(ItemStack stack, Vec3 position, Vec3 velocity, float progress) {
            this.stack = stack.copy();
            this.position = position;
            this.velocity = velocity;
            this.progress = progress;
            this.timestamp = System.currentTimeMillis();
        }

        /**
         * Get interpolated position based on time since data was received
         */
        public Vec3 getInterpolatedPosition(float partialTick) {
            if (velocity.lengthSqr() < 0.0001) {
                return position;
            }
            // Interpolate based on time since last server update
            // Server updates at 20 TPS, so interpolate over 50ms (1 tick)
            long timeSinceUpdate = System.currentTimeMillis() - timestamp;
            double interpolationFactor = Math.min(timeSinceUpdate / 50.0, 2.0); // Cap at 2 ticks worth
            return position.add(velocity.scale(interpolationFactor * 0.05)); // 0.05 = ITEM_SPEED
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            ItemTransitData other = (ItemTransitData) obj;
            return ItemStack.matches(stack, other.stack) && position.equals(other.position);
        }

        @Override
        public int hashCode() {
            return position.hashCode();
        }
    }

}