package cofh.thermal.dynamics.data.providers;

import cofh.lib.data.LootTableProviderCoFH;
import cofh.thermal.dynamics.data.tables.TDynBlockLootTables;
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
