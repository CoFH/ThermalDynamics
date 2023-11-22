package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.common.item.AttachmentItem;
import net.minecraft.world.item.Item;

import static cofh.lib.util.Utils.itemProperties;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.init.registries.ThermalCreativeTabs.toolsTab;
import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynItems {

    private TDynItems() {

    }

    public static void register() {

        toolsTab(registerItem(ID_ENERGY_LIMITER_ATTACHMENT, () -> new AttachmentItem(itemProperties(), ENERGY_LIMITER).setModId(ID_THERMAL_DYNAMICS)));

        toolsTab(registerItem(ID_FILTER_ATTACHMENT, () -> new AttachmentItem(itemProperties(), FILTER).setModId(ID_THERMAL_DYNAMICS)));

        toolsTab(registerItem(ID_SERVO_ATTACHMENT, () -> new AttachmentItem(itemProperties(), SERVO).setModId(ID_THERMAL_DYNAMICS)));
        toolsTab(registerItem(ID_TURBO_SERVO_ATTACHMENT, () -> new AttachmentItem(itemProperties(), TURBO_SERVO).setModId(ID_THERMAL_DYNAMICS)));

        registerTools();
    }

    // region HELPERS
    private static void registerTools() {

        // CreativeModeTab group = THERMAL_TOOLS;

        // registerItem("ender_tuner", () -> new EnderTunerItem(itemProperties().stacksTo(1)).setModId(ID_THERMAL_DYNAMICS));
    }
    // endregion
}
