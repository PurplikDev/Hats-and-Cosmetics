package net.minecraft.world.level.chunk;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.data.worldgen.StructureFeatures;
import net.minecraft.network.protocol.game.DebugPackets;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
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
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.StructureSettings;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.StructureFeature;
import net.minecraft.world.level.levelgen.feature.configurations.StrongholdConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.StructureFeatureConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureManager;

public abstract class ChunkGenerator implements BiomeManager.NoiseBiomeSource {
   public static final Codec<ChunkGenerator> CODEC = Registry.CHUNK_GENERATOR.byNameCodec().dispatchStable(ChunkGenerator::codec, Function.identity());
   protected final BiomeSource biomeSource;
   protected final BiomeSource runtimeBiomeSource;
   private final StructureSettings settings;
   private final long strongholdSeed;
   private final List<ChunkPos> strongholdPositions = Lists.newArrayList();

   public ChunkGenerator(BiomeSource pBiomeSource, StructureSettings pSettings) {
      this(pBiomeSource, pBiomeSource, pSettings, 0L);
   }

   public ChunkGenerator(BiomeSource pBiomeSource, BiomeSource pRuntimeBiomeSource, StructureSettings pSettings, long pStrongholdSeed) {
      this.biomeSource = pBiomeSource;
      this.runtimeBiomeSource = pRuntimeBiomeSource;
      this.settings = pSettings;
      this.strongholdSeed = pStrongholdSeed;
   }

   private void generateStrongholds() {
      if (this.strongholdPositions.isEmpty()) {
         StrongholdConfiguration strongholdconfiguration = this.settings.stronghold();
         if (strongholdconfiguration != null && strongholdconfiguration.count() != 0) {
            List<Biome> list = Lists.newArrayList();

            for(Biome biome : this.biomeSource.possibleBiomes()) {
               if (validStrongholdBiome(biome)) {
                  list.add(biome);
               }
            }

            int k1 = strongholdconfiguration.distance();
            int l1 = strongholdconfiguration.count();
            int i = strongholdconfiguration.spread();
            Random random = new Random();
            random.setSeed(this.strongholdSeed);
            double d0 = random.nextDouble() * Math.PI * 2.0D;
            int j = 0;
            int k = 0;

            for(int l = 0; l < l1; ++l) {
               double d1 = (double)(4 * k1 + k1 * k * 6) + (random.nextDouble() - 0.5D) * (double)k1 * 2.5D;
               int i1 = (int)Math.round(Math.cos(d0) * d1);
               int j1 = (int)Math.round(Math.sin(d0) * d1);
               BlockPos blockpos = this.biomeSource.findBiomeHorizontal(SectionPos.sectionToBlockCoord(i1, 8), 0, SectionPos.sectionToBlockCoord(j1, 8), 112, list::contains, random, this.climateSampler());
               if (blockpos != null) {
                  i1 = SectionPos.blockToSectionCoord(blockpos.getX());
                  j1 = SectionPos.blockToSectionCoord(blockpos.getZ());
               }

               this.strongholdPositions.add(new ChunkPos(i1, j1));
               d0 += (Math.PI * 2D) / (double)i;
               ++j;
               if (j == i) {
                  ++k;
                  j = 0;
                  i += 2 * i / (k + 1);
                  i = Math.min(i, l1 - l);
                  d0 += random.nextDouble() * Math.PI * 2.0D;
               }
            }

         }
      }
   }

