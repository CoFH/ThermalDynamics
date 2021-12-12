package cofh.thermal.dynamics.api;

import cofh.thermal.dynamics.api.grid.GridContainer;
import cofh.thermal.dynamics.api.grid.GridHost;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

public class TDApi {

    private TDApi() {

    }

    @CapabilityInject (GridHost.class)
    public static final Capability<GridHost> GRID_HOST_CAPABILITY = null;

    @CapabilityInject (GridContainer.class)
    public static final Capability<GridContainer> GRID_CONTAINER_CAPABILITY = null;

}
