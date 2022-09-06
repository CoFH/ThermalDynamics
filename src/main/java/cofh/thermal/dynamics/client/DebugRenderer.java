package cofh.thermal.dynamics.client;

import cofh.lib.util.helpers.BlockHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3d;
import com.mojang.math.Vector3f;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.opengl.GL11;

import java.util.*;

import static cofh.lib.util.constants.Constants.ID_THERMAL;

/**
 * Created by covers1624 on 12/12/21.
 */
@Mod.EventBusSubscriber (value = Dist.CLIENT, modid = ID_THERMAL)
public class DebugRenderer {

    private static final AxisAlignedBB smolBox = new AxisAlignedBB(0.25, 0.25, 0.25, 0.75, 0.75, 0.75);
    private static final RenderType laserBox = RenderType.create("td:laser", DefaultVertexFormats.POSITION_COLOR, GL11.GL_QUADS, 256, false, true, RenderType.State.builder()
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderType.NO_TEXTURE)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(RenderType.NO_LIGHTMAP)
            .createCompositeState(false)
    );
    private static final RenderType laserLine = RenderType.create("td:laser", DefaultVertexFormats.POSITION_COLOR, GL11.GL_LINES, 256, false, true, RenderType.State.builder()
            .setLineState(new RenderState.LineState(OptionalDouble.of(2.5D)))
            .setLayeringState(RenderType.VIEW_OFFSET_Z_LAYERING)
            .setTransparencyState(RenderType.TRANSLUCENT_TRANSPARENCY)
            .setTextureState(RenderType.NO_TEXTURE)
            .setCullState(RenderType.NO_CULL)
            .setLightmapState(RenderType.NO_LIGHTMAP)
            .createCompositeState(false)
    );
    private static final IRenderTypeBuffer.Impl BUFFERS = IRenderTypeBuffer.immediateWithBuffers(Util.make(new HashMap<>(), map -> {
        map.put(laserBox, new BufferBuilder(laserBox.bufferSize()));
        map.put(laserLine, new BufferBuilder(laserLine.bufferSize()));
    }), new BufferBuilder(256));

    public static Map<UUID, Map<BlockPos, List<BlockPos>>> grids = new HashMap<>();

    public static void register() {

        MinecraftForge.EVENT_BUS.addListener(DebugRenderer::renderWorldLast);
    }

    private static void renderWorldLast(RenderWorldLastEvent event) {

        MatrixStack mStack = event.getMatrixStack();
        mStack.pushPose();

        Vector3d projectedView = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        mStack.translate(-projectedView.x, -projectedView.y, -projectedView.z);

        Random random = new Random();
        for (Map.Entry<UUID, Map<BlockPos, List<BlockPos>>> gridEntry : grids.entrySet()) {
            UUID uuid = gridEntry.getKey();
            random.setSeed(uuid.getLeastSignificantBits() ^ uuid.getMostSignificantBits());
            float r = random.nextFloat();
            float g = random.nextFloat();
            float b = random.nextFloat();
            for (Map.Entry<BlockPos, List<BlockPos>> entry : gridEntry.getValue().entrySet()) {
                BlockPos pos = entry.getKey();

                IVertexBuilder builder = BUFFERS.getBuffer(laserBox);

                mStack.pushPose();
                mStack.translate(pos.getX(), pos.getY(), pos.getZ());
                bufferCuboidSolid(builder, mStack.last().pose(), smolBox, r, g, b, 0.25F);
                mStack.popPose();

                RenderSystem.disableDepthTest();

                IVertexBuilder vb = BUFFERS.getBuffer(laserLine);

                for (BlockPos edge : entry.getValue()) {
                    BlockPos offset = edge.subtract(pos);
                    Direction side = BlockHelper.getSide(offset);
                    Vector3f sub = new Vector3f();
                    if (side != null) {
                        Vector3i norm = side.getNormal();
                        sub = new Vector3f(norm.getX(), norm.getY(), norm.getZ());
                        sub.mul((1F / 16F) * 4);
                    }

                    Vector3f start = new Vector3f(pos.getX() + 0.5F, pos.getY() + 0.5F, pos.getZ() + 0.5F);
                    start.add(sub);
                    Vector3f end = new Vector3f(edge.getX() + 0.5F, edge.getY() + 0.5F, edge.getZ() + 0.5F);
                    end.sub(sub);

                    vb.vertex(mStack.last().pose(), start.x(), start.y(), start.z()).color(1F, 0F, 0F, 0.25F).endVertex();
                    vb.vertex(mStack.last().pose(), end.x(), end.y(), end.z()).color(1F, 0F, 0F, 0.25F).endVertex();
                }
            }
        }

        BUFFERS.endBatch(laserLine);
        BUFFERS.endBatch(laserBox);
        mStack.popPose();
    }

    // region HELPERS
    private static void bufferCuboidSolid(IVertexBuilder builder, Matrix4f matrix, AxisAlignedBB c, float r, float g, float b, float a) {

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
