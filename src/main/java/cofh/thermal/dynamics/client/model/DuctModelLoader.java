package cofh.thermal.dynamics.client.model;

import cofh.thermal.dynamics.client.DuctModelData;
import cofh.thermal.dynamics.client.model.DuctModelLoader.DuctGeometry;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.block.BlockState;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.client.model.geometry.IModelGeometryPart;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Created by covers1624 on 20/12/21.
 */
public class DuctModelLoader implements IModelLoader<DuctGeometry> {

    private static final ResourceLocation NO_TEXTURE = new ResourceLocation("cofh:no_texture");

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {

    }

    @Override
    public DuctGeometry read(JsonDeserializationContext ctx, JsonObject model) {

        return new DuctGeometry(parseElements(ctx, model));
    }

    private Map<String, Map<String, BlockPart>> parseElements(JsonDeserializationContext ctx, JsonObject model) {

        Map<String, Map<String, BlockPart>> parts = new HashMap<>();
        if (model.has("elements")) {
            for (JsonElement element : JSONUtils.getAsJsonArray(model, "elements")) {
                JsonObject obj = element.getAsJsonObject();
                BlockPart part = ctx.deserialize(obj, BlockPart.class);
                String group = JSONUtils.getAsString(obj, "group", null);
                String name = JSONUtils.getAsString(obj, "name");
                Map<String, BlockPart> groupParts = parts.computeIfAbsent(group, e -> new HashMap<>());
                groupParts.put(name, part);
            }
        }
        return parts;
    }

    public static class DuctGeometry implements IModelGeometry<DuctGeometry> {

        //@formatter:off
        private static final IModelGeometryPart INV = new IModelGeometryPart() {
            @Override public String name() { return "inv"; }
            @Override public void addQuads(IModelConfiguration owner, IModelBuilder<?> modelBuilder, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ResourceLocation modelLocation) { }
        };
        //@formatter:on

        private final Map<String, Map<String, BlockPart>> parts;

        public DuctGeometry(Map<String, Map<String, BlockPart>> parts) {

            this.parts = parts;
        }

        @Override
        public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteFunc, IModelTransform transform, ItemOverrideList overrides, ResourceLocation modelLoc) {

            boolean isInventory = owner.getPartVisibility(INV, false);
            // Map<Face, List(FrontFace, BackFace)>
            EnumMap<Direction, List<BakedQuad>> center = buildCenter(owner, spriteFunc, transform, modelLoc);
            // Map<Connection Side, List(FrontFaces & BackFaces)>
            EnumMap<Direction, List<BakedQuad>> ductSides = buildGroupParts("duct", owner, spriteFunc, transform, modelLoc);
            // Map<Connection Side, List(FrontFaces & BackFaces)>
            EnumMap<Direction, List<BakedQuad>> ductFill = buildGroupParts("fill", owner, spriteFunc, transform, modelLoc);
            // Map<Connection Side, List(FrontFaces & BackFaces)>
            EnumMap<Direction, List<BakedQuad>> connections = buildGroupParts("attach", owner, spriteFunc, transform, modelLoc);

            return new DuctModel(owner, spriteFunc.apply(owner.resolveTexture("particle")), center, ductSides, ductFill, connections, isInventory);
        }

        private EnumMap<Direction, List<BakedQuad>> buildCenter(IModelConfiguration owner, Function<RenderMaterial, TextureAtlasSprite> spriteFunc, IModelTransform transform, ResourceLocation modelLoc) {

            EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);

