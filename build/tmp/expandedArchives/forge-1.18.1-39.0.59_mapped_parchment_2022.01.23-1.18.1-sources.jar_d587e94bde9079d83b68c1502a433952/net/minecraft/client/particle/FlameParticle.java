package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FlameParticle extends RisingParticle {
   FlameParticle(ClientLevel p_106800_, double p_106801_, double p_106802_, double p_106803_, double p_106804_, double p_106805_, double p_106806_) {
      super(p_106800_, p_106801_, p_106802_, p_106803_, p_106804_, p_106805_, p_106806_);
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void move(double pX, double pY, double pZ) {
      this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
      this.setLocationFromBoundingbox();
   }

   public float getQuadSize(float pScaleFactor) {
      float f = ((float)this.age + pScaleFactor) / (float)this.lifetime;
      return this.quadSize * (1.0F - f * f * 0.5F);
   }

   public int getLightColor(float pPartialTick) {
      float f = ((float)this.age + pPartialTick) / (float)this.lifetime;
      f = Mth.clamp(f, 0.0F, 1.0F);
      int i = super.getLightColor(pPartialTick);
      int j = i & 255;
      int k = i >> 16 & 255;
      j += (int)(f * 15.0F * 16.0F);
      if (j > 240) {
         j = 240;
      }

      return j | k << 16;
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         FlameParticle flameparticle = new FlameParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         flameparticle.pickSprite(this.sprite);
         return flameparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class SmallFlameProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public SmallFlameProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         FlameParticle flameparticle = new FlameParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         flameparticle.pickSprite(this.sprite);
         flameparticle.scale(0.5F);
         return flameparticle;
      }
   }
}