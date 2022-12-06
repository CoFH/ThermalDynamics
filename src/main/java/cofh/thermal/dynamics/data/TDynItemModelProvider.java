package cofh.thermal.dynamics.data;

import cofh.lib.data.ItemModelProviderCoFH;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class TDynItemModelProvider extends ItemModelProviderCoFH {

    public TDynItemModelProvider(DataGenerator generator, ExistingFileHelper existingFileHelper) {

        super(generator, ID_THERMAL, existingFileHelper);
    }

    @Override
    public String getName() {

        return "Thermal Dynamics: Item Models";
    }

    @Override
    protected void registerModels() {

        registerBlockItemModels();

        var reg = ITEMS;

        generated(reg.getSup(ID_ENERGY_LIMITER_ATTACHMENT));

        generated(reg.getSup(ID_FILTER_ATTACHMENT));
        generated(reg.getSup(ID_SERVO_ATTACHMENT));
    }

    private void registerBlockItemModels() {

        var reg = BLOCKS;

    }

}
