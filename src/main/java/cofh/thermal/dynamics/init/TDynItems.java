package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.item.EnderTunerItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;

import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.lib.common.ThermalItemGroups.THERMAL_TOOLS;

public class TDynItems {

    private TDynItems() {

    }

    public static void register() {

        registerTools();
    }

    // region HELPERS
    private static void registerTools() {

        ItemGroup group = THERMAL_TOOLS;

        registerItem("ender_tuner", () -> new EnderTunerItem(new Item.Properties().maxStackSize(1).group(group)));
    }
    // endregion
}
