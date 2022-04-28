package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Random;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public abstract class StuckInBodyLayer<T extends LivingEntity, M extends PlayerModel<T>> extends RenderLayer<T, M> {
   public StuckInBodyLayer(LivingEntityRenderer<T, M> p_117564_) {
      super(p_117564_);
   }

   protected abstract int numStuck(T p_117565_);

   protected abstract void renderStuckItem(PoseStack p_117566_, MultiBufferSource p_117567_, int p_117568_, Entity p_117569_, float p_117570_, float p_117571_, float p_117572_, float p_117573_);

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      int i = this.numStuck(pLivingEntity);
      Random random = new Random((long)pLivingEntity.getId());
      if (i > 0) {
         for(int j = 0; j < i; ++j) {
            pMatrixStack.pushPose();
            ModelPart modelpart = this.getParentModel().getRandomModelPart(random);
            ModelPart.Cube modelpart$cube = modelpart.getRandomCube(random);
            modelpart.translateAndRotate(pMatrixStack);
            float f = random.nextFloat();
            float f1 = random.nextFloat();
            float f2 = random.nextFloat();
            float f3 = Mth.lerp(f, modelpart$cube.minX, modelpart$cube.maxX) / 16.0F;
            float f4 = Mth.lerp(f1, modelpart$cube.minY, modelpart$cube.maxY) / 16.0F;
            float f5 = Mth.lerp(f2, modelpart$cube.minZ, modelpart$cube.maxZ) / 16.0F;
            pMatrixStack.translate((double)f3, (double)f4, (double)f5);
            f = -1.0F * (f * 2.0F - 1.0F);
            f1 = -1.0F * (f1 * 2.0F - 1.0F);
            f2 = -1.0F * (f2 * 2.0F - 1.0F);
            this.renderStuckItem(pMatrixStack, pBuffer, pPackedLight, pLivingEntity, f, f1, f2, pPartialTicks);
            pMatrixStack.popPose();
         }

      }
   }
}