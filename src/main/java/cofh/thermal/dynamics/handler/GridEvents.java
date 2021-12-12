package cofh.thermal.dynamics.handler;

import cofh.thermal.dynamics.api.grid.GridContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkEvent;

import static cofh.lib.util.constants.Constants.ID_THERMAL;

public class GridEvents {

    public static void register() {

        MinecraftForge.EVENT_BUS.addGenericListener(World.class, GridEvents::attachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(GridEvents::onWorldTick);
        MinecraftForge.EVENT_BUS.addListener(GridEvents::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(GridEvents::onChunkUnload);
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<World> event) {

        event.addCapability(new ResourceLocation(ID_THERMAL, "grid_container"), new GridContainerCapProvider(new GridContainerImpl(event.getObject())));
    }

    private static void onWorldTick(TickEvent.WorldTickEvent event) {

        GridContainer.getCapability(event.world).ifPresent(e -> ((GridContainerImpl) e).onWorldTick(event.phase));
    }

    private static void onChunkLoad(ChunkEvent.Load event) {

        GridContainer.getCapability(event.getWorld()).ifPresent(e -> ((GridContainerImpl) e).onChunkLoad(event.getChunk()));
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {

        GridContainer.getCapability(event.getWorld()).ifPresent(e -> ((GridContainerImpl) e).onChunkUnload(event.getChunk()));
    }

}
