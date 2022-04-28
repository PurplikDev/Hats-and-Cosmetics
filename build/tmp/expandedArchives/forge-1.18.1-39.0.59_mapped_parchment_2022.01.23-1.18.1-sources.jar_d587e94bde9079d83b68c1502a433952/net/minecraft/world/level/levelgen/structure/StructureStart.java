package net.minecraft.world.level.levelgen.structure;

import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class StructureStart<C extends FeatureConfiguration> {
   public static final String INVALID_START_ID = "INVALID";
   public static final StructureStart<?> INVALID_START = new StructureStart(null, new ChunkPos(0, 0), 0, new PiecesContainer(List.of()));
   private final StructureFeature<C> feature;
   private final PiecesContainer pieceContainer;
   private final ChunkPos chunkPos;
   private int references;
   @Nullable
   private volatile BoundingBox cachedBoundingBox;

   public StructureStart(StructureFeature<C> pFeature, ChunkPos pChunkPos, int pReferences, PiecesContainer pPieceContainer) {
      this.feature = pFeature;
      this.chunkPos = pChunkPos;
      this.references = pReferences;
      this.pieceContainer = pPieceContainer;
   }

   public BoundingBox getBoundingBox() {
      BoundingBox boundingbox = this.cachedBoundingBox;
      if (boundingbox == null) {
         boundingbox = this.feature.adjustBoundingBox(this.pieceContainer.calculateBoundingBox());
         this.cachedBoundingBox = boundingbox;
      }

      return boundingbox;
   }

   public void placeInChunk(WorldGenLevel pLevel, StructureFeatureManager pStructureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, ChunkPos pChunkPos) {
      List<StructurePiece> list = this.pieceContainer.pieces();
      if (!list.isEmpty()) {
         BoundingBox boundingbox = (list.get(0)).boundingBox;
         BlockPos blockpos = boundingbox.getCenter();
         BlockPos blockpos1 = new BlockPos(blockpos.getX(), boundingbox.minY(), blockpos.getZ());

         for(StructurePiece structurepiece : list) {
            if (structurepiece.getBoundingBox().intersects(pBox)) {
               structurepiece.postProcess(pLevel, pStructureManager, pChunkGenerator, pRandom, pBox, pChunkPos, blockpos1);
            }
         }

         this.feature.getPostPlacementProcessor().afterPlace(pLevel, pStructureManager, pChunkGenerator, pRandom, pBox, pChunkPos, this.pieceContainer);
      }
   }

   public CompoundTag createTag(StructurePieceSerializationContext pContext, ChunkPos pChunkPos) {
      CompoundTag compoundtag = new CompoundTag();
      if (this.isValid()) {
         if (Registry.STRUCTURE_FEATURE.getKey(this.getFeature()) == null) { // FORGE: This is just a more friendly error instead of the 'Null String' below
            throw new RuntimeException("StructureStart \"" + this.getClass().getName() + "\": \"" + this.getFeature() + "\" missing ID Mapping, Modder see MapGenStructureIO");
         }
         compoundtag.putString("id", Registry.STRUCTURE_FEATURE.getKey(this.getFeature()).toString());
         compoundtag.putInt("ChunkX", pChunkPos.x);
         compoundtag.putInt("ChunkZ", pChunkPos.z);
         compoundtag.putInt("references", this.references);
         compoundtag.put("Children", this.pieceContainer.save(pContext));
         return compoundtag;
      } else {
         compoundtag.putString("id", "INVALID");
         return compoundtag;
      }
   }

   public boolean isValid() {
      return !this.pieceContainer.isEmpty();
   }

   public ChunkPos getChunkPos() {
      return this.chunkPos;
   }

   public boolean canBeReferenced() {
      return this.references < this.getMaxReferences();
   }

   public void addReference() {
      ++this.references;
   }

   public int getReferences() {
      return this.references;
   }

   protected int getMaxReferences() {
      return 1;
   }

   public StructureFeature<?> getFeature() {
      return this.feature;
   }

   public List<StructurePiece> getPieces() {
      return this.pieceContainer.pieces();
   }
}
