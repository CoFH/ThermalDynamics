package cofh.thermal.dynamics.client.model;

import cofh.thermal.dynamics.client.renderer.model.DuctBakedModel;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.model.*;
import net.minecraft.client.renderer.texture.MissingTextureSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.IResourceManager;
import net.minecraft.util.Direction;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelBuilder;
import net.minecraftforge.client.model.IModelConfiguration;
import net.minecraftforge.client.model.IModelLoader;
import net.minecraftforge.client.model.geometry.IModelGeometry;
import net.minecraftforge.client.model.geometry.IModelGeometryPart;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class DuctModel implements IModelGeometry<DuctModel> {

    private final Map<String, Map<String, BlockPart>> parts;

    public DuctModel(Map<String, Map<String, BlockPart>> parts) {

        this.parts = parts;
    }

    @Override
    public IBakedModel bake(IModelConfiguration owner, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ItemOverrideList overrides, ResourceLocation modelLocation) {

        boolean isInventory = owner.getPartVisibility(INV, false);
        // Map<Face, List(FrontFace, BackFace)>
        EnumMap<Direction, List<BakedQuad>> center = buildCenter(owner, spriteGetter, modelTransform, modelLocation);
        // Map<Face, List(FrontFace, BackFace)>
        EnumMap<Direction, List<BakedQuad>> centerFill = buildCenterFill(owner, spriteGetter, modelTransform, modelLocation);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> ductSides = buildGroupParts("duct", owner, spriteGetter, modelTransform, modelLocation);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> ductFill = buildGroupParts("fill", owner, spriteGetter, modelTransform, modelLocation);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> connections = buildGroupParts("attach", owner, spriteGetter, modelTransform, modelLocation);

        return new DuctBakedModel(owner, spriteGetter.apply(owner.resolveTexture("particle")), center, centerFill, ductSides, ductFill, connections, isInventory);
    }

    @Override
    public Collection<RenderMaterial> getTextures(IModelConfiguration owner, Function<ResourceLocation, IUnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {

        List<RenderMaterial> materials = new LinkedList<>();
        for (Map<String, BlockPart> namedPart : parts.values()) {
            for (BlockPart part : namedPart.values()) {
                for (BlockPartFace face : part.faces.values()) {
                    RenderMaterial mat = owner.resolveTexture(face.texture);
                    if (MissingTextureSprite.getLocation().equals(mat.texture())) {
                        missingTextureErrors.add(Pair.of(face.texture, owner.getModelName()));
                    }
                    materials.add(mat);
                }
            }
        }
        return materials;
    }

    // region HELPERS
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

    private EnumMap<Direction, List<BakedQuad>> buildCenterFill(IModelConfiguration owner, Function<RenderMaterial, TextureAtlasSprite> spriteFunc, IModelTransform transform, ResourceLocation modelLoc) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        BlockPart frontFill = getPart("center/fill", "frontface");
        if (frontFill != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bake(frontFill, owner, spriteFunc, transform, modelLoc);
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
            TextureAtlasSprite sprite = spriteFunc.apply(owner.resolveTexture(face.texture));
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
    // endregion

    //@formatter:off
    private static final IModelGeometryPart INV = new IModelGeometryPart() {
        @Override public String name() { return "inv"; }
        @Override public void addQuads(IModelConfiguration owner, IModelBuilder<?> modelBuilder, ModelBakery bakery, Function<RenderMaterial, TextureAtlasSprite> spriteGetter, IModelTransform modelTransform, ResourceLocation modelLocation) { }
    };
    //@formatter:on

    // region LOADER
    public static class Loader implements IModelLoader<DuctModel> {

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

        @Override
        public void onResourceManagerReload(IResourceManager resourceManager) {

        }

        @Override
        public DuctModel read(JsonDeserializationContext deserializationContext, JsonObject modelContents) {

            return new DuctModel(parseElements(deserializationContext, modelContents));
        }

    }
    // endregion
}
