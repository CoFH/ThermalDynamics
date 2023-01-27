package cofh.thermal.dynamics;

import cofh.lib.network.PacketHandler;
import cofh.lib.util.DeferredRegisterCoFH;
import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.client.DebugRenderer;
import cofh.thermal.dynamics.client.gui.ItemBufferScreen;
import cofh.thermal.dynamics.client.gui.attachment.EnergyLimiterAttachmentScreen;
import cofh.thermal.dynamics.client.gui.attachment.FluidFilterAttachmentScreen;
import cofh.thermal.dynamics.client.gui.attachment.FluidServoAttachmentScreen;
import cofh.thermal.dynamics.handler.GridEvents;
import cofh.thermal.dynamics.init.*;
import cofh.thermal.dynamics.network.packet.client.AttachmentControlPacket;
import cofh.thermal.dynamics.network.packet.client.GridDebugPacket;
import cofh.thermal.dynamics.network.packet.server.AttachmentConfigPacket;
import cofh.thermal.dynamics.network.packet.server.AttachmentRedstoneControlPacket;
import net.covers1624.quack.util.SneakyUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.IForgeRegistry;
import net.minecraftforge.registries.RegistryBuilder;

import java.util.function.Supplier;

import static cofh.core.network.packet.PacketIDs.*;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.dynamics.init.TDynContainers.*;
import static cofh.thermal.dynamics.init.TDynIDs.*;
import static cofh.thermal.dynamics.util.TDynConstants.PACKET_GRID_DEBUG;
import static cofh.thermal.lib.common.ThermalFlags.FLAG_XP_STORAGE_AUGMENT;
import static cofh.thermal.lib.common.ThermalFlags.setFlag;
import static cofh.thermal.lib.common.ThermalIDs.ID_DEVICE_COLLECTOR;
import static cofh.thermal.lib.common.ThermalIDs.ID_DEVICE_NULLIFIER;

@Mod (ID_THERMAL_DYNAMICS)
public class ThermalDynamics {

    public static final PacketHandler PACKET_HANDLER = new PacketHandler(new ResourceLocation(ID_THERMAL_DYNAMICS, "general"));

    public static final DeferredRegisterCoFH<IGridType<?>> GRIDS =
            DeferredRegisterCoFH.create(new ResourceLocation(ID_THERMAL_DYNAMICS, ID_GRID_TYPE), ID_THERMAL_DYNAMICS);

    public static final Supplier<IForgeRegistry<IGridType<?>>> GRID_TYPE_REGISTRY =
            GRIDS.makeRegistry(SneakyUtils.unsafeCast(IGridType.class), () -> new RegistryBuilder<IGridType<?>>()
                    .disableOverrides() // GridTypes can't be overriden.
                    .disableSaving()    // GridTypes don't need id's saved to disk.
            );

    public ThermalDynamics() {

        setFeatureFlags();

        registerPackets();

        final IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::capSetup);

        GRIDS.register(modEventBus);

        TDynBlocks.register();
        TDynItems.register();
        TDynGrids.register();

        TDynContainers.register();
        TDynTileEntities.register();

        GridEvents.register();
    }

    private void registerPackets() {

        PACKET_HANDLER.registerPacket(PACKET_CONTROL, AttachmentControlPacket::new);
        PACKET_HANDLER.registerPacket(PACKET_CONFIG, AttachmentConfigPacket::new);
        PACKET_HANDLER.registerPacket(PACKET_REDSTONE_CONTROL, AttachmentRedstoneControlPacket::new);

        PACKET_HANDLER.registerPacket(PACKET_GRID_DEBUG, GridDebugPacket::new);
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

        event.enqueueWork(this::registerGuiFactories);
        event.enqueueWork(this::registerRenderLayers);
        DebugRenderer.register();
    }

    private void capSetup(RegisterCapabilitiesEvent event) {

        event.register(IGridContainer.class);
    }
    // endregion

    // region HELPERS
    private void registerGuiFactories() {

        // ScreenManager.register(ENERGY_DISTRIBUTOR_CONTAINER, EnergyDistributorScreen::new);
        MenuScreens.register(ITEM_BUFFER_CONTAINER.get(), ItemBufferScreen::new);

        MenuScreens.register(ENERGY_LIMITER_ATTACHMENT_CONTAINER.get(), EnergyLimiterAttachmentScreen::new);
        MenuScreens.register(FLUID_FILTER_ATTACHMENT_CONTAINER.get(), FluidFilterAttachmentScreen::new);
        MenuScreens.register(FLUID_SERVO_ATTACHMENT_CONTAINER.get(), FluidServoAttachmentScreen::new);
    }

    private void registerRenderLayers() {

        RenderType cutout = RenderType.cutout();

        // RenderTypeLookup.setRenderLayer(ENERGY_DISTRIBUTOR_BLOCK, cutout);

        // RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_ENDER_TUNNEL), translucent);

        // RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_DEVICE_FLUID_BUFFER), cutout);
        // RenderTypeLookup.setRenderLayer(BLOCKS.get(ID_DEVICE_ITEM_BUFFER), cutout);

        ItemBlockRenderTypes.setRenderLayer(BLOCKS.get(ID_ENERGY_DUCT), cutout);
        ItemBlockRenderTypes.setRenderLayer(BLOCKS.get(ID_FLUID_DUCT), cutout);
        ItemBlockRenderTypes.setRenderLayer(BLOCKS.get(ID_FLUID_DUCT_GLASS), cutout);
    }
    // endregion
}
