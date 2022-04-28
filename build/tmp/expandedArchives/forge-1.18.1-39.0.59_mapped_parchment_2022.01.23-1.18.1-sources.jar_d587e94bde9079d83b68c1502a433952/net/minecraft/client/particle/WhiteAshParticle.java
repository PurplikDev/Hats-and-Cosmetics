package net.minecraft.client.particle;

import java.util.Random;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WhiteAshParticle extends BaseAshSmokeParticle {
   private static final int COLOR_RGB24 = 12235202;

   protected WhiteAshParticle(ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed, float pQuadSizeMultiplier, SpriteSet pSprites) {
      super(pLevel, pX, pY, pZ, 0.1F, -0.1F, 0.1F, pXSpeed, pYSpeed, pZSpeed, pQuadSizeMultiplier, pSprites, 0.0F, 20, 0.0125F, false);
      this.rCol = 0.7294118F;
      this.gCol = 0.69411767F;
      this.bCol = 0.7607843F;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         Random random = pLevel.random;
         double d0 = (double)random.nextFloat() * -1.9D * (double)random.nextFloat() * 0.1D;
         double d1 = (double)random.nextFloat() * -0.5D * (double)random.nextFloat() * 0.1D * 5.0D;
         double d2 = (double)random.nextFloat() * -1.9D * (double)random.nextFloat() * 0.1D;
         return new WhiteAshParticle(pLevel, pX, pY, pZ, d0, d1, d2, 1.0F, this.sprites);
      }
   }
}