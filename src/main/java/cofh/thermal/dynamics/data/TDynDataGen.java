package cofh.thermal.dynamics.data;

import cofh.thermal.dynamics.data.providers.TDynItemModelProvider;
import cofh.thermal.dynamics.data.providers.TDynLootTableProvider;
import cofh.thermal.dynamics.data.providers.TDynRecipeProvider;
import cofh.thermal.dynamics.data.providers.TDynTagsProvider;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

@Mod.EventBusSubscriber (bus = Mod.EventBusSubscriber.Bus.MOD, modid = ID_THERMAL_DYNAMICS)
public class TDynDataGen {

    @SubscribeEvent
    public static void gatherData(final GatherDataEvent event) {

        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper exFileHelper = event.getExistingFileHelper();

        TDynTagsProvider.Block blockTags = new TDynTagsProvider.Block(output, event.getLookupProvider(), exFileHelper);
        gen.addProvider(event.includeServer(), blockTags);
        gen.addProvider(event.includeServer(), new TDynTagsProvider.Item(output, event.getLookupProvider(), blockTags.contentsGetter(), exFileHelper));

        gen.addProvider(event.includeServer(), new TDynLootTableProvider(output));
        gen.addProvider(event.includeServer(), new TDynRecipeProvider(output));

        gen.addProvider(event.includeClient(), new TDynItemModelProvider(output, exFileHelper));
    }

}
