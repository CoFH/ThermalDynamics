package cofh.thermal.dynamics.attachment;

import cofh.lib.api.control.IRedstoneControllable;
import cofh.lib.util.Utils;
import cofh.thermal.dynamics.network.packet.server.AttachmentRedstoneControlPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

import static cofh.lib.util.Constants.TRUE;
import static cofh.lib.util.constants.NBTTags.*;

public class RedstoneControlLogic implements IRedstoneControllable {

    protected IRedstoneControllableAttachment attachment;
    protected Supplier<Boolean> enabled;

    protected int power;
    protected int threshold;
    protected ControlMode mode = ControlMode.DISABLED;

    public RedstoneControlLogic(IRedstoneControllableAttachment attachment) {

        this(attachment, TRUE);
    }

    public RedstoneControlLogic(IRedstoneControllableAttachment attachment, Supplier<Boolean> enabled) {

        this.attachment = attachment;
        this.enabled = enabled;
    }

    public RedstoneControlLogic setEnabled(Supplier<Boolean> enabled) {

        this.enabled = enabled;
        return this;
    }

    // region NETWORK
    public void readFromBuffer(FriendlyByteBuf buffer) {

        power = buffer.readByte();
        threshold = buffer.readByte();
        mode = ControlMode.VALUES[buffer.readByte()];
    }

    public void writeToBuffer(FriendlyByteBuf buffer) {

        buffer.writeByte(power);
        buffer.writeByte(threshold);
        buffer.writeByte(mode.ordinal());
    }
    // endregion

    // region NBT
    public RedstoneControlLogic read(CompoundTag nbt) {

        CompoundTag subTag = nbt.getCompound(TAG_RS_CONTROL);

        if (!subTag.isEmpty()) {
            power = subTag.getByte(TAG_RS_POWER);
            threshold = subTag.getByte(TAG_RS_THRESHOLD);
            mode = !isControllable() ? ControlMode.DISABLED : ControlMode.VALUES[subTag.getByte(TAG_RS_MODE)];
        }
        return this;
    }

    public CompoundTag write(CompoundTag nbt) {

        if (isControllable()) {
            CompoundTag subTag = new CompoundTag();

            subTag.putByte(TAG_RS_POWER, (byte) power);
            subTag.putByte(TAG_RS_THRESHOLD, (byte) threshold);
            subTag.putByte(TAG_RS_MODE, (byte) mode.ordinal());

            nbt.put(TAG_RS_CONTROL, subTag);
        }
        return nbt;
    }

    public RedstoneControlLogic readSettings(CompoundTag nbt) {

        CompoundTag subTag = nbt.getCompound(TAG_RS_CONTROL);

        if (!subTag.isEmpty() && isControllable()) {
            threshold = subTag.getByte(TAG_RS_THRESHOLD);
            mode = !isControllable() ? ControlMode.DISABLED : ControlMode.VALUES[subTag.getByte(TAG_RS_MODE)];
        }
        return this;
    }

    public CompoundTag writeSettings(CompoundTag nbt) {

        if (isControllable()) {
            CompoundTag subTag = new CompoundTag();

            subTag.putByte(TAG_RS_THRESHOLD, (byte) threshold);
            subTag.putByte(TAG_RS_MODE, (byte) mode.ordinal());

            nbt.put(TAG_RS_CONTROL, subTag);
        }
        return nbt;
    }
    // endregion

    // region IRedstoneControl
    @Override
    public boolean isControllable() {

        return enabled.get();
    }

    @Override
    public int getPower() {

        return power;
    }

    @Override
    public int getThreshold() {

        return threshold;
    }

    @Override
    public ControlMode getMode() {

        return mode;
    }

    @Override
    public void setPower(int power) {

        this.power = power;
    }

    @Override
    public void setControl(int threshold, ControlMode mode) {

        int curThreshold = this.threshold;
        ControlMode curMode = this.mode;
        this.threshold = threshold;
        this.mode = mode;

        if (Utils.isClientWorld(attachment.world())) {
            AttachmentRedstoneControlPacket.sendToServer(this.attachment);
            this.threshold = curThreshold;
            this.mode = curMode;
        } else {
            attachment.onControlUpdate();
        }
    }
    // endregion
}
