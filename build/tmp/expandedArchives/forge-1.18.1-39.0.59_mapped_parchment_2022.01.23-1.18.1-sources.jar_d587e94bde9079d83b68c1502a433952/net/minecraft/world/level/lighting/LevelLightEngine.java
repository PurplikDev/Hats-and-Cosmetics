package net.minecraft.world.level.lighting;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LightChunkGetter;

public class LevelLightEngine implements LightEventListener {
   public static final int MAX_SOURCE_LEVEL = 15;
   public static final int LIGHT_SECTION_PADDING = 1;
   protected final LevelHeightAccessor levelHeightAccessor;
   @Nullable
   private final LayerLightEngine<?, ?> blockEngine;
   @Nullable
   private final LayerLightEngine<?, ?> skyEngine;

   public LevelLightEngine(LightChunkGetter p_75805_, boolean p_75806_, boolean p_75807_) {
      this.levelHeightAccessor = p_75805_.getLevel();
      this.blockEngine = p_75806_ ? new BlockLightEngine(p_75805_) : null;
      this.skyEngine = p_75807_ ? new SkyLightEngine(p_75805_) : null;
   }

   public void checkBlock(BlockPos p_75823_) {
      if (this.blockEngine != null) {
         this.blockEngine.checkBlock(p_75823_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.checkBlock(p_75823_);
      }

   }

   public void onBlockEmissionIncrease(BlockPos p_75824_, int p_75825_) {
      if (this.blockEngine != null) {
         this.blockEngine.onBlockEmissionIncrease(p_75824_, p_75825_);
      }

   }

   public boolean hasLightWork() {
      if (this.skyEngine != null && this.skyEngine.hasLightWork()) {
         return true;
      } else {
         return this.blockEngine != null && this.blockEngine.hasLightWork();
      }
   }

   public int runUpdates(int p_75809_, boolean p_75810_, boolean p_75811_) {
      if (this.blockEngine != null && this.skyEngine != null) {
         int i = p_75809_ / 2;
         int j = this.blockEngine.runUpdates(i, p_75810_, p_75811_);
         int k = p_75809_ - i + j;
         int l = this.skyEngine.runUpdates(k, p_75810_, p_75811_);
         return j == 0 && l > 0 ? this.blockEngine.runUpdates(l, p_75810_, p_75811_) : l;
      } else if (this.blockEngine != null) {
         return this.blockEngine.runUpdates(p_75809_, p_75810_, p_75811_);
      } else {
         return this.skyEngine != null ? this.skyEngine.runUpdates(p_75809_, p_75810_, p_75811_) : p_75809_;
      }
   }

   public void updateSectionStatus(SectionPos pPos, boolean pIsEmpty) {
      if (this.blockEngine != null) {
         this.blockEngine.updateSectionStatus(pPos, pIsEmpty);
      }

      if (this.skyEngine != null) {
         this.skyEngine.updateSectionStatus(pPos, pIsEmpty);
      }

   }

   public void enableLightSources(ChunkPos p_75812_, boolean p_75813_) {
      if (this.blockEngine != null) {
         this.blockEngine.enableLightSources(p_75812_, p_75813_);
      }

      if (this.skyEngine != null) {
         this.skyEngine.enableLightSources(p_75812_, p_75813_);
      }

   }

   public LayerLightEventListener getLayerListener(LightLayer pType) {
      if (pType == LightLayer.BLOCK) {
         return (LayerLightEventListener)(this.blockEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.blockEngine);
      } else {
         return (LayerLightEventListener)(this.skyEngine == null ? LayerLightEventListener.DummyLightLayerEventListener.INSTANCE : this.skyEngine);
      }
   }

   public String getDebugData(LightLayer p_75817_, SectionPos p_75818_) {
      if (p_75817_ == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            return this.blockEngine.getDebugData(p_75818_.asLong());
         }
      } else if (this.skyEngine != null) {
         return this.skyEngine.getDebugData(p_75818_.asLong());
      }

      return "n/a";
   }

   public void queueSectionData(LightLayer p_75819_, SectionPos p_75820_, @Nullable DataLayer p_75821_, boolean p_75822_) {
      if (p_75819_ == LightLayer.BLOCK) {
         if (this.blockEngine != null) {
            this.blockEngine.queueSectionData(p_75820_.asLong(), p_75821_, p_75822_);
         }
      } else if (this.skyEngine != null) {
         this.skyEngine.queueSectionData(p_75820_.asLong(), p_75821_, p_75822_);
      }

   }

   public void retainData(ChunkPos pPos, boolean pRetain) {
      if (this.blockEngine != null) {
         this.blockEngine.retainData(pPos, pRetain);
      }

      if (this.skyEngine != null) {
         this.skyEngine.retainData(pPos, pRetain);
      }

   }

   public int getRawBrightness(BlockPos pBlockPos, int pAmount) {
      int i = this.skyEngine == null ? 0 : this.skyEngine.getLightValue(pBlockPos) - pAmount;
      int j = this.blockEngine == null ? 0 : this.blockEngine.getLightValue(pBlockPos);
      return Math.max(j, i);
   }

   public int getLightSectionCount() {
      return this.levelHeightAccessor.getSectionsCount() + 2;
   }

   public int getMinLightSection() {
      return this.levelHeightAccessor.getMinSection() - 1;
   }

   public int getMaxLightSection() {
      return this.getMinLightSection() + this.getLightSectionCount();
   }
}