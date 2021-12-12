package cofh.thermal.dynamics.data;

import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.GatherDataEvent;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;

@Mod.EventBusSubscriber (bus = Mod.EventBusSubscriber.Bus.MOD, modid = ID_THERMAL_DYNAMICS)
public class TDynDataGen {

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {

        if (event.includeServer()) {
            registerServerProviders(event);
        }
        if (event.includeClient()) {
            registerClientProviders(event);
        }
    }

    private static void registerServerProviders(GatherDataEvent event) {

        DataGenerator gen = event.getGenerator();
        ExistingFileHelper exFileHelper = event.getExistingFileHelper();

        gen.addProvider(new TDynLootTableProvider(gen));
        gen.addProvider(new TDynRecipeProvider(gen));
    }

    private static void registerClientProviders(GatherDataEvent event) {

        DataGenerator gen = event.getGenerator();
        ExistingFileHelper exFileHelper = event.getExistingFileHelper();

        gen.addProvider(new TDynItemModelProvider(gen, exFileHelper));
    }

}
