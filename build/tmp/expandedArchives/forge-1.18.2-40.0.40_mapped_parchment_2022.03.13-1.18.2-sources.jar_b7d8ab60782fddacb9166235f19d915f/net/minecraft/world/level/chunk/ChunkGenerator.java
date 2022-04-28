package net.minecraft.world.level.chunk;

import com.google.common.base.Stopwatch;
import com.mojang.datafixers.Products.P1;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import com.mojang.serialization.codecs.RecordCodecBuilder.Mu;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureFeatureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.levelgen.DebugLevelSource;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureCheckResult;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.StructureSpawnOverride;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.slf4j.Logger;

public abstract class ChunkGenerator implements BiomeManager.NoiseBiomeSource {
   private static final Logger LOGGER;
   public static final Codec<ChunkGenerator> CODEC;
   protected final Registry<StructureSet> structureSets;
   protected final BiomeSource biomeSource;
   protected final BiomeSource runtimeBiomeSource;
   protected final Optional<HolderSet<StructureSet>> structureOverrides;
   private final Map<ConfiguredStructureFeature<?, ?>, List<StructurePlacement>> placementsForFeature = new Object2ObjectOpenHashMap<>();
   private final Map<ConcentricRingsStructurePlacement, CompletableFuture<List<ChunkPos>>> ringPositions = new Object2ObjectArrayMap<>();
   private boolean hasGeneratedPositions;
   /** @deprecated */
   @Deprecated
   private final long ringPlacementSeed;

   protected static final <T extends ChunkGenerator> P1<Mu<T>, Registry<StructureSet>> commonCodec(Instance<T> p_208006_) {
      return p_208006_.group(RegistryOps.retrieveRegistry(Registry.STRUCTURE_SET_REGISTRY).forGetter((p_208008_) -> {
         return p_208008_.structureSets;
      }));
   }

   public ChunkGenerator(Registry<StructureSet> pStructureSets, Optional<HolderSet<StructureSet>> pStructureOverrides, BiomeSource pBiomeSource) {
      this(pStructureSets, pStructureOverrides, pBiomeSource, pBiomeSource, 0L);
   }

   public ChunkGenerator(Registry<StructureSet> pStructureSets, Optional<HolderSet<StructureSet>> pStructureOverrides, BiomeSource pBiomeSource, BiomeSource pRuntimeBiomeSource, long pRingPlacementSeed) {
      this.structureSets = pStructureSets;
      this.biomeSource = pBiomeSource;
      this.runtimeBiomeSource = pRuntimeBiomeSource;
      this.structureOverrides = pStructureOverrides;
      this.ringPlacementSeed = pRingPlacementSeed;
   }

   public Stream<Holder<StructureSet>> possibleStructureSets() {
      return this.structureOverrides.isPresent() ? this.structureOverrides.get().stream() : this.structureSets.holders().map(Holder::hackyErase);
   }

   private void generatePositions() {
      Set<Holder<Biome>> set = this.runtimeBiomeSource.possibleBiomes();
      this.possibleStructureSets().forEach((p_208094_) -> {
         StructureSet structureset = p_208094_.value();

         for(StructureSet.StructureSelectionEntry structureset$structureselectionentry : structureset.structures()) {
            this.placementsForFeature.computeIfAbsent(structureset$structureselectionentry.structure().value(), (p_208087_) -> {
               return new ArrayList();
            }).add(structureset.placement());
         }

         StructurePlacement structureplacement = structureset.placement();
         if (structureplacement instanceof ConcentricRingsStructurePlacement) {
            ConcentricRingsStructurePlacement concentricringsstructureplacement = (ConcentricRingsStructurePlacement)structureplacement;
            if (structureset.structures().stream().anyMatch((p_208071_) -> {
               return p_208071_.generatesInMatchingBiome(set::contains);
            })) {
               this.ringPositions.put(concentricringsstructureplacement, this.generateRingPositions(p_208094_, concentricringsstructureplacement));
            }
         }

      });
   }

