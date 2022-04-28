package net.minecraft.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.gson.JsonParseException;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.codecs.UnboundedMapCodec;
import java.util.Map;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.data.BuiltinRegistries;
import net.minecraft.resources.RegistryLookupCodec;
import net.minecraft.resources.RegistryReadOps;
import net.minecraft.resources.RegistryResourceAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.carver.ConfiguredWorldCarver;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.ConfiguredStructureFeature;
import net.minecraft.world.level.levelgen.feature.structures.StructureTemplatePool;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The root level registry, essentially a registry of registries. It is also an access point, hence the name, for other
 * dynamic registries.
 */
public abstract class RegistryAccess {
   static final Logger LOGGER = LogManager.getLogger();
   /**
    * Metadata about all registries. Maps registry keys to a {@link RegistryData} object, which defines the codecs, and
    * if applicable, codecs for synchronization of the registry's elements.
    */
   static final Map<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> REGISTRIES = Util.make(() -> {
      Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> builder = ImmutableMap.builder();
      put(builder, Registry.DIMENSION_TYPE_REGISTRY, DimensionType.DIRECT_CODEC, DimensionType.DIRECT_CODEC);
      put(builder, Registry.BIOME_REGISTRY, Biome.DIRECT_CODEC, Biome.NETWORK_CODEC);
      put(builder, Registry.CONFIGURED_CARVER_REGISTRY, ConfiguredWorldCarver.DIRECT_CODEC);
      put(builder, Registry.CONFIGURED_FEATURE_REGISTRY, ConfiguredFeature.DIRECT_CODEC);
      put(builder, Registry.PLACED_FEATURE_REGISTRY, PlacedFeature.DIRECT_CODEC);
      put(builder, Registry.CONFIGURED_STRUCTURE_FEATURE_REGISTRY, ConfiguredStructureFeature.DIRECT_CODEC);
      put(builder, Registry.PROCESSOR_LIST_REGISTRY, StructureProcessorType.DIRECT_CODEC);
      put(builder, Registry.TEMPLATE_POOL_REGISTRY, StructureTemplatePool.DIRECT_CODEC);
      put(builder, Registry.NOISE_GENERATOR_SETTINGS_REGISTRY, NoiseGeneratorSettings.DIRECT_CODEC);
      put(builder, Registry.NOISE_REGISTRY, NormalNoise.NoiseParameters.DIRECT_CODEC);
      return builder.build();
   });
   /**
    * A registry access containing the builtin registries (excluding the dimension type registry).
    * When this class is loaded, this registry holder is initialized, which involves copying all elements from the
    * builtin registries at {@link net.minecraft.data.BuiltinRegistries} into this field, which contains the static,
    * code defined registries such as configured features, etc.
    * Early classloading of this class <strong>can cause issues</strong> because this field will not contain any
    * elements registered to the builtin registries after classloading of {@code RegistryAccess}.
    */
   private static final RegistryAccess.RegistryHolder BUILTIN = Util.make(() -> {
      RegistryAccess.RegistryHolder registryaccess$registryholder = new RegistryAccess.RegistryHolder();
      DimensionType.registerBuiltin(registryaccess$registryholder);
      REGISTRIES.keySet().stream().filter((p_175518_) -> {
         return !p_175518_.equals(Registry.DIMENSION_TYPE_REGISTRY);
      }).forEach((p_175511_) -> {
         copyBuiltin(registryaccess$registryholder, p_175511_);
      });
      return registryaccess$registryholder;
   });

