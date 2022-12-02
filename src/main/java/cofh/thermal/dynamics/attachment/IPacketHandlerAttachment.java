package cofh.thermal.dynamics.attachment;

import net.minecraft.network.FriendlyByteBuf;

public interface IPacketHandlerAttachment extends IAttachment {

    // CONFIG
    default FriendlyByteBuf getConfigPacket(FriendlyByteBuf buffer) {

        return buffer;
    }

    default void handleConfigPacket(FriendlyByteBuf buffer) {

    }

    // CONTROL
    default FriendlyByteBuf getControlPacket(FriendlyByteBuf buffer) {

        return buffer;
    }

    default void handleControlPacket(FriendlyByteBuf buffer) {

    }

    // GUI
    default FriendlyByteBuf getGuiPacket(FriendlyByteBuf buffer) {

        return buffer;
    }

    default void handleGuiPacket(FriendlyByteBuf buffer) {

    }

}
