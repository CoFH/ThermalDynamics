package cofh.thermal.dynamics.interblock;

import cofh.core.util.filter.BaseFluidFilter;
import cofh.core.util.filter.IFilter;
import cofh.thermal.dynamics.common.inventory.interblock.FluidServoInterblockContainer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.jetbrains.annotations.Nullable;

public class FluidServoInterblock implements MenuProvider {

    protected final BlockPos pos;
    protected final Direction side;

    protected BaseFluidFilter filter = new BaseFluidFilter(5);

    public static final Component DISPLAY_NAME = Component.translatable("attachment.thermal.shim_servo");

    public FluidServoInterblock(BlockPos pos, Direction side) {

        this.pos = pos;
        this.side = side;
    }

    public FluidServoInterblock read(CompoundTag nbt) {

        if (nbt.isEmpty()) {
            return this;
        }
        filter.read(nbt);
        return this;
    }

    public CompoundTag write(CompoundTag nbt) {

        filter.write(nbt);
        return nbt;
    }

    public IFilter getFilter() {

        return filter;
    }

    public ResourceLocation[] getTextures() {

        return null;
    }

    @Override
    public Component getDisplayName() {

        return DISPLAY_NAME;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {

        return new FluidServoInterblockContainer(i, player.level, pos, side, inventory);
    }

}
