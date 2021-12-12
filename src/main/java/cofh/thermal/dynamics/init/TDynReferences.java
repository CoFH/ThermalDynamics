package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.api.grid.GridType;
import cofh.thermal.dynamics.api.grid.energy.EnergyGrid;
import cofh.thermal.dynamics.api.grid.item.ItemGrid;
import cofh.thermal.dynamics.api.grid.multi.MultiGrid;
import cofh.thermal.dynamics.inventory.container.ItemBufferContainer;
import cofh.thermal.dynamics.tileentity.DuctTileEnergy;
import cofh.thermal.dynamics.tileentity.EnderTunnelTile;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.dynamics.init.TDynIDs.*;

@ObjectHolder(ID_THERMAL)
public class TDynReferences {

    private TDynReferences() {

    }

    @ObjectHolder(ID_GRID_ENERGY)
    public static final GridType<EnergyGrid> ENERGY_GRID = null;
//    @ObjectHolder(ID_GRID_FLUID)
//    public static final GridType<FluidGrid> FLUID_GRID = null;
    @ObjectHolder(ID_GRID_ITEM)
    public static final GridType<ItemGrid> ITEM_GRID = null;
    @ObjectHolder(ID_GRID_MULTI)
    public static final GridType<MultiGrid> MULTI_GRID = null;

    @ObjectHolder(ID_DUCT_ENERGY)
    public static final Block ENERGY_DUCT_BLOCK = null;
    @ObjectHolder(ID_DUCT_ENERGY)
    public static final TileEntityType<DuctTileEnergy> ENERGY_DUCT_TILE = null;

    @ObjectHolder(ID_ENDER_TUNNEL)
    public static final Block ENDER_TUNNEL_BLOCK = null;
    @ObjectHolder(ID_ENDER_TUNNEL)
    public static final TileEntityType<EnderTunnelTile> ENDER_TUNNEL_TILE = null;

    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final Block DEVICE_FLUID_BUFFER_BLOCK = null;
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final TileEntityType<DeviceFluidBufferTile> DEVICE_FLUID_BUFFER_TILE = null;
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final ContainerType<DeviceFluidBufferContainer> DEVICE_FLUID_BUFFER_CONTAINER = null;

    @ObjectHolder(ID_ITEM_BUFFER)
    public static final Block ITEM_BUFFER_BLOCK = null;
    @ObjectHolder(ID_ITEM_BUFFER)
    public static final TileEntityType<ItemBufferTile> ITEM_BUFFER_TILE = null;
    @ObjectHolder(ID_ITEM_BUFFER)
    public static final ContainerType<ItemBufferContainer> ITEM_BUFFER_CONTAINER = null;

}
