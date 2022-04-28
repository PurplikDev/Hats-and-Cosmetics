package net.minecraft.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EnchantmentTableParticle extends TextureSheetParticle {
   private final double xStart;
   private final double yStart;
   private final double zStart;

   EnchantmentTableParticle(ClientLevel p_106464_, double p_106465_, double p_106466_, double p_106467_, double p_106468_, double p_106469_, double p_106470_) {
      super(p_106464_, p_106465_, p_106466_, p_106467_);
      this.xd = p_106468_;
      this.yd = p_106469_;
      this.zd = p_106470_;
      this.xStart = p_106465_;
      this.yStart = p_106466_;
      this.zStart = p_106467_;
      this.xo = p_106465_ + p_106468_;
      this.yo = p_106466_ + p_106469_;
      this.zo = p_106467_ + p_106470_;
      this.x = this.xo;
      this.y = this.yo;
      this.z = this.zo;
      this.quadSize = 0.1F * (this.random.nextFloat() * 0.5F + 0.2F);
      float f = this.random.nextFloat() * 0.6F + 0.4F;
      this.rCol = 0.9F * f;
      this.gCol = 0.9F * f;
      this.bCol = f;
      this.hasPhysics = false;
      this.lifetime = (int)(Math.random() * 10.0D) + 30;
   }

   public ParticleRenderType getRenderType() {
      return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
   }

   public void move(double pX, double pY, double pZ) {
      this.setBoundingBox(this.getBoundingBox().move(pX, pY, pZ));
      this.setLocationFromBoundingbox();
   }

   public int getLightColor(float pPartialTick) {
      int i = super.getLightColor(pPartialTick);
      float f = (float)this.age / (float)this.lifetime;
      f *= f;
      f *= f;
      int j = i & 255;
      int k = i >> 16 & 255;
      k += (int)(f * 15.0F * 16.0F);
      if (k > 240) {
         k = 240;
      }

      return j | k << 16;
   }

   public void tick() {
      this.xo = this.x;
      this.yo = this.y;
      this.zo = this.z;
      if (this.age++ >= this.lifetime) {
         this.remove();
      } else {
         float f = (float)this.age / (float)this.lifetime;
         f = 1.0F - f;
         float f1 = 1.0F - f;
         f1 *= f1;
         f1 *= f1;
         this.x = this.xStart + this.xd * (double)f;
         this.y = this.yStart + this.yd * (double)f - (double)(f1 * 1.2F);
         this.z = this.zStart + this.zd * (double)f;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class NautilusProvider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public NautilusProvider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         EnchantmentTableParticle enchantmenttableparticle = new EnchantmentTableParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         enchantmenttableparticle.pickSprite(this.sprite);
         return enchantmenttableparticle;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public static class Provider implements ParticleProvider<SimpleParticleType> {
      private final SpriteSet sprite;

      public Provider(SpriteSet pSprites) {
         this.sprite = pSprites;
      }

      public Particle createParticle(SimpleParticleType pType, ClientLevel pLevel, double pX, double pY, double pZ, double pXSpeed, double pYSpeed, double pZSpeed) {
         EnchantmentTableParticle enchantmenttableparticle = new EnchantmentTableParticle(pLevel, pX, pY, pZ, pXSpeed, pYSpeed, pZSpeed);
         enchantmenttableparticle.pickSprite(this.sprite);
         return enchantmenttableparticle;
      }
   }
}