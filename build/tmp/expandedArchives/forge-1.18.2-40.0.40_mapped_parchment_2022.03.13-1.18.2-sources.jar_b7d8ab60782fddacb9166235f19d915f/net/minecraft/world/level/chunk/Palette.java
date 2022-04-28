package net.minecraft.world.level.chunk;

import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;

public interface Palette<T> {
   int idFor(T pState);

   boolean maybeHas(Predicate<T> pFilter);

   T valueFor(int pId);

   void read(FriendlyByteBuf pBuffer);

   void write(FriendlyByteBuf pBuffer);

   int getSerializedSize();

   int getSize();

   Palette<T> copy();

   public interface Factory {
      <A> Palette<A> create(int p_188027_, IdMap<A> p_188028_, PaletteResize<A> p_188029_, List<A> p_188030_);
   }
}