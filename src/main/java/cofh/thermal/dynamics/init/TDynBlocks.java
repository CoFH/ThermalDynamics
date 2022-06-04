package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.dynamics.block.TileBlockDuct;
import cofh.thermal.dynamics.tileentity.EnergyDuctTile;
import cofh.thermal.dynamics.tileentity.FluidDuctGlassTile;
import cofh.thermal.dynamics.tileentity.FluidDuctTile;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import cofh.thermal.lib.common.ThermalConfig;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.ToolType;

import java.util.function.IntSupplier;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.TILE_ENTITIES;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.TDynIDs.*;
import static cofh.thermal.dynamics.init.TDynReferences.*;
import static net.minecraft.block.AbstractBlock.Properties.of;

public class TDynBlocks {

    private TDynBlocks() {

    }

    public static void register() {

        registerTileBlocks();
        registerTileEntities();
    }

    // region HELPERS
    private static void registerTileBlocks() {


        registerBlock(ID_ENERGY_DUCT, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).harvestTool(ToolType.PICKAXE).dynamicShape().noOcclusion(), EnergyDuctTile::new));
        registerBlock(ID_FLUID_DUCT, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).harvestTool(ToolType.PICKAXE).dynamicShape().noOcclusion(), FluidDuctTile::new));
        registerBlock(ID_FLUID_DUCT_GLASS, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).harvestTool(ToolType.PICKAXE).dynamicShape().noOcclusion(), FluidDuctGlassTile::new));


        IntSupplier storageAugs = () -> ThermalConfig.storageAugments;

        // registerAugmentableBlock(ID_ENERGY_DISTRIBUTOR, () -> new TileBlockActive6Way(of(Material.METAL).sound(SoundType.LANTERN).strength(2.0F).harvestTool(ToolType.PICKAXE).noOcclusion(), EnergyDistributorTile::new), storageAugs, ENERGY_STORAGE_VALIDATOR, ID_THERMAL_DYNAMICS);

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(2.0F).harvestTool(ToolType.PICKAXE), ItemBufferTile::new), ID_THERMAL_DYNAMICS);
    }

    private static void registerTileEntities() {

        TILE_ENTITIES.register(ID_ENERGY_DUCT, () -> TileEntityType.Builder.of(EnergyDuctTile::new, ENERGY_DUCT_BLOCK).build(null));
        TILE_ENTITIES.register(ID_FLUID_DUCT, () -> TileEntityType.Builder.of(FluidDuctTile::new, FLUID_DUCT_BLOCK).build(null));
        TILE_ENTITIES.register(ID_FLUID_DUCT_GLASS, () -> TileEntityType.Builder.of(FluidDuctGlassTile::new, FLUID_DUCT_GLASS_BLOCK).build(null));

        //        TILE_ENTITIES.register(ID_ENERGY_DISTRIBUTOR, () -> TileEntityType.Builder.of(EnergyDistributorTile::new, ENERGY_DISTRIBUTOR_BLOCK).build(null));

        TILE_ENTITIES.register(ID_ITEM_BUFFER, () -> TileEntityType.Builder.of(ItemBufferTile::new, ITEM_BUFFER_BLOCK).build(null));
    }
    // endregion
}
