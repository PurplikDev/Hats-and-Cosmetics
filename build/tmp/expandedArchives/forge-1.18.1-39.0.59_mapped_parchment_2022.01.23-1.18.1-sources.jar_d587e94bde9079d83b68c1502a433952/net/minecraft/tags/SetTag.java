package net.minecraft.tags;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

public class SetTag<T> implements Tag<T> {
   private final ImmutableList<T> valuesList;
   private final Set<T> values;
   @VisibleForTesting
   protected final Class<?> closestCommonSuperType;

   protected SetTag(Set<T> pValues, Class<?> pClosestCommonSuperType) {
      this.closestCommonSuperType = pClosestCommonSuperType;
      this.values = pValues;
      this.valuesList = ImmutableList.copyOf(pValues);
   }

   public static <T> SetTag<T> empty() {
      return new SetTag<>(ImmutableSet.of(), Void.class);
   }

   public static <T> SetTag<T> create(Set<T> pValues) {
      return new SetTag<>(pValues, findCommonSuperClass(pValues));
   }

   public boolean contains(T pElement) {
      return this.closestCommonSuperType.isInstance(pElement) && this.values.contains(pElement);
   }

   public List<T> getValues() {
      return this.valuesList;
   }

   private static <T> Class<?> findCommonSuperClass(Set<T> pValues) {
      if (pValues.isEmpty()) {
         return Void.class;
      } else {
         Class<?> oclass = null;

         for(T t : pValues) {
            if (oclass == null) {
               oclass = t.getClass();
            } else {
               oclass = findClosestAncestor(oclass, t.getClass());
            }
         }

         return oclass;
      }
   }

   private static Class<?> findClosestAncestor(Class<?> pInput, Class<?> pOther) {
      while(!pInput.isAssignableFrom(pOther)) {
         pInput = pInput.getSuperclass();
      }

      return pInput;
   }
}