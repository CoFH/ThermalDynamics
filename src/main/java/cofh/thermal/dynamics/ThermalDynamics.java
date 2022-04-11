package cofh.thermal.dynamics;

import cofh.lib.capability.NullCapabilityStorage;
import cofh.lib.network.PacketHandler;
import cofh.lib.util.DeferredRegisterCoFH;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.client.DebugRenderer;
import cofh.thermal.dynamics.client.gui.EnergyDistributorScreen;
import cofh.thermal.dynamics.client.gui.ItemBufferScreen;
import cofh.thermal.dynamics.handler.GridEvents;
import cofh.thermal.dynamics.init.TDynBlocks;
import cofh.thermal.dynamics.init.TDynContainers;
import cofh.thermal.dynamics.init.TDynGrids;
import cofh.thermal.dynamics.init.TDynItems;
import cofh.thermal.dynamics.network.client.GridDebugPacket;
import net.covers1624.quack.util.SneakyUtils;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_GRID_TYPE;
import static cofh.thermal.dynamics.init.TDynReferences.*;
import static cofh.thermal.dynamics.util.TDynConstants.PACKET_GRID_DEBUG;
import static cofh.thermal.lib.common.ThermalFlags.FLAG_XP_STORAGE_AUGMENT;
import static cofh.thermal.lib.common.ThermalFlags.setFlag;
import static cofh.thermal.lib.common.ThermalIDs.ID_DEVICE_COLLECTOR;
import static cofh.thermal.lib.common.ThermalIDs.ID_DEVICE_NULLIFIER;
import static net.covers1624.quack.util.SneakyUtils.nullC;

@Mod (ID_THERMAL_DYNAMICS)
public class ThermalDynamics {

    public static final PacketHandler PACKET_HANDLER = new PacketHandler(new ResourceLocation(ID_THERMAL_DYNAMICS, "general"));

    public static final DeferredRegisterCoFH<IGridType<?>> GRIDS =
            DeferredRegisterCoFH.create(SneakyUtils.<Class<IGridType<?>>>unsafeCast(IGridType.class), ID_THERMAL_DYNAMICS);

    public static final Supplier<IForgeRegistry<IGridType<?>>> GRID_TYPE_REGISTRY =
            GRIDS.makeRegistry(ID_GRID_TYPE, () -> new RegistryBuilder<IGridType<?>>()
                    .disableOverrides() // GridTypes can't be overriden.
                    .disableSaving()    // GridTypes don't need id's saved to disk.
            );

    public ThermalDynamics() {

        setFeatureFlags();

        registerPackets();

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        GRIDS.register(modEventBus);

        TDynBlocks.register();
        TDynItems.register();
        TDynGrids.register();

        TDynContainers.register();

        GridEvents.register();
    }

    private void registerPackets() {

        PACKET_HANDLER.registerPacket(PACKET_GRID_DEBUG, GridDebugPacket::new);
    }

    private void setFeatureFlags() {

        setFlag(ID_DEVICE_COLLECTOR, true);
        setFlag(ID_DEVICE_NULLIFIER, true);

        setFlag(FLAG_XP_STORAGE_AUGMENT, true);
    }

    // region INITIALIZATION
    private void commonSetup(final FMLCommonSetupEvent event) {

        CapabilityManager.INSTANCE.register(IGridContainer.class, NullCapabilityStorage.instance(), nullC());
    }

    private void clientSetup(final FMLClientSetupEvent event) {

        this.registerGuiFactories();
        this.registerRenderLayers();
        DebugRenderer.register();
    }
    // endregion

    // region HELPERS
    private void registerGuiFactories() {

        ScreenManager.register(ENERGY_DISTRIBUTOR_CONTAINER, EnergyDistributorScreen::new);
        ScreenManager.register(ITEM_BUFFER_CONTAINER, ItemBufferScreen::new);
    }

    private void registerRenderLayers() {

        RenderType cutout = RenderType.cutout();
        RenderType cutoutMipped = RenderType.cutoutMipped();
        RenderType translucent = RenderType.translucent();

        RenderTypeLookup.setRenderLayer(ENERGY_DISTRIBUTOR_BLOCK, cutout);

        // RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_ENDER_TUNNEL), translucent);

        //        RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_DEVICE_FLUID_BUFFER), cutout);
        //        RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_DEVICE_ITEM_BUFFER), cutout);

        RenderTypeLookup.setRenderLayer(ENERGY_DUCT_BLOCK, cutoutMipped);
    }
    // endregion
}
