package cofh.thermal.dynamics.lib;

import net.minecraft.client.renderer.model.BakedQuad;
import net.minecraft.client.renderer.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.Direction;

import java.util.Arrays;

public class BackfaceBakedQuad extends BakedQuad {

    public BackfaceBakedQuad(int[] vertices, int tintIndex, Direction direction, TextureAtlasSprite sprite, boolean shade) {

        super(vertices, tintIndex, direction, sprite, shade);
    }

    public static BackfaceBakedQuad from(BakedQuad quad) {

        return new BackfaceBakedQuad(Arrays.copyOf(quad.getVertices(), quad.getVertices().length), quad.getTintIndex(), FaceBakery.calculateFacing(quad.getVertices()), quad.getSprite(), quad.isShade());
    }

}
