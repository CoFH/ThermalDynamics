package cofh.thermal.dynamics.data;

import cofh.lib.data.ItemModelProviderCoFH;
import net.minecraft.data.DataGenerator;
import net.minecraftforge.common.data.ExistingFileHelper;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.ITEMS;

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
    }

    private void registerBlockItemModels() {

        var reg = BLOCKS;

    }

}
