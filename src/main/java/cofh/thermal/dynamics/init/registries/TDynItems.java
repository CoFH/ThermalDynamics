package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.common.item.AttachmentItem;
import net.minecraft.world.item.Item;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;
import static cofh.thermal.lib.init.ThermalCreativeTabs.toolsTab;

public class TDynItems {

    private TDynItems() {

    }

    public static void register() {

        toolsTab(registerItem(ID_ENERGY_LIMITER_ATTACHMENT, () -> new AttachmentItem(new Item.Properties(), ENERGY_LIMITER).setModId(ID_THERMAL_DYNAMICS)));

        toolsTab(registerItem(ID_FILTER_ATTACHMENT, () -> new AttachmentItem(new Item.Properties(), FILTER).setModId(ID_THERMAL_DYNAMICS)));

        toolsTab(registerItem(ID_SERVO_ATTACHMENT, () -> new AttachmentItem(new Item.Properties(), SERVO).setModId(ID_THERMAL_DYNAMICS)));
        toolsTab(registerItem(ID_TURBO_SERVO_ATTACHMENT, () -> new AttachmentItem(new Item.Properties(), TURBO_SERVO).setModId(ID_THERMAL_DYNAMICS)));

        registerTools();
    }

    // region HELPERS
    private static void registerTools() {

        // CreativeModeTab group = THERMAL_TOOLS;

        // registerItem("ender_tuner", () -> new EnderTunerItem(new Item.Properties().stacksTo(1)).setModId(ID_THERMAL_DYNAMICS));
    }
    // endregion
}
