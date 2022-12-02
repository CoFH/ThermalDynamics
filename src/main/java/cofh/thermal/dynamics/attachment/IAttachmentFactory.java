package cofh.thermal.dynamics.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public interface IAttachmentFactory<T extends IAttachment> {

    T createAttachment(CompoundTag nbt, BlockPos pos, Direction side);

}
