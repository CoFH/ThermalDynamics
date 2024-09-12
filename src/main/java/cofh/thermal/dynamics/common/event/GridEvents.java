package cofh.thermal.dynamics.common.event;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.common.grid.GridContainer;
import cofh.thermal.dynamics.common.grid.GridContainerCapProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AttachCapabilitiesEvent;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;

public class GridEvents {

    public static void register() {

        NeoForge.EVENT_BUS.addGenericListener(Level.class, GridEvents::attachCapabilities);
        NeoForge.EVENT_BUS.addListener(GridEvents::onWorldTick);
        NeoForge.EVENT_BUS.addListener(GridEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(GridEvents::onChunkUnload);
    }

    private static void attachCapabilities(AttachCapabilitiesEvent<Level> event) {

        event.addCapability(new ResourceLocation(ID_THERMAL_DYNAMICS, "grid_container"), new GridContainerCapProvider(new GridContainer(event.getObject())));
    }

    private static void onWorldTick(TickEvent.LevelTickEvent event) {

        if (event.side.isClient()) {
            return;
        }
        IGridContainer gridContainer = IGridContainer.getCapability(event.level);
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onWorldTick(event.phase);
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {

        IGridContainer gridContainer = IGridContainer.getCapability(event.getLevel());
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onChunkLoad(event.getChunk());
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {

        IGridContainer gridContainer = IGridContainer.getCapability(event.getLevel());
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onChunkUnload(event.getChunk());
        }
    }

}
