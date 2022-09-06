package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.dynamics.block.TileBlockDuct;
import cofh.thermal.dynamics.block.entity.EnergyDuctTile;
import cofh.thermal.dynamics.block.entity.FluidDuctGlassTile;
import cofh.thermal.dynamics.block.entity.FluidDuctTile;
import cofh.thermal.dynamics.block.entity.ItemBufferTile;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Material;

import java.util.function.IntSupplier;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.TILE_ENTITIES;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.TDynIDs.*;
import static cofh.thermal.dynamics.init.TDynReferences.*;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.of;

public class TDynBlocks {

    private TDynBlocks() {

    }

    public static void register() {

        registerTileBlocks();
        registerTileEntities();
    }

    // region HELPERS
    private static void registerTileBlocks() {


        registerBlock(ID_ENERGY_DUCT, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), EnergyDuctTile::new));
        registerBlock(ID_FLUID_DUCT, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), FluidDuctTile::new));
        registerBlock(ID_FLUID_DUCT_GLASS, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), FluidDuctGlassTile::new));


        IntSupplier storageAugs = () -> ThermalCoreConfig.storageAugments;

        // registerAugmentableBlock(ID_ENERGY_DISTRIBUTOR, () -> new TileBlockActive6Way(of(Material.METAL).sound(SoundType.LANTERN).strength(2.0F).harvestTool(ToolType.PICKAXE).noOcclusion(), EnergyDistributorTile::new), storageAugs, ENERGY_STORAGE_VALIDATOR, ID_THERMAL_DYNAMICS);

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(2.0F), ItemBufferTile::new), ID_THERMAL_DYNAMICS);
    }

    private static void registerTileEntities() {

        TILE_ENTITIES.register(ID_ENERGY_DUCT, () -> BlockEntityType.Builder.of(EnergyDuctTile::new, ENERGY_DUCT_BLOCK).build(null));
        TILE_ENTITIES.register(ID_FLUID_DUCT, () -> BlockEntityType.Builder.of(FluidDuctTile::new, FLUID_DUCT_BLOCK).build(null));
        TILE_ENTITIES.register(ID_FLUID_DUCT_GLASS, () -> BlockEntityType.Builder.of(FluidDuctGlassTile::new, FLUID_DUCT_GLASS_BLOCK).build(null));

        //        TILE_ENTITIES.register(ID_ENERGY_DISTRIBUTOR, () -> TileEntityType.Builder.of(EnergyDistributorTile::new, ENERGY_DISTRIBUTOR_BLOCK).build(null));

        TILE_ENTITIES.register(ID_ITEM_BUFFER, () -> BlockEntityType.Builder.of(ItemBufferTile::new, ITEM_BUFFER_BLOCK).build(null));
    }
    // endregion
}
