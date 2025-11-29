package cofh.thermal.dynamics.common.block;

import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.function.Supplier;

public class ItemDuctBlock extends DuctBlock {

    private final boolean windowed;

    public ItemDuctBlock(Properties builder, boolean windowed, Supplier<BlockEntityType<?>> blockEntityType) {
        super(builder, blockEntityType);
        this.windowed = windowed;
    }

    public boolean isWindowed() {
        return windowed;
    }

}
