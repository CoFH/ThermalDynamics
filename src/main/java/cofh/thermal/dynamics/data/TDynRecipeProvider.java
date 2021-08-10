package cofh.thermal.dynamics.data;

import cofh.lib.data.RecipeProviderCoFH;
import cofh.lib.util.DeferredRegisterCoFH;
import cofh.lib.util.references.ItemTagsCoFH;
import cofh.thermal.lib.common.ThermalFlags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Item;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ITEM_BUFFER;

public class TDynRecipeProvider extends RecipeProviderCoFH {

    public TDynRecipeProvider(DataGenerator generatorIn) {

        super(generatorIn, ID_THERMAL);
        manager = ThermalFlags.manager();
    }

    @Override
    public String getName() {

        return "Thermal Dynamics: Recipes";
    }

    @Override
    protected void registerRecipes(Consumer<IFinishedRecipe> consumer) {

        generateTileRecipes(consumer);
    }

    private void generateTileRecipes(Consumer<IFinishedRecipe> consumer) {

        DeferredRegisterCoFH<Item> reg = ITEMS;

        Item redstoneServo = reg.get("redstone_servo");

        ShapedRecipeBuilder.shapedRecipe(reg.get(ID_ITEM_BUFFER))
                .key('C', ItemTagsCoFH.INGOTS_SIGNALUM)
                .key('I', ItemTagsCoFH.INGOTS_TIN)
                .key('Q', Tags.Items.GEMS_QUARTZ)
                .key('R', reg.get("cured_rubber"))
                .patternLine("IQI")
                .patternLine("RCR")
                .patternLine("IQI")
                .addCriterion("has_quartz", hasItem(Tags.Items.GEMS_QUARTZ))
                .build(consumer);
    }

}
