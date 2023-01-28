package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.dynamics.block.DuctBlock;
import cofh.thermal.dynamics.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.item.DuctBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;

import java.util.function.IntSupplier;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.core.util.RegistrationHelper.registerBlockAndItem;
import static cofh.thermal.dynamics.init.TDynBlockEntities.*;
import static cofh.thermal.dynamics.init.TDynIDs.*;
import static cofh.thermal.lib.common.ThermalItemGroups.THERMAL_DEVICES;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.of;

public class TDynBlocks {

    private TDynBlocks() {

    }

    public static void register() {

        registerTileBlocks();
    }

    // region HELPERS
    private static void registerTileBlocks() {

        registerBlockAndItem(ID_ENERGY_DUCT,
                () -> new DuctBlock(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), ENERGY_DUCT_BLOCK_ENTITY),
                () -> new DuctBlockItem(BLOCKS.get(ID_ENERGY_DUCT), new Item.Properties().tab(THERMAL_DEVICES)).setModId(ID_THERMAL_DYNAMICS));
        registerBlockAndItem(ID_FLUID_DUCT,
                () -> new DuctBlock(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), FLUID_DUCT_BLOCK_ENTITY),
                () -> new DuctBlockItem(BLOCKS.get(ID_FLUID_DUCT), new Item.Properties().tab(THERMAL_DEVICES)).setModId(ID_THERMAL_DYNAMICS));
        registerBlockAndItem(ID_FLUID_DUCT_WINDOWED,
                () -> new DuctBlock(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), FLUID_DUCT_GLASS_BLOCK_ENTITY),
                () -> new DuctBlockItem(BLOCKS.get(ID_FLUID_DUCT_WINDOWED), new Item.Properties().tab(THERMAL_DEVICES)).setModId(ID_THERMAL_DYNAMICS));


        IntSupplier storageAugs = () -> ThermalCoreConfig.storageAugments;

        // registerAugmentableBlock(ID_ENERGY_DISTRIBUTOR, () -> new TileBlockActive6Way(of(Material.METAL).sound(SoundType.LANTERN).strength(2.0F).harvestTool(ToolType.PICKAXE).noOcclusion(), EnergyDistributorTile::new), storageAugs, ENERGY_STORAGE_VALIDATOR, ID_THERMAL_DYNAMICS);

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(2.0F), ItemBufferBlockEntity.class, ITEM_BUFFER_BLOCK_ENTITY), THERMAL_DEVICES, ID_THERMAL_DYNAMICS);
    }
    // endregion
}
