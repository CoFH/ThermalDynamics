package cofh.thermal.dynamics.client;

import net.minecraft.resources.ResourceLocation;

import static cofh.lib.util.constants.ModIds.ID_THERMAL;

public class TDynTextures {

    private TDynTextures() {

    }

    // Energy Limiter
    public static ResourceLocation ENERGY_LIMITER_ATTACHMENT_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/energy_limiter_attachment_active");
    public static ResourceLocation ENERGY_LIMITER_ATTACHMENT_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/energy_limiter_attachment");

    // Filter
    public static ResourceLocation FILTER_ATTACHMENT_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_active");
    public static ResourceLocation FILTER_ATTACHMENT_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment");
    public static ResourceLocation FILTER_ATTACHMENT_ACTIVE_OVERFLOW_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_active_overflown");
    public static ResourceLocation FILTER_ATTACHMENT_OVERFLOW_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_overflown");

    public static ResourceLocation FILTER_ATTACHMENT_TO_GRID_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_to_grid_active");
    public static ResourceLocation FILTER_ATTACHMENT_TO_GRID_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_to_grid");

    public static ResourceLocation FILTER_ATTACHMENT_TO_EXTERNAL_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_to_external_active");
    public static ResourceLocation FILTER_ATTACHMENT_TO_EXTERNAL_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/filter_attachment_to_external");

    // Servo
    public static ResourceLocation SERVO_ATTACHMENT_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/servo_attachment_active");
    public static ResourceLocation SERVO_ATTACHMENT_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/servo_attachment");
    public static ResourceLocation SERVO_ATTACHMENT_ACTIVE_OVERFLOW_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/servo_attachment_active_overflown");
    public static ResourceLocation SERVO_ATTACHMENT_OVERFLOW_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/servo_attachment_overflown");

    // Turbo Servo
    public static ResourceLocation TURBO_SERVO_ATTACHMENT_ACTIVE_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/turbo_servo_attachment_active");
    public static ResourceLocation TURBO_SERVO_ATTACHMENT_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/turbo_servo_attachment");
    public static ResourceLocation TURBO_SERVO_ATTACHMENT_ACTIVE_OVERFLOW_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/turbo_servo_attachment_active_overflown");
    public static ResourceLocation TURBO_SERVO_ATTACHMENT_OVERFLOW_LOC = new ResourceLocation(ID_THERMAL + ":block/ducts/turbo_servo_attachment_overflown");

}