   /**
    * Get the registry owned by this registry access. The returned value, if it exists, will be writable.
    */
   public abstract <E> Optional<WritableRegistry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey);

   /**
    * A variant of {@link #ownedRegistry(ResourceKey)} that throws if the registry does not exist.
    */
   public <E> WritableRegistry<E> ownedRegistryOrThrow(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
      return this.ownedRegistry(pRegistryKey).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + pRegistryKey);
      });
   }

   /**
    * Get the registry owned by this registry access by the given key. If it doesn't exist, the default registry of
    * registries is queried instead, which contains static registries such as blocks.
    * The returned registry can not gaurentee that it is writable here, so the return type is widened to {@code
    * Registry<E>} instead.
    */
   public <E> Optional<? extends Registry<E>> registry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
      Optional<? extends Registry<E>> optional = this.ownedRegistry(pRegistryKey);
      return optional.isPresent() ? optional : (Optional<? extends Registry<E>>)Registry.REGISTRY.getOptional(pRegistryKey.location());
   }

   /**
    * A variant of {@link #registry(ResourceKey)} that throws if the registry does not exist.
    */
   public <E> Registry<E> registryOrThrow(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
      return this.registry(pRegistryKey).orElseThrow(() -> {
         return new IllegalStateException("Missing registry: " + pRegistryKey);
      });
   }

   private static <E> void put(Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> pBuilder, ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec) {
      pBuilder.put(pRegistryKey, new RegistryAccess.RegistryData<>(pRegistryKey, pElementCodec, (Codec<E>)null));
   }

   private static <E> void put(Builder<ResourceKey<? extends Registry<?>>, RegistryAccess.RegistryData<?>> pBuilder, ResourceKey<? extends Registry<E>> pRegistryKey, Codec<E> pElementCodec, Codec<E> pNetworkCodec) {
      pBuilder.put(pRegistryKey, new RegistryAccess.RegistryData<>(pRegistryKey, pElementCodec, pNetworkCodec));
   }

   public static Iterable<RegistryAccess.RegistryData<?>> knownRegistries() {
      return REGISTRIES.values();
   }

   /**
    * Creates a {@link RegistryHolder} containing the builtin vanilla registries.
    * The way it does so is a little convoluted.
    * <ol>
    * <li>It copies the contents of the noise generator settings and dimension types, directly from the {@link #BUILTIN}
    * field. (Note that since {@link #BUILTIN} does not contain entries for dimension types the latter copy is rather
    * pointless)</li>
    * <li>All other registry elements are serialized to JSON, and stored (including their registry int ID and
    * lifecycles), in the {@code MemoryMap}.</li>
    * <li>A {@link net.minecraft.resources.RegistryReadOps} is created, and stores the {@code MemoryMap} as the ops'
    * {code ResourceAccess}. The ops is then read from, which internally lists resources from the {@code
    * ResourceAccess}, and deserializes all elements from JSON.</li>
    * </ol>
    * Despite seeming like the worlds worst deep copy, this actually has an explicit purpose: Registry elements are
    * totally unknown to the registry - they do not expose a copy method, they can be of an arbitrary type, and more
    * importantly, <strong>they may reference other registry elements</strong>.
    * This is a key reason why registries need to be copied in this serialize, deserialize loop, as opposed to simply
    * copying the elements. References between registry elements need to be maintained, as the registry elements need to
    * still point to valid elements in the overall registries.
    */
   public static RegistryAccess.RegistryHolder builtin() {
      RegistryAccess.RegistryHolder registryaccess$registryholder = new RegistryAccess.RegistryHolder();
      RegistryResourceAccess.InMemoryStorage registryresourceaccess$inmemorystorage = new RegistryResourceAccess.InMemoryStorage();

      for(RegistryAccess.RegistryData<?> registrydata : REGISTRIES.values()) {
         addBuiltinElements(registryaccess$registryholder, registryresourceaccess$inmemorystorage, registrydata);
      }

      RegistryReadOps.createAndLoad(JsonOps.INSTANCE, registryresourceaccess$inmemorystorage, registryaccess$registryholder);
      return registryaccess$registryholder;
   }

   /**
    * Adds builtin elements from the builtin registries to the {@code destinationRegistryHolder} with several quirks.
    * The source for all builtin elements is the {@link #BUILTIN} field, which contains builtin elements of all
    * registries excluding the dimension type registry, as they were at time this class was initialized.
    * Then, depending on the registry, one of two things will occur:
    * <ul>
    * <li>If the registry is the noise generator settings, or dimension type registry, elements will be copied (id,
    * object, name, and lifecycle) directly into the registry of {@code destinationRegistryHolder}</li>
    * <li>However, in all other cases, the registry element is <strong>encoded into JSON</strong> and entered into the
    * {@code destinationRegistryAccess}. The registry holder is not modified in these cases.</li>
    * </ul>
    */
   private static <E> void addBuiltinElements(RegistryAccess.RegistryHolder pDestinationRegistryHolder, RegistryResourceAccess.InMemoryStorage pResourceAccess, RegistryAccess.RegistryData<E> pData) {
      ResourceKey<? extends Registry<E>> resourcekey = pData.key();
      boolean flag = !resourcekey.equals(Registry.NOISE_GENERATOR_SETTINGS_REGISTRY) && !resourcekey.equals(Registry.DIMENSION_TYPE_REGISTRY);
      Registry<E> registry = BUILTIN.registryOrThrow(resourcekey);
      if (!resourcekey.equals(Registry.DIMENSION_TYPE_REGISTRY))
         registry = ((Registry<Registry<E>>)BuiltinRegistries.REGISTRY).get((ResourceKey<Registry<E>>)resourcekey);
      WritableRegistry<E> writableregistry = pDestinationRegistryHolder.ownedRegistryOrThrow(resourcekey);

      for(Entry<ResourceKey<E>, E> entry : registry.entrySet()) {
         ResourceKey<E> resourcekey1 = entry.getKey();
         E e = entry.getValue();
         if (flag) {
            pResourceAccess.add(BUILTIN, resourcekey1, pData.codec(), registry.getId(e), e, registry.lifecycle(e));
         } else {
            writableregistry.registerMapping(registry.getId(e), resourcekey1, e, registry.lifecycle(e));
         }
      }

   }

   /**
    * Copy the values of the builtin registry {@code registryKey} into the {@code destinationRegistryHolder} registry
    * holder.
    */
   private static <R extends Registry<?>> void copyBuiltin(RegistryAccess.RegistryHolder pDestinationRegistryHolder, ResourceKey<R> pRegistryKey) {
      Registry<R> registry = (Registry<R>)BuiltinRegistries.REGISTRY;
      Registry<?> registry1 = registry.getOrThrow(pRegistryKey);
      copy(pDestinationRegistryHolder, registry1);
   }

   /**
    * Copy the values of the {@code sourceRegistry} into the {@code destinationRegistryHolder}
    */
   private static <E> void copy(RegistryAccess.RegistryHolder pDestinationRegistryHolder, Registry<E> pSourceRegistry) {
      WritableRegistry<E> writableregistry = pDestinationRegistryHolder.ownedRegistryOrThrow(pSourceRegistry.key());

      for(Entry<ResourceKey<E>, E> entry : pSourceRegistry.entrySet()) {
         E e = entry.getValue();
         writableregistry.registerMapping(pSourceRegistry.getId(e), entry.getKey(), e, pSourceRegistry.lifecycle(e));
      }

   }

   /**
    * Loads all registries from the {@code ops} into the {@code destinationRegistryAccess}.
    */
   public static void load(RegistryAccess pDestinationRegistryAccess, RegistryReadOps<?> pOps) {
      for(RegistryAccess.RegistryData<?> registrydata : REGISTRIES.values()) {
         readRegistry(pOps, pDestinationRegistryAccess, registrydata);
      }

   }

   /**
    * Load, or reads, a single registry from the containing {@code ops}, into the {@code destinationRegistryAccess}.
    */
   private static <E> void readRegistry(RegistryReadOps<?> pOps, RegistryAccess pDestinationRegistryAccess, RegistryAccess.RegistryData<E> pData) {
      ResourceKey<? extends Registry<E>> resourcekey = pData.key();
      MappedRegistry<E> mappedregistry = (MappedRegistry)pDestinationRegistryAccess.<E>ownedRegistryOrThrow(resourcekey);
      DataResult<MappedRegistry<E>> dataresult = pOps.decodeElements(mappedregistry, pData.key(), pData.codec());
      dataresult.error().ifPresent((p_175499_) -> {
         throw new JsonParseException("Error loading registry data: " + p_175499_.message());
      });
   }

   public static record RegistryData<E>(ResourceKey<? extends Registry<E>> key, Codec<E> codec, @Nullable Codec<E> networkCodec) {
      /**
       * @return {@code true} if this registry should be synchronized with the client.
       */
      public boolean sendToClient() {
         return this.networkCodec != null;
      }
   }

   /**
    * The default implementation of {@link RegistryAccess}, which stores it's registries in a backing map of registry
    * keys to registries.
    */
   public static final class RegistryHolder extends RegistryAccess {
      /**
       * This is the codec used to serialize the entire contents of the builtin registries to send to client. It is
       * built using the metadata information of {@link #REGISTRIES} in order to filter what registries to sync.
       * Internally, the codec is built as a wrapper around a {@code Map<ResourceKey<?>, Registry<?>>}.
       * Each registry that defines a network codec is wrapped with {@link
       * net.minecraft.core.MappedRegistry.networkCodec} to create a codec which preserves id, name and element values.
       */
      public static final Codec<RegistryAccess.RegistryHolder> NETWORK_CODEC = makeNetworkCodec();
      private final Map<? extends ResourceKey<? extends Registry<?>>, ? extends MappedRegistry<?>> registries;

      private static <E> Codec<RegistryAccess.RegistryHolder> makeNetworkCodec() {
         Codec<ResourceKey<? extends Registry<E>>> codec = ResourceLocation.CODEC.xmap(ResourceKey::createRegistryKey, ResourceKey::location);
         Codec<MappedRegistry<E>> codec1 = codec.partialDispatch("type", (p_123134_) -> {
            return DataResult.success(p_123134_.key());
         }, (p_123145_) -> {
            return getNetworkCodec(p_123145_).map((p_175531_) -> {
               return MappedRegistry.networkCodec(p_123145_, Lifecycle.experimental(), p_175531_);
            });
         });
         UnboundedMapCodec<? extends ResourceKey<? extends Registry<?>>, ? extends MappedRegistry<?>> unboundedmapcodec = Codec.unboundedMap(codec, codec1);
         return captureMap(unboundedmapcodec);
      }

      private static <K extends ResourceKey<? extends Registry<?>>, V extends MappedRegistry<?>> Codec<RegistryAccess.RegistryHolder> captureMap(UnboundedMapCodec<K, V> pUnboundedMapCodec) {
         return pUnboundedMapCodec.xmap(RegistryAccess.RegistryHolder::new, (p_123136_) -> {
            return ((Map<K, V>)p_123136_.registries).entrySet().stream().filter((p_175526_) -> {
               return RegistryAccess.REGISTRIES.get(p_175526_.getKey()).sendToClient();
            }).collect(ImmutableMap.toImmutableMap(Entry::getKey, Entry::getValue));
         });
      }

      private static <E> DataResult<? extends Codec<E>> getNetworkCodec(ResourceKey<? extends Registry<E>> pRegistryKey) {
         return Optional.ofNullable(RegistryAccess.REGISTRIES.get(pRegistryKey)).map((p_123123_) -> {
            return (Codec<E>)p_123123_.networkCodec();
         }).map(DataResult::success).orElseGet(() -> {
            return DataResult.error("Unknown or not serializable registry: " + pRegistryKey);
         });
      }

      public RegistryHolder() {
         this(RegistryAccess.REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), RegistryAccess.RegistryHolder::createRegistry)));
      }

      public static RegistryAccess readFromDisk(Dynamic<?> p_194623_) {
         return new RegistryAccess.RegistryHolder(RegistryAccess.REGISTRIES.keySet().stream().collect(Collectors.toMap(Function.identity(), (p_194626_) -> {
            return parseRegistry(p_194626_, p_194623_);
         })));
      }

      private static <E> MappedRegistry<?> parseRegistry(ResourceKey<? extends Registry<?>> p_194630_, Dynamic<?> p_194631_) {
         return (MappedRegistry)RegistryLookupCodec.create((ResourceKey<? extends Registry<E>>)p_194630_).codec().parse(p_194631_).resultOrPartial(Util.prefix(p_194630_ + " registry: ", RegistryAccess.LOGGER::error)).orElseThrow(() -> {
            return new IllegalStateException("Failed to get " + p_194630_ + " registry");
         });
      }

      private RegistryHolder(Map<? extends ResourceKey<? extends Registry<?>>, ? extends MappedRegistry<?>> p_123117_) {
         this.registries = p_123117_;
      }

      private static <E> MappedRegistry<?> createRegistry(ResourceKey<? extends Registry<?>> p_123141_) {
         return new MappedRegistry(p_123141_, Lifecycle.stable());
      }

      /**
       * Get the registry owned by this registry access. The returned value, if it exists, will be writable.
       */
      public <E> Optional<WritableRegistry<E>> ownedRegistry(ResourceKey<? extends Registry<? extends E>> pRegistryKey) {
         return Optional.ofNullable(this.registries.get(pRegistryKey)).map((p_194628_) -> {
            return (WritableRegistry<E>)p_194628_;
         });
      }
   }
}
