package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import java.util.List;
import java.util.Random;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.feature.configurations.OceanRuinConfiguration;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockRotProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;

public class OceanRuinPieces {
   private static final ResourceLocation[] WARM_RUINS = new ResourceLocation[]{new ResourceLocation("underwater_ruin/warm_1"), new ResourceLocation("underwater_ruin/warm_2"), new ResourceLocation("underwater_ruin/warm_3"), new ResourceLocation("underwater_ruin/warm_4"), new ResourceLocation("underwater_ruin/warm_5"), new ResourceLocation("underwater_ruin/warm_6"), new ResourceLocation("underwater_ruin/warm_7"), new ResourceLocation("underwater_ruin/warm_8")};
   private static final ResourceLocation[] RUINS_BRICK = new ResourceLocation[]{new ResourceLocation("underwater_ruin/brick_1"), new ResourceLocation("underwater_ruin/brick_2"), new ResourceLocation("underwater_ruin/brick_3"), new ResourceLocation("underwater_ruin/brick_4"), new ResourceLocation("underwater_ruin/brick_5"), new ResourceLocation("underwater_ruin/brick_6"), new ResourceLocation("underwater_ruin/brick_7"), new ResourceLocation("underwater_ruin/brick_8")};
   private static final ResourceLocation[] RUINS_CRACKED = new ResourceLocation[]{new ResourceLocation("underwater_ruin/cracked_1"), new ResourceLocation("underwater_ruin/cracked_2"), new ResourceLocation("underwater_ruin/cracked_3"), new ResourceLocation("underwater_ruin/cracked_4"), new ResourceLocation("underwater_ruin/cracked_5"), new ResourceLocation("underwater_ruin/cracked_6"), new ResourceLocation("underwater_ruin/cracked_7"), new ResourceLocation("underwater_ruin/cracked_8")};
   private static final ResourceLocation[] RUINS_MOSSY = new ResourceLocation[]{new ResourceLocation("underwater_ruin/mossy_1"), new ResourceLocation("underwater_ruin/mossy_2"), new ResourceLocation("underwater_ruin/mossy_3"), new ResourceLocation("underwater_ruin/mossy_4"), new ResourceLocation("underwater_ruin/mossy_5"), new ResourceLocation("underwater_ruin/mossy_6"), new ResourceLocation("underwater_ruin/mossy_7"), new ResourceLocation("underwater_ruin/mossy_8")};
   private static final ResourceLocation[] BIG_RUINS_BRICK = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_brick_1"), new ResourceLocation("underwater_ruin/big_brick_2"), new ResourceLocation("underwater_ruin/big_brick_3"), new ResourceLocation("underwater_ruin/big_brick_8")};
   private static final ResourceLocation[] BIG_RUINS_MOSSY = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_mossy_1"), new ResourceLocation("underwater_ruin/big_mossy_2"), new ResourceLocation("underwater_ruin/big_mossy_3"), new ResourceLocation("underwater_ruin/big_mossy_8")};
   private static final ResourceLocation[] BIG_RUINS_CRACKED = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_cracked_1"), new ResourceLocation("underwater_ruin/big_cracked_2"), new ResourceLocation("underwater_ruin/big_cracked_3"), new ResourceLocation("underwater_ruin/big_cracked_8")};
   private static final ResourceLocation[] BIG_WARM_RUINS = new ResourceLocation[]{new ResourceLocation("underwater_ruin/big_warm_4"), new ResourceLocation("underwater_ruin/big_warm_5"), new ResourceLocation("underwater_ruin/big_warm_6"), new ResourceLocation("underwater_ruin/big_warm_7")};

   private static ResourceLocation getSmallWarmRuin(Random pRandom) {
      return Util.getRandom(WARM_RUINS, pRandom);
   }

   private static ResourceLocation getBigWarmRuin(Random pRandom) {
      return Util.getRandom(BIG_WARM_RUINS, pRandom);
   }

   public static void addPieces(StructureManager pStructureManager, BlockPos pPos, Rotation pRotation, StructurePieceAccessor pPieces, Random pRandom, OceanRuinConfiguration pConfig) {
      boolean flag = pRandom.nextFloat() <= pConfig.largeProbability;
      float f = flag ? 0.9F : 0.8F;
      addPiece(pStructureManager, pPos, pRotation, pPieces, pRandom, pConfig, flag, f);
      if (flag && pRandom.nextFloat() <= pConfig.clusterProbability) {
         addClusterRuins(pStructureManager, pRandom, pRotation, pPos, pConfig, pPieces);
      }

   }

