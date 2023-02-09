package cofh.thermal.dynamics.inventory.container.attachment;

import cofh.core.inventory.container.ContainerCoFH;
import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.attachment.IAttachment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public abstract class AttachmentContainer extends ContainerCoFH {

    public final BlockEntity hostTile;
    public final IAttachment baseAttachment;

    public AttachmentContainer(@Nullable MenuType<?> type, int id, Level world, BlockPos pos, Direction side, Inventory inventory, Player player) {

        super(type, id, inventory, player);
        hostTile = world.getBlockEntity(pos);

        if (hostTile instanceof IDuct<?, ?> duct) {
            this.baseAttachment = duct.getAttachment(side);
        } else {
            this.baseAttachment = null;
        }
    }

    @Override
    public boolean stillValid(Player player) {

        return baseAttachment != null && hostTile != null && !hostTile.isRemoved();
    }

}
