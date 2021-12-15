package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.dynamics.block.TileBlockDuctEnergy;
import cofh.thermal.dynamics.tileentity.DuctTileEnergy;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.ToolType;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.thermal.core.ThermalCore.TILE_ENTITIES;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.TDynIDs.ID_DUCT_ENERGY;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ITEM_BUFFER;
import static cofh.thermal.dynamics.init.TDynReferences.ENERGY_DUCT_BLOCK;
import static cofh.thermal.dynamics.init.TDynReferences.ITEM_BUFFER_BLOCK;
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

        registerBlock(ID_DUCT_ENERGY, () -> new TileBlockDuctEnergy(of(Material.METAL).noCollission().instabreak())); // TODO proper properties.

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(2.0F).harvestTool(ToolType.PICKAXE), ItemBufferTile::new), ID_THERMAL_DYNAMICS);
    }

    private static void registerTileEntities() {

        TILE_ENTITIES.register(ID_DUCT_ENERGY, () -> TileEntityType.Builder.of(DuctTileEnergy::new, ENERGY_DUCT_BLOCK).build(null));

        TILE_ENTITIES.register(ID_ITEM_BUFFER, () -> TileEntityType.Builder.of(ItemBufferTile::new, ITEM_BUFFER_BLOCK).build(null));
    }
    // endregion
}
