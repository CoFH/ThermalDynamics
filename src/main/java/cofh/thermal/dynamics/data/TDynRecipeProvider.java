package cofh.thermal.dynamics.data;

import cofh.lib.data.RecipeProviderCoFH;
import cofh.lib.util.DeferredRegisterCoFH;
import cofh.lib.util.references.ItemTagsCoFH;
import cofh.thermal.lib.common.ThermalFlags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.IFinishedRecipe;
import net.minecraft.data.ShapedRecipeBuilder;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENDER_TUNNEL;
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
    protected void buildShapelessRecipes(Consumer<IFinishedRecipe> consumer) {

        generateTileRecipes(consumer);
    }

    private void generateTileRecipes(Consumer<IFinishedRecipe> consumer) {

        DeferredRegisterCoFH<Item> reg = ITEMS;

        Item redstoneServo = reg.get("redstone_servo");

        ShapedRecipeBuilder.shaped(reg.get(ID_ITEM_BUFFER))
                .define('C', ItemTagsCoFH.INGOTS_SIGNALUM)
                .define('I', ItemTagsCoFH.INGOTS_TIN)
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('R', reg.get("cured_rubber"))
                .pattern("IQI")
                .pattern("RCR")
                .pattern("IQI")
                .unlockedBy("has_quartz", has(Tags.Items.GEMS_QUARTZ))
                .save(consumer);

        ShapedRecipeBuilder.shaped(reg.get(ID_ENDER_TUNNEL))
                .define('E', Items.ENDER_EYE)
                .define('I', ItemTagsCoFH.INGOTS_ENDERIUM)
                .pattern(" I ")
                .pattern("IEI")
                .pattern(" I ")
                .unlockedBy("has_ender_eye", has(Items.ENDER_EYE))
                .save(consumer);
    }

}
