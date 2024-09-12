package cofh.thermal.dynamics.init.registries;

import cofh.core.util.ProxyUtils;
import cofh.thermal.dynamics.common.inventory.ItemBufferMenu;
import cofh.thermal.dynamics.common.inventory.attachment.EnergyLimiterAttachmentMenu;
import cofh.thermal.dynamics.common.inventory.attachment.FluidFilterAttachmentMenu;
import cofh.thermal.dynamics.common.inventory.attachment.FluidServoAttachmentMenu;
import cofh.thermal.dynamics.common.inventory.attachment.FluidTurboServoAttachmentMenu;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

import static cofh.thermal.core.ThermalCore.CONTAINERS;
import static cofh.thermal.dynamics.init.registries.TDynIDs.*;

public class TDynContainers {

    private TDynContainers() {

    }

    public static void register() {

        // CONTAINERS.register(ID_ENERGY_DISTRIBUTOR, () -> IForgeContainerType.create((windowId, inv, data) -> new EnergyDistributorContainer(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())));

    }

    public static final DeferredHolder<MenuType<?>, MenuType<ItemBufferMenu>> ITEM_BUFFER_CONTAINER = CONTAINERS.register(ID_ITEM_BUFFER, () -> IMenuTypeExtension.create((windowId, inv, data) -> new ItemBufferMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), inv, ProxyUtils.getClientPlayer())));

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyLimiterAttachmentMenu>> ENERGY_LIMITER_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_ENERGY_LIMITER_ATTACHMENT, () -> IMenuTypeExtension.create((windowId, inv, data) -> new EnergyLimiterAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidFilterAttachmentMenu>> FLUID_FILTER_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_FILTER_ATTACHMENT, () -> IMenuTypeExtension.create((windowId, inv, data) -> new FluidFilterAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));
    public static final DeferredHolder<MenuType<?>, MenuType<FluidServoAttachmentMenu>> FLUID_SERVO_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_SERVO_ATTACHMENT, () -> IMenuTypeExtension.create((windowId, inv, data) -> new FluidServoAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));
    public static final DeferredHolder<MenuType<?>, MenuType<FluidTurboServoAttachmentMenu>> FLUID_TURBO_SERVO_ATTACHMENT_CONTAINER = CONTAINERS.register(ID_FLUID_TURBO_SERVO_ATTACHMENT, () -> IMenuTypeExtension.create((windowId, inv, data) -> new FluidTurboServoAttachmentMenu(windowId, ProxyUtils.getClientWorld(), data.readBlockPos(), data.readEnum(Direction.class), inv, ProxyUtils.getClientPlayer())));

}
