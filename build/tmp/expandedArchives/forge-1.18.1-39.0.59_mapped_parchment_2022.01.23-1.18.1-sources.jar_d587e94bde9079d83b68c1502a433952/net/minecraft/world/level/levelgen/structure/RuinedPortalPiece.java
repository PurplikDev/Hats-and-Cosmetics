package net.minecraft.world.level.levelgen.structure;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.templatesystem.AlwaysTrueTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlackstoneReplaceProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockAgeProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.LavaSubmergedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProcessorRule;
import net.minecraft.world.level.levelgen.structure.templatesystem.ProtectedBlockProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.RandomBlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class RuinedPortalPiece extends TemplateStructurePiece {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final float PROBABILITY_OF_GOLD_GONE = 0.3F;
   private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_NETHERRACK = 0.07F;
   private static final float PROBABILITY_OF_MAGMA_INSTEAD_OF_LAVA = 0.2F;
   private static final float DEFAULT_MOSSINESS = 0.2F;
   private final RuinedPortalPiece.VerticalPlacement verticalPlacement;
   private final RuinedPortalPiece.Properties properties;

   public RuinedPortalPiece(StructureManager pStructureManager, BlockPos pTemplatePosition, RuinedPortalPiece.VerticalPlacement pVerticalPlacement, RuinedPortalPiece.Properties pProperties, ResourceLocation pLocation, StructureTemplate pTemplate, Rotation pRotation, Mirror pMirror, BlockPos pPivotPos) {
      super(StructurePieceType.RUINED_PORTAL, 0, pStructureManager, pLocation, pLocation.toString(), makeSettings(pMirror, pRotation, pVerticalPlacement, pPivotPos, pProperties), pTemplatePosition);
      this.verticalPlacement = pVerticalPlacement;
      this.properties = pProperties;
   }

   public RuinedPortalPiece(StructureManager pStructureManager, CompoundTag pTag) {
      super(StructurePieceType.RUINED_PORTAL, pTag, pStructureManager, (p_192466_) -> {
         return makeSettings(pStructureManager, pTag, p_192466_);
      });
      this.verticalPlacement = RuinedPortalPiece.VerticalPlacement.byName(pTag.getString("VerticalPlacement"));
      this.properties = RuinedPortalPiece.Properties.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, pTag.get("Properties"))).getOrThrow(true, LOGGER::error);
   }

   protected void addAdditionalSaveData(StructurePieceSerializationContext pContext, CompoundTag pTag) {
      super.addAdditionalSaveData(pContext, pTag);
      pTag.putString("Rotation", this.placeSettings.getRotation().name());
      pTag.putString("Mirror", this.placeSettings.getMirror().name());
      pTag.putString("VerticalPlacement", this.verticalPlacement.getName());
      RuinedPortalPiece.Properties.CODEC.encodeStart(NbtOps.INSTANCE, this.properties).resultOrPartial(LOGGER::error).ifPresent((p_163169_) -> {
         pTag.put("Properties", p_163169_);
      });
   }

   private static StructurePlaceSettings makeSettings(StructureManager pStructureManager, CompoundTag pTag, ResourceLocation pLocation) {
      StructureTemplate structuretemplate = pStructureManager.getOrCreate(pLocation);
      BlockPos blockpos = new BlockPos(structuretemplate.getSize().getX() / 2, 0, structuretemplate.getSize().getZ() / 2);
      return makeSettings(Mirror.valueOf(pTag.getString("Mirror")), Rotation.valueOf(pTag.getString("Rotation")), RuinedPortalPiece.VerticalPlacement.byName(pTag.getString("VerticalPlacement")), blockpos, RuinedPortalPiece.Properties.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, pTag.get("Properties"))).getOrThrow(true, LOGGER::error));
   }

   private static StructurePlaceSettings makeSettings(Mirror pMirror, Rotation pRotation, RuinedPortalPiece.VerticalPlacement pPlacement, BlockPos pPivotPos, RuinedPortalPiece.Properties pProperties) {
      BlockIgnoreProcessor blockignoreprocessor = pProperties.airPocket ? BlockIgnoreProcessor.STRUCTURE_BLOCK : BlockIgnoreProcessor.STRUCTURE_AND_AIR;
      List<ProcessorRule> list = Lists.newArrayList();
      list.add(getBlockReplaceRule(Blocks.GOLD_BLOCK, 0.3F, Blocks.AIR));
      list.add(getLavaProcessorRule(pPlacement, pProperties));
      if (!pProperties.cold) {
         list.add(getBlockReplaceRule(Blocks.NETHERRACK, 0.07F, Blocks.MAGMA_BLOCK));
      }

      StructurePlaceSettings structureplacesettings = (new StructurePlaceSettings()).setRotation(pRotation).setMirror(pMirror).setRotationPivot(pPivotPos).addProcessor(blockignoreprocessor).addProcessor(new RuleProcessor(list)).addProcessor(new BlockAgeProcessor(pProperties.mossiness)).addProcessor(new ProtectedBlockProcessor(BlockTags.FEATURES_CANNOT_REPLACE.getName())).addProcessor(new LavaSubmergedBlockProcessor());
      if (pProperties.replaceWithBlackstone) {
         structureplacesettings.addProcessor(BlackstoneReplaceProcessor.INSTANCE);
      }

      return structureplacesettings;
   }

   private static ProcessorRule getLavaProcessorRule(RuinedPortalPiece.VerticalPlacement pPlacement, RuinedPortalPiece.Properties pProperties) {
      if (pPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR) {
         return getBlockReplaceRule(Blocks.LAVA, Blocks.MAGMA_BLOCK);
      } else {
         return pProperties.cold ? getBlockReplaceRule(Blocks.LAVA, Blocks.NETHERRACK) : getBlockReplaceRule(Blocks.LAVA, 0.2F, Blocks.MAGMA_BLOCK);
      }
   }

   public void postProcess(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkGenerator pChunkGenerator, Random pRandom, BoundingBox pBox, ChunkPos pChunkPos, BlockPos pPos) {
      BoundingBox boundingbox = this.template.getBoundingBox(this.placeSettings, this.templatePosition);
      if (pBox.isInside(boundingbox.getCenter())) {
         pBox.encapsulate(boundingbox);
         super.postProcess(pLevel, pStructureFeatureManager, pChunkGenerator, pRandom, pBox, pChunkPos, pPos);
         this.spreadNetherrack(pRandom, pLevel);
         this.addNetherrackDripColumnsBelowPortal(pRandom, pLevel);
         if (this.properties.vines || this.properties.overgrown) {
            BlockPos.betweenClosedStream(this.getBoundingBox()).forEach((p_163166_) -> {
               if (this.properties.vines) {
                  this.maybeAddVines(pRandom, pLevel, p_163166_);
               }

               if (this.properties.overgrown) {
                  this.maybeAddLeavesAbove(pRandom, pLevel, p_163166_);
               }

            });
         }

      }
   }

   protected void handleDataMarker(String pMarker, BlockPos pPos, ServerLevelAccessor pLevel, Random pRandom, BoundingBox pBox) {
   }

   private void maybeAddVines(Random pRandom, LevelAccessor pLevel, BlockPos pPos) {
      BlockState blockstate = pLevel.getBlockState(pPos);
      if (!blockstate.isAir() && !blockstate.is(Blocks.VINE)) {
         Direction direction = getRandomHorizontalDirection(pRandom);
         BlockPos blockpos = pPos.relative(direction);
         BlockState blockstate1 = pLevel.getBlockState(blockpos);
         if (blockstate1.isAir()) {
            if (Block.isFaceFull(blockstate.getCollisionShape(pLevel, pPos), direction)) {
               BooleanProperty booleanproperty = VineBlock.getPropertyForFace(direction.getOpposite());
               pLevel.setBlock(blockpos, Blocks.VINE.defaultBlockState().setValue(booleanproperty, Boolean.valueOf(true)), 3);
            }
         }
      }
   }

   private void maybeAddLeavesAbove(Random pRandom, LevelAccessor pLevel, BlockPos pPos) {
      if (pRandom.nextFloat() < 0.5F && pLevel.getBlockState(pPos).is(Blocks.NETHERRACK) && pLevel.getBlockState(pPos.above()).isAir()) {
         pLevel.setBlock(pPos.above(), Blocks.JUNGLE_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, Boolean.valueOf(true)), 3);
      }

   }

   private void addNetherrackDripColumnsBelowPortal(Random pRandom, LevelAccessor pLevel) {
      for(int i = this.boundingBox.minX() + 1; i < this.boundingBox.maxX(); ++i) {
         for(int j = this.boundingBox.minZ() + 1; j < this.boundingBox.maxZ(); ++j) {
            BlockPos blockpos = new BlockPos(i, this.boundingBox.minY(), j);
            if (pLevel.getBlockState(blockpos).is(Blocks.NETHERRACK)) {
               this.addNetherrackDripColumn(pRandom, pLevel, blockpos.below());
            }
         }
      }

   }

   private void addNetherrackDripColumn(Random pRandom, LevelAccessor pLevel, BlockPos pPos) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = pPos.mutable();
      this.placeNetherrackOrMagma(pRandom, pLevel, blockpos$mutableblockpos);
      int i = 8;

      while(i > 0 && pRandom.nextFloat() < 0.5F) {
         blockpos$mutableblockpos.move(Direction.DOWN);
         --i;
         this.placeNetherrackOrMagma(pRandom, pLevel, blockpos$mutableblockpos);
      }

   }

   private void spreadNetherrack(Random pRandom, LevelAccessor pLevel) {
      boolean flag = this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_LAND_SURFACE || this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR;
      BlockPos blockpos = this.boundingBox.getCenter();
      int i = blockpos.getX();
      int j = blockpos.getZ();
      float[] afloat = new float[]{1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 0.9F, 0.9F, 0.8F, 0.7F, 0.6F, 0.4F, 0.2F};
      int k = afloat.length;
      int l = (this.boundingBox.getXSpan() + this.boundingBox.getZSpan()) / 2;
      int i1 = pRandom.nextInt(Math.max(1, 8 - l / 2));
      int j1 = 3;
      BlockPos.MutableBlockPos blockpos$mutableblockpos = BlockPos.ZERO.mutable();

      for(int k1 = i - k; k1 <= i + k; ++k1) {
         for(int l1 = j - k; l1 <= j + k; ++l1) {
            int i2 = Math.abs(k1 - i) + Math.abs(l1 - j);
            int j2 = Math.max(0, i2 + i1);
            if (j2 < k) {
               float f = afloat[j2];
               if (pRandom.nextDouble() < (double)f) {
                  int k2 = getSurfaceY(pLevel, k1, l1, this.verticalPlacement);
                  int l2 = flag ? k2 : Math.min(this.boundingBox.minY(), k2);
                  blockpos$mutableblockpos.set(k1, l2, l1);
                  if (Math.abs(l2 - this.boundingBox.minY()) <= 3 && this.canBlockBeReplacedByNetherrackOrMagma(pLevel, blockpos$mutableblockpos)) {
                     this.placeNetherrackOrMagma(pRandom, pLevel, blockpos$mutableblockpos);
                     if (this.properties.overgrown) {
                        this.maybeAddLeavesAbove(pRandom, pLevel, blockpos$mutableblockpos);
                     }

                     this.addNetherrackDripColumn(pRandom, pLevel, blockpos$mutableblockpos.below());
                  }
               }
            }
         }
      }

   }

   private boolean canBlockBeReplacedByNetherrackOrMagma(LevelAccessor pLevel, BlockPos pPos) {
      BlockState blockstate = pLevel.getBlockState(pPos);
      return !blockstate.is(Blocks.AIR) && !blockstate.is(Blocks.OBSIDIAN) && !blockstate.is(BlockTags.FEATURES_CANNOT_REPLACE) && (this.verticalPlacement == RuinedPortalPiece.VerticalPlacement.IN_NETHER || !blockstate.is(Blocks.LAVA));
   }

   private void placeNetherrackOrMagma(Random pRandom, LevelAccessor pLevel, BlockPos pPos) {
      if (!this.properties.cold && pRandom.nextFloat() < 0.07F) {
         pLevel.setBlock(pPos, Blocks.MAGMA_BLOCK.defaultBlockState(), 3);
      } else {
         pLevel.setBlock(pPos, Blocks.NETHERRACK.defaultBlockState(), 3);
      }

   }

   private static int getSurfaceY(LevelAccessor pLevel, int pX, int pZ, RuinedPortalPiece.VerticalPlacement pPlacement) {
      return pLevel.getHeight(getHeightMapType(pPlacement), pX, pZ) - 1;
   }

   public static Heightmap.Types getHeightMapType(RuinedPortalPiece.VerticalPlacement pPlacement) {
      return pPlacement == RuinedPortalPiece.VerticalPlacement.ON_OCEAN_FLOOR ? Heightmap.Types.OCEAN_FLOOR_WG : Heightmap.Types.WORLD_SURFACE_WG;
   }

   private static ProcessorRule getBlockReplaceRule(Block pBlock, float pProbability, Block pReplaceBlock) {
      return new ProcessorRule(new RandomBlockMatchTest(pBlock, pProbability), AlwaysTrueTest.INSTANCE, pReplaceBlock.defaultBlockState());
   }

   private static ProcessorRule getBlockReplaceRule(Block pBlock, Block pReplaceBlock) {
      return new ProcessorRule(new BlockMatchTest(pBlock), AlwaysTrueTest.INSTANCE, pReplaceBlock.defaultBlockState());
   }

   public static class Properties {
      public static final Codec<RuinedPortalPiece.Properties> CODEC = RecordCodecBuilder.create((p_72752_) -> {
         return p_72752_.group(Codec.BOOL.fieldOf("cold").forGetter((p_163185_) -> {
            return p_163185_.cold;
         }), Codec.FLOAT.fieldOf("mossiness").forGetter((p_163183_) -> {
            return p_163183_.mossiness;
         }), Codec.BOOL.fieldOf("air_pocket").forGetter((p_163181_) -> {
            return p_163181_.airPocket;
         }), Codec.BOOL.fieldOf("overgrown").forGetter((p_163179_) -> {
            return p_163179_.overgrown;
         }), Codec.BOOL.fieldOf("vines").forGetter((p_163177_) -> {
            return p_163177_.vines;
         }), Codec.BOOL.fieldOf("replace_with_blackstone").forGetter((p_163175_) -> {
            return p_163175_.replaceWithBlackstone;
         })).apply(p_72752_, RuinedPortalPiece.Properties::new);
      });
      public boolean cold;
      public float mossiness = 0.2F;
      public boolean airPocket;
      public boolean overgrown;
      public boolean vines;
      public boolean replaceWithBlackstone;

      public Properties() {
      }

      public Properties(boolean p_72745_, float p_72746_, boolean p_72747_, boolean p_72748_, boolean p_72749_, boolean p_72750_) {
         this.cold = p_72745_;
         this.mossiness = p_72746_;
         this.airPocket = p_72747_;
         this.overgrown = p_72748_;
         this.vines = p_72749_;
         this.replaceWithBlackstone = p_72750_;
      }
   }

   public static enum VerticalPlacement {
      ON_LAND_SURFACE("on_land_surface"),
      PARTLY_BURIED("partly_buried"),
      ON_OCEAN_FLOOR("on_ocean_floor"),
      IN_MOUNTAIN("in_mountain"),
      UNDERGROUND("underground"),
      IN_NETHER("in_nether");

      private static final Map<String, RuinedPortalPiece.VerticalPlacement> BY_NAME = Arrays.stream(values()).collect(Collectors.toMap(RuinedPortalPiece.VerticalPlacement::getName, (p_72781_) -> {
         return p_72781_;
      }));
      private final String name;

      private VerticalPlacement(String p_72778_) {
         this.name = p_72778_;
      }

      public String getName() {
         return this.name;
      }

      public static RuinedPortalPiece.VerticalPlacement byName(String pName) {
         return BY_NAME.get(pName);
      }
   }
}