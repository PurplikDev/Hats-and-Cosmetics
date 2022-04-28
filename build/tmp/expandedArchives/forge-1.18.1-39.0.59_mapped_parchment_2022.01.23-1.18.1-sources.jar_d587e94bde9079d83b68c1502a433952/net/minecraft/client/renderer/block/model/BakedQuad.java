package net.minecraft.client.renderer.block.model;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BakedQuad implements net.minecraftforge.client.model.pipeline.IVertexProducer {
   /**
    * Joined 4 vertex records, each stores packed data according to the VertexFormat of the quad. Vanilla minecraft uses
    * DefaultVertexFormats.BLOCK, Forge uses (usually) ITEM, use BakedQuad.getFormat() to get the correct format.
    */
   protected final int[] vertices;
   protected final int tintIndex;
   protected final Direction direction;
   protected final TextureAtlasSprite sprite;
   private final boolean shade;

   public BakedQuad(int[] pVertices, int pTintIndex, Direction pDirection, TextureAtlasSprite pSprite, boolean pShade) {
      this.vertices = pVertices;
      this.tintIndex = pTintIndex;
      this.direction = pDirection;
      this.sprite = pSprite;
      this.shade = pShade;
   }

   public TextureAtlasSprite getSprite() {
      return this.sprite;
   }

   public int[] getVertices() {
      return this.vertices;
   }

   public boolean isTinted() {
      return this.tintIndex != -1;
   }

   public int getTintIndex() {
      return this.tintIndex;
   }

   public Direction getDirection() {
      return this.direction;
   }

   @Override
   public void pipe(net.minecraftforge.client.model.pipeline.IVertexConsumer consumer) {
      net.minecraftforge.client.model.pipeline.LightUtil.putBakedQuad(consumer, this);
   }

   public boolean isShade() {
      return this.shade;
   }
}
