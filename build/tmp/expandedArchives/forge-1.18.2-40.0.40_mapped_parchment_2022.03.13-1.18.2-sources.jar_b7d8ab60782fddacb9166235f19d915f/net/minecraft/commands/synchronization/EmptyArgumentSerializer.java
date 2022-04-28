package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;

public class EmptyArgumentSerializer<T extends ArgumentType<?>> implements ArgumentSerializer<T> {
   private final Supplier<T> constructor;

   public EmptyArgumentSerializer(Supplier<T> pConstructor) {
      this.constructor = pConstructor;
   }

   public void serializeToNetwork(T pArgument, FriendlyByteBuf pBuffer) {
   }

   public T deserializeFromNetwork(FriendlyByteBuf pBuffer) {
      return this.constructor.get();
   }

   public void serializeToJson(T pArgument, JsonObject pJson) {
   }
}