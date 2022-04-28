package net.minecraft.world.level.levelgen;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Properties;
import java.util.Random;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.MappedRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldGenSettings {
   public static final Codec<WorldGenSettings> CODEC = RecordCodecBuilder.<WorldGenSettings>create((p_64626_) -> {
      return p_64626_.group(Codec.LONG.fieldOf("seed").stable().forGetter(WorldGenSettings::seed), Codec.BOOL.fieldOf("generate_features").orElse(true).stable().forGetter(WorldGenSettings::generateFeatures), Codec.BOOL.fieldOf("bonus_chest").orElse(false).stable().forGetter(WorldGenSettings::generateBonusChest), MappedRegistry.dataPackCodec(Registry.LEVEL_STEM_REGISTRY, Lifecycle.stable(), LevelStem.CODEC).xmap(LevelStem::sortMap, Function.identity()).fieldOf("dimensions").forGetter(WorldGenSettings::dimensions), Codec.STRING.optionalFieldOf("legacy_custom_options").stable().forGetter((p_158959_) -> {
         return p_158959_.legacyCustomOptions;
      })).apply(p_64626_, p_64626_.stable(WorldGenSettings::new));
   }).xmap(net.minecraftforge.common.ForgeHooks::loadDimensionsWithServerSeed, wgs -> {return wgs; // forge: when loading/registering json dimensions, replace hardcoded seeds of custom dimensions with the server/overworld's seed, where requested; fixes MC-195717
   }).comapFlatMap(WorldGenSettings::guardExperimental, Function.identity());
   private static final Logger LOGGER = LogManager.getLogger();
   private final long seed;
   private final boolean generateFeatures;
   private final boolean generateBonusChest;
   private final MappedRegistry<LevelStem> dimensions;
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

   public WorldGenSettings(long pSeed, boolean pGenerateFeatures, boolean pGenerateBonusChest, MappedRegistry<LevelStem> pDimensions) {
      this(pSeed, pGenerateFeatures, pGenerateBonusChest, pDimensions, Optional.empty());
      LevelStem levelstem = pDimensions.get(LevelStem.OVERWORLD);
      if (levelstem == null) {
         throw new IllegalStateException("Overworld settings missing");
      }
   }

   public WorldGenSettings(long p_64614_, boolean p_64615_, boolean p_64616_, MappedRegistry<LevelStem> p_64617_, Optional<String> p_64618_) {
      this.seed = p_64614_;
      this.generateFeatures = p_64615_;
      this.generateBonusChest = p_64616_;
      this.dimensions = p_64617_;
      this.legacyCustomOptions = p_64618_;
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
      return new NoiseBasedChunkGenerator(p_190035_.registryOrThrow(Registry.NOISE_REGISTRY), MultiNoiseBiomeSource.Preset.OVERWORLD.biomeSource(p_190035_.registryOrThrow(Registry.BIOME_REGISTRY), p_190038_), p_190036_, () -> {
         return p_190035_.registryOrThrow(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY).getOrThrow(p_190037_);
      });
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

   public static MappedRegistry<LevelStem> withOverworld(Registry<DimensionType> pDimensionTypes, MappedRegistry<LevelStem> pDimensions, ChunkGenerator pChunkGenerator) {
      LevelStem levelstem = pDimensions.get(LevelStem.OVERWORLD);
      Supplier<DimensionType> supplier = () -> {
         return levelstem == null ? pDimensionTypes.getOrThrow(DimensionType.OVERWORLD_LOCATION) : levelstem.type();
      };
      return withOverworld(pDimensions, supplier, pChunkGenerator);
   }

   public static MappedRegistry<LevelStem> withOverworld(MappedRegistry<LevelStem> pDimensions, Supplier<DimensionType> pDimensionTypes, ChunkGenerator pChunkGenerator) {
      MappedRegistry<LevelStem> mappedregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
      mappedregistry.register(LevelStem.OVERWORLD, new LevelStem(pDimensionTypes, pChunkGenerator), Lifecycle.stable());

      for(Entry<ResourceKey<LevelStem>, LevelStem> entry : pDimensions.entrySet()) {
         ResourceKey<LevelStem> resourcekey = entry.getKey();
         if (resourcekey != LevelStem.OVERWORLD) {
            mappedregistry.register(resourcekey, entry.getValue(), pDimensions.lifecycle(entry.getValue()));
         }
      }

      return mappedregistry;
   }

   public MappedRegistry<LevelStem> dimensions() {
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

   public static WorldGenSettings create(RegistryAccess pRegistryAccess, Properties pProperties) {
      String s = MoreObjects.firstNonNull((String)pProperties.get("generator-settings"), "");
      pProperties.put("generator-settings", s);
      String s1 = MoreObjects.firstNonNull((String)pProperties.get("level-seed"), "");
      pProperties.put("level-seed", s1);
      String s2 = (String)pProperties.get("generate-structures");
      boolean flag = s2 == null || Boolean.parseBoolean(s2);
      pProperties.put("generate-structures", Objects.toString(flag));
      String s3 = (String)pProperties.get("level-type");
      String s4 = Optional.ofNullable(s3).map((p_190047_) -> {
         return p_190047_.toLowerCase(Locale.ROOT);
      }).orElseGet(net.minecraftforge.common.ForgeHooks::getDefaultWorldPreset);
      pProperties.put("level-type", s4);
      long i = (new Random()).nextLong();
      if (!s1.isEmpty()) {
         try {
            long j = Long.parseLong(s1);
            if (j != 0L) {
               i = j;
            }
         } catch (NumberFormatException numberformatexception) {
            i = (long)s1.hashCode();
         }
      }

      Registry<DimensionType> registry1 = pRegistryAccess.registryOrThrow(Registry.DIMENSION_TYPE_REGISTRY);
      Registry<Biome> registry = pRegistryAccess.registryOrThrow(Registry.BIOME_REGISTRY);
      MappedRegistry<LevelStem> mappedregistry = DimensionType.defaultDimensions(pRegistryAccess, i);
      net.minecraftforge.common.world.ForgeWorldPreset type = net.minecraftforge.registries.ForgeRegistries.WORLD_TYPES.getValue(new net.minecraft.resources.ResourceLocation(s4));
      if (type != null) return type.createSettings(pRegistryAccess, i, flag, false, s);
      switch(s4) {
      case "flat":
         JsonObject jsonobject = !s.isEmpty() ? GsonHelper.parse(s) : new JsonObject();
         Dynamic<JsonElement> dynamic = new Dynamic<>(JsonOps.INSTANCE, jsonobject);
         return new WorldGenSettings(i, flag, false, withOverworld(registry1, mappedregistry, new FlatLevelSource(FlatLevelGeneratorSettings.CODEC.parse(dynamic).resultOrPartial(LOGGER::error).orElseGet(() -> {
            return FlatLevelGeneratorSettings.getDefault(registry);
         }))));
      case "debug_all_block_states":
         return new WorldGenSettings(i, flag, false, withOverworld(registry1, mappedregistry, new DebugLevelSource(registry)));
      case "amplified":
         return new WorldGenSettings(i, flag, false, withOverworld(registry1, mappedregistry, makeOverworld(pRegistryAccess, i, NoiseGeneratorSettings.AMPLIFIED)));
      case "largebiomes":
         return new WorldGenSettings(i, flag, false, withOverworld(registry1, mappedregistry, makeOverworld(pRegistryAccess, i, NoiseGeneratorSettings.LARGE_BIOMES)));
      default:
         return new WorldGenSettings(i, flag, false, withOverworld(registry1, mappedregistry, makeDefaultOverworld(pRegistryAccess, i)));
      }
   }

   public WorldGenSettings withSeed(boolean pHardcore, OptionalLong pLevelSeed) {
      long i = pLevelSeed.orElse(this.seed);
      MappedRegistry<LevelStem> mappedregistry;
      if (pLevelSeed.isPresent()) {
         mappedregistry = new MappedRegistry<>(Registry.LEVEL_STEM_REGISTRY, Lifecycle.experimental());
         long j = pLevelSeed.getAsLong();

         for(Entry<ResourceKey<LevelStem>, LevelStem> entry : this.dimensions.entrySet()) {
            ResourceKey<LevelStem> resourcekey = entry.getKey();
            mappedregistry.register(resourcekey, new LevelStem(entry.getValue().typeSupplier(), entry.getValue().generator().withSeed(j)), this.dimensions.lifecycle(entry.getValue()));
         }
      } else {
         mappedregistry = this.dimensions;
      }

      WorldGenSettings worldgensettings;
      if (this.isDebug()) {
         worldgensettings = new WorldGenSettings(i, false, false, mappedregistry);
      } else {
         worldgensettings = new WorldGenSettings(i, this.generateFeatures(), this.generateBonusChest() && !pHardcore, mappedregistry);
      }

      return worldgensettings;
   }
}
