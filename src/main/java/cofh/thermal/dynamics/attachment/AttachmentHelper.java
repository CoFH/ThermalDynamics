package cofh.thermal.dynamics.attachment;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;

public class AttachmentHelper {

    private AttachmentHelper() {

    }

    public static void openAttachmentScreen(ServerPlayer player, MenuProvider containerSupplier, BlockPos pos, Direction side) {

        NetworkHooks.openGui(player, containerSupplier, buf -> {
            buf.writeBlockPos(pos);
            buf.writeEnum(side);
        });
    }

}
