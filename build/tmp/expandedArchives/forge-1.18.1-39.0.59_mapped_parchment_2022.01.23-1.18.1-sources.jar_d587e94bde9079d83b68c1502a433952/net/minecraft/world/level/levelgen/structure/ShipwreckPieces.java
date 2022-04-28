package net.minecraft.world.level.levelgen.structure;

import java.util.Map;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.feature.configurations.ShipwreckConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class ShipwreckPieces {
   static final BlockPos PIVOT = new BlockPos(4, 0, 15);
   private static final ResourceLocation[] STRUCTURE_LOCATION_BEACHED = new ResourceLocation[]{new ResourceLocation("shipwreck/with_mast"), new ResourceLocation("shipwreck/sideways_full"), new ResourceLocation("shipwreck/sideways_fronthalf"), new ResourceLocation("shipwreck/sideways_backhalf"), new ResourceLocation("shipwreck/rightsideup_full"), new ResourceLocation("shipwreck/rightsideup_fronthalf"), new ResourceLocation("shipwreck/rightsideup_backhalf"), new ResourceLocation("shipwreck/with_mast_degraded"), new ResourceLocation("shipwreck/rightsideup_full_degraded"), new ResourceLocation("shipwreck/rightsideup_fronthalf_degraded"), new ResourceLocation("shipwreck/rightsideup_backhalf_degraded")};
   private static final ResourceLocation[] STRUCTURE_LOCATION_OCEAN = new ResourceLocation[]{new ResourceLocation("shipwreck/with_mast"), new ResourceLocation("shipwreck/upsidedown_full"), new ResourceLocation("shipwreck/upsidedown_fronthalf"), new ResourceLocation("shipwreck/upsidedown_backhalf"), new ResourceLocation("shipwreck/sideways_full"), new ResourceLocation("shipwreck/sideways_fronthalf"), new ResourceLocation("shipwreck/sideways_backhalf"), new ResourceLocation("shipwreck/rightsideup_full"), new ResourceLocation("shipwreck/rightsideup_fronthalf"), new ResourceLocation("shipwreck/rightsideup_backhalf"), new ResourceLocation("shipwreck/with_mast_degraded"), new ResourceLocation("shipwreck/upsidedown_full_degraded"), new ResourceLocation("shipwreck/upsidedown_fronthalf_degraded"), new ResourceLocation("shipwreck/upsidedown_backhalf_degraded"), new ResourceLocation("shipwreck/sideways_full_degraded"), new ResourceLocation("shipwreck/sideways_fronthalf_degraded"), new ResourceLocation("shipwreck/sideways_backhalf_degraded"), new ResourceLocation("shipwreck/rightsideup_full_degraded"), new ResourceLocation("shipwreck/rightsideup_fronthalf_degraded"), new ResourceLocation("shipwreck/rightsideup_backhalf_degraded")};
   static final Map<String, ResourceLocation> MARKERS_TO_LOOT = Map.of("map_chest", BuiltInLootTables.SHIPWRECK_MAP, "treasure_chest", BuiltInLootTables.SHIPWRECK_TREASURE, "supply_chest", BuiltInLootTables.SHIPWRECK_SUPPLY);

   public static void addPieces(StructureManager pStructureManager, BlockPos pPos, Rotation pRotation, StructurePieceAccessor pPieces, Random pRandom, ShipwreckConfiguration pConfig) {
      ResourceLocation resourcelocation = Util.getRandom(pConfig.isBeached ? STRUCTURE_LOCATION_BEACHED : STRUCTURE_LOCATION_OCEAN, pRandom);
      pPieces.addPiece(new ShipwreckPieces.ShipwreckPiece(pStructureManager, resourcelocation, pPos, pRotation, pConfig.isBeached));
   }

   public static class ShipwreckPiece extends TemplateStructurePiece {
      private final boolean isBeached;

      public ShipwreckPiece(StructureManager pStructureManager, ResourceLocation pLocation, BlockPos pPos, Rotation pRotation, boolean pIsBeached) {
         super(StructurePieceType.SHIPWRECK_PIECE, 0, pStructureManager, pLocation, pLocation.toString(), makeSettings(pRotation), pPos);
         this.isBeached = pIsBeached;
      }

      public ShipwreckPiece(StructureManager pStructureManager, CompoundTag pTag) {
         super(StructurePieceType.SHIPWRECK_PIECE, pTag, pStructureManager, (p_163217_) -> {
            return makeSettings(Rotation.valueOf(pTag.getString("Rot")));
         });
         this.isBeached = pTag.getBoolean("isBeached");
      }

      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putBoolean("isBeached", this.isBeached);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
      }

      private static StructurePlaceSettings makeSettings(Rotation pRotation) {
         return (new StructurePlaceSettings()).setRotation(pRotation).setMirror(Mirror.NONE).setRotationPivot(ShipwreckPieces.PIVOT).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      protected void handleDataMarker(String pMarker, BlockPos pPos, ServerLevelAccessor pLevel, Random pRandom, BoundingBox pBox) {
         ResourceLocation resourcelocation = ShipwreckPieces.MARKERS_TO_LOOT.get(pMarker);
         if (resourcelocation != null) {
            RandomizableContainerBlockEntity.setLootTable(pLevel, pRandom, pPos.below(), resourcelocation);
         }

      }

      public void postProcess(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, ChunkPos pChunkPos, BlockPos pPos) {
         int i = pLevel.getMaxBuildHeight();
         int j = 0;
         Vec3i vec3i = this.template.getSize();
         Heightmap.Types heightmap$types = this.isBeached ? Heightmap.Types.WORLD_SURFACE_WG : Heightmap.Types.OCEAN_FLOOR_WG;
         int k = vec3i.getX() * vec3i.getZ();
         if (k == 0) {
            j = pLevel.getHeight(heightmap$types, this.templatePosition.getX(), this.templatePosition.getZ());
         } else {
            BlockPos blockpos = this.templatePosition.offset(vec3i.getX() - 1, 0, vec3i.getZ() - 1);

            for(BlockPos blockpos1 : BlockPos.betweenClosed(this.templatePosition, blockpos)) {
               int l = pLevel.getHeight(heightmap$types, blockpos1.getX(), blockpos1.getZ());
               j += l;
               i = Math.min(i, l);
            }

            j /= k;
         }

         int i1 = this.isBeached ? i - vec3i.getY() / 2 - pRandom.nextInt(3) : j;
         this.templatePosition = new BlockPos(this.templatePosition.getX(), i1, this.templatePosition.getZ());
         super.postProcess(pLevel, pStructureFeatureManager, pChunkGenerator, pRandom, pBox, pChunkPos, pPos);
      }
   }
}