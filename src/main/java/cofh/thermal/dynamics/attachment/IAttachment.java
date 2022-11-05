package cofh.thermal.dynamics.attachment;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.common.util.INBTSerializable;

public interface IAttachment extends INBTSerializable<CompoundTag>, MenuProvider {

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

}
