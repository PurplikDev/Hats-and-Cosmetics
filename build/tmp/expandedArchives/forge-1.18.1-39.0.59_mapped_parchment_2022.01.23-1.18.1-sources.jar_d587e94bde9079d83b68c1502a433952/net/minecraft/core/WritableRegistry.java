package net.minecraft.core;

import com.mojang.serialization.Lifecycle;
import java.util.OptionalInt;
import net.minecraft.resources.ResourceKey;

public abstract class WritableRegistry<T> extends Registry<T> {
   public WritableRegistry(ResourceKey<? extends Registry<T>> p_123346_, Lifecycle p_123347_) {
      super(p_123346_, p_123347_);
   }

   public abstract <V extends T> V registerMapping(int pId, ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle);

   public abstract <V extends T> V register(ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle);

   public abstract <V extends T> V registerOrOverride(OptionalInt pId, ResourceKey<T> pKey, V pValue, Lifecycle pLifecycle);

   public abstract boolean isEmpty();
}