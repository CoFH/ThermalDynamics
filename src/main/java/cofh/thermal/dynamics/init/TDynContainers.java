package cofh.thermal.dynamics.init;

import cofh.core.util.ProxyUtils;
import cofh.thermal.dynamics.inventory.container.ItemBufferContainer;
import cofh.thermal.dynamics.inventory.container.attachment.EnergyLimiterAttachmentContainer;
import cofh.thermal.dynamics.inventory.container.attachment.FluidFilterAttachmentContainer;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import static cofh.thermal.core.ThermalCore.CONTAINERS;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class TDynContainers {

    private TDynContainers() {

    }

    public static void register() {

        // CONTAINERS.register(ID_ENERGY_DISTRIBUTOR, () -> IForgeContainerType.create((windowId, inv, data) -> new EnergyDistributorContainer(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())));

    }

    public static final RegistryObject<MenuType<ItemBufferContainer>> ITEM_BUFFER_CONTAINER = CONTAINERS.register(ID_ITEM_BUFFER, () -> IForgeMenuType.create((windowId, inv, data) -> new ItemBufferContainer(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())));

    public static final RegistryObject<MenuType<EnergyLimiterAttachmentContainer>> ENERGY_LIMITER_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_ENERGY_LIMITER_ATTACHMENT, () -> IForgeMenuType.create((windowId, inv, data) -> new EnergyLimiterAttachmentContainer(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));

    public static final RegistryObject<MenuType<FluidFilterAttachmentContainer>> FLUID_FILTER_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_FILTER_ATTACHMENT, () -> IForgeMenuType.create((windowId, inv, data) -> new FluidFilterAttachmentContainer(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));

}
