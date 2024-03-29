package cofh.thermal.dynamics.client.renderer.model;

import cofh.core.util.helpers.RenderHelper;
import cofh.lib.client.renderer.block.model.BackfaceBakedQuad;
import cofh.lib.client.renderer.block.model.RetexturedBakedQuad;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.IDynamicBakedModel;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.*;

import static cofh.lib.util.Constants.DIRECTIONS;
import static cofh.thermal.dynamics.client.model.data.DuctModelData.DUCT_MODEL_DATA;
import static cofh.thermal.dynamics.util.TDynConstants.BLANK_TEXTURE;

public class DuctBakedModel implements IDynamicBakedModel {

    private static final boolean DEBUG = Boolean.getBoolean("DuctModel.debug");

    private static final DuctModelData INV_DATA = Util.make(new DuctModelData(), data -> {
        data.setInternalConnection(Direction.UP, true);
        data.setInternalConnection(Direction.DOWN, true);
    });

    public void clearCache() {

        modelCache.clear();
        centerFillCache.clear();
        fillCache.clear();
        attachmentCache.clear();
    }

    private final IGeometryBakingContext context;
    private final TextureAtlasSprite particle;
    private final Map<Direction, List<BakedQuad>> centerModel;
    private final Map<Direction, List<BakedQuad>> centerFill;
    private final Map<Direction, List<BakedQuad>> sides;
    private final Map<Direction, List<BakedQuad>> fill;
    private final Map<Direction, List<BakedQuad>> connections;
    private final boolean isInventory;
    private final Map<DuctModelData, List<BakedQuad>> modelCache = new HashMap<>();
    private final Map<TexColorWrapper, Map<Direction, List<BakedQuad>>> centerFillCache = new Object2ObjectOpenHashMap<>();
    private final Map<TexColorWrapper, Map<Direction, List<BakedQuad>>> fillCache = new Object2ObjectOpenHashMap<>();
    private final Map<ResourceLocation, Map<Direction, List<BakedQuad>>> attachmentCache = new Object2ObjectOpenHashMap<>();

    public DuctBakedModel(IGeometryBakingContext context, TextureAtlasSprite particle, EnumMap<Direction, List<BakedQuad>> centerModel, EnumMap<Direction, List<BakedQuad>> centerFill, EnumMap<Direction, List<BakedQuad>> sides, EnumMap<Direction, List<BakedQuad>> fill, EnumMap<Direction, List<BakedQuad>> connections, boolean isInventory) {

        this.context = context;
        this.particle = particle;
        this.centerModel = ImmutableMap.copyOf(centerModel);
        this.centerFill = ImmutableMap.copyOf(centerFill);
        this.sides = ImmutableMap.copyOf(sides);
        this.fill = ImmutableMap.copyOf(fill);
        this.connections = ImmutableMap.copyOf(connections);
        this.isInventory = isInventory;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {

        if (side != null) {
            return Collections.emptyList();
        }
        if (isInventory) {
            extraData = ModelData.builder()
                    .with(DUCT_MODEL_DATA, INV_DATA)
                    .build();
        }
        if (!(extraData.has(DUCT_MODEL_DATA))) {
            return ImmutableList.of();
        }
        return getModelFor(extraData.get(DUCT_MODEL_DATA));
    }

    private List<BakedQuad> getModelFor(DuctModelData modelData) {

        List<BakedQuad> modelQuads = modelCache.get(modelData);
        if (!DEBUG && modelQuads != null) {
            return modelQuads;
        }
        synchronized (modelCache) {
            modelQuads = modelCache.get(modelData); // Another thread could have computed whilst we were locked.
            if (!DEBUG && modelQuads != null) return modelQuads;
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
            for (Direction dir : DIRECTIONS) {
                boolean internal = modelData.hasInternalConnection(dir);
                boolean external = modelData.hasExternalConnection(dir);
                ResourceLocation attachment = modelData.getAttachment(dir);

                if (!internal && !external) {
                    List<BakedQuad> fillQuads = rebakeFill(centerFillCache, centerFill, modelData.getFill(), modelData.getFillColor(), dir);
                    quads.addAll(filterBlank(centerModel.get(dir), false));
                    quads.addAll(filterBlank(fillQuads, false));
                } else {
                    List<BakedQuad> fillQuads = rebakeFill(fillCache, fill, modelData.getFill(), modelData.getFillColor(), dir);
                    quads.addAll(filterBlank(sides.get(dir), !fillQuads.isEmpty()));
                    quads.addAll(filterBlank(fillQuads, false));
                    if (external) {
                        quads.addAll(filterBlank(rebakeAttachment(attachmentCache, connections, attachment, dir), true));
                    }
                }
            }
            modelQuads = quads.build();
            modelCache.put(new DuctModelData(modelData), modelQuads);
            return modelQuads;
        }
    }

    private List<BakedQuad> filterBlank(List<BakedQuad> quads, boolean cullBack) {

        List<BakedQuad> newQuads = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (cullBack && quad instanceof BackfaceBakedQuad || quad.getSprite().getName().equals(BLANK_TEXTURE)) {
                // do nothing
            } else {
                newQuads.add(quad);
            }
        }
        return newQuads;
    }

