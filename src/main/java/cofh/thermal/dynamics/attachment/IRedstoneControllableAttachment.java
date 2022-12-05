package cofh.thermal.dynamics.attachment;

import cofh.lib.api.control.IRedstoneControllable;

public interface IRedstoneControllableAttachment extends IAttachment, IPacketHandlerAttachment, IRedstoneControllable {

    RedstoneControlLogic redstoneControl();

    // region IRedstoneControl
    @Override
    default int getPower() {

        return redstoneControl().getPower();
    }

    @Override
    default int getThreshold() {

        return redstoneControl().getThreshold();
    }

    @Override
    default ControlMode getMode() {

        return redstoneControl().getMode();
    }

    @Override
    default void setPower(int power) {

        redstoneControl().setPower(power);
    }

    @Override
    default void setControl(int threshold, ControlMode mode) {

        redstoneControl().setControl(threshold, mode);
    }

    @Override
    default boolean isControllable() {

        return redstoneControl().isControllable();
    }
    // endregion
}