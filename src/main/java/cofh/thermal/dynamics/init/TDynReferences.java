package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.api.grid.IGridType;
import cofh.thermal.dynamics.api.grid.energy.IEnergyGrid;
import cofh.thermal.dynamics.api.grid.item.IItemGrid;
import cofh.thermal.dynamics.api.grid.multi.IMultiGrid;
import cofh.thermal.dynamics.inventory.container.EnergyDistributorContainer;
import cofh.thermal.dynamics.inventory.container.ItemBufferContainer;
import cofh.thermal.dynamics.tileentity.DuctTileEnergy;
import cofh.thermal.dynamics.tileentity.EnergyDistributorTile;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.lib.util.constants.Constants.ID_THERMAL_DYNAMICS;
import static cofh.thermal.dynamics.init.TDynIDs.*;

@ObjectHolder (ID_THERMAL)
public class TDynReferences {

    private TDynReferences() {

    }

    @ObjectHolder (ID_THERMAL_DYNAMICS + ":" + ID_GRID_ENERGY)
    public static final IGridType<IEnergyGrid> ENERGY_GRID = null;
    //    @ObjectHolder(ID_THERMAL_DYNAMICS + ":" + ID_GRID_FLUID)
    //    public static final GridType<FluidGrid> FLUID_GRID = null;
    @ObjectHolder (ID_THERMAL_DYNAMICS + ":" + ID_GRID_ITEM)
    public static final IGridType<IItemGrid> ITEM_GRID = null;
    @ObjectHolder (ID_THERMAL_DYNAMICS + ":" + ID_GRID_MULTI)
    public static final IGridType<IMultiGrid> MULTI_GRID = null;

    @ObjectHolder (ID_DUCT_ENERGY)
    public static final Block ENERGY_DUCT_BLOCK = null;
    @ObjectHolder (ID_DUCT_ENERGY)
    public static final TileEntityType<DuctTileEnergy> ENERGY_DUCT_TILE = null;

    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final Block DEVICE_FLUID_BUFFER_BLOCK = null;
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final TileEntityType<DeviceFluidBufferTile> DEVICE_FLUID_BUFFER_TILE = null;
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final ContainerType<DeviceFluidBufferContainer> DEVICE_FLUID_BUFFER_CONTAINER = null;

    @ObjectHolder (ID_ENERGY_DISTRIBUTOR)
    public static final Block ENERGY_DISTRIBUTOR_BLOCK = null;
    @ObjectHolder (ID_ENERGY_DISTRIBUTOR)
    public static final TileEntityType<EnergyDistributorTile> ENERGY_DISTRIBUTOR_TILE = null;
    @ObjectHolder (ID_ENERGY_DISTRIBUTOR)
    public static final ContainerType<EnergyDistributorContainer> ENERGY_DISTRIBUTOR_CONTAINER = null;

    @ObjectHolder (ID_ITEM_BUFFER)
    public static final Block ITEM_BUFFER_BLOCK = null;
    @ObjectHolder (ID_ITEM_BUFFER)
    public static final TileEntityType<ItemBufferTile> ITEM_BUFFER_TILE = null;
    @ObjectHolder (ID_ITEM_BUFFER)
    public static final ContainerType<ItemBufferContainer> ITEM_BUFFER_CONTAINER = null;

}
