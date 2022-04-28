package net.minecraft.core;

import javax.annotation.Nullable;

public interface IdMap<T> extends Iterable<T> {
   int DEFAULT = -1;

   /**
    * @return the integer ID used to identify the given object
    */
   int getId(T pValue);

   @Nullable
   T byId(int pId);

   default T byIdOrThrow(int p_200958_) {
      T t = this.byId(p_200958_);
      if (t == null) {
         throw new IllegalArgumentException("No value with id " + p_200958_);
      } else {
         return t;
      }
   }

   int size();
}