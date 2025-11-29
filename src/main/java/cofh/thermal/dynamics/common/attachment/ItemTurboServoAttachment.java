package cofh.thermal.dynamics.common.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.common.inventory.attachment.ItemTurboServoAttachmentMenu;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import static cofh.lib.util.constants.NBTTags.TAG_TYPE;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.client.TDynTextures.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.ID_TURBO_SERVO_ATTACHMENT;
import static cofh.thermal.dynamics.init.registries.TDynIDs.TURBO_SERVO;

public class ItemTurboServoAttachment extends ItemServoAttachment {

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.item_turbo_servo");

    public static final int DEFAULT_TRANSFER = 1; // Default items per operation
    public static final int MIN_TRANSFER = 1; // Minimum items per operation
    public static final int MAX_TRANSFER = 64; // Maximum items per operation for turbo servo

    // Turbo extraction interval (71 ticks ≈ 3.5 seconds = 17 extractions/min)
    protected static final int TURBO_BASE_EXTRACTION_INTERVAL = 71;

    public ItemTurboServoAttachment(IDuct<?, ?> duct, Direction side) {
        super(duct, side);
        this.amountTransfer = DEFAULT_TRANSFER;
    }

    @Override
    public int getMaxTransfer() {
        return MAX_TRANSFER; // Override parent's MAX_TRANSFER (8) with turbo's limit (64)
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {
        super.write(nbt);
        nbt.putString(TAG_TYPE, TURBO_SERVO); // Override parent's SERVO type
        return nbt;
    }

    /**
     * Get the extraction interval for turbo servo.
     * Turbo servo is ~2.8x faster than basic servo.
     * @return Ticks between extractions (71 ticks = 17/min)
     */
    @Override
    protected int getExtractionInterval() {
        return TURBO_BASE_EXTRACTION_INTERVAL;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ITEMS.get(ID_TURBO_SERVO_ATTACHMENT));
    }

    @Override
    public Component getDisplayName() {
        return DISPLAY_NAME;
    }

    @Override
    public ResourceLocation getTexture() {
        boolean active = rsControl.getState();
        boolean overflow = isOverflowing();

        if (overflow) {
            return active ? TURBO_SERVO_ATTACHMENT_ACTIVE_OVERFLOW_LOC : TURBO_SERVO_ATTACHMENT_OVERFLOW_LOC;
        }
        return active ? TURBO_SERVO_ATTACHMENT_ACTIVE_LOC : TURBO_SERVO_ATTACHMENT_LOC;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ItemTurboServoAttachmentMenu(i, player.level, pos(), side, inventory, player);
    }

    // Note: tick(), tryReinjectOverflow(), extractAndRouteItems(), and getExternalCapability()
    // are inherited from ItemServoAttachment. The turbo servo uses the same logic but with
    // different parameters (getExtractionInterval returns 71, getMaxTransfer returns 64).
}