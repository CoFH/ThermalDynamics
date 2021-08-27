package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.dynamics.block.TileBlockEnderTunnel;
import cofh.thermal.dynamics.tileentity.EnderTunnelTile;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.ToolType;

import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.lib.util.constants.Constants.ID_THERMAL_LOCOMOTION;
import static cofh.thermal.core.ThermalCore.TILE_ENTITIES;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENDER_TUNNEL;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ITEM_BUFFER;
import static cofh.thermal.dynamics.init.TDynReferences.ENDER_TUNNEL_BLOCK;
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

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(2.0F).harvestTool(ToolType.PICKAXE), ItemBufferTile::new), ID_THERMAL_DYNAMICS);

        registerBlock(ID_ENDER_TUNNEL, () -> new TileBlockEnderTunnel(of(Material.METAL).sound(SoundType.NETHERITE_BLOCK).strength(10.0F).harvestTool(ToolType.PICKAXE), EnderTunnelTile::new), ID_THERMAL_DYNAMICS);
    }

    private static void registerTileEntities() {

        TILE_ENTITIES.register(ID_ITEM_BUFFER, () -> TileEntityType.Builder.of(ItemBufferTile::new, ITEM_BUFFER_BLOCK).build(null));

        TILE_ENTITIES.register(ID_ENDER_TUNNEL, () -> TileEntityType.Builder.of(EnderTunnelTile::new, ENDER_TUNNEL_BLOCK).build(null));
    }
    // endregion
}
