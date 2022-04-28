package net.minecraft.world.level.levelgen;

import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Function;
import net.minecraft.core.Holder;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.WritableRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.dedicated.DedicatedServerProperties;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;

public class WorldGenSettings {
   public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.<WorldGenSettings>create((p_64626_) -> {
      return p_64626_.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateFeatures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), RegistryCodecs.dataPackAwareCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((p_158959_) -> {
         return p_158959_.legacyCustomOptions;
      })).apply(p_64626_, p_64626_.stable(WorldGenSettings::new));
   }).xmap(net.minecraftforge.common.ForgeHooks::loadDimensionsWithServerSeed, wgs -> {return wgs; // forge: when loading/registering json dimensions, replace hardcoded seeds of custom dimensions with the server/overworld's seed, where requested; fixes MC-195717
   }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
   private static final Logger LOGGER = LogUtils.getLogger();
   private final long seed;
   private final boolean generateFeatures;
   private final boolean generateBonusChest;
   private final Registry<LevelStem> dimensions;
   public final Optional<String> legacyCustomOptions;

   private DataResult<WorldGenSettings> guardExperimental() {
      LevelStem levelstem = this.dimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         return DataResult.error("Overworld settings missing");
      } else {
         return this.stable() ? DataResult.success(this, Lifecycle.stable()) : DataResult.success(this);
      }
   }

   private boolean stable() {
      return LevelStem.stable(this.seed, this.dimensions);
   }

   public WorldGenSettings(long pSeed, boolean pGenerateFeatures, boolean pGenerateBonusChest, Registry<LevelStem> pDimensions) {
      this(pSeed, pGenerateFeatures, pGenerateBonusChest, pDimensions, Optional.empty());
      LevelStem levelstem = pDimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      }
   }

   public WorldGenSettings(long p_204638_, boolean p_204639_, boolean p_204640_, Registry<LevelStem> p_204641_, Optional<String> p_204642_) {
      this.seed = p_204638_;
      this.generateFeatures = p_204639_;
      this.generateBonusChest = p_204640_;
      this.dimensions = p_204641_;
      this.legacyCustomOptions = p_204642_;
   }

   public static WorldGenSettings demoSettings(RegistryAccess pRegistryAccess) {
      int i = "North Carolina".hashCode();
      return new WorldGenSettings((long)i, true, true, withOverworld(pRegistryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(pRegistryAccess, (long)i), makeDefaultOverworld(pRegistryAccess, (long)i)));
   }

   public static WorldGenSettings makeDefault(RegistryAccess pRegistryAccess) {
      long i = (new Random()).nextLong();
      return new WorldGenSettings(i, true, false, withOverworld(pRegistryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY), DimensionType.defaultDimensions(pRegistryAccess, i), makeDefaultOverworld(pRegistryAccess, i)));
   }

   public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess p_190028_, long p_190029_) {
      return makeDefaultOverworld(p_190028_, p_190029_, true);
   }

   public static NoiseBasedChunkGenerator makeDefaultOverworld(RegistryAccess p_190040_, long p_190041_, boolean p_190042_) {
      return makeOverworld(p_190040_, p_190041_, NoiseGeneratorSettings.OVERWORLD, p_190042_);
   }

   public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess p_190031_, long p_190032_, ResourceKey<NoiseGeneratorSettings> p_190033_) {
      return makeOverworld(p_190031_, p_190032_, p_190033_, true);
   }

   public static NoiseBasedChunkGenerator makeOverworld(RegistryAccess p_190035_, long p_190036_, ResourceKey<NoiseGeneratorSettings> p_190037_, boolean p_190038_) {
      Registry<Biome> registry = p_190035_.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry<StructureSet> registry1 = p_190035_.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      Registry<NoiseGeneratorSettings> registry2 = p_190035_.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY);
      Registry<NormalNoise.NoiseParameters> registry3 = p_190035_.registryOrThrow(Registry.NOISE_REGISTRY);
      return new NoiseBasedChunkGenerator(registry1, registry3, MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(registry, p_190038_), p_190036_, registry2.getOrCreateHolder(p_190037_));
   }

   public long seed() {
      return this.seed;
   }

   public boolean generateFeatures() {
      return this.generateFeatures;
   }

   public boolean generateBonusChest() {
      return this.generateBonusChest;
   }

   public static Registry<LevelStem> withOverworld(Registry<DimensionType> pDimensionTypes, Registry<LevelStem> pDimensions, ChunkGenerator pChunkGenerator) {
      LevelStem levelstem = pDimensions.get(LevelStem.OVERWORLD);
      Holder<DimensionType> holder = levelstem == null ? pDimensionTypes.getOrCreateHolder(DimensionType.OVERWORLD_LOCATION) : levelstem.typeHolder();
      return withOverworld(pDimensions, holder, pChunkGenerator);
   }

   public static Registry<LevelStem> withOverworld(Registry<LevelStem> pDimensions, Holder<DimensionType> pDimensionType, ChunkGenerator pChunkGenerator) {
      WritableRegistry<LevelStem> writableregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
      writableregistry.register(LevelStem.OVERWORLD, new LevelStem(pDimensionType, pChunkGenerator), Lifecycle.stable());

      for(Entry<ResourceKey<LevelStem>, LevelStem> entry : pDimensions.entrySet()) {
         ResourceKey<LevelStem> resourcekey = entry.getKey();
         if (resourcekey != LevelStem.OVERWORLD) {
            writableregistry.register(resourcekey, entry.getValue(), pDimensions.lifecycle(entry.getValue()));
         }
      }

      return writableregistry;
   }

   public Registry<LevelStem> dimensions() {
      return this.dimensions;
   }

   public ChunkGenerator overworld() {
      LevelStem levelstem = this.dimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      } else {
         return levelstem.generator();
      }
   }

   public ImmutableSet<ResourceKey<Level>> levels() {
      return this.dimensions().entrySet().stream().map(Entry::getKey).map(WorldGenSettings::levelStemToLevel).collect(ImmutableSet.toImmutableSet());
   }

   public static ResourceKey<Level> levelStemToLevel(ResourceKey<LevelStem> p_190049_) {
      return ResourceKey.create(Registry.DIMENSION_REGISTRY, p_190049_.location());
   }

   public static ResourceKey<LevelStem> levelToLevelStem(ResourceKey<Level> p_190053_) {
      return ResourceKey.create(Registry.LEVEL_STEM_REGISTRY, p_190053_.location());
   }

   public boolean isDebug() {
      return this.overworld() instanceof DebugLevelSource;
   }

   public boolean isFlatWorld() {
      return this.overworld() instanceof FlatLevelSource;
   }

   public boolean isOldCustomizedWorld() {
      return this.legacyCustomOptions.isPresent();
   }

   public WorldGenSettings withBonusChest() {
      return new WorldGenSettings(this.seed, this.generateFeatures, true, this.dimensions, this.legacyCustomOptions);
   }

   public WorldGenSettings withFeaturesToggled() {
      return new WorldGenSettings(this.seed, !this.generateFeatures, this.generateBonusChest, this.dimensions);
   }

   public WorldGenSettings withBonusChestToggled() {
      return new WorldGenSettings(this.seed, this.generateFeatures, !this.generateBonusChest, this.dimensions);
   }

   public static WorldGenSettings create(RegistryAccess pRegistryAccess, DedicatedServerProperties.WorldGenProperties pProperties) {
      long i = parseSeed(pProperties.levelSeed()).orElse((new Random()).nextLong());
      Registry<DimensionType> registry = pRegistryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
      Registry<Biome> registry1 = pRegistryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
      Registry<StructureSet> registry2 = pRegistryAccess.registryOrThrow(Registry.STRUCTURE_SET_REGISTRY);
      Registry<LevelStem> registry3 = DimensionType.defaultDimensions(pRegistryAccess, i);
      String s = pProperties.levelType();
      net.minecraftforge.common.world.ForgeWorldPreset type = net.minecraftforge.registries.ForgeRegistries.WORLD_TYPES.get().getValue(new net.minecraft.resources.ResourceLocation(s));
      if (type != null) return type.createSettings(pRegistryAccess, i, pProperties.generateStructures(), false, s);
      switch(s) {
      case "flat":
         Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, pProperties.generatorSettings());
         return new WorldGenSettings(i, pProperties.generateStructures(), false, withOverworld(registry, registry3, new FlatLevelSource(registry2, FlatLevelGeneratorSettings.CODEC.parse(dynamic).resultOrPartial(LOGGER::error).orElseGet(() -> {
            return FlatLevelGeneratorSettings.getDefault(registry1, registry2);
         }))));
      case "debug_all_block_states":
         return new WorldGenSettings(i, pProperties.generateStructures(), false, withOverworld(registry, registry3, new DebugLevelSource(registry2, registry1)));
      case "amplified":
         return new WorldGenSettings(i, pProperties.generateStructures(), false, withOverworld(registry, registry3, makeOverworld(pRegistryAccess, i, NoiseGeneratorSettings.AMPLIFIED)));
      case "largebiomes":
         return new WorldGenSettings(i, pProperties.generateStructures(), false, withOverworld(registry, registry3, makeOverworld(pRegistryAccess, i, NoiseGeneratorSettings.LARGE_BIOMES)));
      default:
         return new WorldGenSettings(i, pProperties.generateStructures(), false, withOverworld(registry, registry3, makeDefaultOverworld(pRegistryAccess, i)));
      }
   }

   public WorldGenSettings withSeed(boolean pHardcore, OptionalLong pLevelSeed) {
      long i = pLevelSeed.orElse(this.seed);
      Registry<LevelStem> registry;
      if (pLevelSeed.isPresent()) {
         WritableRegistry<LevelStem> writableregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental(), (Function<LevelStem, Holder.Reference<LevelStem>>)null);
         long j = pLevelSeed.getAsLong();

         for(Entry<ResourceKey<LevelStem>, LevelStem> entry : this.dimensions.entrySet()) {
            ResourceKey<LevelStem> resourcekey = entry.getKey();
            writableregistry.register(resourcekey, new LevelStem(entry.getValue().typeHolder(), entry.getValue().generator().withSeed(j)), this.dimensions.lifecycle(entry.getValue()));
         }

         registry = writableregistry;
      } else {
         registry = this.dimensions;
      }

      WorldGenSettings worldgensettings;
      if (this.isDebug()) {
         worldgensettings = new WorldGenSettings(i, false, false, registry);
      } else {
         worldgensettings = new WorldGenSettings(i, this.generateFeatures(), this.generateBonusChest() && !pHardcore, registry);
      }

      return worldgensettings;
   }

   public static OptionalLong parseSeed(String p_202193_) {
      p_202193_ = p_202193_.trim();
      if (StringUtils.isEmpty(p_202193_)) {
         return OptionalLong.empty();
      } else {
         try {
            return OptionalLong.of(Long.parseLong(p_202193_));
         } catch (NumberFormatException numberformatexception) {
            return OptionalLong.of((long)p_202193_.hashCode());
         }
      }
   }
}
