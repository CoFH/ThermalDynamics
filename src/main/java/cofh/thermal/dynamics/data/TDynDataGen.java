//package cofh.thermal.dynamics.data;
//
//import net.minecraft.data.DataGenerator;
//import net.minecraft.data.PackOutput;
//import net.minecraftforge.common.data.ExistingFileHelper;
//import net.minecraftforge.data.event.GatherDataEvent;
//import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.Mod;
//
//import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
//
//@Mod.EventBusSubscriber (bus = Mod.EventBusSubscriber.Bus.MOD, modid = ID_THERMAL_DYNAMICS)
//public class TDynDataGen {
//
//    @SubscribeEvent
//    public static void gatherData(final GatherDataEvent event) {
//
//        DataGenerator gen = event.getGenerator();
//        PackOutput pOutput = gen.getPackOutput();
//        ExistingFileHelper exFileHelper = event.getExistingFileHelper();
//
//        TDynTagsProvider.Block blockTags = new TDynTagsProvider.Block(pOutput, event.getLookupProvider(), exFileHelper);
//
//        gen.addProvider(event.includeServer(), blockTags);
//        gen.addProvider(event.includeServer(), new TDynTagsProvider.Item(pOutput, event.getLookupProvider(), blockTags.contentsGetter(), exFileHelper));
//
//        gen.addProvider(event.includeServer(), new TDynLootTableProvider(gen));
//        gen.addProvider(event.includeServer(), new TDynRecipeProvider(gen));
//
//        gen.addProvider(event.includeClient(), new TDynItemModelProvider(gen, exFileHelper));
//    }
//
//}