            BlockPart front = getPart("center/duct", "frontface");
            if (front != null) {
                EnumMap<Direction, List<BakedQuad>> baked = bake(front, owner, spriteFunc, transform, modelLoc);
                merge(quads, baked);
            }
            BlockPart frontFill = getPart("center/fill", "frontface");
            if (frontFill != null) {
                EnumMap<Direction, List<BakedQuad>> baked = bake(frontFill, owner, spriteFunc, transform, modelLoc);
                merge(quads, baked);
            }
            BlockPart back = getPart("center/duct", "backface");
            if (back != null) {
                EnumMap<Direction, List<BakedQuad>> baked = bake(back, owner, spriteFunc, transform, modelLoc);
                // These are inverse in the json.
                flip(baked);
                merge(quads, baked);
            }
            return quads;
        }

        private EnumMap<Direction, List<BakedQuad>> buildGroupParts(String groupPart, IModelConfiguration owner, Function<RenderMaterial, TextureAtlasSprite> spriteFunc, IModelTransform transform, ResourceLocation modelLoc) {

            EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
            fill(quads, Arrays.asList(Direction.values()), LinkedList::new);

            for (Direction dir : Direction.values()) {
                String group = dir.getName() + "/" + groupPart;
                Map<String, BlockPart> groupParts = parts.get(group);
                if (groupParts == null) continue;

                List<BakedQuad> list = quads.get(dir);
                for (BlockPart part : groupParts.values()) {
                    Map<Direction, List<BakedQuad>> baked = bake(part, owner, spriteFunc, transform, modelLoc);
                    flatMerge(list, baked);
                }
            }

            return quads;
        }

        private EnumMap<Direction, List<BakedQuad>> bake(BlockPart part, IModelConfiguration owner, Function<RenderMaterial, TextureAtlasSprite> spriteFunc, IModelTransform transform, ResourceLocation modelLoc) {

            EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
            fill(quads, part.faces.keySet(), LinkedList::new);

            for (Map.Entry<Direction, BlockPartFace> entry : part.faces.entrySet()) {
                Direction dir = entry.getKey();
                BlockPartFace face = entry.getValue();
                RenderMaterial material = owner.resolveTexture(face.texture);
                if (material.texture().equals(NO_TEXTURE)) continue;

                TextureAtlasSprite sprite = spriteFunc.apply(material);
                quads.get(dir).add(BlockModel.makeBakedQuad(part, face, sprite, dir, transform, modelLoc));
            }
            return quads;
        }

        @Nullable
        private BlockPart getPart(String group, String name) {

            Map<String, BlockPart> namedParts = parts.get(group);
            if (namedParts == null) return null;

            return namedParts.get(name);
        }

        @Override
        public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {

            List<RenderMaterial> materials = new LinkedList<>();
            for (Map<String, BlockPart> namedPart : parts.values()) {
                for (BlockPart part : namedPart.values()) {
                    for (BlockPartFace face : part.faces.values()) {
                        RenderMaterial mat = owner.resolveTexture(face.texture);
                        if (mat.texture().equals(NO_TEXTURE)) continue;

                        if (MissingTextureSprite.getLocation().equals(mat.texture())) {
                            missingTextureErrors.add(Pair.of(face.texture, owner.getModelName()));
                        }
                        materials.add(mat);
                    }
                }
            }
            return materials;
        }

        private static <K, V> void fill(Map<K, V> map, Iterable<K> keys, Supplier<V> sup) {

            for (K key : keys) {
                map.put(key, sup.get());
            }
        }

        private static <K, T> void merge(Map<K, List<T>> dest, Map<K, List<T>> src) {

            for (K k : src.keySet()) {
                List<T> destList = dest.get(k);
                List<T> srcList = src.get(k);
                if (destList == null) {
                    dest.put(k, srcList);
                } else {
                    destList.addAll(srcList);
                }
            }
        }

        private static <T> void flatMerge(List<T> dest, Map<?, List<T>> src) {

            for (List<T> value : src.values()) {
                dest.addAll(value);
            }
        }

        private static <T> void flip(EnumMap<Direction, T> map) {

            map.put(Direction.UP, map.put(Direction.DOWN, map.get(Direction.UP)));
            map.put(Direction.SOUTH, map.put(Direction.NORTH, map.get(Direction.SOUTH)));
            map.put(Direction.EAST, map.put(Direction.WEST, map.get(Direction.EAST)));
        }

    }

    public static class DuctModel implements IBakedModel {

        private static final boolean DEBUG = Boolean.getBoolean("DuctModel.debug");

        private static final DuctModelData INV_DATA = Util.make(new DuctModelData(), data -> {
            data.setInternalConnection(Direction.UP, true);
            data.setInternalConnection(Direction.DOWN, true);
        });

        private final IModelConfiguration config;
        private final TextureAtlasSprite particle;
        private final Map<Direction, List<BakedQuad>> centerModel;
        private final Map<Direction, List<BakedQuad>> sides;
        private final Map<Direction, List<BakedQuad>> fill;
        private final Map<Direction, List<BakedQuad>> connections;
        private final boolean isInventory;
        private final Map<DuctModelData, List<BakedQuad>> modelCache = new HashMap<>();

        public DuctModel(IModelConfiguration config, TextureAtlasSprite particle, EnumMap<Direction, List<BakedQuad>> centerModel, EnumMap<Direction, List<BakedQuad>> sides, EnumMap<Direction, List<BakedQuad>> fill, EnumMap<Direction, List<BakedQuad>> connections, boolean isInventory) {

            this.config = config;
            this.particle = particle;
            this.centerModel = ImmutableMap.copyOf(centerModel);
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

        private List<BakedQuad> getModelFor(DuctModelData modelData) {

            List<BakedQuad> modelQuads = modelCache.get(modelData); // TODO race condition with DuctModelData being mutable?
            if (!DEBUG && modelQuads != null) return modelQuads;

            synchronized (modelCache) {
                modelQuads = modelCache.get(modelData); // Another thread could have computed whilst we were locked.
                if (!DEBUG && modelQuads != null) return modelQuads;
                ImmutableList.Builder<BakedQuad> quads = ImmutableList.builder();
                for (Direction dir : Direction.values()) {
                    boolean internal = modelData.hasInternalConnection(dir);
                    boolean external = modelData.hasExternalConnection(dir);
                    if (!internal && !external) {
                        quads.addAll(centerModel.get(dir));
                    }
                    if (internal) {
                        quads.addAll(sides.get(dir));
                        quads.addAll(fill.get(dir));
                    } else if (external) {
                        quads.addAll(sides.get(dir));
                        quads.addAll(fill.get(dir));
                        quads.addAll(connections.get(dir));
                    }
                }
                modelQuads = quads.build();
                modelCache.put(new DuctModelData(modelData), modelQuads);
                return modelQuads;
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

}