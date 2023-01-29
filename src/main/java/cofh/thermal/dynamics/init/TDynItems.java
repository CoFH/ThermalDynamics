package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.item.AttachmentItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.util.RegistrationHelper.registerItem;
import static cofh.thermal.dynamics.init.TDynIDs.*;
import static cofh.thermal.lib.common.ThermalItemGroups.THERMAL_ITEMS;
import static cofh.thermal.lib.common.ThermalItemGroups.THERMAL_TOOLS;

public class TDynItems {

    private TDynItems() {

    }

    public static void register() {

        CreativeModeTab group = THERMAL_ITEMS;

        registerItem(ID_ENERGY_LIMITER_ATTACHMENT, () -> new AttachmentItem(new Item.Properties().tab(group), ENERGY_LIMITER).setModId(ID_THERMAL_DYNAMICS));

        registerItem(ID_FILTER_ATTACHMENT, () -> new AttachmentItem(new Item.Properties().tab(group), FILTER).setModId(ID_THERMAL_DYNAMICS));
        registerItem(ID_INPUT_FILTER_ATTACHMENT, () -> new AttachmentItem(new Item.Properties().tab(group), INPUT_FILTER).setModId(ID_THERMAL_DYNAMICS));
        registerItem(ID_OUTPUT_FILTER_ATTACHMENT, () -> new AttachmentItem(new Item.Properties().tab(group), OUTPUT_FILTER).setModId(ID_THERMAL_DYNAMICS));

        registerItem(ID_SERVO_ATTACHMENT, () -> new AttachmentItem(new Item.Properties().tab(group), SERVO).setModId(ID_THERMAL_DYNAMICS));

        registerTools();
    }

    // region HELPERS
    private static void registerTools() {

        CreativeModeTab group = THERMAL_TOOLS;

        // registerItem("ender_tuner", () -> new EnderTunerItem(new Item.Properties().stacksTo(1).tab(group)).setModId(ID_THERMAL_DYNAMICS));
    }
    // endregion
}