   private static boolean validStrongholdBiome(Biome p_187716_) {
      Biome.BiomeCategory biome$biomecategory = p_187716_.getBiomeCategory();
      return biome$biomecategory != Biome.BiomeCategory.OCEAN && biome$biomecategory != Biome.BiomeCategory.RIVER && biome$biomecategory != Biome.BiomeCategory.BEACH && biome$biomecategory != Biome.BiomeCategory.SWAMP && biome$biomecategory != Biome.BiomeCategory.NETHER && biome$biomecategory != Biome.BiomeCategory.THEEND;
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
   public Biome getNoiseBiome(int pX, int pY, int pZ) {
      return this.getBiomeSource().getNoiseBiome(pX, pY, pZ, this.climateSampler());
   }

   public abstract void applyCarvers(WorldGenRegion pLevel, long pSeed, BiomeManager pBiomeManager, StructureFeatureManager pStructureFeatureManager, ChunkAccess pChunk, GenerationStep.Carving pStep);

   @Nullable
   public BlockPos findNearestMapFeature(ServerLevel pLevel, StructureFeature<?> pStructure, BlockPos pPos, int pSearchRadius, boolean pSkipKnownStructures) {
      if (pStructure == StructureFeature.STRONGHOLD) {
         this.generateStrongholds();
         BlockPos blockpos = null;
         double d1 = Double.MAX_VALUE;
         BlockPos.MutableBlockPos blockpos$mutableblockpos = new BlockPos.MutableBlockPos();

         for(ChunkPos chunkpos : this.strongholdPositions) {
            blockpos$mutableblockpos.set(SectionPos.sectionToBlockCoord(chunkpos.x, 8), 32, SectionPos.sectionToBlockCoord(chunkpos.z, 8));
            double d0 = blockpos$mutableblockpos.distSqr(pPos);
            if (blockpos == null) {
               blockpos = new BlockPos(blockpos$mutableblockpos);
               d1 = d0;
            } else if (d0 < d1) {
               blockpos = new BlockPos(blockpos$mutableblockpos);
               d1 = d0;
            }
         }

         return blockpos;
      } else {
         StructureFeatureConfiguration structurefeatureconfiguration = this.settings.getConfig(pStructure);
         ImmutableMultimap<ConfiguredStructureFeature<?, ?>, ResourceKey<Biome>> immutablemultimap = this.settings.structures(pStructure);
         if (structurefeatureconfiguration != null && !immutablemultimap.isEmpty()) {
            Registry<Biome> registry = pLevel.registryAccess().registryOrThrow(Registry.BIOME_REGISTRY);
            Set<ResourceKey<Biome>> set = this.runtimeBiomeSource.possibleBiomes().stream().flatMap((p_187725_) -> {
               return registry.getResourceKey(p_187725_).stream();
            }).collect(Collectors.toSet());
            return immutablemultimap.values().stream().noneMatch(set::contains) ? null : pStructure.getNearestGeneratedFeature(pLevel, pLevel.structureFeatureManager(), pPos, pSearchRadius, pSkipKnownStructures, pLevel.getSeed(), structurefeatureconfiguration);
         } else {
            return null;
         }
      }
   }

   public void applyBiomeDecoration(WorldGenLevel pLevel, ChunkAccess pChunk, StructureFeatureManager pStructureFeatureManager) {
      ChunkPos chunkpos = pChunk.getPos();
      if (!SharedConstants.debugVoidTerrain(chunkpos)) {
         SectionPos sectionpos = SectionPos.of(chunkpos, pLevel.getMinSection());
         BlockPos blockpos = sectionpos.origin();
         Map<Integer, List<StructureFeature<?>>> map = Registry.STRUCTURE_FEATURE.stream().collect(Collectors.groupingBy((p_187720_) -> {
            return p_187720_.step().ordinal();
         }));
         List<BiomeSource.StepFeatureData> list = this.biomeSource.featuresPerStep();
         WorldgenRandom worldgenrandom = new WorldgenRandom(new XoroshiroRandomSource(RandomSupport.seedUniquifier()));
         long i = worldgenrandom.setDecorationSeed(pLevel.getSeed(), blockpos.getX(), blockpos.getZ());
         Set<Biome> set = new ObjectArraySet<>();
         if (this instanceof FlatLevelSource) {
            set.addAll(this.biomeSource.possibleBiomes());
         } else {
            ChunkPos.rangeClosed(sectionpos.chunk(), 1).forEach((p_196730_) -> {
               ChunkAccess chunkaccess = pLevel.getChunk(p_196730_.x, p_196730_.z);

               for(LevelChunkSection levelchunksection : chunkaccess.getSections()) {
                  levelchunksection.getBiomes().getAll(set::add);
               }

            });
            set.retainAll(this.biomeSource.possibleBiomes());
         }

         int j = list.size();

         try {
            Registry<PlacedFeature> registry = pLevel.registryAccess().registryOrThrow(Registry.PLACED_FEATURE_REGISTRY);
            Registry<StructureFeature<?>> registry1 = pLevel.registryAccess().registryOrThrow(Registry.STRUCTURE_FEATURE_REGISTRY);
            int k = Math.max(GenerationStep.Decoration.values().length, j);

            for(int l = 0; l < k; ++l) {
               int i1 = 0;
               if (pStructureFeatureManager.shouldGenerateFeatures()) {
                  for(StructureFeature<?> structurefeature : map.getOrDefault(l, Collections.emptyList())) {
                     worldgenrandom.setFeatureSeed(i, i1, l);
                     Supplier<String> supplier = () -> {
                        return registry1.getResourceKey(structurefeature).map(Object::toString).orElseGet(structurefeature::toString);
                     };

                     try {
                        pLevel.setCurrentlyGenerating(supplier);
                        pStructureFeatureManager.startsForFeature(sectionpos, structurefeature).forEach((p_196726_) -> {
                           p_196726_.placeInChunk(pLevel, pStructureFeatureManager, this, worldgenrandom, getWritableArea(pChunk), chunkpos);
                        });
                     } catch (Exception exception) {
                        CrashReport crashreport1 = CrashReport.forThrowable(exception, "Feature placement");
                        crashreport1.addCategory("Feature").setDetail("Description", supplier::get);
                        throw new ReportedException(crashreport1);
                     }

                     ++i1;
                  }
               }

               if (l < j) {
                  IntSet intset = new IntArraySet();

                  for(Biome biome : set) {
                     List<List<Supplier<PlacedFeature>>> list2 = biome.getGenerationSettings().features();
                     if (l < list2.size()) {
                        List<Supplier<PlacedFeature>> list1 = list2.get(l);
                        BiomeSource.StepFeatureData biomesource$stepfeaturedata1 = list.get(l);
                        list1.stream().map(Supplier::get).forEach((p_196751_) -> {
                           intset.add(biomesource$stepfeaturedata1.indexMapping().applyAsInt(p_196751_));
                        });
                     }
                  }

                  int j1 = intset.size();
                  int[] aint = intset.toIntArray();
                  Arrays.sort(aint);
                  BiomeSource.StepFeatureData biomesource$stepfeaturedata = list.get(l);

                  for(int k1 = 0; k1 < j1; ++k1) {
                     int l1 = aint[k1];
                     PlacedFeature placedfeature = biomesource$stepfeaturedata.features().get(l1);
                     Supplier<String> supplier1 = () -> {
                        return registry.getResourceKey(placedfeature).map(Object::toString).orElseGet(placedfeature::toString);
                     };
                     worldgenrandom.setFeatureSeed(i, l1, l);

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

   public StructureSettings getSettings() {
      return this.settings;
   }

   public int getSpawnHeight(LevelHeightAccessor pLevel) {
      return 64;
   }

   public BiomeSource getBiomeSource() {
      return this.runtimeBiomeSource;
   }

   public abstract int getGenDepth();

   public WeightedRandomList<MobSpawnSettings.SpawnerData> getMobsAt(Biome pBiome, StructureFeatureManager pStructureFeatureManager, MobCategory pCategory, BlockPos pPos) {
      return pBiome.getMobSettings().getMobs(pCategory);
   }

   public void createStructures(RegistryAccess pRegistryAccess, StructureFeatureManager pStructureFeatureManager, ChunkAccess pChunk, StructureManager pStructureManager, long pSeed) {
      ChunkPos chunkpos = pChunk.getPos();
      SectionPos sectionpos = SectionPos.bottomOf(pChunk);
      StructureFeatureConfiguration structurefeatureconfiguration = this.settings.getConfig(StructureFeature.STRONGHOLD);
      if (structurefeatureconfiguration != null) {
         StructureStart<?> structurestart = pStructureFeatureManager.getStartForFeature(sectionpos, StructureFeature.STRONGHOLD, pChunk);
         if (structurestart == null || !structurestart.isValid()) {
            StructureStart<?> structurestart1 = StructureFeatures.STRONGHOLD.generate(pRegistryAccess, this, this.biomeSource, pStructureManager, pSeed, chunkpos, fetchReferences(pStructureFeatureManager, pChunk, sectionpos, StructureFeature.STRONGHOLD), structurefeatureconfiguration, pChunk, ChunkGenerator::validStrongholdBiome);
            pStructureFeatureManager.setStartForFeature(sectionpos, StructureFeature.STRONGHOLD, structurestart1, pChunk);
         }
      }

      Registry<Biome> registry = pRegistryAccess.registryOrThrow(Registry.BIOME_REGISTRY);

      label48:
      for(StructureFeature<?> structurefeature : Registry.STRUCTURE_FEATURE) {
         if (structurefeature != StructureFeature.STRONGHOLD) {
            StructureFeatureConfiguration structurefeatureconfiguration1 = this.settings.getConfig(structurefeature);
            if (structurefeatureconfiguration1 != null) {
               StructureStart<?> structurestart2 = pStructureFeatureManager.getStartForFeature(sectionpos, structurefeature, pChunk);
               if (structurestart2 == null || !structurestart2.isValid()) {
                  int i = fetchReferences(pStructureFeatureManager, pChunk, sectionpos, structurefeature);

                  for(Entry<ConfiguredStructureFeature<?, ?>, Collection<ResourceKey<Biome>>> entry : this.settings.structures(structurefeature).asMap().entrySet()) {
                     StructureStart<?> structurestart3 = entry.getKey().generate(pRegistryAccess, this, this.biomeSource, pStructureManager, pSeed, chunkpos, i, structurefeatureconfiguration1, pChunk, (p_196742_) -> {
                        return this.validBiome(registry, entry.getValue()::contains, p_196742_);
                     });
                     if (structurestart3.isValid()) {
                        pStructureFeatureManager.setStartForFeature(sectionpos, structurefeature, structurestart3, pChunk);
                        continue label48;
                     }
                  }

                  pStructureFeatureManager.setStartForFeature(sectionpos, structurefeature, StructureStart.INVALID_START, pChunk);
               }
            }
         }
      }

   }

   private static int fetchReferences(StructureFeatureManager p_187701_, ChunkAccess p_187702_, SectionPos p_187703_, StructureFeature<?> p_187704_) {
      StructureStart<?> structurestart = p_187701_.getStartForFeature(p_187703_, p_187704_, p_187702_);
      return structurestart != null ? structurestart.getReferences() : 0;
   }

   protected boolean validBiome(Registry<Biome> p_187736_, Predicate<ResourceKey<Biome>> p_187737_, Biome p_187738_) {
      return p_187736_.getResourceKey(p_187738_).filter(p_187737_).isPresent();
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

            for(StructureStart<?> structurestart : pLevel.getChunk(j1, k1).getAllStarts().values()) {
               try {
                  if (structurestart.isValid() && structurestart.getBoundingBox().intersects(l, i1, l + 15, i1 + 15)) {
                     pStructureFeatureManager.addReferenceForFeature(sectionpos, structurestart.getFeature(), l1, pChunk);
                     DebugPackets.sendStructurePacket(pLevel, structurestart);
                  }
               } catch (Exception exception) {
                  CrashReport crashreport = CrashReport.forThrowable(exception, "Generating structure reference");
                  CrashReportCategory crashreportcategory = crashreport.addCategory("Structure");
                  crashreportcategory.setDetail("Id", () -> {
                     return Registry.STRUCTURE_FEATURE.getKey(structurestart.getFeature()).toString();
                  });
                  crashreportcategory.setDetail("Name", () -> {
                     return structurestart.getFeature().getFeatureName();
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

   public boolean hasStronghold(ChunkPos pPos) {
      this.generateStrongholds();
      return this.strongholdPositions.contains(pPos);
   }

   static {
      Registry.register(Registry.CHUNK_GENERATOR, "noise", NoiseBasedChunkGenerator.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, "flat", FlatLevelSource.CODEC);
      Registry.register(Registry.CHUNK_GENERATOR, "debug", DebugLevelSource.CODEC);
   }
}