   private CompletableFuture<List<ChunkPos>> generateRingPositions(Holder<StructureSet> p_211668_, ConcentricRingsStructurePlacement p_211669_) {
      return p_211669_.count() == 0 ? CompletableFuture.completedFuture(List.of()) : CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("placement calculation", () -> {
         Stopwatch stopwatch = Stopwatch.createStarted(Util.TICKER);
         List<ChunkPos> list = new ArrayList<>();
         Set<Holder<Biome>> set = p_211668_.value().structures().stream().flatMap((p_208015_) -> {
            return p_208015_.structure().value().biomes().stream();
         }).collect(Collectors.toSet());
         int i = p_211669_.distance();
         int j = p_211669_.count();
         int k = p_211669_.spread();
         Random random = new Random();
         random.setSeed(this.ringPlacementSeed);
         double d0 = random.nextDouble() * Math.PI * 2.0D;
         int l = 0;
         int i1 = 0;

         for(int j1 = 0; j1 < j; ++j1) {
            double d1 = (double)(4 * i + i * i1 * 6) + (random.nextDouble() - 0.5D) * (double)i * 2.5D;
            int k1 = (int)Math.round(Math.cos(d0) * d1);
            int l1 = (int)Math.round(Math.sin(d0) * d1);
            Pair<BlockPos, Holder<Biome>> pair = this.biomeSource.findBiomeHorizontal(SectionPos.sectionToBlockCoord(k1, 8), 0, SectionPos.sectionToBlockCoord(l1, 8), 112, set::contains, random, this.climateSampler());
            if (pair != null) {
               BlockPos blockpos = pair.getFirst();
               k1 = SectionPos.blockToSectionCoord(blockpos.getX());
               l1 = SectionPos.blockToSectionCoord(blockpos.getZ());
            }

            list.add(new ChunkPos(k1, l1));
            d0 += (Math.PI * 2D) / (double)k;
            ++l;
            if (l == k) {
               ++i1;
               l = 0;
               k += 2 * k / (i1 + 1);
               k = Math.min(k, j - j1);
               d0 += random.nextDouble() * Math.PI * 2.0D;
            }
         }

         double d2 = (double)stopwatch.stop().elapsed(TimeUnit.MILLISECONDS) / 1000.0D;
         LOGGER.debug("Calculation for {} took {}s", p_211668_, d2);
         return list;
      }), Util.backgroundExecutor());
   }

   protected abstract Codec<? extends ChunkGenerator> codec();

   public Optional<ResourceKey<Codec<? extends ChunkGenerator>>> getTypeNameForDataFixer() {
      return Registry.CHUNK_GENERATOR.getResourceKey(this.codec());
   }

   public abstract ChunkGenerator withSeed(long pSeed);

   public CompletableFuture<ChunkAccess> createBiomes(Registry<Biome> p_196743_, Executor p_196744_, Blender p_196745_, StructureFeatureManager p_196746_, ChunkAccess p_196747_) {
      return CompletableFuture.supplyAsync(Util.wrapThreadWithTaskName("init_biomes", () -> {
         p_196747_.fillBiomesFromNoise(this.runtimeBiomeSource::getNoiseBiome, this.climateSampler());
         return p_196747_;
      }), Util.backgroundExecutor());
   }

   public abstract Climate.Sampler climateSampler();

   /**
    * Gets the biome at the given quart positions.
    * Note that the coordinates passed into this method are 1/4 the scale of block coordinates. The noise biome is then
    * used by the {@link net.minecraft.world.level.biome.BiomeZoomer} to produce a biome for each unique position,
    * whilst only saving the biomes once per each 4x4x4 cube.
    */
   public Holder<Biome> getNoiseBiome(int pX, int pY, int pZ) {
      return this.getBiomeSource().getNoiseBiome(pX, pY, pZ, this.climateSampler());
   }

   public abstract void applyCarvers(WorldGenRegion pLevel, long pSeed, BiomeManager pBiomeManager, StructureFeatureManager pStructureFeatureManager, ChunkAccess pChunk, GenerationStep.Carving pStep);

   @Nullable
   public Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> findNearestMapFeature(ServerLevel pLevel, HolderSet<ConfiguredStructureFeature<?, ?>> pStructureSet, BlockPos pPos, int pSearchRadius, boolean pSkipKnownStructures) {
      Set<Holder<Biome>> set = pStructureSet.stream().flatMap((p_211699_) -> {
         return p_211699_.value().biomes().stream();
      }).collect(Collectors.toSet());
      if (set.isEmpty()) {
         return null;
      } else {
         Set<Holder<Biome>> set1 = this.runtimeBiomeSource.possibleBiomes();
         if (Collections.disjoint(set1, set)) {
            return null;
         } else {
            Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair = null;
            double d0 = Double.MAX_VALUE;
            Map<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> map = new Object2ObjectArrayMap<>();

            for(Holder<ConfiguredStructureFeature<?, ?>> holder : pStructureSet) {
               if (!set1.stream().noneMatch(holder.value().biomes()::contains)) {
                  for(StructurePlacement structureplacement : this.getPlacementsForFeature(holder)) {
                     map.computeIfAbsent(structureplacement, (p_211663_) -> {
                        return new ObjectArraySet();
                     }).add(holder);
                  }
               }
            }

            List<Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>>> list = new ArrayList<>(map.size());

            for(Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> entry : map.entrySet()) {
               StructurePlacement structureplacement1 = entry.getKey();
               if (structureplacement1 instanceof ConcentricRingsStructurePlacement) {
                  ConcentricRingsStructurePlacement concentricringsstructureplacement = (ConcentricRingsStructurePlacement)structureplacement1;
                  BlockPos blockpos = this.getNearestGeneratedStructure(pPos, concentricringsstructureplacement);
                  double d1 = pPos.distSqr(blockpos);
                  if (d1 < d0) {
                     d0 = d1;
                     pair = Pair.of(blockpos, entry.getValue().iterator().next());
                  }
               } else if (structureplacement1 instanceof RandomSpreadStructurePlacement) {
                  list.add(entry);
               }
            }

            if (!list.isEmpty()) {
               int i = SectionPos.blockToSectionCoord(pPos.getX());
               int j = SectionPos.blockToSectionCoord(pPos.getZ());

               for(int k = 0; k <= pSearchRadius; ++k) {
                  boolean flag = false;

                  for(Entry<StructurePlacement, Set<Holder<ConfiguredStructureFeature<?, ?>>>> entry1 : list) {
                     RandomSpreadStructurePlacement randomspreadstructureplacement = (RandomSpreadStructurePlacement)entry1.getKey();
                     Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> pair1 = getNearestGeneratedStructure(entry1.getValue(), pLevel, pLevel.structureFeatureManager(), i, j, k, pSkipKnownStructures, pLevel.getSeed(), randomspreadstructureplacement);
                     if (pair1 != null) {
                        flag = true;
                        double d2 = pPos.distSqr(pair1.getFirst());
                        if (d2 < d0) {
                           d0 = d2;
                           pair = pair1;
                        }
                     }
                  }

                  if (flag) {
                     return pair;
                  }
               }
            }

            return pair;
         }
      }
   }

   @Nullable
   private BlockPos getNearestGeneratedStructure(BlockPos p_204383_, ConcentricRingsStructurePlacement p_204384_) {
      List<ChunkPos> list = this.getRingPositionsFor(p_204384_);
      if (list == null) {
         throw new IllegalStateException("Somehow tried to find structures for a placement that doesn't exist");
      } else {
         BlockPos blockpos = null;
         double d0 = Double.MAX_VALUE;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(ChunkPos chunkpos : list) {
            blockpos$mutableblockpos.set(SectionPos.sectionToBlockCoord(chunkpos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkpos.z, 8));
            double d1 = blockpos$mutableblockpos.distSqr(p_204383_);
            if (blockpos == null) {
               blockpos = new BlockPos(blockpos$mutableblockpos);
               d0 = d1;
            } else if (d1 < d0) {
               blockpos = new BlockPos(blockpos$mutableblockpos);
               d0 = d1;
            }
         }

         return blockpos;
      }
   }

   @Nullable
   private static Pair<BlockPos, Holder<ConfiguredStructureFeature<?, ?>>> getNearestGeneratedStructure(Set<Holder<ConfiguredStructureFeature<?, ?>>> p_208060_, LevelReader p_208061_, StructureFeatureManager p_208062_, int p_208063_, int p_208064_, int p_208065_, boolean p_208066_, long p_208067_, RandomSpreadStructurePlacement p_208068_) {
      int i = p_208068_.spacing();

      for(int j = -p_208065_; j <= p_208065_; ++j) {
         boolean flag = j == -p_208065_ || j == p_208065_;

         for(int k = -p_208065_; k <= p_208065_; ++k) {
            boolean flag1 = k == -p_208065_ || k == p_208065_;
            if (flag || flag1) {
               int l = p_208063_ + i * j;
               int i1 = p_208064_ + i * k;
               ChunkPos chunkpos = p_208068_.getPotentialFeatureChunk(p_208067_, l, i1);

               for(Holder<ConfiguredStructureFeature<?, ?>> holder : p_208060_) {
                  StructureCheckResult structurecheckresult = p_208062_.checkStructurePresence(chunkpos, holder.value(), p_208066_);
                  if (structurecheckresult != StructureCheckResult.START_NOT_PRESENT) {
                     if (!p_208066_ && structurecheckresult == StructureCheckResult.START_PRESENT) {
                        return Pair.of(StructureFeature.getLocatePos(p_208068_, chunkpos), holder);
                     }

                     ChunkAccess chunkaccess = p_208061_.getChunk(chunkpos.x, chunkpos.z, ChunkStatus.STRUCTURE_STARTS);
                     StructureStart structurestart = p_208062_.getStartForFeature(SectionPos.bottomOf(chunkaccess), holder.value(), chunkaccess);
                     if (structurestart != null && structurestart.isValid()) {
                        if (p_208066_ && structurestart.canBeReferenced()) {
                           p_208062_.addReference(structurestart);
                           return Pair.of(StructureFeature.getLocatePos(p_208068_, structurestart.getChunkPos()), holder);
                        }

                        if (!p_208066_) {
                           return Pair.of(StructureFeature.getLocatePos(p_208068_, structurestart.getChunkPos()), holder);
                        }
                     }
                  }
               }
            }
         }
      }

      return null;
   }

   public void applyBiomeDecoration(WorldGenLevel pLevel, ChunkAccess pChunk, StructureFeatureManager pStructureFeatureManager) {
      ChunkPos chunkpos = pChunk.getPos();
      if (!SharedConstants.debugVoidTerrain(chunkpos)) {
         SectionPos sectionpos = SectionPos.of(chunkpos, pLevel.getMinSection());
         BlockPos blockpos = sectionpos.origin();
         Registry<ConfiguredStructureFeature<?, ?>> registry = pLevel.registryAccess().registryOrThrow(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
         Map<Integer, List<ConfiguredStructureFeature<?, ?>>> map = registry.stream().collect(Collectors.groupingBy((p_211653_) -> {
            return p_211653_.feature.step().ordinal();
         }));
         List<BiomeSource.StepFeatureData> list = this.biomeSource.featuresPerStep();
         WorldgenRandom worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
         long i = worldgenrandom.setDecorationSeed(pLevel.getSeed(), blockpos.getX(), blockpos.getZ());
         Set<Biome> set = new ObjectArraySet<>();
         if (this instanceof FlatLevelSource) {
            this.biomeSource.possibleBiomes().stream().map(Holder::value).forEach(set::add);
         } else {
            ChunkPos.rangeClosed(sectionpos.chunk(), 1).forEach((p_211651_) -> {
               ChunkAccess chunkaccess = pLevel.getChunk(p_211651_.x, p_211651_.z);

               for(LevelChunkSection levelchunksection : chunkaccess.getSections()) {
                  levelchunksection.getBiomes().getAll((p_211688_) -> {
                     set.add(p_211688_.value());
                  });
               }

            });
            set.retainAll(this.biomeSource.possibleBiomes().stream().map(Holder::value).collect(Collectors.toSet()));
         }

         int j = list.size();

         try {
            Registry<PlacedFeature> registry1 = pLevel.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
            int i1 = Math.max(GenerationStep.Decoration.values().length, j);

            for(int k = 0; k < i1; ++k) {
               int l = 0;
               if (pStructureFeatureManager.shouldGenerateFeatures()) {
                  for(ConfiguredStructureFeature<?, ?> configuredstructurefeature : map.getOrDefault(k, Collections.emptyList())) {
                     worldgenrandom.setFeatureSeed(i, l, k);
                     Supplier<String> supplier = () -> {
                        return registry.getResourceKey(configuredstructurefeature).map(Object::toString).orElseGet(configuredstructurefeature::toString);
                     };

                     try {
                        pLevel.setCurrentlyGenerating(supplier);
                        pStructureFeatureManager.startsForFeature(sectionpos, configuredstructurefeature).forEach((p_211647_) -> {
                           p_211647_.placeInChunk(pLevel, pStructureFeatureManager, this, worldgenrandom, getWritableArea(pChunk), chunkpos);
                        });
                     } catch (Exception exception) {
                        CrashReport crashreport1 = CrashReport.forThrowable(exception, "Feature placement");
                        crashreport1.addCategory("Feature").setDetail("Description", supplier::get);
                        throw new ReportedException(crashreport1);
                     }

                     ++l;
                  }
               }

               if (k < j) {
                  IntSet intset = new IntArraySet();

                  for(Biome biome : set) {
                     List<HolderSet<PlacedFeature>> list1 = biome.getGenerationSettings().features();
                     if (k < list1.size()) {
                        HolderSet<PlacedFeature> holderset = list1.get(k);
                        BiomeSource.StepFeatureData biomesource$stepfeaturedata1 = list.get(k);
                        holderset.stream().map(Holder::value).forEach((p_211682_) -> {
                           intset.add(biomesource$stepfeaturedata1.indexMapping().applyAsInt(p_211682_));
                        });
                     }
                  }

                  int j1 = intset.size();
                  int[] aint = intset.toIntArray();
                  Arrays.sort(aint);
                  BiomeSource.StepFeatureData biomesource$stepfeaturedata = list.get(k);

                  for(int k1 = 0; k1 < j1; ++k1) {
                     int l1 = aint[k1];
                     PlacedFeature placedfeature = biomesource$stepfeaturedata.features().get(l1);
                     Supplier<String> supplier1 = () -> {
                        return registry1.getResourceKey(placedfeature).map(Object::toString).orElseGet(placedfeature::toString);
                     };
                     worldgenrandom.setFeatureSeed(i, l1, k);

                     try {
                        pLevel.setCurrentlyGenerating(supplier1);
                        placedfeature.placeWithBiomeCheck(pLevel, this, worldgenrandom, blockpos);
                     } catch (Exception exception1) {
                        CrashReport crashreport2 = CrashReport.forThrowable(exception1, "Feature placement");
                        crashreport2.addCategory("Feature").setDetail("Description", supplier1::get);
                        throw new ReportedException(crashreport2);
                     }
                  }
               }
            }

            pLevel.setCurrentlyGenerating((Supplier<String>)null);
         } catch (Exception exception2) {
            CrashReport crashreport = CrashReport.forThrowable(exception2, "Biome decoration");
            crashreport.addCategory("Generation").setDetail("CenterX", chunkpos.x).setDetail("CenterZ", chunkpos.z).setDetail("Seed", i);
            throw new ReportedException(crashreport);
         }
      }
   }

   public boolean hasFeatureChunkInRange(ResourceKey<StructureSet> p_212266_, long p_212267_, int p_212268_, int p_212269_, int p_212270_) {
      StructureSet structureset = this.structureSets.get(p_212266_);
      if (structureset == null) {
         return false;
      } else {
         StructurePlacement structureplacement = structureset.placement();

         for(int i = p_212268_ - p_212270_; i <= p_212268_ + p_212270_; ++i) {
            for(int j = p_212269_ - p_212270_; j <= p_212269_ + p_212270_; ++j) {
               if (structureplacement.isFeatureChunk(this, p_212267_, i, j)) {
                  return true;
               }
            }
         }

         return false;
      }
   }

   private static BoundingBox getWritableArea(ChunkAccess p_187718_) {
      ChunkPos chunkpos = p_187718_.getPos();
      int i = chunkpos.getMinBlockX();
      int j = chunkpos.getMinBlockZ();
      LevelHeightAccessor levelheightaccessor = p_187718_.getHeightAccessorForGeneration();
      int k = levelheightaccessor.getMinBuildHeight() + 1;
      int l = levelheightaccessor.getMaxBuildHeight() - 1;
      return new BoundingBox(i, k, j, i + 15, l, j + 15);
   }

   public abstract void buildSurface(WorldGenRegion pLevel, StructureFeatureManager pStructureFeatureManager, ChunkAccess pChunk);

   public abstract void spawnOriginalMobs(WorldGenRegion pLevel);

   public int getSpawnHeight(LevelHeightAccessor pLevel) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.runtimeBiomeSource;
   }

   public abstract int getGenDepth();

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Holder<Biome> pBiome, StructureFeatureManager pStructureFeatureManager, MobCategory pCategory, BlockPos pPos) {
      Map<ConfiguredStructureFeature<?, ?>, LongSet> map = pStructureFeatureManager.getAllStructuresAt(pPos);

      for(Entry<ConfiguredStructureFeature<?, ?>, LongSet> entry : map.entrySet()) {
         ConfiguredStructureFeature<?, ?> configuredstructurefeature = entry.getKey();
         StructureSpawnOverride structurespawnoverride = configuredstructurefeature.spawnOverrides.get(pCategory);
         if (structurespawnoverride != null) {
            MutableBoolean mutableboolean = new MutableBoolean(false);
            Predicate<StructureStart> predicate = structurespawnoverride.boundingBox() == StructureSpawnOverride.BoundingBoxType.PIECE ? (p_211631_) -> {
               return pStructureFeatureManager.structureHasPieceAt(pPos, p_211631_);
            } : (p_211666_) -> {
               return p_211666_.getBoundingBox().isInside(pPos);
            };
            pStructureFeatureManager.fillStartsForFeature(configuredstructurefeature, entry.getValue(), (p_211692_) -> {
               if (mutableboolean.isFalse() && predicate.test(p_211692_)) {
                  mutableboolean.setTrue();
               }

            });
            if (mutableboolean.isTrue()) {
               return structurespawnoverride.spawns();
            }
         }
      }

      return pBiome.value().getMobSettings().getMobs(pCategory);
   }

   public static Stream<ConfiguredStructureFeature<?, ?>> allConfigurations(Registry<ConfiguredStructureFeature<?, ?>> p_208045_, StructureFeature<?> p_208046_) {
      return p_208045_.stream().filter((p_211656_) -> {
         return p_211656_.feature == p_208046_;
      });
   }

   public void createStructures(RegistryAccess pRegistryAccess, StructureFeatureManager pStructureFeatureManager, ChunkAccess pChunk, StructureManager pStructureManager, long pSeed) {
      ChunkPos chunkpos = pChunk.getPos();
      SectionPos sectionpos = SectionPos.bottomOf(pChunk);
      this.possibleStructureSets().forEach((p_212264_) -> {
         StructurePlacement structureplacement = p_212264_.value().placement();
         List<StructureSet.StructureSelectionEntry> list = p_212264_.value().structures();

         for(StructureSet.StructureSelectionEntry structureset$structureselectionentry : list) {
            StructureStart structurestart = pStructureFeatureManager.getStartForFeature(sectionpos, structureset$structureselectionentry.structure().value(), pChunk);
            if (structurestart != null && structurestart.isValid()) {
               return;
            }
         }

         if (structureplacement.isFeatureChunk(this, pSeed, chunkpos.x, chunkpos.z)) {
            if (list.size() == 1) {
               this.tryGenerateStructure(list.get(0), pStructureFeatureManager, pRegistryAccess, pStructureManager, pSeed, pChunk, chunkpos, sectionpos);
            } else {
               ArrayList<StructureSet.StructureSelectionEntry> arraylist = new ArrayList<>(list.size());
               arraylist.addAll(list);
               WorldgenRandom worldgenrandom = new WorldgenRandom(new LegacyRandomSource(0L));
               worldgenrandom.setLargeFeatureSeed(pSeed, chunkpos.x, chunkpos.z);
               int i = 0;

               for(StructureSet.StructureSelectionEntry structureset$structureselectionentry1 : arraylist) {
                  i += structureset$structureselectionentry1.weight();
               }

               while(!arraylist.isEmpty()) {
                  int j = worldgenrandom.nextInt(i);
                  int k = 0;

                  for(StructureSet.StructureSelectionEntry structureset$structureselectionentry2 : arraylist) {
                     j -= structureset$structureselectionentry2.weight();
                     if (j < 0) {
                        break;
                     }

                     ++k;
                  }

                  StructureSet.StructureSelectionEntry structureset$structureselectionentry3 = arraylist.get(k);
                  if (this.tryGenerateStructure(structureset$structureselectionentry3, pStructureFeatureManager, pRegistryAccess, pStructureManager, pSeed, pChunk, chunkpos, sectionpos)) {
                     return;
                  }

                  arraylist.remove(k);
                  i -= structureset$structureselectionentry3.weight();
               }

            }
         }
      });
   }

   private boolean tryGenerateStructure(StructureSet.StructureSelectionEntry p_208017_, StructureFeatureManager p_208018_, RegistryAccess p_208019_, StructureManager p_208020_, long p_208021_, ChunkAccess p_208022_, ChunkPos p_208023_, SectionPos p_208024_) {
      ConfiguredStructureFeature<?, ?> configuredstructurefeature = p_208017_.structure().value();
      int i = fetchReferences(p_208018_, p_208022_, p_208024_, configuredstructurefeature);
      HolderSet<Biome> holderset = configuredstructurefeature.biomes();
      Predicate<Holder<Biome>> predicate = (p_211672_) -> {
         return holderset.contains(this.adjustBiome(p_211672_));
      };
      StructureStart structurestart = configuredstructurefeature.generate(p_208019_, this, this.biomeSource, p_208020_, p_208021_, p_208023_, i, p_208022_, predicate);
      if (structurestart.isValid()) {
         p_208018_.setStartForFeature(p_208024_, configuredstructurefeature, structurestart, p_208022_);
         return true;
      } else {
         return false;
      }
   }

   private static int fetchReferences(StructureFeatureManager p_207977_, ChunkAccess p_207978_, SectionPos p_207979_, ConfiguredStructureFeature<?, ?> p_207980_) {
      StructureStart structurestart = p_207977_.getStartForFeature(p_207979_, p_207980_, p_207978_);
      return structurestart != null ? structurestart.getReferences() : 0;
   }

   protected Holder<Biome> adjustBiome(Holder<Biome> p_204385_) {
      return p_204385_;
   }

   public void createReferences(WorldGenLevel pLevel, StructureFeatureManager pStructureFeatureManager, ChunkAccess pChunk) {
      int i = 8;
      ChunkPos chunkpos = pChunk.getPos();
      int j = chunkpos.x;
      int k = chunkpos.z;
      int l = chunkpos.getMinBlockX();
      int i1 = chunkpos.getMinBlockZ();
      SectionPos sectionpos = SectionPos.bottomOf(pChunk);

      for(int j1 = j - 8; j1 <= j + 8; ++j1) {
         for(int k1 = k - 8; k1 <= k + 8; ++k1) {
            long l1 = ChunkPos.asLong(j1, k1);

            for(StructureStart structurestart : pLevel.getChunk(j1, k1).getAllStarts().values()) {
               try {
                  if (structurestart.isValid() && structurestart.getBoundingBox().intersects(l, i1, l + 15, i1 + 15)) {
                     pStructureFeatureManager.addReferenceForFeature(sectionpos, structurestart.getFeature(), l1, pChunk);
                     DebugPackets.sendStructurePacket(pLevel, structurestart);
                  }
               } catch (Exception exception) {
                  CrashReport crashreport = CrashReport.forThrowable(exception, "Generating structure reference");
                  CrashReportCategory crashreportcategory = crashreport.addCategory("Structure");
                  Optional<? extends Registry<ConfiguredStructureFeature<?, ?>>> optional = pLevel.registryAccess().registry(Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY);
                  crashreportcategory.setDetail("Id", () -> {
                     return optional.map((p_211661_) -> {
                        return p_211661_.getKey(structurestart.getFeature()).toString();
                     }).orElse("UNKNOWN");
                  });
                  crashreportcategory.setDetail("Name", () -> {
                     return Registry.STRUCTURE_FEATURE.getKey(structurestart.getFeature().feature).toString();
                  });
                  crashreportcategory.setDetail("Class", () -> {
                     return structurestart.getFeature().getClass().getCanonicalName();
                  });
                  throw new ReportedException(crashreport);
               }
            }
         }
      }

   }

   public abstract CompletableFuture<ChunkAccess> fillFromNoise(Executor p_187748_, Blender p_187749_, StructureFeatureManager p_187750_, ChunkAccess p_187751_);

   public abstract int getSeaLevel();

   public abstract int getMinY();

   public abstract int getBaseHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel);

   public abstract NoiseColumn getBaseColumn(int pX, int pZ, LevelHeightAccessor pLevel);

   public int getFirstFreeHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel) {
      return this.getBaseHeight(pX, pZ, pType, pLevel);
   }

   public int getFirstOccupiedHeight(int pX, int pZ, Heightmap.Types pType, LevelHeightAccessor pLevel) {
      return this.getBaseHeight(pX, pZ, pType, pLevel) - 1;
   }

   public void ensureStructuresGenerated() {
      if (!this.hasGeneratedPositions) {
         this.generatePositions();
         this.hasGeneratedPositions = true;
      }

   }

   @Nullable
   public List<ChunkPos> getRingPositionsFor(ConcentricRingsStructurePlacement p_204381_) {
      this.ensureStructuresGenerated();
      CompletableFuture<List<ChunkPos>> completablefuture = this.ringPositions.get(p_204381_);
      return completablefuture != null ? completablefuture.join() : null;
   }

   private List<StructurePlacement> getPlacementsForFeature(Holder<ConfiguredStructureFeature<?, ?>> p_208091_) {
      this.ensureStructuresGenerated();
      return this.placementsForFeature.getOrDefault(p_208091_.value(), List.of());
   }

   public abstract void addDebugScreenInfo(List<String> p_208054_, BlockPos p_208055_);

   static {
      Registry.register(Registry.CHUNK_GENERATOR, "noise", NoiseBasedChunkGenerator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, "flat", FlatLevelSource.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, "debug", DebugLevelSource.CODEC);
      LOGGER = LogUtils.getLogger();
      CODEC = Registry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
   }
}