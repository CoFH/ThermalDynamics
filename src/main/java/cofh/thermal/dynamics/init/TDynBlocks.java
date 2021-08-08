package cofh.thermal.dynamics.init;

import cofh.core.block.TileBlock6Way;
import cofh.thermal.dynamics.block.TileBlockEnderTunnel;
import cofh.thermal.dynamics.tileentity.EnderTunnelTile;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.ToolType;

import static cofh.thermal.core.ThermalCore.TILE_ENTITIES;
import static cofh.thermal.core.util.RegistrationHelper.registerBlock;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENDER_TUNNEL;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ITEM_BUFFER;
import static cofh.thermal.dynamics.init.TDynReferences.ENDER_TUNNEL_BLOCK;
import static cofh.thermal.dynamics.init.TDynReferences.ITEM_BUFFER_BLOCK;
import static net.minecraft.block.AbstractBlock.Properties.create;

public class TDynBlocks {

    private TDynBlocks() {

    }

    public static void register() {

        registerTileBlocks();
        registerTileEntities();
    }

    // region HELPERS
    private static void registerTileBlocks() {

        registerBlock(ID_ITEM_BUFFER, () -> new TileBlock6Way(create(Material.IRON).sound(SoundType.NETHERITE).hardnessAndResistance(2.0F).harvestTool(ToolType.PICKAXE), ItemBufferTile::new));

        registerBlock(ID_ENDER_TUNNEL, () -> new TileBlockEnderTunnel(create(Material.IRON).sound(SoundType.NETHERITE).hardnessAndResistance(10.0F).harvestTool(ToolType.PICKAXE), EnderTunnelTile::new));
    }

    private static void registerTileEntities() {

        TILE_ENTITIES.register(ID_ITEM_BUFFER, () -> TileEntityType.Builder.create(ItemBufferTile::new, ITEM_BUFFER_BLOCK).build(null));

        TILE_ENTITIES.register(ID_ENDER_TUNNEL, () -> TileEntityType.Builder.create(EnderTunnelTile::new, ENDER_TUNNEL_BLOCK).build(null));
    }
    // endregion
}
