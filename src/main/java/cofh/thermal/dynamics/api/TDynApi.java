package cofh.thermal.dynamics.api;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class TDynApi {

    private TDynApi() {

    }

    @CapabilityInject (IGridHost.class)
    public static final Capability<IGridHost> GRID_HOST_CAPABILITY = null;

    @CapabilityInject (IGridContainer.class)
    public static final Capability<IGridContainer> GRID_CONTAINER_CAPABILITY = null;

}
