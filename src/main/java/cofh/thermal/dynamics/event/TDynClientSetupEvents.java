package cofh.thermal.dynamics.event;

import cofh.thermal.dynamics.client.model.DuctModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL_DYNAMICS, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TDynClientSetupEvents {

    @SubscribeEvent
    public static void registerModels(final ModelRegistryEvent event) {

        ModelLoaderRegistry.registerLoader(new ResourceLocation(ID_THERMAL_DYNAMICS, "duct"), new DuctModel.Loader());
    }

    @SubscribeEvent
    public static void postStitch(TextureStitchEvent.Post event) {

        DuctModel.clearCaches();
    }

}
