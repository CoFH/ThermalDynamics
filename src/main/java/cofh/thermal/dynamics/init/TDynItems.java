package cofh.thermal.dynamics.init;

import net.minecraft.world.item.CreativeModeTab;

import static cofh.thermal.lib.common.ThermalItemGroups.THERMAL_TOOLS;

public class TDynItems {

    private TDynItems() {

    }

    public static void register() {

        registerTools();
    }

    // region HELPERS
    private static void registerTools() {

        CreativeModeTab group = THERMAL_TOOLS;

        // registerItem("ender_tuner", () -> new EnderTunerItem(new Item.Properties().stacksTo(1).tab(group)).setModId(ID_THERMAL_DYNAMICS));
    }
    // endregion
}
