package net.minecraft.world.level.levelgen.structure.pools;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.data.worldgen.ProcessorLists;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorList;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

public abstract class StructurePoolElement {
   public static final Codec<StructurePoolElement> CODEC = Registry.STRUCTURE_POOL_ELEMENT.byNameCodec().dispatch("element_type", StructurePoolElement::getType, StructurePoolElementType::codec);
   @Nullable
   private volatile StructureTemplatePool.Projection projection;

   protected static <E extends StructurePoolElement> RecordCodecBuilder<E, StructureTemplatePool.Projection> projectionCodec() {
      return StructureTemplatePool.Projection.CODEC.fieldOf("projection").forGetter(StructurePoolElement::getProjection);
   }

   protected StructurePoolElement(StructureTemplatePool.Projection pProjection) {
      this.projection = pProjection;
   }

   public abstract Vec3i getSize(StructureManager pStructureManager, Rotation pRotation);

   public abstract List<StructureTemplate.StructureBlockInfo> getShuffledJigsawBlocks(StructureManager pStructureManager, BlockPos pPos, Rotation pRotation, Random pRandom);

   public abstract BoundingBox getBoundingBox(StructureManager pStructureManager, BlockPos pPos, Rotation pRotation);

   public abstract boolean place(StructureManager p_210483_, WorldGenLevel p_210484_, StructureFeatureManager p_210485_, ChunkGenerator p_210486_, BlockPos p_210487_, BlockPos p_210488_, Rotation p_210489_, BoundingBox p_210490_, Random p_210491_, boolean p_210492_);

   public abstract StructurePoolElementType<?> getType();

   public void handleDataMarker(LevelAccessor pLevel, StructureTemplate.StructureBlockInfo pBlockInfo, BlockPos pPos, Rotation pRotation, Random pRandom, BoundingBox pBox) {
   }

   public StructurePoolElement setProjection(StructureTemplatePool.Projection pProjection) {
      this.projection = pProjection;
      return this;
   }

   public StructureTemplatePool.Projection getProjection() {
      StructureTemplatePool.Projection structuretemplatepool$projection = this.projection;
      if (structuretemplatepool$projection == null) {
         throw new IllegalStateException();
      } else {
         return structuretemplatepool$projection;
      }
   }

   public int getGroundLevelDelta() {
      return 1;
   }

   public static Function<StructureTemplatePool.Projection, EmptyPoolElement> empty() {
      return (p_210525_) -> {
         return EmptyPoolElement.INSTANCE;
      };
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String pKey) {
      return (p_210530_) -> {
         return new LegacySinglePoolElement(Either.left(new ResourceLocation(pKey)), ProcessorLists.EMPTY, p_210530_);
      };
   }

   public static Function<StructureTemplatePool.Projection, LegacySinglePoolElement> legacy(String pKey, Holder<StructureProcessorList> pProcessors) {
      return (p_210537_) -> {
         return new LegacySinglePoolElement(Either.left(new ResourceLocation(pKey)), pProcessors, p_210537_);
      };
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String pKey) {
      return (p_210511_) -> {
         return new SinglePoolElement(Either.left(new ResourceLocation(pKey)), ProcessorLists.EMPTY, p_210511_);
      };
   }

   public static Function<StructureTemplatePool.Projection, SinglePoolElement> single(String pKey, Holder<StructureProcessorList> pProcessors) {
      return (p_210518_) -> {
         return new SinglePoolElement(Either.left(new ResourceLocation(pKey)), pProcessors, p_210518_);
      };
   }

   public static Function<StructureTemplatePool.Projection, FeaturePoolElement> feature(Holder<PlacedFeature> pFeature) {
      return (p_210506_) -> {
         return new FeaturePoolElement(pFeature, p_210506_);
      };
   }

   public static Function<StructureTemplatePool.Projection, ListPoolElement> list(List<Function<StructureTemplatePool.Projection, ? extends StructurePoolElement>> pElements) {
      return (p_210523_) -> {
         return new ListPoolElement(pElements.stream().map((p_210482_) -> {
            return p_210482_.apply(p_210523_);
         }).collect(Collectors.toList()), p_210523_);
      };
   }
}