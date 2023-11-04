package cofh.thermal.dynamics.client;

import cofh.lib.util.helpers.BlockHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.*;

import static cofh.core.client.CoreRenderType.THICK_LINES;
import static cofh.lib.util.constants.ModIds.ID_THERMAL;
import static net.minecraft.client.renderer.RenderStateShard.COLOR_DEPTH_WRITE;
import static net.minecraft.client.renderer.RenderStateShard.NO_DEPTH_TEST;

/**
 * Created by covers1624 on 12/12/21.
 */
@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL)
public class DebugRenderer {

    private static final AABB smolBox = new AABB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    private static final RenderType laserBox = RenderType.create("td:laser", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, true, RenderType.CompositeState.builder()
            .setShaderState(RenderType.POSITION_COLOR_SHADER)
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderType.NO_TEXTURE)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(RenderType.NO_LIGHTMAP)
            .createCompositeState(false)
    );
    private static final RenderType laserLine = RenderType.create("td:laser", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.LINES, 256, false, true, RenderType.CompositeState.builder()
            .setShaderState(RenderType.RENDERTYPE_LINES_SHADER)
            .setLineState(THICK_LINES)
            .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderType.NO_TEXTURE)
            .setDepthTestState(NO_DEPTH_TEST)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(RenderType.NO_LIGHTMAP)
            .setWriteMaskState(COLOR_DEPTH_WRITE)
            .createCompositeState(false)
    );
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediateWithBuffers(Util.make(new HashMap<>(), map -> {
        map.put(laserBox, new BufferBuilder(laserBox.bufferSize()));
        map.put(laserLine, new BufferBuilder(laserLine.bufferSize()));
    }), new BufferBuilder(256));

    public static Map<UUID, Map<BlockPos, List<BlockPos>>> grids = new HashMap<>();

    public static void register() {

        MinecraftForge.EVENT_BUS.addListener(DebugRenderer::renderWorldLast);
    }

    private static void renderWorldLast(RenderLevelStageEvent event) {

        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }
        PoseStack pStack = event.getPoseStack();
        pStack.pushPose();

        Vec3 projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        pStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

        Random random = new Random();
        for (Map.Entry<UUID, Map<BlockPos, List<BlockPos>>> gridEntry : grids.entrySet()) {
            UUID uuid = gridEntry.getKey();
            random.setSeed(uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits());
            float r = random.nextFloat();
            float g = random.nextFloat();
            float b = random.nextFloat();
            for (Map.Entry<BlockPos, List<BlockPos>> entry : gridEntry.getValue().entrySet()) {
                BlockPos pos = entry.getKey();

                VertexConsumer builder = BUFFERS.getBuffer(laserBox);

                pStack.pushPose();
                pStack.translate(pos.getX(), pos.getY(), pos.getZ());
                bufferCuboidSolid(builder, pStack.last().pose(), smolBox, r, g, b, 0.25F);
                pStack.popPose();

                RenderSystem.disableDepthTest();

                VertexConsumer vb = BUFFERS.getBuffer(laserLine);

                for (BlockPos edge : entry.getValue()) {
                    BlockPos offset = edge.subtract(pos);
                    Direction side = BlockHelper.getSide(offset);
                    Vector3f sub = new Vector3f();
                    if (side != null) {
                        Vec3i norm = side.getNormal();
                        sub = new Vector3f(norm.getX(), norm.getY(), norm.getZ());
                        sub.mul((1F / 16F) * 4);
                    }

                    Vector3f start = new Vector3f(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
                    start.add(sub);
                    Vector3f end = new Vector3f(edge.getX() + 0.5F, edge.getY() + 0.5F, edge.getZ() + 0.5F);
                    end.sub(sub);

                    vb.vertex(pStack.last().pose(), start.x(), start.y(), start.z()).color(1F, 0F, 0F, 0.25F).endVertex();
                    vb.vertex(pStack.last().pose(), end.x(), end.y(), end.z()).color(1F, 0F, 0F, 0.25F).endVertex();
                }
            }
        }

        BUFFERS.endBatch(laserLine);
        BUFFERS.endBatch(laserBox);
        pStack.popPose();
    }

    // region HELPERS
    private static void bufferCuboidSolid(VertexConsumer builder, Matrix4f matrix, AABB c, float r, float g, float b, float a) {

        builder.vertex(matrix, (float) c.minX, (float) c.maxY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.maxY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.minY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.minY, (float) c.minZ).color(r, g, b, a).endVertex();

        builder.vertex(matrix, (float) c.minX, (float) c.minY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.minY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.maxY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.maxY, (float) c.maxZ).color(r, g, b, a).endVertex();

        builder.vertex(matrix, (float) c.minX, (float) c.minY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.minY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.minY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.minY, (float) c.maxZ).color(r, g, b, a).endVertex();

        builder.vertex(matrix, (float) c.minX, (float) c.maxY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.maxY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.maxY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.maxY, (float) c.minZ).color(r, g, b, a).endVertex();

        builder.vertex(matrix, (float) c.minX, (float) c.minY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.maxY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.maxY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.minX, (float) c.minY, (float) c.minZ).color(r, g, b, a).endVertex();

        builder.vertex(matrix, (float) c.maxX, (float) c.minY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.maxY, (float) c.minZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.maxY, (float) c.maxZ).color(r, g, b, a).endVertex();
        builder.vertex(matrix, (float) c.maxX, (float) c.minY, (float) c.maxZ).color(r, g, b, a).endVertex();
    }
    // endregion
}
