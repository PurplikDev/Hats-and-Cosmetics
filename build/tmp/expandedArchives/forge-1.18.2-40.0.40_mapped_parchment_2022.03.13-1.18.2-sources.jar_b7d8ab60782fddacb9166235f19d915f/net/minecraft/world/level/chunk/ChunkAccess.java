package net.minecraft.world.level.chunk;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortArrayList;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEventDispatcher;
import net.minecraft.world.level.levelgen.Aquifer;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.DensityFunctions;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.NoiseRouter;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.SerializableTickContainer;
import net.minecraft.world.ticks.TickContainerAccess;
import org.slf4j.Logger;

public abstract class ChunkAccess implements BlockGetter, BiomeManager.NoiseBiomeSource, FeatureAccess {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final LongSet EMPTY_REFERENCE_SET = new LongOpenHashSet();
   protected final ShortList[] postProcessing;
   protected volatile boolean unsaved;
   private volatile boolean isLightCorrect;
   protected final ChunkPos chunkPos;
   private long inhabitedTime;
   /** @deprecated */
   @Nullable
   @Deprecated
   private Holder<Biome> carverBiome;
   @Nullable
   protected NoiseChunk noiseChunk;
   protected final UpgradeData upgradeData;
   @Nullable
   protected BlendingData blendingData;
   protected final Map<Heightmap.Types, Heightmap> heightmaps = Maps.newEnumMap(Heightmap.Types.class);
   private final Map<ConfiguredStructureFeature<?, ?>, StructureStart> structureStarts = Maps.newHashMap();
   private final Map<ConfiguredStructureFeature<?, ?>, LongSet> structuresRefences = Maps.newHashMap();
   protected final Map<BlockPos, CompoundTag> pendingBlockEntities = Maps.newHashMap();
   protected final Map<BlockPos, BlockEntity> blockEntities = Maps.newHashMap();
   protected final LevelHeightAccessor levelHeightAccessor;
   protected final LevelChunkSection[] sections;

   public ChunkAccess(ChunkPos p_187621_, UpgradeData p_187622_, LevelHeightAccessor p_187623_, Registry<Biome> p_187624_, long p_187625_, @Nullable LevelChunkSection[] p_187626_, @Nullable BlendingData p_187627_) {
      this.chunkPos = p_187621_;
      this.upgradeData = p_187622_;
      this.levelHeightAccessor = p_187623_;
      this.sections = new LevelChunkSection[p_187623_.getSectionsCount()];
      this.inhabitedTime = p_187625_;
      this.postProcessing = new ShortList[p_187623_.getSectionsCount()];
      this.blendingData = p_187627_;
      if (p_187626_ != null) {
         if (this.sections.length == p_187626_.length) {
            System.arraycopy(p_187626_, 0, this.sections, 0, this.sections.length);
         } else {
            LOGGER.warn("Could not set level chunk sections, array length is {} instead of {}", p_187626_.length, this.sections.length);
         }
      }

      replaceMissingSections(p_187623_, p_187624_, this.sections);
   }

   private static void replaceMissingSections(LevelHeightAccessor p_187635_, Registry<Biome> p_187636_, LevelChunkSection[] p_187637_) {
      for(int i = 0; i < p_187637_.length; ++i) {
         if (p_187637_[i] == null) {
            p_187637_[i] = new LevelChunkSection(p_187635_.getSectionYFromSectionIndex(i), p_187636_);
         }
      }

   }

   public GameEventDispatcher getEventDispatcher(int pSectionY) {
      return GameEventDispatcher.NOOP;
   }

