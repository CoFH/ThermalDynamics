package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.core.config.ThermalCoreConfig;
import cofh.thermal.dynamics.block.TileBlockDuct;
import cofh.thermal.dynamics.block.entity.ItemBufferTile;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;

import java.util.function.IntSupplier;

import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.TDynIDs.*;
import static cofh.thermal.dynamics.init.TDynTileEntities.*;
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


        registerBlock(ID_ENERGY_DUCT, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), DUCT_ENERGY_TILE), THERMAL_DEVICES, ID_THERMAL_DYNAMICS);
        registerBlock(ID_FLUID_DUCT, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), DUCT_FLUID_TILE), THERMAL_DEVICES, ID_THERMAL_DYNAMICS);
        registerBlock(ID_FLUID_DUCT_GLASS, () -> new TileBlockDuct(of(Material.METAL).sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), DUCT_FLUID_GLASS_TILE), THERMAL_DEVICES, ID_THERMAL_DYNAMICS);


        IntSupplier storageAugs = () -> ThermalCoreConfig.storageAugments;

        // registerAugmentableBlock(ID_ENERGY_DISTRIBUTOR, () -> new TileBlockActive6Way(of(Material.METAL).sound(SoundType.LANTERN).strength(2.0F).harvestTool(ToolType.PICKAXE).noOcclusion(), EnergyDistributorTile::new), storageAugs, ENERGY_STORAGE_VALIDATOR, ID_THERMAL_DYNAMICS);

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(2.0F), ItemBufferTile.class, ITEM_BUFFER_TILE), THERMAL_DEVICES, ID_THERMAL_DYNAMICS);
    }
    // endregion
}
