package cofh.thermal.dynamics.data.providers;

import cofh.thermal.lib.util.references.ThermalTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class TDynTagsProvider {

    public static class Block extends BlockTagsProvider {

        public Block(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {

            super(output, lookupProvider, ID_THERMAL, existingFileHelper);
        }

        @SuppressWarnings ("unchecked")
        @Override
        protected void addTags(HolderLookup.Provider pProvider) {

            // region TILE BLOCKS
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BLOCKS.get(ID_ENERGY_DUCT));
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BLOCKS.get(ID_FLUID_DUCT));
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BLOCKS.get(ID_FLUID_DUCT_WINDOWED));

            tag(BlockTags.MINEABLE_WITH_AXE).add(BLOCKS.get(ID_ITEM_BUFFER));

            tag(ThermalTags.Blocks.DUCTS).add(BLOCKS.get(ID_ENERGY_DUCT));
            tag(ThermalTags.Blocks.DUCTS).add(BLOCKS.get(ID_FLUID_DUCT));
            tag(ThermalTags.Blocks.DUCTS).add(BLOCKS.get(ID_FLUID_DUCT_WINDOWED));
            // endregion
        }

    }

    public static class Item extends ItemTagsProvider {

        public Item(PackOutput pOutput, CompletableFuture<HolderLookup.Provider> pLookupProvider, CompletableFuture<TagsProvider.TagLookup<net.minecraft.world.level.block.Block>> pBlockTags, ExistingFileHelper existingFileHelper) {

            super(pOutput, pLookupProvider, pBlockTags, ID_THERMAL, existingFileHelper);
        }

        @SuppressWarnings ("unchecked")
        @Override
        protected void addTags(HolderLookup.Provider pProvider) {

            copy(ThermalTags.Blocks.DUCTS, ThermalTags.Items.DUCTS);
        }

    }

}

