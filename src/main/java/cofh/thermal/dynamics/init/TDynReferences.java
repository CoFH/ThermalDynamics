package cofh.thermal.dynamics.init;

import cofh.thermal.dynamics.inventory.container.ItemBufferContainer;
import cofh.thermal.dynamics.tileentity.EnderTunnelTile;
import cofh.thermal.dynamics.tileentity.ItemBufferTile;
import net.minecraft.block.Block;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.registries.ObjectHolder;

import static cofh.lib.util.constants.Constants.ID_THERMAL;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ENDER_TUNNEL;
import static cofh.thermal.dynamics.init.TDynIDs.ID_ITEM_BUFFER;

@ObjectHolder(ID_THERMAL)
public class TDynReferences {

    private TDynReferences() {

    }

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
