package net.minecraft.world.level.levelgen.structure;

import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;

public class NetherFossilPieces {
   private static final ResourceLocation[] FOSSILS = new ResourceLocation[]{new ResourceLocation("nether_fossils/fossil_1"), new ResourceLocation("nether_fossils/fossil_2"), new ResourceLocation("nether_fossils/fossil_3"), new ResourceLocation("nether_fossils/fossil_4"), new ResourceLocation("nether_fossils/fossil_5"), new ResourceLocation("nether_fossils/fossil_6"), new ResourceLocation("nether_fossils/fossil_7"), new ResourceLocation("nether_fossils/fossil_8"), new ResourceLocation("nether_fossils/fossil_9"), new ResourceLocation("nether_fossils/fossil_10"), new ResourceLocation("nether_fossils/fossil_11"), new ResourceLocation("nether_fossils/fossil_12"), new ResourceLocation("nether_fossils/fossil_13"), new ResourceLocation("nether_fossils/fossil_14")};

   public static void addPieces(StructureManager pStructureManager, StructurePieceAccessor pPieces, Random pRandom, BlockPos pPos) {
      Rotation rotation = Rotation.getRandom(pRandom);
      pPieces.addPiece(new NetherFossilPieces.NetherFossilPiece(pStructureManager, Util.getRandom(FOSSILS, pRandom), pPos, rotation));
   }

   public static class NetherFossilPiece extends TemplateStructurePiece {
      public NetherFossilPiece(StructureManager pStructureManager, ResourceLocation pLocation, BlockPos pPos, Rotation pRotation) {
         super(StructurePieceType.NETHER_FOSSIL, 0, pStructureManager, pLocation, pLocation.toString(), makeSettings(pRotation), pPos);
      }

      public NetherFossilPiece(StructureManager pStructureManager, CompoundTag pTag) {
         super(StructurePieceType.NETHER_FOSSIL, pTag, pStructureManager, (p_162980_) -> {
            return makeSettings(Rotation.valueOf(pTag.getString("Rot")));
         });
      }

      private static StructurePlaceSettings makeSettings(Rotation pRotation) {
         return (new StructurePlaceSettings()).setRotation(pRotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
      }

      protected void handleDataMarker(String pMarker, BlockPos pPos, ServerLevelAccessor pLevel, Random pRandom, BoundingBox pBox) {
      }

      public void postProcess(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, ChunkPos pChunkPos, BlockPos pPos) {
         pBox.encapsulate(this.template.getBoundingBox(this.placeSettings, this.templatePosition));
         super.postProcess(pLevel, pStructureFeatureManager, pChunkGenerator, pRandom, pBox, pChunkPos, pPos);
      }
   }
}