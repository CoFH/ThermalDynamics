package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.inventory.container.logistics.LogisticsItemBufferContainer;
import cofh.thermal.dynamics.tileentity.ender.EnderTunnelTile;
import cofh.thermal.dynamics.tileentity.logistics.LogisticsItemBufferTile;
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

    // region DUCTS
    @ObjectHolder(ID_DUCT_ENERGY)
    public static final Block DUCT_ENERGY_BLOCK = null;
    // endregion

    // region ENDER
    @ObjectHolder(ID_ENDER_TUNNEL)
    public static final Block ENDER_TUNNEL_BLOCK = null;
    @ObjectHolder(ID_ENDER_TUNNEL)
    public static final TileEntityType<EnderTunnelTile> ENDER_TUNNEL_TILE = null;
    // endregion

    // region LOGISTICS
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final Block DEVICE_FLUID_BUFFER_BLOCK = null;
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final TileEntityType<DeviceFluidBufferTile> DEVICE_FLUID_BUFFER_TILE = null;
    //    @ObjectHolder(ID_DEVICE_FLUID_BUFFER)
    //    public static final ContainerType<DeviceFluidBufferContainer> DEVICE_FLUID_BUFFER_CONTAINER = null;

    @ObjectHolder(ID_LOGISTICS_ITEM_BUFFER)
    public static final Block LOGISTICS_ITEM_BUFFER_BLOCK = null;
    @ObjectHolder(ID_LOGISTICS_ITEM_BUFFER)
    public static final TileEntityType<LogisticsItemBufferTile> LOGISTICS_ITEM_BUFFER_TILE = null;
    @ObjectHolder(ID_LOGISTICS_ITEM_BUFFER)
    public static final ContainerType<LogisticsItemBufferContainer> LOGISTICS_ITEM_BUFFER_CONTAINER = null;
    // endregion
}
