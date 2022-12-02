package cofh.thermal.dynamics.attachment;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

import java.util.Map;

public class AttachmentRegistry {

    public static final String ENERGY_LIMITER = "energy_limiter";

    public static final String FLUID_SERVO = "fluid_servo";
    public static final String FLUID_FILTER = "fluid_filter";

    protected static final Map<String, IAttachmentFactory<? extends IAttachment>> ATTACHMENT_FACTORY_MAP = new Object2ObjectOpenHashMap<>();

    static {
        registerAttachmentFactory(ENERGY_LIMITER, EnergyLimiterAttachment.FACTORY);
    }

    public static boolean registerAttachmentFactory(String type, IAttachmentFactory<?> factory) {

        if (type == null || type.isEmpty() || factory == null) {
            return false;
        }
        ATTACHMENT_FACTORY_MAP.put(type, factory);
        return true;
    }

    public static IAttachment getAttachment(String type, CompoundTag nbt, BlockPos pos, Direction side) {

        if (ATTACHMENT_FACTORY_MAP.containsKey(type)) {
            return ATTACHMENT_FACTORY_MAP.get(type).createAttachment(nbt, pos, side);
        }
        return EmptyAttachment.INSTANCE;
    }

}
