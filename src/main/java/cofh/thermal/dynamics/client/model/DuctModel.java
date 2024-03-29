package cofh.thermal.dynamics.client.model;

import cofh.lib.client.renderer.block.model.BackfaceBakedQuad;
import cofh.thermal.dynamics.client.renderer.model.DuctBakedModel;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.renderer.block.model.*;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import static cofh.lib.util.Constants.DIRECTIONS;

public class DuctModel implements IUnbakedGeometry<DuctModel> {

    private final Map<String, Map<String, BlockElement>> parts;

    protected static List<DuctBakedModel> bakedModels = new ArrayList<>();

    public static void clearCaches() {

        for (DuctBakedModel model : bakedModels) {
            model.clearCache();
        }
    }

    public DuctModel(Map<String, Map<String, BlockElement>> parts) {

        this.parts = parts;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelState, ItemOverrides overrides, ResourceLocation modelLocation) {

        boolean isInventory = context.isComponentVisible("inv", false);
        // Map<Face, List(FrontFace, BackFace)>
        EnumMap<Direction, List<BakedQuad>> center = buildCenter(context, spriteGetter, modelState, modelLocation);
        // Map<Face, List(FrontFace, BackFace)>
        EnumMap<Direction, List<BakedQuad>> centerFill = buildCenterFill(context, spriteGetter, modelState, modelLocation);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> ductSides = buildGroupParts("duct", context, spriteGetter, modelState, modelLocation);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> ductFill = buildGroupParts("fill", context, spriteGetter, modelState, modelLocation);
        // Map<Connection Side, List(FrontFaces & BackFaces)>
        EnumMap<Direction, List<BakedQuad>> connections = buildGroupParts("attach", context, spriteGetter, modelState, modelLocation);

        DuctBakedModel model = new DuctBakedModel(context, spriteGetter.apply(context.getMaterial("particle")), center, centerFill, ductSides, ductFill, connections, isInventory);
        bakedModels.add(model);
        return model;
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext context, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {

        List<Material> materials = new LinkedList<>();
        for (Map<String, BlockElement> namedPart : parts.values()) {
            for (BlockElement part : namedPart.values()) {
                for (BlockElementFace face : part.faces.values()) {
                    Material mat = context.getMaterial(face.texture);
                    if (MissingTextureAtlasSprite.getLocation().equals(mat.texture())) {
                        missingTextureErrors.add(Pair.of(face.texture, context.getModelName()));
                    }
                    materials.add(mat);
                }
            }
        }
        return materials;
    }

    // region HELPERS
    private EnumMap<Direction, List<BakedQuad>> buildCenter(IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteFunc, ModelState modelState, ResourceLocation modelLoc) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);

        BlockElement front = getPart("center/duct", "frontface");
        if (front != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bake(front, context, spriteFunc, modelState, modelLoc);
            merge(quads, baked);
        }
        BlockElement back = getPart("center/duct", "backface");
        if (back != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bakeBack(back, context, spriteFunc, modelState, modelLoc);
            // These are inverse in the json.
            flip(baked);
            merge(quads, baked);
        }
        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> buildCenterFill(IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteFunc, ModelState modelState, ResourceLocation modelLoc) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        BlockElement frontFill = getPart("center/fill", "frontface");
        if (frontFill != null) {
            EnumMap<Direction, List<BakedQuad>> baked = bake(frontFill, context, spriteFunc, modelState, modelLoc);
            merge(quads, baked);
        }
        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> buildGroupParts(String groupPart, IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteFunc, ModelState modelState, ResourceLocation modelLoc) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        fill(quads, Arrays.asList(DIRECTIONS), LinkedList::new);

        for (Direction dir : DIRECTIONS) {
            String group = dir.getName() + "/" + groupPart;
            Map<String, BlockElement> groupParts = parts.get(group);
            if (groupParts == null) continue;

            List<BakedQuad> list = quads.get(dir);
            for (BlockElement part : groupParts.values()) {
                Map<Direction, List<BakedQuad>> baked = bake(part, context, spriteFunc, modelState, modelLoc);
                flatMerge(list, baked);
            }
        }

        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> bake(BlockElement part, IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteFunc, ModelState modelState, ResourceLocation modelLoc) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        fill(quads, part.faces.keySet(), LinkedList::new);

        for (Map.Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
            Direction dir = entry.getKey();
            BlockElementFace face = entry.getValue();
            TextureAtlasSprite sprite = spriteFunc.apply(context.getMaterial(face.texture));
            quads.get(dir).add(BlockModel.bakeFace(part, face, sprite, dir, modelState, modelLoc));
        }
        return quads;
    }

    private EnumMap<Direction, List<BakedQuad>> bakeBack(BlockElement part, IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteFunc, ModelState modelState, ResourceLocation modelLoc) {

        EnumMap<Direction, List<BakedQuad>> quads = new EnumMap<>(Direction.class);
        fill(quads, part.faces.keySet(), LinkedList::new);

        for (Map.Entry<Direction, BlockElementFace> entry : part.faces.entrySet()) {
            Direction dir = entry.getKey();
            BlockElementFace face = entry.getValue();
            TextureAtlasSprite sprite = spriteFunc.apply(context.getMaterial(face.texture));
            quads.get(dir).add(BackfaceBakedQuad.from(BlockModel.bakeFace(part, face, sprite, dir, modelState, modelLoc)));
        }
        return quads;
    }

    @Nullable
    private BlockElement getPart(String group, String name) {

        Map<String, BlockElement> namedParts = parts.get(group);
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

    // region LOADER
    public static class Loader implements IGeometryLoader<DuctModel> {

        private Map<String, Map<String, BlockElement>> parseElements(JsonDeserializationContext ctx, JsonObject model) {

            Map<String, Map<String, BlockElement>> parts = new HashMap<>();
            if (model.has("elements")) {
                for (JsonElement element : GsonHelper.getAsJsonArray(model, "elements")) {
                    JsonObject obj = element.getAsJsonObject();
                    BlockElement part = ctx.deserialize(obj, BlockElement.class);
                    String group = GsonHelper.getAsString(obj, "group", null);
                    String name = GsonHelper.getAsString(obj, "name");
                    Map<String, BlockElement> groupParts = parts.computeIfAbsent(group, e -> new HashMap<>());
                    groupParts.put(name, part);
                }
            }
            return parts;
        }

        @Override
        public DuctModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {

            return new DuctModel(parseElements(deserializationContext, jsonObject));
        }

    }
    // endregion
}
