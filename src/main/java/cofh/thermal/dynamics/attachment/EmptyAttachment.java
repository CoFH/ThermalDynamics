package cofh.thermal.dynamics.attachment;

import net.minecraft.nbt.CompoundTag;

public class EmptyAttachment implements IAttachment {

    public static final EmptyAttachment INSTANCE = new EmptyAttachment();

    @Override
    public IAttachment read(CompoundTag nbt) {

        return INSTANCE;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        return nbt;
    }

}
