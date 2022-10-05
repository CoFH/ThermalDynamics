package cofh.thermal.dynamics.data;

import cofh.lib.data.RecipeProviderCoFH;
import cofh.lib.tags.ItemTagsCoFH;
import cofh.thermal.lib.common.ThermalFlags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENERGY_DUCT;
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
    protected void buildCraftingRecipes(Consumer<FinishedRecipe> consumer) {

        generateTileRecipes(consumer);
    }

    private void generateTileRecipes(Consumer<FinishedRecipe> consumer) {

        var reg = ITEMS;

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

        ShapedRecipeBuilder.shaped(reg.get(ID_ENERGY_DUCT), 6)
                .define('G', Tags.Items.GLASS)
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .pattern("RRR")
                .pattern("LGL")
                .pattern("RRR")
                .unlockedBy("has_lead", has(ItemTagsCoFH.INGOTS_LEAD))
                .save(consumer);

        //        ShapedRecipeBuilder.shaped(reg.get(ID_FLUID_DUCT), 4)
        //                .define('L', ItemTagsCoFH.INGOTS_LEAD)
        //                .define('C', ItemTagsCoFH.INGOTS_BRONZE)
        //                .pattern("CLC")
        //                .unlockedBy("has_copper", has(ItemTagsCoFH.INGOTS_BRONZE))
        //                .save(consumer);
        //
        //        ShapedRecipeBuilder.shaped(reg.get(ID_FLUID_DUCT_GLASS), 4)
        //                .define('G', ItemTagsCoFH.HARDENED_GLASS)
        //                .define('C', ItemTagsCoFH.INGOTS_BRONZE)
        //                .pattern("CGC")
        //                .unlockedBy("has_copper", has(ItemTagsCoFH.INGOTS_BRONZE))
        //                .save(consumer);
    }

}
