package cofh.thermal.dynamics.init.registries;

import cofh.core.util.ProxyUtils;
import cofh.thermal.dynamics.common.inventory.ItemBufferMenu;
import cofh.thermal.dynamics.common.inventory.attachment.EnergyLimiterAttachmentMenu;
import cofh.thermal.dynamics.common.inventory.attachment.FluidFilterAttachmentMenu;
import cofh.thermal.dynamics.common.inventory.attachment.FluidServoAttachmentMenu;
import cofh.thermal.dynamics.common.inventory.attachment.FluidTurboServoAttachmentMenu;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import static cofh.thermal.core.ThermalCore.CONTAINERS;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynContainers {

    private TDynContainers() {

    }

    public static void register() {

        // CONTAINERS.register(ID_ENERGY_DISTRIBUTOR, () -> IForgeContainerType.create((windowId, inv, data) -> new EnergyDistributorContainer(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())));

    }

    public static final RegistryObject<MenuType<ItemBufferMenu>> ITEM_BUFFER_CONTAINER = CONTAINERS.register(ID_ITEM_BUFFER, () -> IForgeMenuType.create((windowId, inv, data) -> new ItemBufferMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())));

    public static final RegistryObject<MenuType<EnergyLimiterAttachmentMenu>> ENERGY_LIMITER_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_ENERGY_LIMITER_ATTACHMENT, () -> IForgeMenuType.create((windowId, inv, data) -> new EnergyLimiterAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));

    public static final RegistryObject<MenuType<FluidFilterAttachmentMenu>> FLUID_FILTER_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_FILTER_ATTACHMENT, () -> IForgeMenuType.create((windowId, inv, data) -> new FluidFilterAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));
    public static final RegistryObject<MenuType<FluidServoAttachmentMenu>> FLUID_SERVO_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_SERVO_ATTACHMENT, () -> IForgeMenuType.create((windowId, inv, data) -> new FluidServoAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));
    public static final RegistryObject<MenuType<FluidTurboServoAttachmentMenu>> FLUID_TURBO_SERVO_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_TURBO_SERVO_ATTACHMENT, () -> IForgeMenuType.create((windowId, inv, data) -> new FluidTurboServoAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));

}
