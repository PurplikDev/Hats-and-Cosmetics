package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.CowModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.entity.animal.MushroomCow;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MushroomCowMushroomLayer<T extends MushroomCow> extends RenderLayer<T, CowModel<T>> {
   public MushroomCowMushroomLayer(RenderLayerParent<T, CowModel<T>> p_117243_) {
      super(p_117243_);
   }

   public void render(PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight, T pLivingEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTicks, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch) {
      if (!pLivingEntity.isBaby()) {
         Minecraft minecraft = Minecraft.getInstance();
         boolean flag = minecraft.shouldEntityAppearGlowing(pLivingEntity) && pLivingEntity.isInvisible();
         if (!pLivingEntity.isInvisible() || flag) {
            BlockRenderDispatcher blockrenderdispatcher = minecraft.getBlockRenderer();
            BlockState blockstate = pLivingEntity.getMushroomType().getBlockState();
            int i = LivingEntityRenderer.getOverlayCoords(pLivingEntity, 0.0F);
            BakedModel bakedmodel = blockrenderdispatcher.getBlockModel(blockstate);
            pMatrixStack.pushPose();
            pMatrixStack.translate((double)0.2F, (double)-0.35F, 0.5D);
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-48.0F));
            pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
            pMatrixStack.translate(-0.5D, -0.5D, -0.5D);
            this.renderMushroomBlock(pMatrixStack, pBuffer, pPackedLight, flag, blockrenderdispatcher, blockstate, i, bakedmodel);
            pMatrixStack.popPose();
            pMatrixStack.pushPose();
            pMatrixStack.translate((double)0.2F, (double)-0.35F, 0.5D);
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(42.0F));
            pMatrixStack.translate((double)0.1F, 0.0D, (double)-0.6F);
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-48.0F));
            pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
            pMatrixStack.translate(-0.5D, -0.5D, -0.5D);
            this.renderMushroomBlock(pMatrixStack, pBuffer, pPackedLight, flag, blockrenderdispatcher, blockstate, i, bakedmodel);
            pMatrixStack.popPose();
            pMatrixStack.pushPose();
            this.getParentModel().getHead().translateAndRotate(pMatrixStack);
            pMatrixStack.translate(0.0D, (double)-0.7F, (double)-0.2F);
            pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(-78.0F));
            pMatrixStack.scale(-1.0F, -1.0F, 1.0F);
            pMatrixStack.translate(-0.5D, -0.5D, -0.5D);
            this.renderMushroomBlock(pMatrixStack, pBuffer, pPackedLight, flag, blockrenderdispatcher, blockstate, i, bakedmodel);
            pMatrixStack.popPose();
         }
      }
   }

   private void renderMushroomBlock(PoseStack p_174502_, MultiBufferSource p_174503_, int p_174504_, boolean p_174505_, BlockRenderDispatcher p_174506_, BlockState p_174507_, int p_174508_, BakedModel p_174509_) {
      if (p_174505_) {
         p_174506_.getModelRenderer().renderModel(p_174502_.last(), p_174503_.getBuffer(RenderType.outline(TextureAtlas.LOCATION_BLOCKS)), p_174507_, p_174509_, 0.0F, 0.0F, 0.0F, p_174504_, p_174508_);
      } else {
         p_174506_.renderSingleBlock(p_174507_, p_174502_, p_174503_, p_174504_, p_174508_);
      }

   }
}