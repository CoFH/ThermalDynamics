package cofh.thermal.dynamics.common.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;

public class AttachmentHelper {

    private AttachmentHelper() {

    }

    public static void openAttachmentScreen(ServerPlayer player, MenuProvider containerSupplier, BlockPos pos, Direction side) {

        player.openMenu(containerSupplier, buf -> {
            buf.writeBlockPos(pos);
            buf.writeEnum(side);
        });
    }

}
