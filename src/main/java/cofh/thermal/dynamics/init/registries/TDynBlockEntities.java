package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.common.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.EnergyDuctBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctWindowedBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.BLOCK_ENTITIES;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynBlockEntities {

    private TDynBlockEntities() {

    }

    public static void register() {

    }

    public static final RegistryObject<BlockEntityType<?>> ENERGY_DUCT_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_ENERGY_DUCT, () -> BlockEntityType.Builder.of(EnergyDuctBlockEntity::new, BLOCKS.get(ID_ENERGY_DUCT)).build(null));
    public static final RegistryObject<BlockEntityType<?>> FLUID_DUCT_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_FLUID_DUCT, () -> BlockEntityType.Builder.of(FluidDuctBlockEntity::new, BLOCKS.get(ID_FLUID_DUCT)).build(null));
    public static final RegistryObject<BlockEntityType<?>> FLUID_DUCT_WINDOWED_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_FLUID_DUCT_WINDOWED, () -> BlockEntityType.Builder.of(FluidDuctWindowedBlockEntity::new, BLOCKS.get(ID_FLUID_DUCT_WINDOWED)).build(null));

    //        TILE_ENTITIES.register(ID_ENERGY_DISTRIBUTOR, () -> TileEntityType.Builder.of(EnergyDistributorTile::new, ENERGY_DISTRIBUTOR_BLOCK).build(null));

    public static final RegistryObject<BlockEntityType<?>> ITEM_BUFFER_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_ITEM_BUFFER, () -> BlockEntityType.Builder.of(ItemBufferBlockEntity::new, BLOCKS.get(ID_ITEM_BUFFER)).build(null));

}
