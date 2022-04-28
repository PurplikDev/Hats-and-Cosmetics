package net.minecraft.client.resources.model;

import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandom;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WeightedBakedModel implements net.minecraftforge.client.model.data.IDynamicBakedModel {
   private final int totalWeight;
   private final List<WeightedEntry.Wrapper<BakedModel>> list;
   private final BakedModel wrapped;

   public WeightedBakedModel(List<WeightedEntry.Wrapper<BakedModel>> pList) {
      this.list = pList;
      this.totalWeight = WeightedRandom.getTotalWeight(pList);
      this.wrapped = pList.get(0).getData();
   }

   // FORGE: Implement our overloads (here and below) so child models can have custom logic 
   public List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pSide, Random pRand, net.minecraftforge.client.model.data.IModelData modelData) {
       return WeightedRandom.getWeightedItem(this.list, Math.abs((int)pRand.nextLong()) % this.totalWeight).map((p_174916_) -> {
           return p_174916_.getData().getQuads(pState, pSide, pRand, modelData);
       }).orElse(Collections.emptyList());
   }

   public boolean useAmbientOcclusion() {
      return this.wrapped.useAmbientOcclusion();
   }

   @Override
   public boolean useAmbientOcclusion(BlockState state) {
      return this.wrapped.useAmbientOcclusion(state);
   }

   public boolean isGui3d() {
      return this.wrapped.isGui3d();
   }

   public boolean usesBlockLight() {
      return this.wrapped.usesBlockLight();
   }

   public boolean isCustomRenderer() {
      return this.wrapped.isCustomRenderer();
   }

   public TextureAtlasSprite getParticleIcon() {
      return this.wrapped.getParticleIcon();
   }

   public TextureAtlasSprite getParticleIcon(net.minecraftforge.client.model.data.IModelData modelData) {
      return this.wrapped.getParticleIcon(modelData);
   }

   public ItemTransforms getTransforms() {
      return this.wrapped.getTransforms();
   }

   public BakedModel handlePerspective(net.minecraft.client.renderer.block.model.ItemTransforms.TransformType transformType, com.mojang.blaze3d.vertex.PoseStack poseStack) {
      return this.wrapped.handlePerspective(transformType, poseStack);
   }

   public ItemOverrides getOverrides() {
      return this.wrapped.getOverrides();
   }

   @OnlyIn(Dist.CLIENT)
   public static class Builder {
      private final List<WeightedEntry.Wrapper<BakedModel>> list = Lists.newArrayList();

      public WeightedBakedModel.Builder add(@Nullable BakedModel pModel, int pWeight) {
         if (pModel != null) {
            this.list.add(WeightedEntry.wrap(pModel, pWeight));
         }

         return this;
      }

      @Nullable
      public BakedModel build() {
         if (this.list.isEmpty()) {
            return null;
         } else {
            return (BakedModel)(this.list.size() == 1 ? this.list.get(0).getData() : new WeightedBakedModel(this.list));
         }
      }
   }
}
