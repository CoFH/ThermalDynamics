package cofh.thermal.dynamics;

import cofh.thermal.dynamics.client.gui.logistics.LogisticsItemBufferScreen;
import cofh.thermal.dynamics.init.TDynBlocks;
import cofh.thermal.dynamics.init.TDynContainers;
import cofh.thermal.dynamics.init.TDynItems;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.init.TCoreIDs.ID_DEVICE_COLLECTOR;
import static cofh.thermal.core.init.TCoreIDs.ID_DEVICE_NULLIFIER;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENDER_TUNNEL;
import static cofh.thermal.dynamics.init.TDynReferences.LOGISTICS_ITEM_BUFFER_CONTAINER;
import static cofh.thermal.lib.common.ThermalFlags.FLAG_XP_STORAGE_AUGMENT;
import static cofh.thermal.lib.common.ThermalFlags.setFlag;

@Mod(ID_THERMAL_DYNAMICS)
public class ThermalDynamics {

    public ThermalDynamics() {

        setFeatureFlags();

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        TDynBlocks.register();
        TDynItems.register();

        TDynContainers.register();
    }

    private void setFeatureFlags() {

        setFlag(ID_DEVICE_COLLECTOR, true);
        setFlag(ID_DEVICE_NULLIFIER, true);

        setFlag(FLAG_XP_STORAGE_AUGMENT, true);
    }

    // region INITIALIZATION
    private void commonSetup(final FMLCommonSetupEvent event) {

    }

    private void clientSetup(final FMLClientSetupEvent event) {

        this.registerGuiFactories();
        this.registerRenderLayers();
    }
    // endregion

    // region HELPERS
    private void registerGuiFactories() {

        ScreenManager.registerFactory(LOGISTICS_ITEM_BUFFER_CONTAINER, LogisticsItemBufferScreen::new);
    }

    private void registerRenderLayers() {

        RenderType cutout = RenderType.getCutout();
        RenderType translucent = RenderType.getTranslucent();

        RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_ENDER_TUNNEL), cutout);
        // RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_ENDER_TUNNEL), translucent);

        //        RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_DEVICE_FLUID_BUFFER), cutout);
        //        RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_DEVICE_ITEM_BUFFER), cutout);
    }
    // endregion
}
