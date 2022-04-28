package net.minecraft.world.level;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.FeatureAccess;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureCheck;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

public class StructureFeatureManager {
   private final LevelAccessor level;
   private final WorldGenSettings worldGenSettings;
   private final StructureCheck structureCheck;

   public StructureFeatureManager(LevelAccessor pLevel, WorldGenSettings pWorldGenSettings, StructureCheck pStructureCheck) {
      this.level = pLevel;
      this.worldGenSettings = pWorldGenSettings;
      this.structureCheck = pStructureCheck;
   }

   public StructureFeatureManager forWorldGenRegion(WorldGenRegion pRegion) {
      if (pRegion.getLevel() != this.level) {
         throw new IllegalStateException("Using invalid feature manager (source level: " + pRegion.getLevel() + ", region: " + pRegion);
      } else {
         return new StructureFeatureManager(pRegion, this.worldGenSettings, this.structureCheck);
      }
   }

   public List<? extends StructureStart<?>> startsForFeature(SectionPos pPos, StructureFeature<?> pStructure) {
      LongSet longset = this.level.getChunk(pPos.x(), pPos.z(), ChunkStatus.STRUCTURE_REFERENCES).getReferencesForFeature(pStructure);
      Builder<StructureStart<?>> builder = ImmutableList.builder();

      for(long i : longset) {
         SectionPos sectionpos = SectionPos.of(new ChunkPos(i), this.level.getMinSection());
         StructureStart<?> structurestart = this.getStartForFeature(sectionpos, pStructure, this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_STARTS));
         if (structurestart != null && structurestart.isValid()) {
            builder.add(structurestart);
         }
      }

      return builder.build();
   }

   @Nullable
   public StructureStart<?> getStartForFeature(SectionPos pSectionPos, StructureFeature<?> pStructure, FeatureAccess pReader) {
      return pReader.getStartForFeature(pStructure);
   }

   public void setStartForFeature(SectionPos pSectionPos, StructureFeature<?> pStructure, StructureStart<?> pStart, FeatureAccess pReader) {
      pReader.setStartForFeature(pStructure, pStart);
   }

   public void addReferenceForFeature(SectionPos pSectionPos, StructureFeature<?> pStructure, long pChunkValue, FeatureAccess pReader) {
      pReader.addReferenceForFeature(pStructure, pChunkValue);
   }

   public boolean shouldGenerateFeatures() {
      return this.worldGenSettings.generateFeatures();
   }

   public StructureStart<?> getStructureAt(BlockPos pPos, StructureFeature<?> pStructure) {
      for(StructureStart<?> structurestart : this.startsForFeature(SectionPos.of(pPos), pStructure)) {
         if (structurestart.getBoundingBox().isInside(pPos)) {
            return structurestart;
         }
      }

      return StructureStart.INVALID_START;
   }

   public StructureStart<?> getStructureWithPieceAt(BlockPos p_186614_, StructureFeature<?> p_186615_) {
      for(StructureStart<?> structurestart : this.startsForFeature(SectionPos.of(p_186614_), p_186615_)) {
         for(StructurePiece structurepiece : structurestart.getPieces()) {
            if (structurepiece.getBoundingBox().isInside(p_186614_)) {
               return structurestart;
            }
         }
      }

      return StructureStart.INVALID_START;
   }

   public boolean hasAnyStructureAt(BlockPos p_186606_) {
      SectionPos sectionpos = SectionPos.of(p_186606_);
      return this.level.getChunk(sectionpos.x(), sectionpos.z(), ChunkStatus.STRUCTURE_REFERENCES).hasAnyStructureReferences();
   }

   public StructureCheckResult checkStructurePresence(ChunkPos p_196671_, StructureFeature<?> p_196672_, boolean p_196673_) {
      return this.structureCheck.checkStart(p_196671_, p_196672_, p_196673_);
   }

   public void addReference(StructureStart<?> p_196675_) {
      p_196675_.addReference();
      this.structureCheck.incrementReference(p_196675_.getChunkPos(), p_196675_.getFeature());
   }
}