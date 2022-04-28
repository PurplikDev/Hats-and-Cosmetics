package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ThrownItemRenderer<T extends Entity & ItemSupplier> extends EntityRenderer<T> {
   private static final float MIN_CAMERA_DISTANCE_SQUARED = 12.25F;
   private final ItemRenderer itemRenderer;
   private final float scale;
   private final boolean fullBright;

   public ThrownItemRenderer(EntityRendererProvider.Context p_174416_, float p_174417_, boolean p_174418_) {
      super(p_174416_);
      this.itemRenderer = p_174416_.getItemRenderer();
      this.scale = p_174417_;
      this.fullBright = p_174418_;
   }

   public ThrownItemRenderer(EntityRendererProvider.Context p_174414_) {
      this(p_174414_, 1.0F, false);
   }

   protected int getBlockLightLevel(T pEntity, BlockPos pPos) {
      return this.fullBright ? 15 : super.getBlockLightLevel(pEntity, pPos);
   }

   public void render(T pEntity, float pEntityYaw, float pPartialTicks, PoseStack pMatrixStack, MultiBufferSource pBuffer, int pPackedLight) {
      if (pEntity.tickCount >= 2 || !(this.entityRenderDispatcher.camera.getEntity().distanceToSqr(pEntity) < 12.25D)) {
         pMatrixStack.pushPose();
         pMatrixStack.scale(this.scale, this.scale, this.scale);
         pMatrixStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
         pMatrixStack.mulPose(Vector3f.YP.rotationDegrees(180.0F));
         this.itemRenderer.renderStatic(pEntity.getItem(), ItemTransforms.TransformType.GROUND, pPackedLight, OverlayTexture.NO_OVERLAY, pMatrixStack, pBuffer, pEntity.getId());
         pMatrixStack.popPose();
         super.render(pEntity, pEntityYaw, pPartialTicks, pMatrixStack, pBuffer, pPackedLight);
      }
   }

   /**
    * Returns the location of an entity's texture.
    */
   public ResourceLocation getTextureLocation(Entity pEntity) {
      return TextureAtlas.LOCATION_BLOCKS;
   }
}