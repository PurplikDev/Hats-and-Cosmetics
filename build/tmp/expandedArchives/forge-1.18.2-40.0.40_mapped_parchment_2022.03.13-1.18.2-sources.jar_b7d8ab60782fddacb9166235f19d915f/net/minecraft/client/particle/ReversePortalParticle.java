package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ReversePortalParticle extends PortalParticle {
   ReversePortalParticle(ClientLevel p_107590_, double p_107591_, double p_107592_, double p_107593_, double p_107594_, double p_107595_, double p_107596_) {
      super(p_107590_, p_107591_, p_107592_, p_107593_, p_107594_, p_107595_, p_107596_);
      this.quadSize *= 1.5F;
      this.lifetime = (int)(Math.random() * 2.0D) + 60;
   }

   public float getQuadSize(float pScaleFactor) {
      float f = 1.0F - ((float)this.age + pScaleFactor) / ((float)this.lifetime * 1.5F);
      return this.quadSize * f;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = (float)this.age / (float)this.lifetime;
         this.x += this.xd * (double)f;
         this.y += this.yd * (double)f;
         this.z += this.zd * (double)f;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class ReversePortalProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public ReversePortalProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         ReversePortalParticle reverseportalparticle = new ReversePortalParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         reverseportalparticle.pickSprite(this.sprite);
         return reverseportalparticle;
      }
   }
}