   @Nullable
   public abstract BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving);

   public abstract void setBlockEntity(BlockEntity pBlockEntity);

   public abstract void addEntity(Entity pEntity);

   @Nullable
   public LevelChunkSection getHighestSection() {
      LevelChunkSection[] alevelchunksection = this.getSections();

      for(int i = alevelchunksection.length - 1; i >= 0; --i) {
         LevelChunkSection levelchunksection = alevelchunksection[i];
         if (!levelchunksection.hasOnlyAir()) {
            return levelchunksection;
         }
      }

      return null;
   }

   public int getHighestSectionPosition() {
      LevelChunkSection levelchunksection = this.getHighestSection();
      return levelchunksection == null ? this.getMinBuildHeight() : levelchunksection.bottomBlockY();
   }

   public Set<BlockPos> getBlockEntitiesPos() {
      Set<BlockPos> set = Sets.newHashSet(this.pendingBlockEntities.keySet());
      set.addAll(this.blockEntities.keySet());
      return set;
   }

   public LevelChunkSection[] getSections() {
      return this.sections;
   }

   public LevelChunkSection getSection(int p_187657_) {
      return this.getSections()[p_187657_];
   }

   public Collection<Entry<Heightmap.Types, Heightmap>> getHeightmaps() {
      return Collections.unmodifiableSet(this.heightmaps.entrySet());
   }

   public void setHeightmap(Heightmap.Types pType, long[] pData) {
      this.getOrCreateHeightmapUnprimed(pType).setRawData(this, pType, pData);
   }

   public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types pType) {
      return this.heightmaps.computeIfAbsent(pType, (p_187665_) -> {
         return new Heightmap(this, p_187665_);
      });
   }

   public boolean hasPrimedHeightmap(Heightmap.Types p_187659_) {
      return this.heightmaps.get(p_187659_) != null;
   }

   public int getHeight(Heightmap.Types pType, int pX, int pZ) {
      Heightmap heightmap = this.heightmaps.get(pType);
      if (heightmap == null) {
         if (SharedConstants.IS_RUNNING_IN_IDE && this instanceof LevelChunk) {
            LOGGER.error("Unprimed heightmap: " + pType + " " + pX + " " + pZ);
         }

         Heightmap.primeHeightmaps(this, EnumSet.of(pType));
         heightmap = this.heightmaps.get(pType);
      }

      return heightmap.getFirstAvailable(pX & 15, pZ & 15) - 1;
   }

   public ChunkPos getPos() {
      return this.chunkPos;
   }

   @Nullable
   public StructureStart getStartForFeature(ConfiguredStructureFeature<?, ?> pStructure) {
      return this.structureStarts.get(pStructure);
   }

   public void setStartForFeature(ConfiguredStructureFeature<?, ?> pStructure, StructureStart pStart) {
      this.structureStarts.put(pStructure, pStart);
      this.unsaved = true;
   }

   public Map<ConfiguredStructureFeature<?, ?>, StructureStart> getAllStarts() {
      return Collections.unmodifiableMap(this.structureStarts);
   }

   public void setAllStarts(Map<ConfiguredStructureFeature<?, ?>, StructureStart> pStructureStarts) {
      this.structureStarts.clear();
      this.structureStarts.putAll(pStructureStarts);
      this.unsaved = true;
   }

   public LongSet getReferencesForFeature(ConfiguredStructureFeature<?, ?> pStructure) {
      return this.structuresRefences.getOrDefault(pStructure, EMPTY_REFERENCE_SET);
   }

   public void addReferenceForFeature(ConfiguredStructureFeature<?, ?> pStructure, long pReference) {
      this.structuresRefences.computeIfAbsent(pStructure, (p_207954_) -> {
         return new LongOpenHashSet();
      }).add(pReference);
      this.unsaved = true;
   }

   public Map<ConfiguredStructureFeature<?, ?>, LongSet> getAllReferences() {
      return Collections.unmodifiableMap(this.structuresRefences);
   }

   public void setAllReferences(Map<ConfiguredStructureFeature<?, ?>, LongSet> pStructureReferences) {
      this.structuresRefences.clear();
      this.structuresRefences.putAll(pStructureReferences);
      this.unsaved = true;
   }

   public boolean isYSpaceEmpty(int pStartY, int pEndY) {
      if (pStartY < this.getMinBuildHeight()) {
         pStartY = this.getMinBuildHeight();
      }

      if (pEndY >= this.getMaxBuildHeight()) {
         pEndY = this.getMaxBuildHeight() - 1;
      }

      for(int i = pStartY; i <= pEndY; i += 16) {
         if (!this.getSection(this.getSectionIndex(i)).hasOnlyAir()) {
            return false;
         }
      }

      return true;
   }

   public void setUnsaved(boolean pUnsaved) {
      this.unsaved = pUnsaved;
   }

   public boolean isUnsaved() {
      return this.unsaved;
   }

   public abstract ChunkStatus getStatus();

   public abstract void removeBlockEntity(BlockPos pPos);

   public void markPosForPostprocessing(BlockPos pPos) {
      LOGGER.warn("Trying to mark a block for PostProcessing @ {}, but this operation is not supported.", (Object)pPos);
   }

   public ShortList[] getPostProcessing() {
      return this.postProcessing;
   }

   public void addPackedPostProcess(short pPackedPosition, int pIndex) {
      getOrCreateOffsetList(this.getPostProcessing(), pIndex).add(pPackedPosition);
   }

   public void setBlockEntityNbt(CompoundTag pTag) {
      this.pendingBlockEntities.put(BlockEntity.getPosFromTag(pTag), pTag);
   }

   @Nullable
   public CompoundTag getBlockEntityNbt(BlockPos pPos) {
      return this.pendingBlockEntities.get(pPos);
   }

   @Nullable
   public abstract CompoundTag getBlockEntityNbtForSaving(BlockPos pPos);

   public abstract Stream<BlockPos> getLights();

   public abstract TickContainerAccess<Block> getBlockTicks();

   public abstract TickContainerAccess<Fluid> getFluidTicks();

   public abstract ChunkAccess.TicksToSave getTicksForSerialization();

   public UpgradeData getUpgradeData() {
      return this.upgradeData;
   }

   public boolean isOldNoiseGeneration() {
      return this.blendingData != null && this.blendingData.oldNoise();
   }

   @Nullable
   public BlendingData getBlendingData() {
      return this.blendingData;
   }

   public void setBlendingData(BlendingData p_187646_) {
      this.blendingData = p_187646_;
   }

   public long getInhabitedTime() {
      return this.inhabitedTime;
   }

   public void incrementInhabitedTime(long p_187633_) {
      this.inhabitedTime += p_187633_;
   }

   public void setInhabitedTime(long pInhabitedTime) {
      this.inhabitedTime = pInhabitedTime;
   }

   public static ShortList getOrCreateOffsetList(ShortList[] pPackedPositions, int pIndex) {
      if (pPackedPositions[pIndex] == null) {
         pPackedPositions[pIndex] = new ShortArrayList();
      }

      return pPackedPositions[pIndex];
   }

   public boolean isLightCorrect() {
      return this.isLightCorrect;
   }

   public void setLightCorrect(boolean pLightCorrect) {
      this.isLightCorrect = pLightCorrect;
      this.setUnsaved(true);
   }

   public int getMinBuildHeight() {
      return this.levelHeightAccessor.getMinBuildHeight();
   }

   public int getHeight() {
      return this.levelHeightAccessor.getHeight();
   }

   public NoiseChunk getOrCreateNoiseChunk(NoiseRouter p_207938_, Supplier<DensityFunctions.BeardifierOrMarker> p_207939_, NoiseGeneratorSettings p_207940_, Aquifer.FluidPicker p_207941_, Blender p_207942_) {
      if (this.noiseChunk == null) {
         this.noiseChunk = NoiseChunk.forChunk(this, p_207938_, p_207939_, p_207940_, p_207941_, p_207942_);
      }

      return this.noiseChunk;
   }

   /** @deprecated */
   @Deprecated
   public Holder<Biome> carverBiome(Supplier<Holder<Biome>> p_204345_) {
      if (this.carverBiome == null) {
         this.carverBiome = p_204345_.get();
      }

      return this.carverBiome;
   }

   /**
    * Gets the biome at the given quart positions.
    * Note that the coordinates passed into this method are 1/4 the scale of block coordinates. The noise biome is then
    * used by the {@link net.minecraft.world.level.biome.BiomeZoomer} to produce a biome for each unique position,
    * whilst only saving the biomes once per each 4x4x4 cube.
    */
   public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
      try {
         int i = QuartPos.fromBlock(this.getMinBuildHeight());
         int k = i + QuartPos.fromBlock(this.getHeight()) - 1;
         int l = Mth.clamp(pY, i, k);
         int j = this.getSectionIndex(QuartPos.toBlock(l));
         return this.sections[j].getNoiseBiome(pX & 3, l & 3, pZ & 3);
      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Getting biome");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Biome being got");
         crashreportcategory.setDetail("Location", () -> {
            return CrashReportCategory.formatLocation(this, pX, pY, pZ);
         });
         throw new ReportedException(crashreport);
      }
   }

   public void fillBiomesFromNoise(BiomeResolver p_187638_, Climate.Sampler p_187639_) {
      ChunkPos chunkpos = this.getPos();
      int i = QuartPos.fromBlock(chunkpos.getMinBlockX());
      int j = QuartPos.fromBlock(chunkpos.getMinBlockZ());
      LevelHeightAccessor levelheightaccessor = this.getHeightAccessorForGeneration();

      for(int k = levelheightaccessor.getMinSection(); k < levelheightaccessor.getMaxSection(); ++k) {
         LevelChunkSection levelchunksection = this.getSection(this.getSectionIndexFromSectionY(k));
         levelchunksection.fillBiomesFromNoise(p_187638_, p_187639_, i, j);
      }

   }

   public boolean hasAnyStructureReferences() {
      return !this.getAllReferences().isEmpty();
   }

   @Nullable
   public BelowZeroRetrogen getBelowZeroRetrogen() {
      return null;
   }

   public boolean isUpgrading() {
      return this.getBelowZeroRetrogen() != null;
   }

   public LevelHeightAccessor getHeightAccessorForGeneration() {
      return this;
   }

   public static record TicksToSave(SerializableTickContainer<Block> blocks, SerializableTickContainer<Fluid> fluids) {
   }

   @Nullable
   public net.minecraft.world.level.LevelAccessor getWorldForge() { return null; }
}
