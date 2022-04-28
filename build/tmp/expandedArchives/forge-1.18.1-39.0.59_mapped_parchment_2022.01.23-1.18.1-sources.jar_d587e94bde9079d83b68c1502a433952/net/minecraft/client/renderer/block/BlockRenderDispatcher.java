package net.minecraft.client.renderer.block;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.Random;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class BlockRenderDispatcher implements ResourceManagerReloadListener {
   private final BlockModelShaper blockModelShaper;
   private final ModelBlockRenderer modelRenderer;
   private final BlockEntityWithoutLevelRenderer blockEntityRenderer;
   private final LiquidBlockRenderer liquidBlockRenderer;
   private final Random random = new Random();
   private final BlockColors blockColors;

   public BlockRenderDispatcher(BlockModelShaper pBlockModelShaper, BlockEntityWithoutLevelRenderer pBlockEntityRenderer, BlockColors pBlockColors) {
      this.blockModelShaper = pBlockModelShaper;
      this.blockEntityRenderer = pBlockEntityRenderer;
      this.blockColors = pBlockColors;
      this.modelRenderer = new net.minecraftforge.client.model.pipeline.ForgeBlockModelRenderer(this.blockColors);
      this.liquidBlockRenderer = new LiquidBlockRenderer();
   }

   public BlockModelShaper getBlockModelShaper() {
      return this.blockModelShaper;
   }

   @Deprecated //Forge: Model parameter
   public void renderBreakingTexture(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer) {
       renderBreakingTexture(pState,pPos, pLevel, pPoseStack, pConsumer, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
   }
   public void renderBreakingTexture(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer, net.minecraftforge.client.model.data.IModelData modelData) {
      if (pState.getRenderShape() == RenderShape.MODEL) {
         BakedModel bakedmodel = this.blockModelShaper.getBlockModel(pState);
         long i = pState.getSeed(pPos);
         this.modelRenderer.tesselateBlock(pLevel, bakedmodel, pState, pPos, pPoseStack, pConsumer, true, this.random, i, OverlayTexture.NO_OVERLAY, modelData);
      }
   }

   @Deprecated //Forge: Model parameter
   public boolean renderBatched(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer, boolean pCheckSides, Random pRandom) {
       return renderBatched(pState, pPos, pLevel, pPoseStack, pConsumer, pCheckSides, pRandom, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
   }
   public boolean renderBatched(BlockState pState, BlockPos pPos, BlockAndTintGetter pLevel, PoseStack pPoseStack, VertexConsumer pConsumer, boolean pCheckSides, Random pRandom, net.minecraftforge.client.model.data.IModelData modelData) {
      try {
         RenderShape rendershape = pState.getRenderShape();
         return rendershape != RenderShape.MODEL ? false : this.modelRenderer.tesselateBlock(pLevel, this.getBlockModel(pState), pState, pPos, pPoseStack, pConsumer, pCheckSides, pRandom, pState.getSeed(pPos), OverlayTexture.NO_OVERLAY, modelData);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating block in world");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
         CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, pState);
         throw new ReportedException(crashreport);
      }
   }

   public boolean renderLiquid(BlockPos pPos, BlockAndTintGetter pLevel, VertexConsumer pConsumer, FluidState pFluidState) {
      try {
         return this.liquidBlockRenderer.tesselate(pLevel, pPos, pConsumer, pFluidState);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Tesselating liquid in world");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Block being tesselated");
         CrashReportCategory.populateBlockDetails(crashreportcategory, pLevel, pPos, (BlockState)null);
         throw new ReportedException(crashreport);
      }
   }

   public ModelBlockRenderer getModelRenderer() {
      return this.modelRenderer;
   }

   public BakedModel getBlockModel(BlockState pState) {
      return this.blockModelShaper.getBlockModel(pState);
   }

   @Deprecated //Forge: Model parameter
   public void renderSingleBlock(BlockState pState, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay) {
      renderSingleBlock(pState, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay, net.minecraftforge.client.model.data.EmptyModelData.INSTANCE);
   }
   public void renderSingleBlock(BlockState pState, PoseStack pPoseStack, MultiBufferSource pBufferSource, int pPackedLight, int pPackedOverlay, net.minecraftforge.client.model.data.IModelData modelData) {
      RenderShape rendershape = pState.getRenderShape();
      if (rendershape != RenderShape.INVISIBLE) {
         switch(rendershape) {
         case MODEL:
            BakedModel bakedmodel = this.getBlockModel(pState);
            int i = this.blockColors.getColor(pState, (BlockAndTintGetter)null, (BlockPos)null, 0);
            float f = (float)(i >> 16 & 255) / 255.0F;
            float f1 = (float)(i >> 8 & 255) / 255.0F;
            float f2 = (float)(i & 255) / 255.0F;
            this.modelRenderer.renderModel(pPoseStack.last(), pBufferSource.getBuffer(ItemBlockRenderTypes.getRenderType(pState, false)), pState, bakedmodel, f, f1, f2, pPackedLight, pPackedOverlay, modelData);
            break;
         case ENTITYBLOCK_ANIMATED:
            ItemStack stack = new ItemStack(pState.getBlock());
            net.minecraftforge.client.RenderProperties.get(stack).getItemStackRenderer().renderByItem(stack, ItemTransforms.TransformType.NONE, pPoseStack, pBufferSource, pPackedLight, pPackedOverlay);
         }

      }
   }

   public void onResourceManagerReload(ResourceManager pResourceManager) {
      this.liquidBlockRenderer.setupSprites();
   }
}
