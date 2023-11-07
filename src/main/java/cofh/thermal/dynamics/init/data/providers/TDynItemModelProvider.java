package cofh.thermal.dynamics.init.data.providers;

import cofh.lib.init.data.ItemModelProviderCoFH;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynItemModelProvider extends ItemModelProviderCoFH {

    public TDynItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {

        super(output, ID_THERMAL, existingFileHelper);
    }

    @Override
    protected void registerModels() {

        registerBlockItemModels();

        var reg = ITEMS;

        generated(reg.getSup(ID_ENERGY_LIMITER_ATTACHMENT));

        generated(reg.getSup(ID_FILTER_ATTACHMENT));

        generated(reg.getSup(ID_SERVO_ATTACHMENT));
        generated(reg.getSup(ID_TURBO_SERVO_ATTACHMENT));
    }

    private void registerBlockItemModels() {

        var reg = BLOCKS;

    }

}
