package cofh.thermal.dynamics.event;

import cofh.thermal.dynamics.client.model.DuctModel;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent.RegisterGeometryLoaders;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.dynamics.client.TDynTextures.*;

@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL_DYNAMICS, bus = Mod.EventBusSubscriber.Bus.MOD)
public class TDynClientSetupEvents {

    private static final String BLOCK_ATLAS = "minecraft:textures/atlas/blocks.png";

    private TDynClientSetupEvents() {

    }

    @SubscribeEvent
    public static void registerModels(final RegisterGeometryLoaders event) {

        event.register("duct", new DuctModel.Loader());
    }

    @SubscribeEvent
    public static void preStitch(TextureStitchEvent.Pre event) {

        if (!event.getAtlas().location().toString().equals(BLOCK_ATLAS)) {
            return;
        }
        event.addSprite(ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC);
        event.addSprite(ENERGY_LIMITER_ATTACHMENT_LOC);

        event.addSprite(FILTER_ATTACHMENT_ACTIVE_LOC);
        event.addSprite(FILTER_ATTACHMENT_LOC);

        event.addSprite(FILTER_ATTACHMENT_TO_EXTERNAL_ACTIVE_LOC);
        event.addSprite(FILTER_ATTACHMENT_TO_EXTERNAL_LOC);

        event.addSprite(FILTER_ATTACHMENT_TO_GRID_ACTIVE_LOC);
        event.addSprite(FILTER_ATTACHMENT_TO_GRID_LOC);

        event.addSprite(SERVO_ATTACHMENT_ACTIVE_LOC);
        event.addSprite(SERVO_ATTACHMENT_LOC);

        event.addSprite(TURBO_SERVO_ATTACHMENT_ACTIVE_LOC);
        event.addSprite(TURBO_SERVO_ATTACHMENT_LOC);
    }

    @SubscribeEvent
    public static void postStitch(TextureStitchEvent.Post event) {

        DuctModel.clearCaches();
    }

}