   private static void addClusterRuins(StructureManager pStructureManager, Random pRandom, Rotation pRotation, BlockPos pPos, OceanRuinConfiguration pConfig, StructurePieceAccessor pPieces) {
      BlockPos blockpos = new BlockPos(pPos.getX(), 90, pPos.getZ());
      BlockPos blockpos1 = StructureTemplate.transform(new BlockPos(15, 0, 15), Mirror.NONE, pRotation, BlockPos.ZERO).offset(blockpos);
      BoundingBox boundingbox = BoundingBox.fromCorners(blockpos, blockpos1);
      BlockPos blockpos2 = new BlockPos(Math.min(blockpos.getX(), blockpos1.getX()), blockpos.getY(), Math.min(blockpos.getZ(), blockpos1.getZ()));
      List<BlockPos> list = allPositions(pRandom, blockpos2);
      int i = Mth.nextInt(pRandom, 4, 8);

      for(int j = 0; j < i; ++j) {
         if (!list.isEmpty()) {
            int k = pRandom.nextInt(list.size());
            BlockPos blockpos3 = list.remove(k);
            Rotation rotation = Rotation.getRandom(pRandom);
            BlockPos blockpos4 = StructureTemplate.transform(new BlockPos(5, 0, 6), Mirror.NONE, rotation, BlockPos.ZERO).offset(blockpos3);
            BoundingBox boundingbox1 = BoundingBox.fromCorners(blockpos3, blockpos4);
            if (!boundingbox1.intersects(boundingbox)) {
               addPiece(pStructureManager, blockpos3, rotation, pPieces, pRandom, pConfig, false, 0.8F);
            }
         }
      }

   }

   private static List<BlockPos> allPositions(Random pRandom, BlockPos pPos) {
      List<BlockPos> list = Lists.newArrayList();
      list.add(pPos.offset(-16 + Mth.nextInt(pRandom, 1, 8), 0, 16 + Mth.nextInt(pRandom, 1, 7)));
      list.add(pPos.offset(-16 + Mth.nextInt(pRandom, 1, 8), 0, Mth.nextInt(pRandom, 1, 7)));
      list.add(pPos.offset(-16 + Mth.nextInt(pRandom, 1, 8), 0, -16 + Mth.nextInt(pRandom, 4, 8)));
      list.add(pPos.offset(Mth.nextInt(pRandom, 1, 7), 0, 16 + Mth.nextInt(pRandom, 1, 7)));
      list.add(pPos.offset(Mth.nextInt(pRandom, 1, 7), 0, -16 + Mth.nextInt(pRandom, 4, 6)));
      list.add(pPos.offset(16 + Mth.nextInt(pRandom, 1, 7), 0, 16 + Mth.nextInt(pRandom, 3, 8)));
      list.add(pPos.offset(16 + Mth.nextInt(pRandom, 1, 7), 0, Mth.nextInt(pRandom, 1, 7)));
      list.add(pPos.offset(16 + Mth.nextInt(pRandom, 1, 7), 0, -16 + Mth.nextInt(pRandom, 4, 8)));
      return list;
   }

   private static void addPiece(StructureManager pStructureManager, BlockPos pPos, Rotation pRotation, StructurePieceAccessor pPieces, Random pRandom, OceanRuinConfiguration pConfig, boolean pIsLarge, float pIntegrity) {
      switch(pConfig.biomeTemp) {
      case WARM:
      default:
         ResourceLocation resourcelocation = pIsLarge ? getBigWarmRuin(pRandom) : getSmallWarmRuin(pRandom);
         pPieces.addPiece(new OceanRuinPieces.OceanRuinPiece(pStructureManager, resourcelocation, pPos, pRotation, pIntegrity, pConfig.biomeTemp, pIsLarge));
         break;
      case COLD:
         ResourceLocation[] aresourcelocation = pIsLarge ? BIG_RUINS_BRICK : RUINS_BRICK;
         ResourceLocation[] aresourcelocation1 = pIsLarge ? BIG_RUINS_CRACKED : RUINS_CRACKED;
         ResourceLocation[] aresourcelocation2 = pIsLarge ? BIG_RUINS_MOSSY : RUINS_MOSSY;
         int i = pRandom.nextInt(aresourcelocation.length);
         pPieces.addPiece(new OceanRuinPieces.OceanRuinPiece(pStructureManager, aresourcelocation[i], pPos, pRotation, pIntegrity, pConfig.biomeTemp, pIsLarge));
         pPieces.addPiece(new OceanRuinPieces.OceanRuinPiece(pStructureManager, aresourcelocation1[i], pPos, pRotation, 0.7F, pConfig.biomeTemp, pIsLarge));
         pPieces.addPiece(new OceanRuinPieces.OceanRuinPiece(pStructureManager, aresourcelocation2[i], pPos, pRotation, 0.5F, pConfig.biomeTemp, pIsLarge));
      }

   }

   public static class OceanRuinPiece extends TemplateStructurePiece {
      private final OceanRuinFeature.Type biomeType;
      private final float integrity;
      private final boolean isLarge;

      public OceanRuinPiece(StructureManager pStructureManager, ResourceLocation pLocation, BlockPos pPos, Rotation pRotation, float pIntegrity, OceanRuinFeature.Type pBiomeType, boolean pIsLarge) {
         super(StructurePieceType.OCEAN_RUIN, 0, pStructureManager, pLocation, pLocation.toString(), makeSettings(pRotation), pPos);
         this.integrity = pIntegrity;
         this.biomeType = pBiomeType;
         this.isLarge = pIsLarge;
      }

