package cofh.thermal.dynamics.client;

import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;

public class TDynTextures {

    private TDynTextures() {

    }

    public static ResourceLocation ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/energy_limiter_attachment_active");
    public static ResourceLocation ENERGY_LIMITER_ATTACHMENT_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/energy_limiter_attachment");

    public static ResourceLocation FLUID_FILTER_ATTACHMENT_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/fluid_filter_attachment_active");
    public static ResourceLocation FLUID_FILTER_ATTACHMENT_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/fluid_filter_attachment");

}
