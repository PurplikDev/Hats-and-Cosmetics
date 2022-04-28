package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.DolphinModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.animal.Dolphin;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DolphinCarryingItemLayer extends RenderLayer<Dolphin, DolphinModel<Dolphin>> {
   public DolphinCarryingItemLayer(RenderLayerParent<Dolphin, DolphinModel<Dolphin>> p_116884_) {
      super(p_116884_);
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, Dolphin pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      boolean flag = pLivingEntity.getMainArm() == HumanoidArm.RIGHT;
      pMatrixStack.pushPose();
      float f = 1.0F;
      float f1 = -1.0F;
      float f2 = Mth.abs(pLivingEntity.getXRot()) / 60.0F;
      if (pLivingEntity.getXRot() < 0.0F) {
         pMatrixStack.translate(0.0D, (double)(1.0F - f2 * 0.5F), (double)(-1.0F + f2 * 0.5F));
      } else {
         pMatrixStack.translate(0.0D, (double)(1.0F + f2 * 0.8F), (double)(-1.0F + f2 * 0.2F));
      }

      ItemStack itemstack = flag ? pLivingEntity.getMainHandItem() : pLivingEntity.getOffhandItem();
      Minecraft.getInstance().getItemInHandRenderer().renderItem(pLivingEntity, itemstack, ItemTransforms.TransformType.GROUND, false, pMatrixStack, pBuffer, pPackedLight);
      pMatrixStack.popPose();
   }
}