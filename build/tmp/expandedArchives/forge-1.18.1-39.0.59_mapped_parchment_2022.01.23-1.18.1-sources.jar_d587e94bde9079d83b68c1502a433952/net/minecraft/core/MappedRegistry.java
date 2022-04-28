package net.minecraft.core;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.resources.RegistryDataPackCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MappedRegistry<T> extends WritableRegistry<T> {
   protected static final Logger LOGGER = LogManager.getLogger();
   private final ObjectList<T> byId = new ObjectArrayList<>(256);
   private final Object2IntMap<T> toId = Util.make(new Object2IntOpenCustomHashMap<>(Util.identityStrategy()), (p_194539_) -> {
      p_194539_.defaultReturnValue(-1);
   });
   private final BiMap<ResourceLocation, T> storage = HashBiMap.create();
   private final BiMap<ResourceKey<T>, T> keyStorage = HashBiMap.create();
   private final Map<T, Lifecycle> lifecycles = Maps.newIdentityHashMap();
   private Lifecycle elementsLifecycle;
   @Nullable
   protected Object[] randomCache;
   private int nextId;

   public MappedRegistry(ResourceKey<? extends Registry<T>> p_122681_, Lifecycle p_122682_) {
      super(p_122681_, p_122682_);
      this.elementsLifecycle = p_122682_;
   }

   public static <T> MapCodec<MappedRegistry.RegistryEntry<T>> withNameAndId(ResourceKey<? extends Registry<T>> pRegistryKey, MapCodec<T> pMapCodec) {
      return RecordCodecBuilder.mapCodec((p_194551_) -> {
         return p_194551_.group(ResourceLocation.CODEC.xmap(ResourceKey.elementKey(pRegistryKey), ResourceKey::location).fieldOf("name").forGetter(MappedRegistry.RegistryEntry::key), Codec.INT.fieldOf("id").forGetter(MappedRegistry.RegistryEntry::id), pMapCodec.forGetter(MappedRegistry.RegistryEntry::value)).apply(p_194551_, MappedRegistry.RegistryEntry::new);
      });
   }

   public <V extends T> V registerMapping(int pId, ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle) {
      return this.registerMapping(pId, pKey, pValue, pLifecycle, true);
   }

   private <V extends T> V registerMapping(int pId, ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle, boolean pLogDuplicateKeys) {
      Validate.notNull(pKey);
      Validate.notNull((T)pValue);
      this.byId.size(Math.max(this.byId.size(), pId + 1));
      this.byId.set(pId, pValue);
      this.toId.put((T)pValue, pId);
      this.randomCache = null;
      if (pLogDuplicateKeys && this.keyStorage.containsKey(pKey)) {
         Util.logAndPauseIfInIde("Adding duplicate key '" + pKey + "' to registry");
      }

      if (this.storage.containsValue(pValue)) {
         Util.logAndPauseIfInIde("Adding duplicate value '" + pValue + "' to registry");
      }

      this.storage.put(pKey.location(), (T)pValue);
      this.keyStorage.put(pKey, (T)pValue);
      this.lifecycles.put((T)pValue, pLifecycle);
      this.elementsLifecycle = this.elementsLifecycle.add(pLifecycle);
      if (this.nextId <= pId) {
         this.nextId = pId + 1;
      }

      return pValue;
   }

   public <V extends T> V register(ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle) {
      return this.registerMapping(this.nextId, pKey, pValue, pLifecycle);
   }

   public <V extends T> V registerOrOverride(OptionalInt pId, ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle) {
      Validate.notNull(pKey);
      Validate.notNull((T)pValue);
      T t = this.keyStorage.get(pKey);
      int i;
      if (t == null) {
         i = pId.isPresent() ? pId.getAsInt() : this.nextId;
      } else {
         i = this.toId.getInt(t);
         if (pId.isPresent() && pId.getAsInt() != i) {
            throw new IllegalStateException("ID mismatch");
         }

         this.toId.removeInt(t);
         this.lifecycles.remove(t);
      }

      return this.registerMapping(i, pKey, pValue, pLifecycle, false);
   }

   /**
    * @return the name used to identify the given object within this registry or {@code null} if the object is not
    * within this registry
    */
   @Nullable
   public ResourceLocation getKey(T pValue) {
      return this.storage.inverse().get(pValue);
   }

   public Optional<ResourceKey<T>> getResourceKey(T pValue) {
      return Optional.ofNullable(this.keyStorage.inverse().get(pValue));
   }

   /**
    * @return the integer ID used to identify the given object
    */
   public int getId(@Nullable T pValue) {
      return this.toId.getInt(pValue);
   }

   @Nullable
   public T get(@Nullable ResourceKey<T> pKey) {
      return this.keyStorage.get(pKey);
   }

   @Nullable
   public T byId(int pId) {
      return (T)(pId >= 0 && pId < this.byId.size() ? this.byId.get(pId) : null);
   }

   public int size() {
      return this.storage.size();
   }

   public Lifecycle lifecycle(T pValue) {
      return this.lifecycles.get(pValue);
   }

   public Lifecycle elementsLifecycle() {
      return this.elementsLifecycle;
   }

   public Iterator<T> iterator() {
      return Iterators.filter(this.byId.iterator(), Objects::nonNull);
   }

   @Nullable
   public T get(@Nullable ResourceLocation pName) {
      return this.storage.get(pName);
   }

   /**
    * @return all keys in this registry
    */
   public Set<ResourceLocation> keySet() {
      return Collections.unmodifiableSet(this.storage.keySet());
   }

   public Set<Entry<ResourceKey<T>, T>> entrySet() {
      return Collections.unmodifiableMap(this.keyStorage).entrySet();
   }

   public boolean isEmpty() {
      return this.storage.isEmpty();
   }

   @Nullable
   public T getRandom(Random pRandom) {
      if (this.randomCache == null) {
         Collection<?> collection = this.storage.values();
         if (collection.isEmpty()) {
            return (T)null;
         }

         this.randomCache = collection.toArray((p_194554_) -> {
            return new Object[p_194554_];
         });
      }

      return Util.getRandom((T[])this.randomCache, pRandom);
   }

   public boolean containsKey(ResourceLocation pName) {
      return this.storage.containsKey(pName);
   }

   public boolean containsKey(ResourceKey<T> pKey) {
      return this.keyStorage.containsKey(pKey);
   }

   public static <T> Codec<MappedRegistry<T>> networkCodec(ResourceKey<? extends Registry<T>> pRegistryKey, Lifecycle pLifecycle, Codec<T> pCodec) {
      return withNameAndId(pRegistryKey, pCodec.fieldOf("element")).codec().listOf().xmap((p_194543_) -> {
         MappedRegistry<T> mappedregistry = new MappedRegistry<>(pRegistryKey, pLifecycle);

         for(MappedRegistry.RegistryEntry<T> registryentry : p_194543_) {
            mappedregistry.registerMapping(registryentry.id(), registryentry.key(), registryentry.value(), pLifecycle);
         }

         return mappedregistry;
      }, (p_194556_) -> {
         Builder<MappedRegistry.RegistryEntry<T>> builder = ImmutableList.builder();

         for(T t : p_194556_) {
            builder.add(new MappedRegistry.RegistryEntry<>(p_194556_.getResourceKey(t).get(), p_194556_.getId(t), t));
         }

         return builder.build();
      });
   }

   public static <T> Codec<MappedRegistry<T>> dataPackCodec(ResourceKey<? extends Registry<T>> pRegistryKey, Lifecycle pLifecycle, Codec<T> pMapCodec) {
      return RegistryDataPackCodec.create(pRegistryKey, pLifecycle, pMapCodec);
   }

   public static <T> Codec<MappedRegistry<T>> directCodec(ResourceKey<? extends Registry<T>> pRegistryKey, Lifecycle pLifecycle, Codec<T> pMapCodec) {
      // FORGE: Fix MC-197860
      return new net.minecraftforge.common.LenientUnboundedMapCodec<>(ResourceLocation.CODEC.xmap(ResourceKey.elementKey(pRegistryKey), ResourceKey::location), pMapCodec).xmap((p_122726_) -> {
         MappedRegistry<T> mappedregistry = new MappedRegistry<>(pRegistryKey, pLifecycle);
         p_122726_.forEach((p_194536_, p_194537_) -> {
            mappedregistry.register(p_194536_, p_194537_, pLifecycle);
         });
         return mappedregistry;
      }, (p_194532_) -> {
         return ImmutableMap.copyOf(p_194532_.keyStorage);
      });
   }

   static record RegistryEntry<T>(ResourceKey<T> key, int id, T value) {
   }
}
