package cofh.thermal.dynamics.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nonnull;


public interface IAttachment extends INBTSerializable<CompoundTag> {

    BlockPos pos();

    Direction side();

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

    /**
     * This allows for the grid's capability to be "wrapped" by an attachment.
     *
     * @param cap        The capability being queried (e.g., ENERGY).
     * @param gridLazOpt The returned LazyOptional from the grid (or a LazyOptional.empty())
     * @return The wrapped capability.
     */
    default <T> LazyOptional<T> wrapGridCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> gridLazOpt) {

        return gridLazOpt;
    }

    /**
     * This allows for a tile's capability to be "wrapped" by an attachment.
     *
     * @param cap       The capability being queried (e.g., ENERGY).
     * @param extLazOpt The returned LazyOptional from the tile (or a LazyOptional.empty())
     * @return The wrapped capability.
     */
    default <T> LazyOptional<T> wrapExternalCapability(@Nonnull Capability<T> cap, @Nonnull LazyOptional<T> extLazOpt) {

        return extLazOpt;
    }

}
