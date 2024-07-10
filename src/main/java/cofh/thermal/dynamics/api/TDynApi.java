package cofh.thermal.dynamics.api;

import cofh.thermal.dynamics.api.grid.IDuct;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import net.neoforged.neoforge.common.capabilities.Capability;
import net.neoforged.neoforge.common.capabilities.CapabilityManager;
import net.neoforged.neoforge.common.capabilities.CapabilityToken;
import net.neoforged.neoforge.common.capabilities.RegisterCapabilitiesEvent;

public class TDynApi {

    public static final Capability<IDuct> GRID_HOST_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});
    public static final Capability<IGridContainer> GRID_CONTAINER_CAPABILITY = CapabilityManager.get(new CapabilityToken<>() {});

    private TDynApi() {

    }

    public static void register(RegisterCapabilitiesEvent event) {

        event.register(IDuct.class);
        event.register(IGridContainer.class);
    }

}
