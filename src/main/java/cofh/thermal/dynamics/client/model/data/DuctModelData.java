package cofh.thermal.dynamics.client.model.data;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

/**
 * Created by covers1624 on 12/26/21.
 */
public class DuctModelData implements IModelData {

    // I - Internal connection
    // E - External connection
    // X - Unused
    // XXXXXXXXXXXXXXXX_XXXXEEEEEEIIIIII
    private int state;
    @Nullable
    private ResourceLocation fill;
    @Nullable
    private ResourceLocation[] servos;

    private int fillColor = 0xFFFFFF;

    public DuctModelData() {

    }

    public DuctModelData(DuctModelData other) {

        state = other.state;
        fill = other.fill;
        if (other.servos != null) {
            servos = other.servos.clone();
        }
    }

    public void setInternalConnection(Direction dir, boolean present) {

        setStateBit(dir.ordinal(), present);
    }

    public void setExternalConnection(Direction dir, boolean present) {

        setStateBit(dir.ordinal() + 6, present);
    }

    public void setFill(@Nullable ResourceLocation loc) {

        fill = loc;
    }

    public void setFillColor(int color) {

        fillColor = color;
    }

    public void setServo(Direction dir, @Nullable ResourceLocation loc) {

        if (servos == null) {
            servos = new ResourceLocation[6];
        }
        servos[dir.ordinal()] = null;
    }

    public boolean hasInternalConnection(Direction dir) {

        return isStateBitSet(dir.ordinal());
    }

    public boolean hasExternalConnection(Direction dir) {

        return isStateBitSet(dir.ordinal() + 6);
    }

    public int getConnectionState() {

        return state;
    }

    private void setStateBit(int bit, boolean value) {

        int state = getConnectionState();
        if (value) {
            state |= 1 << bit;
        } else {
            state &= ~(1 << bit);
        }
        setConnectionState(state);
    }

    private boolean isStateBitSet(int bit) {

        return (state & (1 << bit)) > 0;
    }

    private void setConnectionState(int state) {

        this.state = state;
    }

    @Nullable
    public ResourceLocation getFill() {

        return fill;
    }

    @Nullable
    public ResourceLocation getServo(Direction dir) {

        if (servos == null) return null;

        return servos[dir.ordinal()];
    }

    public int getFillColor() {

        return fillColor;
    }

    //@formatter:off
    @Override public boolean hasProperty(ModelProperty<?> prop) { return false; }
    @Nullable @Override public <T> T getData(ModelProperty<T> prop) { return null; }
    @Nullable @Override public <T> T setData(ModelProperty<T> prop, T data) { return null; }
    //@formatter:on

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DuctModelData that = (DuctModelData) o;

        if (state != that.state) return false;
        if (!Objects.equals(fill, that.fill)) return false;
        return Arrays.equals(servos, that.servos);
    }

    @Override
    public int hashCode() {

        int result = state;
        result = 31 * result + (fill != null ? fill.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(servos);
        return result;
    }

}
