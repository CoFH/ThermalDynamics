package cofh.thermal.dynamics.data;

import cofh.lib.data.LootTableProviderCoFH;
import net.minecraft.data.DataGenerator;

import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENERGY_DUCT;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ITEM_BUFFER;

public class TDynLootTableProvider extends LootTableProviderCoFH {

    public TDynLootTableProvider(DataGenerator gen) {

        super(gen);
    }

    @Override
    public String getName() {

        return "Thermal Dynamics: Loot Tables";
    }

    @Override
    protected void addTables() {

        var regBlocks = BLOCKS;
        var regItems = ITEMS;

        createSimpleDropTable(regBlocks.get(ID_ENERGY_DUCT));

        // createSyncDropTable(regBlocks.get(ID_ENERGY_DISTRIBUTOR));

        createSyncDropTable(regBlocks.get(ID_ITEM_BUFFER));
    }

}
