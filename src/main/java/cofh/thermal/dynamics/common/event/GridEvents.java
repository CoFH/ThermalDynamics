package cofh.thermal.dynamics.common.event;

import cofh.thermal.dynamics.api.grid.IGridContainer;
import cofh.thermal.dynamics.common.grid.GridContainer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

public class GridEvents {

    public static void register() {

        NeoForge.EVENT_BUS.addListener(GridEvents::onWorldTick);
        NeoForge.EVENT_BUS.addListener(GridEvents::onChunkLoad);
        NeoForge.EVENT_BUS.addListener(GridEvents::onChunkUnload);
    }

    private static void onWorldTick(TickEvent.LevelTickEvent event) {

        if (event.side.isClient()) {
            return;
        }
        IGridContainer gridContainer = IGridContainer.getGrid(event.level);
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onWorldTick(event.phase);
        }
    }

    private static void onChunkLoad(ChunkEvent.Load event) {

        IGridContainer gridContainer = IGridContainer.getGrid(event.getLevel());
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onChunkLoad(event.getChunk());
        }
    }

    private static void onChunkUnload(ChunkEvent.Unload event) {

        IGridContainer gridContainer = IGridContainer.getGrid(event.getLevel());
        if (gridContainer != null) {
            ((GridContainer) gridContainer).onChunkUnload(event.getChunk());
        }
    }

}
