package cofh.thermal.dynamics.init.registries;

import cofh.core.common.block.EntityBlock6Way;
import cofh.thermal.core.common.config.ThermalCoreConfig;
import cofh.thermal.dynamics.common.block.DuctBlock;
import cofh.thermal.dynamics.common.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.common.item.DuctBlockItem;
import net.minecraft.world.level.block.SoundType;

import java.util.function.IntSupplier;

import static cofh.lib.util.Utils.itemProperties;
import static cofh.lib.util.constants.ModIds.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.init.registries.ThermalCreativeTabs.devicesTab;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.registries.TDynBlockEntities.*;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;
import static net.minecraft.world.level.block.state.BlockBehaviour.Properties.of;

public class TDynBlocks {

    private TDynBlocks() {

    }

    public static void register() {

        registerTileBlocks();
    }

    // region HELPERS
    private static void registerTileBlocks() {

        devicesTab(50, registerBlock(ID_ENERGY_DUCT,
                () -> new DuctBlock(of().sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), ENERGY_DUCT_BLOCK_ENTITY),
                () -> new DuctBlockItem(BLOCKS.get(ID_ENERGY_DUCT), itemProperties()).setModId(ID_THERMAL_DYNAMICS)));
        devicesTab(50, registerBlock(ID_FLUID_DUCT,
                () -> new DuctBlock(of().sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), FLUID_DUCT_BLOCK_ENTITY),
                () -> new DuctBlockItem(BLOCKS.get(ID_FLUID_DUCT), itemProperties()).setModId(ID_THERMAL_DYNAMICS)));
        devicesTab(50, registerBlock(ID_FLUID_DUCT_WINDOWED,
                () -> new DuctBlock(of().sound(SoundType.LANTERN).strength(1.0F).dynamicShape().noOcclusion(), FLUID_DUCT_WINDOWED_BLOCK_ENTITY),
                () -> new DuctBlockItem(BLOCKS.get(ID_FLUID_DUCT_WINDOWED), itemProperties()).setModId(ID_THERMAL_DYNAMICS)));


        IntSupplier storageAugs = () -> ThermalCoreConfig.storageAugments;

        // registerAugmentableBlock(ID_ENERGY_DISTRIBUTOR, () -> new TileBlockActive6Way(of().sound(SoundType.LANTERN).strength(2.0F).harvestTool(ToolType.PICKAXE).noOcclusion(), EnergyDistributorTile::new), storageAugs, ENERGY_STORAGE_VALIDATOR, ID_THERMAL_DYNAMICS);

        devicesTab(registerBlock(ID_ITEM_BUFFER, () -> new EntityBlock6Way(of().sound(SoundType.NETHERITE_BLOCK).strength(2.0F), ItemBufferBlockEntity.class, ITEM_BUFFER_BLOCK_ENTITY), ID_THERMAL_DYNAMICS));
    }
    // endregion
}
