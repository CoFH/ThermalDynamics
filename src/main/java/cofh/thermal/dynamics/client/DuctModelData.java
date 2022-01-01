package cofh.thermal.dynamics.client;

import net.minecraft.util.Direction;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.data.ModelProperty;

import javax.annotation.Nullable;

/**
 * Created by covers1624 on 12/26/21.
 */
public class DuctModelData implements IModelData {

    // I - Internal connection
    // E - External connection
    // X - Unused
    // XXXXXXXXXXXXXXXX_XXXXEEEEEEIIIIII
    private int state;

    public DuctModelData() {
    }

    public DuctModelData(DuctModelData other) {

        state = other.state;
    }

    public void setInternalConnection(Direction dir, boolean present) {

        int state = getConnectionState();
        if (present) {
            state |= (1 << dir.ordinal());
        } else {
            state &= ~(1 << dir.ordinal());
        }
        setConnectionState(state);
    }

    public void setExternalConnection(Direction dir, boolean present) {

        int state = getConnectionState();
        if (present) {
            state |= 1 << (dir.ordinal() + 6);
        } else {
            state &= ~(1 << (dir.ordinal() + 6));
        }
        setConnectionState(state);
    }

    public boolean hasInternalConnection(Direction dir) {

        return (state & (1 << dir.ordinal())) > 0;
    }

    public boolean hasExternalConnection(Direction dir) {

        return (state & (1 << (dir.ordinal() + 6))) > 0;
    }

    public int getConnectionState() {

        return state;
    }

    private void setConnectionState(int state) {

        this.state = state;
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

        return state == that.state;
    }

    @Override
    public int hashCode() {

        return state;
    }
}
