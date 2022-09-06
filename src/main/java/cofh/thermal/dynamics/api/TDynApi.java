package cofh.thermal.dynamics.api;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridHost;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class TDynApi {

    private TDynApi() {

    }

    public static final Capability<IGridHost> GRID_HOST_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public static final Capability<IGridContainer> GRID_CONTAINER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    public static void register(RegisterCapabilitiesEvent event) {

        event.register(IGridHost.class);
        event.register(IGridContainer.class);
    }

}
