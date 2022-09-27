package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.block.entity.EnergyDuctTile;
import cofh.thermal.dynamics.block.entity.FluidDuctGlassTile;
import cofh.thermal.dynamics.block.entity.FluidDuctTile;
import cofh.thermal.dynamics.block.entity.ItemBufferTile;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.TILE_ENTITIES;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class TDynTileEntities {

    private TDynTileEntities() {

    }

    public static void register() {

    }

    public static final RegistryObject<BlockEntityType<?>> DUCT_ENERGY_TILE = TILE_ENTITIES.register(ID_ENERGY_DUCT, () -> BlockEntityType.Builder.of(EnergyDuctTile::new, BLOCKS.get(ID_ENERGY_DUCT)).build(null));
    public static final RegistryObject<BlockEntityType<?>> DUCT_FLUID_TILE = TILE_ENTITIES.register(ID_FLUID_DUCT, () -> BlockEntityType.Builder.of(FluidDuctTile::new, BLOCKS.get(ID_FLUID_DUCT)).build(null));
    public static final RegistryObject<BlockEntityType<?>> DUCT_FLUID_GLASS_TILE = TILE_ENTITIES.register(ID_FLUID_DUCT_GLASS, () -> BlockEntityType.Builder.of(FluidDuctGlassTile::new, BLOCKS.get(ID_FLUID_DUCT_GLASS)).build(null));

    //        TILE_ENTITIES.register(ID_ENERGY_DISTRIBUTOR, () -> TileEntityType.Builder.of(EnergyDistributorTile::new, ENERGY_DISTRIBUTOR_BLOCK).build(null));

    public static final RegistryObject<BlockEntityType<?>> ITEM_BUFFER_TILE = TILE_ENTITIES.register(ID_ITEM_BUFFER, () -> BlockEntityType.Builder.of(ItemBufferTile::new, BLOCKS.get(ID_ITEM_BUFFER)).build(null));

}
