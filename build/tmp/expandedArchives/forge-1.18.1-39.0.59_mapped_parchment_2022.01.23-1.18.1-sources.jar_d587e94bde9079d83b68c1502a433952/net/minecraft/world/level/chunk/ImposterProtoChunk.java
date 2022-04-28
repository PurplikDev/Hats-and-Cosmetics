package net.minecraft.world.level.chunk;

import it.unimi.dsi.fastutil.longs.LongSet;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeResolver;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.ticks.BlackholeTickAccess;
import net.minecraft.world.ticks.TickContainerAccess;

/**
 * During world generation, adjacent chunks may be fully generated (and thus be level chunks), but are often needed in
 * proto chunk form. This wraps a completely generated chunk as a proto chunk.
 */
public class ImposterProtoChunk extends ProtoChunk {
   private final LevelChunk wrapped;
   private final boolean allowWrites;

   public ImposterProtoChunk(LevelChunk pWrapped, boolean pAllowWrites) {
      super(pWrapped.getPos(), UpgradeData.EMPTY, pWrapped.levelHeightAccessor, pWrapped.getLevel().registryAccess().registryOrThrow(Registry.BIOME_REGISTRY), pWrapped.getBlendingData());
      this.wrapped = pWrapped;
      this.allowWrites = pAllowWrites;
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pPos) {
      return this.wrapped.getBlockEntity(pPos);
   }

   public BlockState getBlockState(BlockPos pPos) {
      return this.wrapped.getBlockState(pPos);
   }

   public FluidState getFluidState(BlockPos pPos) {
      return this.wrapped.getFluidState(pPos);
   }

   public int getMaxLightLevel() {
      return this.wrapped.getMaxLightLevel();
   }

   public LevelChunkSection getSection(int p_187932_) {
      return this.allowWrites ? this.wrapped.getSection(p_187932_) : super.getSection(p_187932_);
   }

   @Nullable
   public BlockState setBlockState(BlockPos pPos, BlockState pState, boolean pIsMoving) {
      return this.allowWrites ? this.wrapped.setBlockState(pPos, pState, pIsMoving) : null;
   }

   public void setBlockEntity(BlockEntity pBlockEntity) {
      if (this.allowWrites) {
         this.wrapped.setBlockEntity(pBlockEntity);
      }

   }

   public void addEntity(Entity pEntity) {
      if (this.allowWrites) {
         this.wrapped.addEntity(pEntity);
      }

   }

   public void setStatus(ChunkStatus pStatus) {
      if (this.allowWrites) {
         super.setStatus(pStatus);
      }

   }

   public LevelChunkSection[] getSections() {
      return this.wrapped.getSections();
   }

   public void setHeightmap(Heightmap.Types pType, long[] pData) {
   }

   private Heightmap.Types fixType(Heightmap.Types pType) {
      if (pType == Heightmap.Types.WORLD_SURFACE_WG) {
         return Heightmap.Types.WORLD_SURFACE;
      } else {
         return pType == Heightmap.Types.OCEAN_FLOOR_WG ? Heightmap.Types.OCEAN_FLOOR : pType;
      }
   }

   public Heightmap getOrCreateHeightmapUnprimed(Heightmap.Types pType) {
      return this.wrapped.getOrCreateHeightmapUnprimed(pType);
   }

   public int getHeight(Heightmap.Types pType, int pX, int pZ) {
      return this.wrapped.getHeight(this.fixType(pType), pX, pZ);
   }

   /**
    * Gets the biome at the given quart positions.
    * Note that the coordinates passed into this method are 1/4 the scale of block coordinates. The noise biome is then
    * used by the {@link net.minecraft.world.level.biome.BiomeZoomer} to produce a biome for each unique position,
    * whilst only saving the biomes once per each 4x4x4 cube.
    */
   public Biome getNoiseBiome(int pX, int pY, int pZ) {
      return this.wrapped.getNoiseBiome(pX, pY, pZ);
   }

   public ChunkPos getPos() {
      return this.wrapped.getPos();
   }

   @Nullable
   public StructureStart<?> getStartForFeature(StructureFeature<?> pStructure) {
      return this.wrapped.getStartForFeature(pStructure);
   }

   public void setStartForFeature(StructureFeature<?> pStructure, StructureStart<?> pStart) {
   }

   public Map<StructureFeature<?>, StructureStart<?>> getAllStarts() {
      return this.wrapped.getAllStarts();
   }

   public void setAllStarts(Map<StructureFeature<?>, StructureStart<?>> pStructureStarts) {
   }

   public LongSet getReferencesForFeature(StructureFeature<?> pStructure) {
      return this.wrapped.getReferencesForFeature(pStructure);
   }

   public void addReferenceForFeature(StructureFeature<?> pStructure, long pReference) {
   }

   public Map<StructureFeature<?>, LongSet> getAllReferences() {
      return this.wrapped.getAllReferences();
   }

   public void setAllReferences(Map<StructureFeature<?>, LongSet> pStructureReferences) {
   }

   public void setUnsaved(boolean pUnsaved) {
   }

   public boolean isUnsaved() {
      return false;
   }

   public ChunkStatus getStatus() {
      return this.wrapped.getStatus();
   }

   public void removeBlockEntity(BlockPos pPos) {
   }

   public void markPosForPostprocessing(BlockPos pPos) {
   }

   public void setBlockEntityNbt(CompoundTag pTag) {
   }

   @Nullable
   public CompoundTag getBlockEntityNbt(BlockPos pPos) {
      return this.wrapped.getBlockEntityNbt(pPos);
   }

   @Nullable
   public CompoundTag getBlockEntityNbtForSaving(BlockPos pPos) {
      return this.wrapped.getBlockEntityNbtForSaving(pPos);
   }

   public Stream<BlockPos> getLights() {
      return this.wrapped.getLights();
   }

   public TickContainerAccess<Block> getBlockTicks() {
      return this.allowWrites ? this.wrapped.getBlockTicks() : BlackholeTickAccess.emptyContainer();
   }

   public TickContainerAccess<Fluid> getFluidTicks() {
      return this.allowWrites ? this.wrapped.getFluidTicks() : BlackholeTickAccess.emptyContainer();
   }

   public ChunkAccess.TicksToSave getTicksForSerialization() {
      return this.wrapped.getTicksForSerialization();
   }

   @Nullable
   public BlendingData getBlendingData() {
      return this.wrapped.getBlendingData();
   }

   public void setBlendingData(BlendingData p_187930_) {
      this.wrapped.setBlendingData(p_187930_);
   }

   public CarvingMask getCarvingMask(GenerationStep.Carving pStep) {
      if (this.allowWrites) {
         return super.getCarvingMask(pStep);
      } else {
         throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
      }
   }

   public CarvingMask getOrCreateCarvingMask(GenerationStep.Carving pStep) {
      if (this.allowWrites) {
         return super.getOrCreateCarvingMask(pStep);
      } else {
         throw (UnsupportedOperationException)Util.pauseInIde(new UnsupportedOperationException("Meaningless in this context"));
      }
   }

   public LevelChunk getWrapped() {
      return this.wrapped;
   }

   public boolean isLightCorrect() {
      return this.wrapped.isLightCorrect();
   }

   public void setLightCorrect(boolean pLightCorrect) {
      this.wrapped.setLightCorrect(pLightCorrect);
   }

   public void fillBiomesFromNoise(BiomeResolver p_187923_, Climate.Sampler p_187924_) {
      if (this.allowWrites) {
         this.wrapped.fillBiomesFromNoise(p_187923_, p_187924_);
      }

   }
}