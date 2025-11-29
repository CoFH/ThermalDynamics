package cofh.thermal.dynamics.init.registries;

import cofh.thermal.dynamics.common.block.entity.ItemBufferBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.EnergyDuctBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.FluidDuctWindowedBlockEntity;
import cofh.thermal.dynamics.common.block.entity.duct.ItemDuctBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

import static cofh.thermal.core.ThermalCore.BLOCKS;
import static cofh.thermal.core.ThermalCore.BLOCK_ENTITIES;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynBlockEntities {

    private TDynBlockEntities() {

    }

    public static void register() {

    }

    // Helper method to create item duct block entity types
    private static RegistryObject<BlockEntityType<?>> registerItemDuct(String id, boolean windowed, Supplier<Block> blockSupplier) {
        // Use array holder to capture self-reference
        final RegistryObject<BlockEntityType<?>>[] holder = new RegistryObject[1];
        holder[0] = BLOCK_ENTITIES.register(id, () -> BlockEntityType.Builder.of(
                (BlockPos pos, BlockState state) -> new ItemDuctBlockEntity(holder[0].get(), pos, state, windowed),
                blockSupplier.get()
        ).build(null));
        return holder[0];
    }

    public static final RegistryObject<BlockEntityType<?>> ENERGY_DUCT_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_ENERGY_DUCT, () -> BlockEntityType.Builder.of(EnergyDuctBlockEntity::new, BLOCKS.get(ID_ENERGY_DUCT)).build(null));
    public static final RegistryObject<BlockEntityType<?>> FLUID_DUCT_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_FLUID_DUCT, () -> BlockEntityType.Builder.of(FluidDuctBlockEntity::new, BLOCKS.get(ID_FLUID_DUCT)).build(null));
    public static final RegistryObject<BlockEntityType<?>> FLUID_DUCT_WINDOWED_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_FLUID_DUCT_WINDOWED, () -> BlockEntityType.Builder.of(FluidDuctWindowedBlockEntity::new, BLOCKS.get(ID_FLUID_DUCT_WINDOWED)).build(null));

    // Item Duct Base Variants
    public static final RegistryObject<BlockEntityType<?>> ITEM_DUCT_BLOCK_ENTITY = registerItemDuct(ID_ITEM_DUCT, true, () -> BLOCKS.get(ID_ITEM_DUCT));
    public static final RegistryObject<BlockEntityType<?>> ITEM_DUCT_OPAQUE_BLOCK_ENTITY = registerItemDuct(ID_ITEM_DUCT_OPAQUE, false, () -> BLOCKS.get(ID_ITEM_DUCT_OPAQUE));

    // Item Duct Dense Variants
    public static final RegistryObject<BlockEntityType<?>> ITEM_DUCT_DENSE_BLOCK_ENTITY = registerItemDuct(ID_ITEM_DUCT_DENSE, true, () -> BLOCKS.get(ID_ITEM_DUCT_DENSE));
    public static final RegistryObject<BlockEntityType<?>> ITEM_DUCT_DENSE_OPAQUE_BLOCK_ENTITY = registerItemDuct(ID_ITEM_DUCT_DENSE_OPAQUE, false, () -> BLOCKS.get(ID_ITEM_DUCT_DENSE_OPAQUE));

    // Item Duct Vacuum Variants
    public static final RegistryObject<BlockEntityType<?>> ITEM_DUCT_VACUUM_BLOCK_ENTITY = registerItemDuct(ID_ITEM_DUCT_VACUUM, true, () -> BLOCKS.get(ID_ITEM_DUCT_VACUUM));
    public static final RegistryObject<BlockEntityType<?>> ITEM_DUCT_VACUUM_OPAQUE_BLOCK_ENTITY = registerItemDuct(ID_ITEM_DUCT_VACUUM_OPAQUE, false, () -> BLOCKS.get(ID_ITEM_DUCT_VACUUM_OPAQUE));

    //        TILE_ENTITIES.register(ID_ENERGY_DISTRIBUTOR, () -> TileEntityType.Builder.of(EnergyDistributorTile::new, ENERGY_DISTRIBUTOR_BLOCK).build(null));

    public static final RegistryObject<BlockEntityType<?>> ITEM_BUFFER_BLOCK_ENTITY = BLOCK_ENTITIES.register(ID_ITEM_BUFFER, () -> BlockEntityType.Builder.of(ItemBufferBlockEntity::new, BLOCKS.get(ID_ITEM_BUFFER)).build(null));

}
