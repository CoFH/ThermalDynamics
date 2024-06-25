package cofh.thermal.dynamics.common.block.entity;

import cofh.thermal.lib.common.block.entity.LogisticsBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class SideExpanderBlockEntity extends LogisticsBlockEntity implements MenuProvider {

    public SideExpanderBlockEntity(BlockPos pos, BlockState state) {

        super(null, pos, state);
    }

    // region MenuProvider
    @Override
    public Component getDisplayName() {

        return Component.translatable(this.getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {

        return null;
    }
    // endregion
}
