package net.minecraft.world.level.levelgen.feature;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelSimulatedReader;
import net.minecraft.world.level.LevelWriter;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import net.minecraft.world.level.levelgen.feature.foliageplacers.FoliagePlacer;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.BitSetDiscreteVoxelShape;
import net.minecraft.world.phys.shapes.DiscreteVoxelShape;

public class TreeFeature extends Feature<TreeConfiguration> {
   private static final int BLOCK_UPDATE_FLAGS = 19;

   public TreeFeature(Codec<TreeConfiguration> p_67201_) {
      super(p_67201_);
   }

   public static boolean isFree(LevelSimulatedReader pLevel, BlockPos pPos) {
      return validTreePos(pLevel, pPos) || pLevel.isStateAtPosition(pPos, (p_67281_) -> {
         return p_67281_.is(BlockTags.LOGS);
      });
   }

   private static boolean isVine(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_67276_) -> {
         return p_67276_.is(Blocks.VINE);
      });
   }

   private static boolean isBlockWater(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_67271_) -> {
         return p_67271_.is(Blocks.WATER);
      });
   }

   public static boolean isAirOrLeaves(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_67266_) -> {
         return p_67266_.isAir() || p_67266_.is(BlockTags.LEAVES);
      });
   }

   private static boolean isReplaceablePlant(LevelSimulatedReader pLevel, BlockPos pPos) {
      return pLevel.isStateAtPosition(pPos, (p_160551_) -> {
         Material material = p_160551_.getMaterial();
         return material == Material.REPLACEABLE_PLANT;
      });
   }

   private static void setBlockKnownShape(LevelWriter pLevel, BlockPos pPos, BlockState pState) {
      pLevel.setBlock(pPos, pState, 19);
   }

   public static boolean validTreePos(LevelSimulatedReader pLevel, BlockPos pPos) {
      return isAirOrLeaves(pLevel, pPos) || isReplaceablePlant(pLevel, pPos) || isBlockWater(pLevel, pPos);
   }

   private boolean doPlace(WorldGenLevel pLevel, Random pRandom, BlockPos pPos, BiConsumer<BlockPos, BlockState> pTrunkBlockSetter, BiConsumer<BlockPos, BlockState> pFoliageBlockSetter, TreeConfiguration pConfig) {
      int i = pConfig.trunkPlacer.getTreeHeight(pRandom);
      int j = pConfig.foliagePlacer.foliageHeight(pRandom, i, pConfig);
      int k = i - j;
      int l = pConfig.foliagePlacer.foliageRadius(pRandom, k);
      if (pPos.getY() >= pLevel.getMinBuildHeight() + 1 && pPos.getY() + i + 1 <= pLevel.getMaxBuildHeight()) {
         OptionalInt optionalint = pConfig.minimumSize.minClippedHeight();
         int i1 = this.getMaxFreeTreeHeight(pLevel, i, pPos, pConfig);
         if (i1 >= i || optionalint.isPresent() && i1 >= optionalint.getAsInt()) {
            List<FoliagePlacer.FoliageAttachment> list = pConfig.trunkPlacer.placeTrunk(pLevel, pTrunkBlockSetter, pRandom, i1, pPos, pConfig);
            list.forEach((p_160539_) -> {
               pConfig.foliagePlacer.createFoliage(pLevel, pFoliageBlockSetter, pRandom, pConfig, i1, p_160539_, j, l);
            });
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   private int getMaxFreeTreeHeight(LevelSimulatedReader pLevel, int pTrunkHeight, BlockPos pTopPosition, TreeConfiguration pConfig) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(int i = 0; i <= pTrunkHeight + 1; ++i) {
         int j = pConfig.minimumSize.getSizeAtHeight(pTrunkHeight, i);

         for(int k = -j; k <= j; ++k) {
            for(int l = -j; l <= j; ++l) {
               blockpos$mutableblockpos.setWithOffset(pTopPosition, k, i, l);
               if (!isFree(pLevel, blockpos$mutableblockpos) || !pConfig.ignoreVines && isVine(pLevel, blockpos$mutableblockpos)) {
                  return i - 2;
               }
            }
         }
      }

      return pTrunkHeight;
   }

   protected void setBlock(LevelWriter pLevel, BlockPos pPos, BlockState pState) {
      setBlockKnownShape(pLevel, pPos, pState);
   }

   /**
    * Places the given feature at the given location.
    * During world generation, features are provided with a 3x3 region of chunks, centered on the chunk being generated,
    * that they can safely generate into.
    * @param pContext A context object with a reference to the level and the position the feature is being placed at
    */
   public final boolean place(FeaturePlaceContext<TreeConfiguration> pContext) {
      WorldGenLevel worldgenlevel = pContext.level();
      Random random = pContext.random();
      BlockPos blockpos = pContext.origin();
      TreeConfiguration treeconfiguration = pContext.config();
      Set<BlockPos> set = Sets.newHashSet();
      Set<BlockPos> set1 = Sets.newHashSet();
      Set<BlockPos> set2 = Sets.newHashSet();
      BiConsumer<BlockPos, BlockState> biconsumer = (p_160555_, p_160556_) -> {
         set.add(p_160555_.immutable());
         worldgenlevel.setBlock(p_160555_, p_160556_, 19);
      };
      BiConsumer<BlockPos, BlockState> biconsumer1 = (p_160548_, p_160549_) -> {
         set1.add(p_160548_.immutable());
         worldgenlevel.setBlock(p_160548_, p_160549_, 19);
      };
      BiConsumer<BlockPos, BlockState> biconsumer2 = (p_160543_, p_160544_) -> {
         set2.add(p_160543_.immutable());
         worldgenlevel.setBlock(p_160543_, p_160544_, 19);
      };
      boolean flag = this.doPlace(worldgenlevel, random, blockpos, biconsumer, biconsumer1, treeconfiguration);
      if (flag && (!set.isEmpty() || !set1.isEmpty())) {
         if (!treeconfiguration.decorators.isEmpty()) {
            List<BlockPos> list = Lists.newArrayList(set);
            List<BlockPos> list1 = Lists.newArrayList(set1);
            list.sort(Comparator.comparingInt(Vec3i::getY));
            list1.sort(Comparator.comparingInt(Vec3i::getY));
            treeconfiguration.decorators.forEach((p_160528_) -> {
               p_160528_.place(worldgenlevel, biconsumer2, random, list, list1);
            });
         }

         return BoundingBox.encapsulatingPositions(Iterables.concat(set, set1, set2)).map((p_160521_) -> {
            DiscreteVoxelShape discretevoxelshape = updateLeaves(worldgenlevel, p_160521_, set, set2);
            StructureTemplate.updateShapeAtEdge(worldgenlevel, 3, discretevoxelshape, p_160521_.minX(), p_160521_.minY(), p_160521_.minZ());
            return true;
         }).orElse(false);
      } else {
         return false;
      }
   }

   private static DiscreteVoxelShape updateLeaves(LevelAccessor pLevel, BoundingBox pBoundingBox, Set<BlockPos> pLogPositions, Set<BlockPos> pFoliagePositions) {
      List<Set<BlockPos>> list = Lists.newArrayList();
      DiscreteVoxelShape discretevoxelshape = new BitSetDiscreteVoxelShape(pBoundingBox.getXSpan(), pBoundingBox.getYSpan(), pBoundingBox.getZSpan());
      int i = 6;

      for(int j = 0; j < 6; ++j) {
         list.add(Sets.newHashSet());
      }

      BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

      for(BlockPos blockpos : Lists.newArrayList(pFoliagePositions)) {
         if (pBoundingBox.isInside(blockpos)) {
            discretevoxelshape.fill(blockpos.getX() - pBoundingBox.minX(), blockpos.getY() - pBoundingBox.minY(), blockpos.getZ() - pBoundingBox.minZ());
         }
      }

      for(BlockPos blockpos1 : Lists.newArrayList(pLogPositions)) {
         if (pBoundingBox.isInside(blockpos1)) {
            discretevoxelshape.fill(blockpos1.getX() - pBoundingBox.minX(), blockpos1.getY() - pBoundingBox.minY(), blockpos1.getZ() - pBoundingBox.minZ());
         }

         for(Direction direction : Direction.values()) {
            blockpos$mutableblockpos.setWithOffset(blockpos1, direction);
            if (!pLogPositions.contains(blockpos$mutableblockpos)) {
               BlockState blockstate = pLevel.getBlockState(blockpos$mutableblockpos);
               if (blockstate.hasProperty(BlockStateProperties.DISTANCE)) {
                  list.get(0).add(blockpos$mutableblockpos.immutable());
                  setBlockKnownShape(pLevel, blockpos$mutableblockpos, blockstate.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(1)));
                  if (pBoundingBox.isInside(blockpos$mutableblockpos)) {
                     discretevoxelshape.fill(blockpos$mutableblockpos.getX() - pBoundingBox.minX(), blockpos$mutableblockpos.getY() - pBoundingBox.minY(), blockpos$mutableblockpos.getZ() - pBoundingBox.minZ());
                  }
               }
            }
         }
      }

      for(int l = 1; l < 6; ++l) {
         Set<BlockPos> set = list.get(l - 1);
         Set<BlockPos> set1 = list.get(l);

         for(BlockPos blockpos2 : set) {
            if (pBoundingBox.isInside(blockpos2)) {
               discretevoxelshape.fill(blockpos2.getX() - pBoundingBox.minX(), blockpos2.getY() - pBoundingBox.minY(), blockpos2.getZ() - pBoundingBox.minZ());
            }

            for(Direction direction1 : Direction.values()) {
               blockpos$mutableblockpos.setWithOffset(blockpos2, direction1);
               if (!set.contains(blockpos$mutableblockpos) && !set1.contains(blockpos$mutableblockpos)) {
                  BlockState blockstate1 = pLevel.getBlockState(blockpos$mutableblockpos);
                  if (blockstate1.hasProperty(BlockStateProperties.DISTANCE)) {
                     int k = blockstate1.getValue(BlockStateProperties.DISTANCE);
                     if (k > l + 1) {
                        BlockState blockstate2 = blockstate1.setValue(BlockStateProperties.DISTANCE, Integer.valueOf(l + 1));
                        setBlockKnownShape(pLevel, blockpos$mutableblockpos, blockstate2);
                        if (pBoundingBox.isInside(blockpos$mutableblockpos)) {
                           discretevoxelshape.fill(blockpos$mutableblockpos.getX() - pBoundingBox.minX(), blockpos$mutableblockpos.getY() - pBoundingBox.minY(), blockpos$mutableblockpos.getZ() - pBoundingBox.minZ());
                        }

                        set1.add(blockpos$mutableblockpos.immutable());
                     }
                  }
               }
            }
         }
      }

      return discretevoxelshape;
   }
}