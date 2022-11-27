package cofh.thermal.dynamics.handler;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.ChunkEvent;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public class GridEvents {

    public static void register() {

        MinecraftForge.EVENT_BUS.addGenericListener(Level.class, GridEvents::attachCapabilities);
        MinecraftForge.EVENT_BUS.addListener(GridEvents::onWorldTick);
        MinecraftForge.EVENT_BUS.addListener(GridEvents::onChunkLoad);
        MinecraftForge.EVENT_BUS.addListener(GridEvents::onChunkUnload);
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<Level> event) {

        event.addCapability(new ResourceLocation(ID_THERMAL_DYNAMICS, "grid_container"), new GridContainerCapProvider(new GridContainer(event.getObject())));
    }

    private static void onWorldTick(TickEvent.WorldTickEvent event) {

        IGridContainer gridContainer = IGridContainer.getCapability(event.world);
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onWorldTick(event.phase);
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {

        IGridContainer gridContainer = IGridContainer.getCapability(event.getWorld());
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onChunkLoad(event.getChunk());
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {

        IGridContainer gridContainer = IGridContainer.getCapability(event.getWorld());
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onChunkUnload(event.getChunk());
        }
    }

}
