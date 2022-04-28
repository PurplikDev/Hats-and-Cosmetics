package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SpitParticle extends ExplodeParticle {
   SpitParticle(ClientLevel p_107888_, double p_107889_, double p_107890_, double p_107891_, double p_107892_, double p_107893_, double p_107894_, SpriteSet p_107895_) {
      super(p_107888_, p_107889_, p_107890_, p_107891_, p_107892_, p_107893_, p_107894_, p_107895_);
      this.gravity = 0.5F;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprites;

      public Provider(SpriteSet pSprites) {
         this.sprites = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         return new SpitParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed, this.sprites);
      }
   }
}