    private List<BakedQuad> rebakeFill(Map<TexColorWrapper, Map<Direction, List<BakedQuad>>> cache, Map<Direction, List<BakedQuad>> raw, @Nullable ResourceLocation texture, int color, Direction dir) {

        // Easy bail if there are no quads.
        List<BakedQuad> fillQuads = raw.get(dir);
        if (fillQuads.isEmpty()) {
            return ImmutableList.of();
        }
        // Again if there is no texture.
        if (texture == null) {
            return fillQuads;
        }
        // Is it cached already?
        Map<Direction, List<BakedQuad>> retextured = cache.get(new TexColorWrapper(texture, color));
        if (retextured != null) {
            List<BakedQuad> quads = retextured.get(dir);
            // Sure is!
            if (quads != null) {
                return quads;
            }
        }
        // Whatever intellij, I know what im doing.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cache) {
            retextured = cache.get(new TexColorWrapper(texture, color)); // Another thread could have computed whilst we were locked.
            if (retextured != null) {
                List<BakedQuad> quads = retextured.get(dir);
                // \o/ memory saved++
                if (quads != null) {
                    return quads;
                }
            } else {
                retextured = new HashMap<>();
                cache.put(new TexColorWrapper(texture, color), retextured);
            }

            // Grab the sprite
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getModelManager()
                    .getAtlas(InventoryMenu.BLOCK_ATLAS)
                    .getSprite(texture);

            // Retexture
            List<BakedQuad> newQuads = new ArrayList<>(fillQuads.size());
            for (BakedQuad quad : fillQuads) {
                newQuads.add(new RetexturedBakedQuad(RenderHelper.mulColor(quad, color), sprite));
            }
            // slap in cache.
            retextured.put(dir, newQuads);
            return newQuads;
        }
    }

    private List<BakedQuad> rebakeAttachment(Map<ResourceLocation, Map<Direction, List<BakedQuad>>> cache, Map<Direction, List<BakedQuad>> raw, @Nullable ResourceLocation texture, Direction dir) {

        // Easy bail if there are no quads.
        List<BakedQuad> connQuads = raw.get(dir);
        if (connQuads.isEmpty()) {
            return ImmutableList.of();
        }
        // Again if there is no texture.
        if (texture == null) {
            return connQuads;
        }
        // Is it cached already?
        Map<Direction, List<BakedQuad>> retextured = cache.get(texture);
        if (retextured != null) {
            List<BakedQuad> quads = retextured.get(dir);
            // Sure is!
            if (quads != null) {
                return quads;
            }
        }
        // Whatever intellij, I know what im doing.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cache) {
            retextured = cache.get(texture); // Another thread could have computed whilst we were locked.
            if (retextured != null) {
                List<BakedQuad> quads = retextured.get(dir);
                // \o/ memory saved++
                if (quads != null) {
                    return quads;
                }
            } else {
                retextured = new HashMap<>();
                cache.put(texture, retextured);
            }
            // Grab the sprite
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getModelManager()
                    .getAtlas(InventoryMenu.BLOCK_ATLAS)
                    .getSprite(texture);

            // Retexture
            List<BakedQuad> newQuads = new ArrayList<>(connQuads.size());
            for (BakedQuad quad : connQuads) {
                newQuads.add(new RetexturedBakedQuad(quad, sprite));
            }
            // slap in cache.
            retextured.put(dir, newQuads);
            return newQuads;
        }
    }

    private static class TexColorWrapper {

        ResourceLocation texture;
        int color;

        public TexColorWrapper(ResourceLocation texture, int color) {

            this.texture = texture;
            this.color = color;
        }

        @Override
        public int hashCode() {

            return texture.hashCode() + color * 31;
        }

    }

    //@formatter:off
    @Override public boolean useAmbientOcclusion() { return context.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return context.isGui3d(); }
    @Override public boolean usesBlockLight() { return context.useBlockLight(); }
    @Override public ItemTransforms getTransforms() { return context.getTransforms(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return particle; }
    @Override public ItemOverrides getOverrides() { return ItemOverrides.EMPTY; }
    //@formatter:on
}
