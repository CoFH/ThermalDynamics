package cofh.thermal.dynamics.attachment;

import cofh.thermal.dynamics.network.packet.client.AttachmentControlPacket;
import net.minecraft.network.FriendlyByteBuf;

public interface IPacketHandlerAttachment extends IAttachment {

    default void onControlUpdate() {

        AttachmentControlPacket.sendToClient(this);
        duct().onAttachmentUpdate();
    }

    // CONFIG
    default boolean hasConfigPacket() {

        return true;
    }

    default FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        return buffer;
    }

    default void handleConfigPacket(FriendlyByteBuf buffer) {

    }

    // CONTROL
    default boolean hasControlPacket() {

        return true;
    }

    default FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {

        return buffer;
    }

    default void handleControlPacket(FriendlyByteBuf buffer) {

    }

    // GUI
    default boolean hasGuiPacket() {

        return true;
    }

    default FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        return buffer;
    }

    default void handleGuiPacket(FriendlyByteBuf buffer) {

    }

}
