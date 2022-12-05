package cofh.thermal.dynamics.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

public interface IAttachmentFactory<T extends IAttachment> {

    T createAttachment(CompoundTag nbt, IDuct<?, ?> duct, Direction side);

}
