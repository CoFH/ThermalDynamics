package cofh.thermal.dynamics.common.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.Nullable;

public interface IAttachment extends INBTSerializable<CompoundTag> {

    IDuct<?, ?> duct();

    default Level world() {

        return duct().getHostWorld();
    }

    default BlockPos pos() {

        return duct().getHostPos();
    }

    Direction side();

    default void invalidate() {

    }

    IAttachment read(CompoundTag nbt);

    // Attachments MUST write their type to the NBT. Reading is optional.
    CompoundTag write(CompoundTag nbt);

    @Override
    default CompoundTag serializeNBT() {

        return write(new CompoundTag());
    }

    @Override
    default void deserializeNBT(CompoundTag nbt) {

        read(nbt);
    }

    default void tick() {

    }

    default ItemStack getItem() {

        return ItemStack.EMPTY;
    }

    default ResourceLocation getTexture() {

        return null;
    }

    /**
     * This allows for the grid's capability to be "wrapped" by an attachment.
     *
     * @param capability The capability being queried (e.g., ENERGY).
     * @param gridCap    The returned cap from the grid (or null)
     * @return The wrapped capability.
     */
    @Nullable
    default <T, C> T wrapGridCapability(BlockCapability<T, C> capability, T gridCap) {

        return gridCap;
    }

    /**
     * This allows for a tile's capability to be "wrapped" by an attachment.
     *
     * @param capability The capability being queried (e.g., ENERGY).
     * @param extCap     The returned cap from the tile (or null)
     * @return The wrapped capability.
     */
    @Nullable
    default <T, C> T wrapExternalCapability(BlockCapability<T, C> capability, T extCap) {

        return extCap;
    }

}
