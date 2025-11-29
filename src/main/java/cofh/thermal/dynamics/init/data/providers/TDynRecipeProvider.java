package cofh.thermal.dynamics.init.data.providers;

import cofh.lib.init.data.RecipeProviderCoFH;
import cofh.lib.init.tags.ItemTagsCoFH;
import cofh.thermal.lib.util.ThermalFlags;
import cofh.thermal.lib.util.references.ThermalTags;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;

import java.util.function.Consumer;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.ITEMS;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;
import static net.minecraft.data.recipes.RecipeCategory.BUILDING_BLOCKS;
import static net.minecraft.data.recipes.RecipeCategory.MISC;

public class TDynRecipeProvider extends RecipeProviderCoFH {

    public TDynRecipeProvider(PackOutput output) {

        super(output, ID_THERMAL);
        manager = ThermalFlags.manager();
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {

        generateTileRecipes(consumer);
    }

    private void generateTileRecipes(Consumer<FinishedRecipe> consumer) {

        var reg = ITEMS;

        Item redstoneServo = reg.get("redstone_servo");

        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_BUFFER))
                .define('C', ItemTagsCoFH.INGOTS_SIGNALUM)
                .define('I', ItemTagsCoFH.INGOTS_TIN)
                .define('Q', Tags.Items.GEMS_QUARTZ)
                .define('P', ItemTags.PLANKS)
                .pattern("IQI")
                .pattern("PCP")
                .pattern("IQI")
                .unlockedBy("has_quartz", has(Tags.Items.GEMS_QUARTZ))
                .save(consumer);

        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ENERGY_DUCT), 4)
                .define('G', Tags.Items.GLASS)
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .pattern("RRR")
                .pattern("LGL")
                .pattern("RRR")
                .unlockedBy("has_lead", has(ItemTagsCoFH.INGOTS_LEAD))
                .save(consumer, ID_THERMAL + ":energy_duct_4");

        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_FLUID_DUCT), 4)
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .define('C', ItemTagsCoFH.INGOTS_BRONZE)
                .pattern("CLC")
                .unlockedBy("has_bronze", has(ItemTagsCoFH.INGOTS_BRONZE))
                .save(consumer, ID_THERMAL + ":fluid_duct_4");

        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_FLUID_DUCT_WINDOWED), 4)
                .define('G', ThermalTags.Items.HARDENED_GLASS)
                .define('C', ItemTagsCoFH.INGOTS_BRONZE)
                .pattern("CGC")
                .unlockedBy("has_bronze", has(ItemTagsCoFH.INGOTS_BRONZE))
                .save(consumer, ID_THERMAL + ":fluid_duct_windowed_4");

        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ENERGY_LIMITER_ATTACHMENT), 2)
                .define('G', Tags.Items.GLASS)
                .define('I', ItemTagsCoFH.INGOTS_ELECTRUM)
                .define('N', ItemTagsCoFH.NUGGETS_LEAD)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .pattern("NGN")
                .pattern("IRI")
                .unlockedBy("has_duct", has(ThermalTags.Items.DUCTS))
                .save(consumer, ID_THERMAL + ":energy_limiter_attachment_2");

        ShapedRecipeBuilder.shaped(MISC, reg.get(ID_FILTER_ATTACHMENT), 2)
                .define('G', Tags.Items.GLASS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', ItemTagsCoFH.NUGGETS_TIN)
                .define('P', Items.PAPER)
                .pattern("NGN")
                .pattern("IPI")
                .unlockedBy("has_duct", has(ThermalTags.Items.DUCTS))
                .save(consumer, ID_THERMAL + ":filter_attachment_2");

        ShapedRecipeBuilder.shaped(MISC, reg.get(ID_SERVO_ATTACHMENT), 2)
                .define('G', Tags.Items.GLASS)
                .define('I', Tags.Items.INGOTS_IRON)
                .define('N', ItemTagsCoFH.NUGGETS_TIN)
                .define('R', Tags.Items.DUSTS_REDSTONE)
                .pattern("NGN")
                .pattern("IRI")
                .unlockedBy("has_duct", has(ThermalTags.Items.DUCTS))
                .save(consumer, ID_THERMAL + ":servo_attachment_2");

        ShapedRecipeBuilder.shaped(MISC, reg.get(ID_TURBO_SERVO_ATTACHMENT), 2)
                .define('G', Tags.Items.GLASS)
                .define('I', ItemTagsCoFH.INGOTS_INVAR)
                .define('N', ItemTagsCoFH.NUGGETS_LEAD)
                .define('R', redstoneServo)
                .pattern("NGN")
                .pattern("IRI")
                .unlockedBy("has_duct", has(ThermalTags.Items.DUCTS))
                .save(consumer, ID_THERMAL + ":turbo_servo_attachment_2");

        // region ITEM DUCT RECIPES
        // Base Item Duct (windowed)
        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_DUCT), 4)
                .define('G', ThermalTags.Items.HARDENED_GLASS)
                .define('T', ItemTagsCoFH.INGOTS_TIN)
                .pattern("TGT")
                .unlockedBy("has_tin", has(ItemTagsCoFH.INGOTS_TIN))
                .save(consumer, ID_THERMAL + ":item_duct_4");

        // Item Duct Opaque (from base item duct)
        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_DUCT_OPAQUE), 4)
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .define('T', ItemTagsCoFH.INGOTS_TIN)
                .pattern("TLT")
                .unlockedBy("has_tin", has(ItemTagsCoFH.INGOTS_TIN))
                .save(consumer, ID_THERMAL + ":item_duct_opaque_4");

        // Dense Item Duct (from base)
        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_DUCT_DENSE), 4)
                .define('D', reg.get(ID_ITEM_DUCT))
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .pattern(" L ")
                .pattern("DDD")
                .pattern(" L ")
                .unlockedBy("has_item_duct", has(reg.get(ID_ITEM_DUCT)))
                .save(consumer, ID_THERMAL + ":item_duct_dense_4");

        // Dense Item Duct Opaque
        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_DUCT_DENSE_OPAQUE), 4)
                .define('D', reg.get(ID_ITEM_DUCT_OPAQUE))
                .define('L', ItemTagsCoFH.INGOTS_LEAD)
                .pattern(" L ")
                .pattern("DDD")
                .pattern(" L ")
                .unlockedBy("has_item_duct", has(reg.get(ID_ITEM_DUCT_OPAQUE)))
                .save(consumer, ID_THERMAL + ":item_duct_dense_opaque_4");

        // Vacuum Item Duct (from base)
        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_DUCT_VACUUM), 4)
                .define('D', reg.get(ID_ITEM_DUCT))
                .define('E', ItemTagsCoFH.INGOTS_ELECTRUM)
                .pattern(" E ")
                .pattern("DDD")
                .pattern(" E ")
                .unlockedBy("has_item_duct", has(reg.get(ID_ITEM_DUCT)))
                .save(consumer, ID_THERMAL + ":item_duct_vacuum_4");

        // Vacuum Item Duct Opaque
        ShapedRecipeBuilder.shaped(BUILDING_BLOCKS, reg.get(ID_ITEM_DUCT_VACUUM_OPAQUE), 4)
                .define('D', reg.get(ID_ITEM_DUCT_OPAQUE))
                .define('E', ItemTagsCoFH.INGOTS_ELECTRUM)
                .pattern(" E ")
                .pattern("DDD")
                .pattern(" E ")
                .unlockedBy("has_item_duct", has(reg.get(ID_ITEM_DUCT_OPAQUE)))
                .save(consumer, ID_THERMAL + ":item_duct_vacuum_opaque_4");
        // endregion
    }

}
