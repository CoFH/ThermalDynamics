package cofh.thermal.dynamics.api;

import cofh.thermal.dynamics.api.grid.IDuct;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.capabilities.BlockCapability;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public class TDynApi {

    public static final BlockCapability<IDuct, Void> GRID_HOST_CAPABILITY = BlockCapability.createVoid(new ResourceLocation(ID_THERMAL_DYNAMICS, "grid_host"), IDuct.class);

    private TDynApi() {

    }

}
