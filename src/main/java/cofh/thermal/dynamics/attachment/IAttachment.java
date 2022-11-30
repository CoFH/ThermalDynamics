package cofh.thermal.dynamics.attachment;

import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;

public interface IAttachment extends INBTSerializable<CompoundTag> {

    IAttachment read(CompoundTag nbt);

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

    /**
     * This allows for the grid's capability to be "wrapped" by an attachment.
     *
     * @param cap     The capability being queried (e.g., ENERGY).
     * @param gridCap The returned LazyOptional from the grid (or a LazyOptional.empty())
     * @return The wrapped capability.
     */
    default <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridCap) {

        return gridCap;
    }

    /**
     * This allows for a tile's capability to be "wrapped" by an attachment.
     *
     * @param cap     The capability being queried (e.g., ENERGY).
     * @param tileCap The returned LazyOptional from the tile (or a LazyOptional.empty())
     * @return The wrapped capability.
     */
    default <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> tileCap) {

        return tileCap;
    }

}
