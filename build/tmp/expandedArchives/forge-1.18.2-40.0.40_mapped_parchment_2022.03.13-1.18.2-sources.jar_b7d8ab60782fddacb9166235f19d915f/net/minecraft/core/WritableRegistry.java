package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceKey;

public abstract class WritableRegistry<T> extends Registry<T> {
   public WritableRegistry(ResourceKey<? extends Registry<T>> p_123346_, Lifecycle p_123347_) {
      super(p_123346_, p_123347_);
   }

   public abstract Holder<T> registerMapping(int pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   public abstract Holder<T> register(ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   public abstract Holder<T> registerOrOverride(OptionalInt pId, ResourceKey<T> pKey, T pValue, Lifecycle pLifecycle);

   public abstract boolean isEmpty();
}