      public OceanRuinPiece(StructureManager pStructureManager, CompoundTag pTag) {
         super(StructurePieceType.OCEAN_RUIN, pTag, pStructureManager, (p_163116_) -> {
            return makeSettings(Rotation.valueOf(pTag.getString("Rot")));
         });
         this.integrity = pTag.getFloat("Integrity");
         this.biomeType = OceanRuinFeature.Type.valueOf(pTag.getString("BiomeType"));
         this.isLarge = pTag.getBoolean("IsLarge");
      }

      private static StructurePlaceSettings makeSettings(Rotation pRotation) {
         return (new StructurePlaceSettings()).setRotation(pRotation).setMirror(Mirror.NONE).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
      }

      protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
         super.addAdditionalSaveData(pContext, pTag);
         pTag.putString("Rot", this.placeSettings.getRotation().name());
         pTag.putFloat("Integrity", this.integrity);
         pTag.putString("BiomeType", this.biomeType.toString());
         pTag.putBoolean("IsLarge", this.isLarge);
      }

      protected void handleDataMarker(String pMarker, BlockPos pPos, ServerLevelAccessor pLevel, Random pRandom, BoundingBox pBox) {
         if ("chest".equals(pMarker)) {
            pLevel.setBlock(pPos, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.WATERLOGGED, Boolean.valueOf(pLevel.getFluidState(pPos).is(FluidTags.WATER))), 2);
            BlockEntity blockentity = pLevel.getBlockEntity(pPos);
            if (blockentity instanceof ChestBlockEntity) {
               ((ChestBlockEntity)blockentity).setLootTable(this.isLarge ? BuiltInLootTables.UNDERWATER_RUIN_BIG : BuiltInLootTables.UNDERWATER_RUIN_SMALL, pRandom.nextLong());
            }
         } else if ("drowned".equals(pMarker)) {
            Drowned drowned = EntityType.DROWNED.create(pLevel.getLevel());
            drowned.setPersistenceRequired();
            drowned.moveTo(pPos, 0.0F, 0.0F);
            drowned.finalizeSpawn(pLevel, pLevel.getCurrentDifficultyAt(pPos), MobSpawnType.STRUCTURE, (SpawnGroupData)null, (CompoundTag)null);
            pLevel.addFreshEntityWithPassengers(drowned);
            if (pPos.getY() > pLevel.getSeaLevel()) {
               pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 2);
            } else {
               pLevel.setBlock(pPos, Blocks.WATER.defaultBlockState(), 2);
            }
         }

      }

      public void postProcess(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, ChunkPos pChunkPos, BlockPos pPos) {
         this.placeSettings.clearProcessors().addProcessor(new BlockRotProcessor(this.integrity)).addProcessor(BlockIgnoreProcessor.STRUCTURE_AND_AIR);
         int i = pLevel.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, this.templatePosition.getX(), this.templatePosition.getZ());
         this.templatePosition = new BlockPos(this.templatePosition.getX(), i, this.templatePosition.getZ());
         BlockPos blockpos = StructureTemplate.transform(new BlockPos(this.template.getSize().getX() - 1, 0, this.template.getSize().getZ() - 1), Mirror.NONE, this.placeSettings.getRotation(), BlockPos.ZERO).offset(this.templatePosition);
         this.templatePosition = new BlockPos(this.templatePosition.getX(), this.getHeight(this.templatePosition, pLevel, blockpos), this.templatePosition.getZ());
         super.postProcess(pLevel, pStructureFeatureManager, pChunkGenerator, pRandom, pBox, pChunkPos, pPos);
      }

      private int getHeight(BlockPos pTemplatePos, BlockGetter pLevel, BlockPos pPos) {
         int i = pTemplatePos.getY();
         int j = 512;
         int k = i - 1;
         int l = 0;

         for(BlockPos blockpos : BlockPos.betweenClosed(pTemplatePos, pPos)) {
            int i1 = blockpos.getX();
            int j1 = blockpos.getZ();
            int k1 = pTemplatePos.getY() - 1;
            BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos(i1, k1, j1);
            BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);

            for(FluidState fluidstate = pLevel.getFluidState(blockpos$mutableblockpos); (blockstate.isAir() || fluidstate.is(FluidTags.WATER) || blockstate.is(BlockTags.ICE)) && k1 > pLevel.getMinBuildHeight() + 1; fluidstate = pLevel.getFluidState(blockpos$mutableblockpos)) {
               --k1;
               blockpos$mutableblockpos.set(i1, k1, j1);
               blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
            }

            j = Math.min(j, k1);
            if (k1 < k - 2) {
               ++l;
            }
         }

         int l1 = Math.abs(pTemplatePos.getX() - pPos.getX());
         if (k - j > 2 && l > l1 - 2) {
            i = j + 1;
         }

         return i;
      }
   }
}