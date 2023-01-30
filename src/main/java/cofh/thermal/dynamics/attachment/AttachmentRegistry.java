package cofh.thermal.dynamics.attachment;

import cofh.thermal.dynamics.api.grid.IDuct;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

import static cofh.thermal.dynamics.init.TDynGrids.FLUID_GRID;
import static cofh.thermal.dynamics.init.TDynIDs.*;

public class AttachmentRegistry {

    public static final IAttachmentFactory<IAttachment> FILTER_FACTORY = ((nbt, duct, side) -> {
        if (duct.getGridType() == FLUID_GRID.get()) {
            return new FluidFilterAttachment(duct, side).read(nbt);
        }
        return EmptyAttachment.INSTANCE;
    });

    public static final IAttachmentFactory<IAttachment> SERVO_FACTORY = ((nbt, duct, side) -> {
        if (duct.getGridType() == FLUID_GRID.get()) {
            return new FluidServoAttachment(duct, side).read(nbt);
        }
        return EmptyAttachment.INSTANCE;
    });

    protected static final Map<String, IAttachmentFactory<? extends IAttachment>> ATTACHMENT_FACTORY_MAP = new Object2ObjectOpenHashMap<>();

    static {
        registerAttachmentFactory(ENERGY_LIMITER, EnergyLimiterAttachment.FACTORY);
        registerAttachmentFactory(FILTER, FILTER_FACTORY);
        registerAttachmentFactory(SERVO, SERVO_FACTORY);
    }

    public static boolean registerAttachmentFactory(String type, IAttachmentFactory<?> factory) {

        if (type == null || type.isEmpty() || factory == null) {
            return false;
        }
        ATTACHMENT_FACTORY_MAP.put(type, factory);
        return true;
    }

    public static IAttachment getAttachment(String type, CompoundTag nbt, IDuct<?, ?> duct, Direction side) {

        if (ATTACHMENT_FACTORY_MAP.containsKey(type)) {
            return ATTACHMENT_FACTORY_MAP.get(type).createAttachment(nbt, duct, side);
        }
        return EmptyAttachment.INSTANCE;
    }

}
