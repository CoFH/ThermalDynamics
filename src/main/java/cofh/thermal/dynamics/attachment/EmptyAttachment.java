package cofh.thermal.dynamics.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public class EmptyAttachment implements IAttachment {

    public static final EmptyAttachment INSTANCE = new EmptyAttachment();

    @Override
    public BlockPos pos() {

        return BlockPos.ZERO;
    }

    @Override
    public Direction side() {

        return null;
    }

    @Override
    public IAttachment read(CompoundTag nbt) {

        return INSTANCE;
    }

    @Override
    public CompoundTag write(CompoundTag nbt) {

        return nbt;
    }

}
