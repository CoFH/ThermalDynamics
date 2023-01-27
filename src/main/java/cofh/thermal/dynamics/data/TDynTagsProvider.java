package cofh.thermal.dynamics.data;

import cofh.thermal.lib.util.references.ThermalTags;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.tags.BlockTagsProvider;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.data.ExistingFileHelper;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class TDynTagsProvider {

    public static class Block extends BlockTagsProvider {

        public Block(DataGenerator gen, ExistingFileHelper existingFileHelper) {

            super(gen, ID_THERMAL, existingFileHelper);
        }

        @Override
        public String getName() {

            return "Thermal Expansion: Block Tags";
        }

        @Override
        protected void addTags() {

            // region TILE BLOCKS
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BLOCKS.get(ID_ENERGY_DUCT));
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BLOCKS.get(ID_FLUID_DUCT));
            tag(BlockTags.MINEABLE_WITH_PICKAXE).add(BLOCKS.get(ID_FLUID_DUCT_GLASS));

            tag(BlockTags.MINEABLE_WITH_AXE).add(BLOCKS.get(ID_ITEM_BUFFER));

            tag(ThermalTags.Blocks.DUCTS).add(BLOCKS.get(ID_ENERGY_DUCT));
            tag(ThermalTags.Blocks.DUCTS).add(BLOCKS.get(ID_FLUID_DUCT));
            tag(ThermalTags.Blocks.DUCTS).add(BLOCKS.get(ID_FLUID_DUCT_GLASS));
            // endregion
        }

    }

    public static class Item extends ItemTagsProvider {

        public Item(DataGenerator gen, BlockTagsProvider blockTagProvider, ExistingFileHelper existingFileHelper) {

            super(gen, blockTagProvider, ID_THERMAL, existingFileHelper);
        }

        @Override
        public String getName() {

            return "Thermal Expansion: Item Tags";
        }

        @Override
        protected void addTags() {

            copy(ThermalTags.Blocks.DUCTS, ThermalTags.Items.DUCTS);
        }

    }

}

