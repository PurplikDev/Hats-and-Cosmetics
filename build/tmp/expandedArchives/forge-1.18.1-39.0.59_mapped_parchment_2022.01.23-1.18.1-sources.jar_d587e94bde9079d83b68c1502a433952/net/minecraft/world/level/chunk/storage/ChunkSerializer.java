package net.minecraft.world.level.chunk.storage;

import com.google.common.collect.Maps;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.shorts.ShortList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.ShortTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.CarvingMask;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.ImposterProtoChunk;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.levelgen.BelowZeroRetrogen;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.blending.BlendingData;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.ticks.LevelChunkTicks;
import net.minecraft.world.ticks.ProtoChunkTicks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ChunkSerializer {
   private static final Codec<PalettedContainer<BlockState>> BLOCK_STATE_CODEC = PalettedContainer.codec(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
   private static final Logger LOGGER = LogManager.getLogger();
   private static final String TAG_UPGRADE_DATA = "UpgradeData";
   private static final String BLOCK_TICKS_TAG = "block_ticks";
   private static final String FLUID_TICKS_TAG = "fluid_ticks";

   public static ProtoChunk read(ServerLevel pLvel, PoiManager pPoiManager, ChunkPos pPos, CompoundTag pTag) {
      ChunkPos chunkpos = new ChunkPos(pTag.getInt("xPos"), pTag.getInt("zPos"));
      if (!Objects.equals(pPos, chunkpos)) {
         LOGGER.error("Chunk file at {} is in the wrong location; relocating. (Expected {}, got {})", pPos, pPos, chunkpos);
      }

      UpgradeData upgradedata = pTag.contains("UpgradeData", 10) ? new UpgradeData(pTag.getCompound("UpgradeData"), pLvel) : UpgradeData.EMPTY;
      boolean flag = pTag.getBoolean("isLightOn");
      ListTag listtag = pTag.getList("sections", 10);
      int i = pLvel.getSectionsCount();
      LevelChunkSection[] alevelchunksection = new LevelChunkSection[i];
      boolean flag1 = pLvel.dimensionType().hasSkyLight();
      ChunkSource chunksource = pLvel.getChunkSource();
      LevelLightEngine levellightengine = chunksource.getLightEngine();
      if (flag) {
         levellightengine.retainData(pPos, true);
      }

      Registry<Biome> registry = pLvel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
      Codec<PalettedContainer<Biome>> codec = makeBiomeCodec(registry);

      for(int j = 0; j < listtag.size(); ++j) {
         CompoundTag compoundtag = listtag.getCompound(j);
         int k = compoundtag.getByte("Y");
         int l = pLvel.getSectionIndexFromSectionY(k);
         if (l >= 0 && l < alevelchunksection.length) {
            PalettedContainer<BlockState> palettedcontainer;
            if (compoundtag.contains("block_states", 10)) {
               palettedcontainer = BLOCK_STATE_CODEC.parse(NbtOps.INSTANCE, compoundtag.getCompound("block_states")).promotePartial((p_188283_) -> {
                  logErrors(pPos, k, p_188283_);
               }).getOrThrow(false, LOGGER::error);
            } else {
               palettedcontainer = new PalettedContainer<>(Block.BLOCK_STATE_REGISTRY, Blocks.AIR.defaultBlockState(), PalettedContainer.Strategy.SECTION_STATES);
            }

            PalettedContainer<Biome> palettedcontainer1;
            if (compoundtag.contains("biomes", 10)) {
               palettedcontainer1 = codec.parse(NbtOps.INSTANCE, compoundtag.getCompound("biomes")).promotePartial((p_188274_) -> {
                  logErrors(pPos, k, p_188274_);
               }).getOrThrow(false, LOGGER::error);
            } else {
               palettedcontainer1 = new PalettedContainer<>(registry, registry.getOrThrow(Biomes.PLAINS), PalettedContainer.Strategy.SECTION_BIOMES);
            }

            LevelChunkSection levelchunksection = new LevelChunkSection(k, palettedcontainer, palettedcontainer1);
            alevelchunksection[l] = levelchunksection;
            pPoiManager.checkConsistencyWithBlocks(pPos, levelchunksection);
         }

         if (flag) {
            if (compoundtag.contains("BlockLight", 7)) {
               levellightengine.queueSectionData(LightLayer.BLOCK, SectionPos.of(pPos, k), new DataLayer(compoundtag.getByteArray("BlockLight")), true);
            }

            if (flag1 && compoundtag.contains("SkyLight", 7)) {
               levellightengine.queueSectionData(LightLayer.SKY, SectionPos.of(pPos, k), new DataLayer(compoundtag.getByteArray("SkyLight")), true);
            }
         }
      }

      long j1 = pTag.getLong("InhabitedTime");
      ChunkStatus.ChunkType chunkstatus$chunktype = getChunkTypeFromTag(pTag);
      BlendingData blendingdata;
      if (pTag.contains("blending_data", 10)) {
         blendingdata = BlendingData.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, pTag.getCompound("blending_data"))).resultOrPartial(LOGGER::error).orElse((BlendingData)null);
      } else {
         blendingdata = null;
      }

      ChunkAccess chunkaccess;
      if (chunkstatus$chunktype == ChunkStatus.ChunkType.LEVELCHUNK) {
         LevelChunkTicks<Block> levelchunkticks = LevelChunkTicks.load(pTag.getList("block_ticks", 10), (p_188287_) -> {
            return Registry.BLOCK.getOptional(ResourceLocation.tryParse(p_188287_));
         }, pPos);
         LevelChunkTicks<Fluid> levelchunkticks1 = LevelChunkTicks.load(pTag.getList("fluid_ticks", 10), (p_188285_) -> {
            return Registry.FLUID.getOptional(ResourceLocation.tryParse(p_188285_));
         }, pPos);
         chunkaccess = new LevelChunk(pLvel.getLevel(), pPos, upgradedata, levelchunkticks, levelchunkticks1, j1, alevelchunksection, postLoadChunk(pLvel, pTag), blendingdata);
         if (pTag.contains("ForgeCaps")) ((LevelChunk)chunkaccess).readCapsFromNBT(pTag.getCompound("ForgeCaps"));
      } else {
         ProtoChunkTicks<Block> protochunkticks = ProtoChunkTicks.load(pTag.getList("block_ticks", 10), (p_196906_) -> {
            return Registry.BLOCK.getOptional(ResourceLocation.tryParse(p_196906_));
         }, pPos);
         ProtoChunkTicks<Fluid> protochunkticks1 = ProtoChunkTicks.load(pTag.getList("fluid_ticks", 10), (p_188276_) -> {
            return Registry.FLUID.getOptional(ResourceLocation.tryParse(p_188276_));
         }, pPos);
         ProtoChunk protochunk = new ProtoChunk(pPos, upgradedata, alevelchunksection, protochunkticks, protochunkticks1, pLvel, registry, blendingdata);
         chunkaccess = protochunk;
         protochunk.setInhabitedTime(j1);
         if (pTag.contains("below_zero_retrogen", 10)) {
            BelowZeroRetrogen.CODEC.parse(new Dynamic<>(NbtOps.INSTANCE, pTag.getCompound("below_zero_retrogen"))).resultOrPartial(LOGGER::error).ifPresent(protochunk::setBelowZeroRetrogen);
         }

         ChunkStatus chunkstatus = ChunkStatus.byName(pTag.getString("Status"));
         protochunk.setStatus(chunkstatus);
         if (chunkstatus.isOrAfter(ChunkStatus.FEATURES)) {
            protochunk.setLightEngine(levellightengine);
         }

         BelowZeroRetrogen belowzeroretrogen = protochunk.getBelowZeroRetrogen();
         boolean flag2 = chunkstatus.isOrAfter(ChunkStatus.LIGHT) || belowzeroretrogen != null && belowzeroretrogen.targetStatus().isOrAfter(ChunkStatus.LIGHT);
         if (!flag && flag2) {
            for(BlockPos blockpos : BlockPos.betweenClosed(pPos.getMinBlockX(), pLvel.getMinBuildHeight(), pPos.getMinBlockZ(), pPos.getMaxBlockX(), pLvel.getMaxBuildHeight() - 1, pPos.getMaxBlockZ())) {
               if (chunkaccess.getBlockState(blockpos).getLightEmission(chunkaccess, blockpos) != 0) {
                  protochunk.addLight(blockpos);
               }
            }
         }
      }

      chunkaccess.setLightCorrect(flag);
      CompoundTag compoundtag2 = pTag.getCompound("Heightmaps");
      EnumSet<Heightmap.Types> enumset = EnumSet.noneOf(Heightmap.Types.class);

      for(Heightmap.Types heightmap$types : chunkaccess.getStatus().heightmapsAfter()) {
         String s = heightmap$types.getSerializationKey();
         if (compoundtag2.contains(s, 12)) {
            chunkaccess.setHeightmap(heightmap$types, compoundtag2.getLongArray(s));
         } else {
            enumset.add(heightmap$types);
         }
      }

      Heightmap.primeHeightmaps(chunkaccess, enumset);
      CompoundTag compoundtag3 = pTag.getCompound("structures");
      chunkaccess.setAllStarts(unpackStructureStart(StructurePieceSerializationContext.fromLevel(pLvel), compoundtag3, pLvel.getSeed()));
      net.minecraftforge.common.ForgeHooks.fixNullStructureReferences(chunkaccess, unpackStructureReferences(pPos, compoundtag3));
      if (pTag.getBoolean("shouldSave")) {
         chunkaccess.setUnsaved(true);
      }

      ListTag listtag2 = pTag.getList("PostProcessing", 9);

      for(int k1 = 0; k1 < listtag2.size(); ++k1) {
         ListTag listtag3 = listtag2.getList(k1);

         for(int l1 = 0; l1 < listtag3.size(); ++l1) {
            chunkaccess.addPackedPostProcess(listtag3.getShort(l1), k1);
         }
      }

      if (chunkstatus$chunktype == ChunkStatus.ChunkType.LEVELCHUNK) {
         net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(chunkaccess, pTag, chunkstatus$chunktype));
         return new ImposterProtoChunk((LevelChunk)chunkaccess, false);
      } else {
         ProtoChunk protochunk1 = (ProtoChunk)chunkaccess;
         ListTag listtag4 = pTag.getList("entities", 10);

         for(int i2 = 0; i2 < listtag4.size(); ++i2) {
            protochunk1.addEntity(listtag4.getCompound(i2));
         }

         ListTag listtag5 = pTag.getList("block_entities", 10);

         for(int j2 = 0; j2 < listtag5.size(); ++j2) {
            CompoundTag compoundtag1 = listtag5.getCompound(j2);
            chunkaccess.setBlockEntityNbt(compoundtag1);
         }

         ListTag listtag6 = pTag.getList("Lights", 9);

         for(int k2 = 0; k2 < listtag6.size(); ++k2) {
            ListTag listtag1 = listtag6.getList(k2);

            for(int i1 = 0; i1 < listtag1.size(); ++i1) {
               protochunk1.addLight(listtag1.getShort(i1), k2);
            }
         }

         CompoundTag compoundtag4 = pTag.getCompound("CarvingMasks");

         for(String s1 : compoundtag4.getAllKeys()) {
            GenerationStep.Carving generationstep$carving = GenerationStep.Carving.valueOf(s1);
            protochunk1.setCarvingMask(generationstep$carving, new CarvingMask(compoundtag4.getLongArray(s1), chunkaccess.getMinBuildHeight()));
         }

         net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.world.ChunkDataEvent.Load(chunkaccess, pTag, chunkstatus$chunktype));
         return protochunk1;
      }
   }

   private static void logErrors(ChunkPos p_188240_, int p_188241_, String p_188242_) {
      LOGGER.error("Recoverable errors when loading section [" + p_188240_.x + ", " + p_188241_ + ", " + p_188240_.z + "]: " + p_188242_);
   }

   private static Codec<PalettedContainer<Biome>> makeBiomeCodec(Registry<Biome> p_188261_) {
      return PalettedContainer.codec(p_188261_, p_188261_.byNameCodec(), PalettedContainer.Strategy.SECTION_BIOMES, p_188261_.getOrThrow(Biomes.PLAINS));
   }

   public static CompoundTag write(ServerLevel pLevel, ChunkAccess pChunk) {
      ChunkPos chunkpos = pChunk.getPos();
      CompoundTag compoundtag = new CompoundTag();
      compoundtag.putInt("DataVersion", SharedConstants.getCurrentVersion().getWorldVersion());
      compoundtag.putInt("xPos", chunkpos.x);
      compoundtag.putInt("yPos", pChunk.getMinSection());
      compoundtag.putInt("zPos", chunkpos.z);
      compoundtag.putLong("LastUpdate", pLevel.getGameTime());
      compoundtag.putLong("InhabitedTime", pChunk.getInhabitedTime());
      compoundtag.putString("Status", pChunk.getStatus().getName());
      BlendingData blendingdata = pChunk.getBlendingData();
      if (blendingdata != null) {
         BlendingData.CODEC.encodeStart(NbtOps.INSTANCE, blendingdata).resultOrPartial(LOGGER::error).ifPresent((p_196909_) -> {
            compoundtag.put("blending_data", p_196909_);
         });
      }

      BelowZeroRetrogen belowzeroretrogen = pChunk.getBelowZeroRetrogen();
      if (belowzeroretrogen != null) {
         BelowZeroRetrogen.CODEC.encodeStart(NbtOps.INSTANCE, belowzeroretrogen).resultOrPartial(LOGGER::error).ifPresent((p_188279_) -> {
            compoundtag.put("below_zero_retrogen", p_188279_);
         });
      }

      UpgradeData upgradedata = pChunk.getUpgradeData();
      if (!upgradedata.isEmpty()) {
         compoundtag.put("UpgradeData", upgradedata.write());
      }

      LevelChunkSection[] alevelchunksection = pChunk.getSections();
      ListTag listtag = new ListTag();
      LevelLightEngine levellightengine = pLevel.getChunkSource().getLightEngine();
      Registry<Biome> registry = pLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
      Codec<PalettedContainer<Biome>> codec = makeBiomeCodec(registry);
      boolean flag = pChunk.isLightCorrect();

      for(int i = levellightengine.getMinLightSection(); i < levellightengine.getMaxLightSection(); ++i) {
         int j = pChunk.getSectionIndexFromSectionY(i);
         boolean flag1 = j >= 0 && j < alevelchunksection.length;
         DataLayer datalayer = levellightengine.getLayerListener(LightLayer.BLOCK).getDataLayerData(SectionPos.of(chunkpos, i));
         DataLayer datalayer1 = levellightengine.getLayerListener(LightLayer.SKY).getDataLayerData(SectionPos.of(chunkpos, i));
         if (flag1 || datalayer != null || datalayer1 != null) {
            CompoundTag compoundtag1 = new CompoundTag();
            if (flag1) {
               LevelChunkSection levelchunksection = alevelchunksection[j];
               compoundtag1.put("block_states", BLOCK_STATE_CODEC.encodeStart(NbtOps.INSTANCE, levelchunksection.getStates()).getOrThrow(false, LOGGER::error));
               compoundtag1.put("biomes", codec.encodeStart(NbtOps.INSTANCE, levelchunksection.getBiomes()).getOrThrow(false, LOGGER::error));
            }

            if (datalayer != null && !datalayer.isEmpty()) {
               compoundtag1.putByteArray("BlockLight", datalayer.getData());
            }

            if (datalayer1 != null && !datalayer1.isEmpty()) {
               compoundtag1.putByteArray("SkyLight", datalayer1.getData());
            }

            if (!compoundtag1.isEmpty()) {
               compoundtag1.putByte("Y", (byte)i);
               listtag.add(compoundtag1);
            }
         }
      }

      compoundtag.put("sections", listtag);
      if (flag) {
         compoundtag.putBoolean("isLightOn", true);
      }

      ListTag listtag1 = new ListTag();

      for(BlockPos blockpos : pChunk.getBlockEntitiesPos()) {
         CompoundTag compoundtag3 = pChunk.getBlockEntityNbtForSaving(blockpos);
         if (compoundtag3 != null) {
            listtag1.add(compoundtag3);
         }
      }

      compoundtag.put("block_entities", listtag1);
      if (pChunk.getStatus().getChunkType() == ChunkStatus.ChunkType.PROTOCHUNK) {
         ProtoChunk protochunk = (ProtoChunk)pChunk;
         ListTag listtag2 = new ListTag();
         listtag2.addAll(protochunk.getEntities());
         compoundtag.put("entities", listtag2);
         compoundtag.put("Lights", packOffsets(protochunk.getPackedLights()));
         CompoundTag compoundtag4 = new CompoundTag();

         for(GenerationStep.Carving generationstep$carving : GenerationStep.Carving.values()) {
            CarvingMask carvingmask = protochunk.getCarvingMask(generationstep$carving);
            if (carvingmask != null) {
               compoundtag4.putLongArray(generationstep$carving.toString(), carvingmask.toArray());
            }
         }

         compoundtag.put("CarvingMasks", compoundtag4);
      }
      else {
          LevelChunk levelChunk = (LevelChunk) pChunk;
          try {
              final CompoundTag capTag = levelChunk.writeCapsToNBT();
              if (capTag != null) compoundtag.put("ForgeCaps", capTag);
          } catch (Exception exception) {
              LogManager.getLogger().error("A capability provider has thrown an exception trying to write state. It will not persist. Report this to the mod author", exception);
          }
      }

      saveTicks(pLevel, compoundtag, pChunk.getTicksForSerialization());
      compoundtag.put("PostProcessing", packOffsets(pChunk.getPostProcessing()));
      CompoundTag compoundtag2 = new CompoundTag();

      for(Entry<Heightmap.Types, Heightmap> entry : pChunk.getHeightmaps()) {
         if (pChunk.getStatus().heightmapsAfter().contains(entry.getKey())) {
            compoundtag2.put(entry.getKey().getSerializationKey(), new LongArrayTag(entry.getValue().getRawData()));
         }
      }

      compoundtag.put("Heightmaps", compoundtag2);
      compoundtag.put("structures", packStructureData(StructurePieceSerializationContext.fromLevel(pLevel), chunkpos, pChunk.getAllStarts(), pChunk.getAllReferences()));
      return compoundtag;
   }

   private static void saveTicks(ServerLevel p_188236_, CompoundTag p_188237_, ChunkAccess.TicksToSave p_188238_) {
      long i = p_188236_.getLevelData().getGameTime();
      p_188237_.put("block_ticks", p_188238_.blocks().save(i, (p_196894_) -> {
         return Registry.BLOCK.getKey(p_196894_).toString();
      }));
      p_188237_.put("fluid_ticks", p_188238_.fluids().save(i, (p_196896_) -> {
         return Registry.FLUID.getKey(p_196896_).toString();
      }));
   }

   public static ChunkStatus.ChunkType getChunkTypeFromTag(@Nullable CompoundTag pChunkNBT) {
      return pChunkNBT != null ? ChunkStatus.byName(pChunkNBT.getString("Status")).getChunkType() : ChunkStatus.ChunkType.PROTOCHUNK;
   }

   @Nullable
   private static LevelChunk.PostLoadProcessor postLoadChunk(ServerLevel pLevel, CompoundTag pTag) {
      ListTag listtag = getListOfCompoundsOrNull(pTag, "entities");
      ListTag listtag1 = getListOfCompoundsOrNull(pTag, "block_entities");
      return listtag == null && listtag1 == null ? null : (p_196904_) -> {
         if (listtag != null) {
            pLevel.addLegacyChunkEntities(EntityType.loadEntitiesRecursive(listtag, pLevel));
         }

         if (listtag1 != null) {
            for(int i = 0; i < listtag1.size(); ++i) {
               CompoundTag compoundtag = listtag1.getCompound(i);
               boolean flag = compoundtag.getBoolean("keepPacked");
               if (flag) {
                  p_196904_.setBlockEntityNbt(compoundtag);
               } else {
                  BlockPos blockpos = BlockEntity.getPosFromTag(compoundtag);
                  BlockEntity blockentity = BlockEntity.loadStatic(blockpos, p_196904_.getBlockState(blockpos), compoundtag);
                  if (blockentity != null) {
                     p_196904_.setBlockEntity(blockentity);
                  }
               }
            }
         }

      };
   }

   @Nullable
   private static ListTag getListOfCompoundsOrNull(CompoundTag p_196898_, String p_196899_) {
      ListTag listtag = p_196898_.getList(p_196899_, 10);
      return listtag.isEmpty() ? null : listtag;
   }

   private static CompoundTag packStructureData(StructurePieceSerializationContext pContext, ChunkPos pPos, Map<StructureFeature<?>, StructureStart<?>> pStructureMap, Map<StructureFeature<?>, LongSet> pReferenceMap) {
      CompoundTag compoundtag = new CompoundTag();
      CompoundTag compoundtag1 = new CompoundTag();

      for(Entry<StructureFeature<?>, StructureStart<?>> entry : pStructureMap.entrySet()) {
         compoundtag1.put(entry.getKey().getFeatureName(), entry.getValue().createTag(pContext, pPos));
      }

      compoundtag.put("starts", compoundtag1);
      CompoundTag compoundtag2 = new CompoundTag();

      for(Entry<StructureFeature<?>, LongSet> entry1 : pReferenceMap.entrySet()) {
         compoundtag2.put(entry1.getKey().getFeatureName(), new LongArrayTag(entry1.getValue()));
      }

      compoundtag.put("References", compoundtag2);
      return compoundtag;
   }

   private static Map<StructureFeature<?>, StructureStart<?>> unpackStructureStart(StructurePieceSerializationContext pContext, CompoundTag pTag, long pSeed) {
      Map<StructureFeature<?>, StructureStart<?>> map = Maps.newHashMap();
      CompoundTag compoundtag = pTag.getCompound("starts");

      for(String s : compoundtag.getAllKeys()) {
         String s1 = s.toLowerCase(Locale.ROOT);
         StructureFeature<?> structurefeature = StructureFeature.STRUCTURES_REGISTRY.get(s1);
         if (structurefeature == null) {
            LOGGER.error("Unknown structure start: {}", (Object)s1);
         } else {
            StructureStart<?> structurestart = StructureFeature.loadStaticStart(pContext, compoundtag.getCompound(s), pSeed);
            if (structurestart != null) {
               map.put(structurefeature, structurestart);
            }
         }
      }

      return map;
   }

   private static Map<StructureFeature<?>, LongSet> unpackStructureReferences(ChunkPos pPos, CompoundTag pTag) {
      Map<StructureFeature<?>, LongSet> map = Maps.newHashMap();
      CompoundTag compoundtag = pTag.getCompound("References");

      for(String s : compoundtag.getAllKeys()) {
         String s1 = s.toLowerCase(Locale.ROOT);
         StructureFeature<?> structurefeature = StructureFeature.STRUCTURES_REGISTRY.get(s1);
         if (structurefeature == null) {
            LOGGER.warn("Found reference to unknown structure '{}' in chunk {}, discarding", s1, pPos);
         } else {
            map.put(structurefeature, new LongOpenHashSet(Arrays.stream(compoundtag.getLongArray(s)).filter((p_188246_) -> {
               ChunkPos chunkpos = new ChunkPos(p_188246_);
               if (chunkpos.getChessboardDistance(pPos) > 8) {
                  LOGGER.warn("Found invalid structure reference [ {} @ {} ] for chunk {}.", s1, chunkpos, pPos);
                  return false;
               } else {
                  return true;
               }
            }).toArray()));
         }
      }

      return map;
   }

   public static ListTag packOffsets(ShortList[] pList) {
      ListTag listtag = new ListTag();

      for(ShortList shortlist : pList) {
         ListTag listtag1 = new ListTag();
         if (shortlist != null) {
            for(Short oshort : shortlist) {
               listtag1.add(ShortTag.valueOf(oshort));
            }
         }

         listtag.add(listtag1);
      }

      return listtag;
   }
}
