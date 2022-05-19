package cofh.thermal.dynamics.client.renderer.model;

import cofh.lib.client.renderer.model.RetexturedBakedQuad;
import cofh.lib.dynamics.BackfaceBakedQuad;
import cofh.thermal.dynamics.client.model.data.DuctModelData;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.IBakedModel;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.model.ItemOverrideList;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

public class DuctBakedModel implements IBakedModel {

    private static final ResourceLocation BLANK = new ResourceLocation("thermal:blank");
    private static final boolean DEBUG = Boolean.getBoolean("DuctModel.debug");

    private static final DuctModelData INV_DATA = Util.make(new DuctModelData(), data -> {
        data.setInternalConnection(Direction.UP, true);
        data.setInternalConnection(Direction.DOWN, true);
    });

    public static void clearCache() {

        MODEL_CACHE.clear();
        CENTER_FILL_CACHE.clear();
        FILL_CACHE.clear();
        SERVO_CACHE.clear();
    }

    private final IModelConfiguration config;
    private final TextureAtlasSprite particle;
    private final Map<Direction, List<BakedQuad>> centerModel;
    private final Map<Direction, List<BakedQuad>> centerFill;
    private final Map<Direction, List<BakedQuad>> sides;
    private final Map<Direction, List<BakedQuad>> fill;
    private final Map<Direction, List<BakedQuad>> connections;
    private final boolean isInventory;
    private static final Map<DuctModelData, List<BakedQuad>> MODEL_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Map<Direction, List<BakedQuad>>> CENTER_FILL_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Map<Direction, List<BakedQuad>>> FILL_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Map<Direction, List<BakedQuad>>> SERVO_CACHE = new HashMap<>();

    public DuctBakedModel(IModelConfiguration config, TextureAtlasSprite particle, EnumMap<Direction, List<BakedQuad>> centerModel, EnumMap<Direction, List<BakedQuad>> centerFill, EnumMap<Direction, List<BakedQuad>> sides, EnumMap<Direction, List<BakedQuad>> fill, EnumMap<Direction, List<BakedQuad>> connections, boolean isInventory) {

        this.config = config;
        this.particle = particle;
        this.centerModel = ImmutableMap.copyOf(centerModel);
        this.centerFill = ImmutableMap.copyOf(centerFill);
        this.sides = ImmutableMap.copyOf(sides);
        this.fill = ImmutableMap.copyOf(fill);
        this.connections = ImmutableMap.copyOf(connections);
        this.isInventory = isInventory;
    }

    @Nonnull
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @Nonnull Random rand, @Nonnull IModelData extraData) {

        if (isInventory) {
            extraData = INV_DATA;
        }
        if (!(extraData instanceof DuctModelData)) return ImmutableList.of();
        DuctModelData modelData = (DuctModelData) extraData;

        return getModelFor(modelData);
    }

    private List<BakedQuad> testQuads;

    private List<BakedQuad> getModelFor(DuctModelData modelData) {

        List<BakedQuad> modelQuads = MODEL_CACHE.get(modelData); // TODO race condition with DuctModelData being mutable?
        if (!DEBUG && modelQuads != null) return modelQuads;

        synchronized (MODEL_CACHE) {
            modelQuads = MODEL_CACHE.get(modelData); // Another thread could have computed whilst we were locked.
            if (!DEBUG && modelQuads != null) return modelQuads;
            ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
            for (Direction dir : Direction.values()) {
                boolean internal = modelData.hasInternalConnection(dir);
                boolean external = modelData.hasExternalConnection(dir);
                ResourceLocation servo = modelData.getServo(dir);

                if (!internal && !external) {
                    List<BakedQuad> fillQuads = rebake(CENTER_FILL_CACHE, centerFill, modelData.getFill(), dir);
                    quads.addAll(filterBlank(centerModel.get(dir), !fillQuads.isEmpty()));
                    quads.addAll(filterBlank(fillQuads, false));
                }
                if (internal) {
                    List<BakedQuad> fillQuads = rebake(FILL_CACHE, fill, modelData.getFill(), dir);
                    quads.addAll(filterBlank(sides.get(dir), !fillQuads.isEmpty()));
                    quads.addAll(filterBlank(fillQuads, false));
                } else if (external) {
                    List<BakedQuad> fillQuads = rebake(FILL_CACHE, fill, modelData.getFill(), dir);
                    quads.addAll(filterBlank(sides.get(dir), !fillQuads.isEmpty()));
                    quads.addAll(filterBlank(fillQuads, false));
                    quads.addAll(filterBlank(rebake(SERVO_CACHE, connections, servo, dir), false));
                }
            }
            modelQuads = quads.build();
            MODEL_CACHE.put(new DuctModelData(modelData), modelQuads);
            return modelQuads;
        }
    }

    private List<BakedQuad> filterBlank(List<BakedQuad> quads, boolean cullBack) {

        List<BakedQuad> newQuads = new ArrayList<>(quads.size());
        for (BakedQuad quad : quads) {
            if (cullBack && quad instanceof BackfaceBakedQuad || quad.getSprite().getName().equals(BLANK)) {
                // do nothing
            } else {
                newQuads.add(quad);
            }
        }
        return newQuads;
    }

    private List<BakedQuad> rebake(Map<ResourceLocation, Map<Direction, List<BakedQuad>>> cache, Map<Direction, List<BakedQuad>> raw, @Nullable ResourceLocation texture, Direction dir) {

        // Easy bail if there are no quads.
        List<BakedQuad> fillQuads = raw.get(dir);
        if (fillQuads.isEmpty()) return ImmutableList.of();

        // Again if there is no texture.
        if (texture == null) return fillQuads;

        // Is it cached already?
        Map<Direction, List<BakedQuad>> retextured = cache.get(texture);
        if (retextured != null) {
            List<BakedQuad> quads = retextured.get(dir);
            // Sure is!
            if (quads != null) return quads;
        }
        // Whatever intellij, I know what im doing.
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (cache) {
            retextured = cache.get(texture); // Another thread could have computed whilst we were locked.
            if (retextured != null) {
                List<BakedQuad> quads = retextured.get(dir);
                // \o/ memory saved++
                if (quads != null) return quads;
            } else {
                retextured = new HashMap<>();
                cache.put(texture, retextured);
            }

            // Grab the sprite
            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getModelManager()
                    .getAtlas(PlayerContainer.BLOCK_ATLAS)
                    .getSprite(texture);

            // Retexture
            List<BakedQuad> newQuads = new ArrayList<>(fillQuads.size());
            for (BakedQuad quad : fillQuads) {
                newQuads.add(new RetexturedBakedQuad(quad, sprite));
            }
            // slap in cache.
            retextured.put(dir, newQuads);
            return newQuads;
        }
    }

    //@formatter:off
    @Override public boolean useAmbientOcclusion() { return config.useSmoothLighting(); }
    @Override public boolean isGui3d() { return config.isShadedInGui(); }
    @Override public boolean usesBlockLight() { return config.isSideLit(); }
    @Override public ItemCameraTransforms getTransforms() { return config.getCameraTransforms(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public TextureAtlasSprite getParticleIcon() { return particle; }
    @Override public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, Random rand) { return getQuads(state, side, rand, EmptyModelData.INSTANCE); }
    @Override public ItemOverrideList getOverrides() { return ItemOverrideList.EMPTY; }
    //@formatter:on
}