package cofh.thermal.dynamics.init.data.providers;

import cofh.lib.init.data.LootTableProviderCoFH;
import cofh.thermal.dynamics.init.data.tables.TDynBlockLootTables;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;

public class TDynLootTableProvider extends LootTableProviderCoFH {

    public TDynLootTableProvider(PackOutput output) {

        super(output, List.of(
                new SubProviderEntry(TDynBlockLootTables::new, LootContextParamSets.BLOCK)
        ));
    }

}
