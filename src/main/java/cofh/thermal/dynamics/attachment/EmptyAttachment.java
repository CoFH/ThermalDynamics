package cofh.thermal.dynamics.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

public class EmptyAttachment implements IAttachment {

    public static final EmptyAttachment INSTANCE = new EmptyAttachment();

    @Override
    public IDuct<?, ?> duct() {

        return null;
    }

    @Override
    public Level world() {

        return null;
    }

    @Override
    public BlockPos pos() {

        return BlockPos.ZERO;
    }

    @Override
    public Direction side() {

        return null;
    }

    @Override
    public void invalidate() {

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
