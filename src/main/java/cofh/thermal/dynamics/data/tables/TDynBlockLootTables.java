package cofh.thermal.dynamics.data.tables;

import cofh.lib.data.loot.BlockLootSubProviderCoFH;

import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class TDynBlockLootTables extends BlockLootSubProviderCoFH {

    @Override
    protected void generate() {

        var regBlocks = BLOCKS;
        var regItems = ITEMS;

        createSimpleDropTable(regBlocks.get(ID_ENERGY_DUCT));
        createSimpleDropTable(regBlocks.get(ID_FLUID_DUCT));
        createSimpleDropTable(regBlocks.get(ID_FLUID_DUCT_WINDOWED));

        // createSyncDropTable(regBlocks.get(ID_ENERGY_DISTRIBUTOR));

        createSyncDropTable(regBlocks.get(ID_ITEM_BUFFER));
    }

}
