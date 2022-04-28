package net.minecraft.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Vector3f;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.CubicSampler;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class FogRenderer {
   private static final int WATER_FOG_DISTANCE = 192;
   public static final float BIOME_FOG_TRANSITION_TIME = 5000.0F;
   private static float fogRed;
   private static float fogGreen;
   private static float fogBlue;
   private static int targetBiomeFog = -1;
   private static int previousBiomeFog = -1;
   private static long biomeChangedTime = -1L;

   public static void setupColor(Camera pActiveRenderInfo, float pPartialTicks, ClientLevel pLevel, int pRenderDistanceChunks, float pBossColorModifier) {
      FogType fogtype = pActiveRenderInfo.getFluidInCamera();
      Entity entity = pActiveRenderInfo.getEntity();
      if (fogtype == FogType.WATER) {
         long i = Util.getMillis();
         int j = pLevel.getBiome(new BlockPos(pActiveRenderInfo.getPosition())).getWaterFogColor();
         if (biomeChangedTime < 0L) {
            targetBiomeFog = j;
            previousBiomeFog = j;
            biomeChangedTime = i;
         }

         int k = targetBiomeFog >> 16 & 255;
         int l = targetBiomeFog >> 8 & 255;
         int i1 = targetBiomeFog & 255;
         int j1 = previousBiomeFog >> 16 & 255;
         int k1 = previousBiomeFog >> 8 & 255;
         int l1 = previousBiomeFog & 255;
         float f = Mth.clamp((float)(i - biomeChangedTime) / 5000.0F, 0.0F, 1.0F);
         float f1 = Mth.lerp(f, (float)j1, (float)k);
         float f2 = Mth.lerp(f, (float)k1, (float)l);
         float f3 = Mth.lerp(f, (float)l1, (float)i1);
         fogRed = f1 / 255.0F;
         fogGreen = f2 / 255.0F;
         fogBlue = f3 / 255.0F;
         if (targetBiomeFog != j) {
            targetBiomeFog = j;
            previousBiomeFog = Mth.floor(f1) << 16 | Mth.floor(f2) << 8 | Mth.floor(f3);
            biomeChangedTime = i;
         }
      } else if (fogtype == FogType.LAVA) {
         fogRed = 0.6F;
         fogGreen = 0.1F;
         fogBlue = 0.0F;
         biomeChangedTime = -1L;
      } else if (fogtype == FogType.POWDER_SNOW) {
         fogRed = 0.623F;
         fogGreen = 0.734F;
         fogBlue = 0.785F;
         biomeChangedTime = -1L;
         RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
      } else {
         float f4 = 0.25F + 0.75F * (float)pRenderDistanceChunks / 32.0F;
         f4 = 1.0F - (float)Math.pow((double)f4, 0.25D);
         Vec3 vec3 = pLevel.getSkyColor(pActiveRenderInfo.getPosition(), pPartialTicks);
         float f5 = (float)vec3.x;
         float f7 = (float)vec3.y;
         float f9 = (float)vec3.z;
         float f10 = Mth.clamp(Mth.cos(pLevel.getTimeOfDay(pPartialTicks) * ((float)Math.PI * 2F)) * 2.0F + 0.5F, 0.0F, 1.0F);
         BiomeManager biomemanager = pLevel.getBiomeManager();
         Vec3 vec31 = pActiveRenderInfo.getPosition().subtract(2.0D, 2.0D, 2.0D).scale(0.25D);
         Vec3 vec32 = CubicSampler.gaussianSampleVec3(vec31, (p_109033_, p_109034_, p_109035_) -> {
            return pLevel.effects().getBrightnessDependentFogColor(Vec3.fromRGB24(biomemanager.getNoiseBiomeAtQuart(p_109033_, p_109034_, p_109035_).getFogColor()), f10);
         });
         fogRed = (float)vec32.x();
         fogGreen = (float)vec32.y();
         fogBlue = (float)vec32.z();
         if (pRenderDistanceChunks >= 4) {
            float f11 = Mth.sin(pLevel.getSunAngle(pPartialTicks)) > 0.0F ? -1.0F : 1.0F;
            Vector3f vector3f = new Vector3f(f11, 0.0F, 0.0F);
            float f15 = pActiveRenderInfo.getLookVector().dot(vector3f);
            if (f15 < 0.0F) {
               f15 = 0.0F;
            }

            if (f15 > 0.0F) {
               float[] afloat = pLevel.effects().getSunriseColor(pLevel.getTimeOfDay(pPartialTicks), pPartialTicks);
               if (afloat != null) {
                  f15 *= afloat[3];
                  fogRed = fogRed * (1.0F - f15) + afloat[0] * f15;
                  fogGreen = fogGreen * (1.0F - f15) + afloat[1] * f15;
                  fogBlue = fogBlue * (1.0F - f15) + afloat[2] * f15;
               }
            }
         }

         fogRed += (f5 - fogRed) * f4;
         fogGreen += (f7 - fogGreen) * f4;
         fogBlue += (f9 - fogBlue) * f4;
         float f12 = pLevel.getRainLevel(pPartialTicks);
         if (f12 > 0.0F) {
            float f13 = 1.0F - f12 * 0.5F;
            float f16 = 1.0F - f12 * 0.4F;
            fogRed *= f13;
            fogGreen *= f13;
            fogBlue *= f16;
         }

         float f14 = pLevel.getThunderLevel(pPartialTicks);
         if (f14 > 0.0F) {
            float f17 = 1.0F - f14 * 0.5F;
            fogRed *= f17;
            fogGreen *= f17;
            fogBlue *= f17;
         }

         biomeChangedTime = -1L;
      }

      double d0 = (pActiveRenderInfo.getPosition().y - (double)pLevel.getMinBuildHeight()) * pLevel.getLevelData().getClearColorScale();
      if (pActiveRenderInfo.getEntity() instanceof LivingEntity && ((LivingEntity)pActiveRenderInfo.getEntity()).hasEffect(MobEffects.BLINDNESS)) {
         int i2 = ((LivingEntity)pActiveRenderInfo.getEntity()).getEffect(MobEffects.BLINDNESS).getDuration();
         if (i2 < 20) {
            d0 *= (double)(1.0F - (float)i2 / 20.0F);
         } else {
            d0 = 0.0D;
         }
      }

      if (d0 < 1.0D && fogtype != FogType.LAVA) {
         if (d0 < 0.0D) {
            d0 = 0.0D;
         }

         d0 *= d0;
         fogRed = (float)((double)fogRed * d0);
         fogGreen = (float)((double)fogGreen * d0);
         fogBlue = (float)((double)fogBlue * d0);
      }

      if (pBossColorModifier > 0.0F) {
         fogRed = fogRed * (1.0F - pBossColorModifier) + fogRed * 0.7F * pBossColorModifier;
         fogGreen = fogGreen * (1.0F - pBossColorModifier) + fogGreen * 0.6F * pBossColorModifier;
         fogBlue = fogBlue * (1.0F - pBossColorModifier) + fogBlue * 0.6F * pBossColorModifier;
      }

      float f6;
      if (fogtype == FogType.WATER) {
         if (entity instanceof LocalPlayer) {
            f6 = ((LocalPlayer)entity).getWaterVision();
         } else {
            f6 = 1.0F;
         }
      } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasEffect(MobEffects.NIGHT_VISION)) {
         f6 = GameRenderer.getNightVisionScale((LivingEntity)entity, pPartialTicks);
      } else {
         f6 = 0.0F;
      }

      if (fogRed != 0.0F && fogGreen != 0.0F && fogBlue != 0.0F) {
         float f8 = Math.min(1.0F / fogRed, Math.min(1.0F / fogGreen, 1.0F / fogBlue));
         // Forge: fix MC-4647 and MC-10480
         if (Float.isInfinite(f8)) f8 = Math.nextAfter(f8, 0.0);
         fogRed = fogRed * (1.0F - f6) + fogRed * f8 * f6;
         fogGreen = fogGreen * (1.0F - f6) + fogGreen * f8 * f6;
         fogBlue = fogBlue * (1.0F - f6) + fogBlue * f8 * f6;
      }

      net.minecraftforge.client.event.EntityViewRenderEvent.FogColors event = new net.minecraftforge.client.event.EntityViewRenderEvent.FogColors(pActiveRenderInfo, pPartialTicks, fogRed, fogGreen, fogBlue);
      net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);

      fogRed = event.getRed();
      fogGreen = event.getGreen();
      fogBlue = event.getBlue();

      RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
   }

   public static void setupNoFog() {
      RenderSystem.setShaderFogStart(Float.MAX_VALUE);
   }
   @Deprecated // FORGE: Pass in partialTicks
   public static void setupFog(Camera pActiveRenderInfo, FogRenderer.FogMode pFogType, float pFarPlaneDistance, boolean pNearFog) {
      setupFog(pActiveRenderInfo, pFogType, pFarPlaneDistance, pNearFog, 0);
   }

   public static void setupFog(Camera pActiveRenderInfo, FogRenderer.FogMode pFogType, float pFarPlaneDistance, boolean pNearFog, float partialTicks) {
      FogType fogtype = pActiveRenderInfo.getFluidInCamera();
      Entity entity = pActiveRenderInfo.getEntity();
      // TODO
      float hook = net.minecraftforge.client.ForgeHooksClient.getFogDensity(pFogType, pActiveRenderInfo, partialTicks, 0.1F);
      if (hook >= 0) {
         RenderSystem.setShaderFogStart(-8.0F);
         RenderSystem.setShaderFogEnd(hook * 0.5F);
      } else
      if (fogtype == FogType.WATER) {
         float f = 192.0F;
         if (entity instanceof LocalPlayer) {
            LocalPlayer localplayer = (LocalPlayer)entity;
            f *= Math.max(0.25F, localplayer.getWaterVision());
            Biome biome = localplayer.level.getBiome(localplayer.blockPosition());
            if (biome.getBiomeCategory() == Biome.BiomeCategory.SWAMP) {
               f *= 0.85F;
            }
         }

         RenderSystem.setShaderFogStart(-8.0F);
         RenderSystem.setShaderFogEnd(f * 0.5F);
      } else {
         float f2;
         float f3;
         if (fogtype == FogType.LAVA) {
            if (entity.isSpectator()) {
               f2 = -8.0F;
               f3 = pFarPlaneDistance * 0.5F;
            } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasEffect(MobEffects.FIRE_RESISTANCE)) {
               f2 = 0.0F;
               f3 = 3.0F;
            } else {
               f2 = 0.25F;
               f3 = 1.0F;
            }
         } else if (entity instanceof LivingEntity && ((LivingEntity)entity).hasEffect(MobEffects.BLINDNESS)) {
            int i = ((LivingEntity)entity).getEffect(MobEffects.BLINDNESS).getDuration();
            float f1 = Mth.lerp(Math.min(1.0F, (float)i / 20.0F), pFarPlaneDistance, 5.0F);
            if (pFogType == FogRenderer.FogMode.FOG_SKY) {
               f2 = 0.0F;
               f3 = f1 * 0.8F;
            } else {
               f2 = f1 * 0.25F;
               f3 = f1;
            }
         } else if (fogtype == FogType.POWDER_SNOW) {
            if (entity.isSpectator()) {
               f2 = -8.0F;
               f3 = pFarPlaneDistance * 0.5F;
            } else {
               f2 = 0.0F;
               f3 = 2.0F;
            }
         } else if (pNearFog) {
            f2 = pFarPlaneDistance * 0.05F;
            f3 = Math.min(pFarPlaneDistance, 192.0F) * 0.5F;
         } else if (pFogType == FogRenderer.FogMode.FOG_SKY) {
            f2 = 0.0F;
            f3 = pFarPlaneDistance;
         } else {
            float f4 = Mth.clamp(pFarPlaneDistance / 10.0F, 4.0F, 64.0F);
            f2 = pFarPlaneDistance - f4;
            f3 = pFarPlaneDistance;
         }

         RenderSystem.setShaderFogStart(f2);
         RenderSystem.setShaderFogEnd(f3);
         net.minecraftforge.client.ForgeHooksClient.onFogRender(pFogType, pActiveRenderInfo, partialTicks, f3);
      }

   }

   public static void levelFogColor() {
      RenderSystem.setShaderFogColor(fogRed, fogGreen, fogBlue);
   }

   @OnlyIn(Dist.CLIENT)
   public static enum FogMode {
      FOG_SKY,
      FOG_TERRAIN;
   }
}
