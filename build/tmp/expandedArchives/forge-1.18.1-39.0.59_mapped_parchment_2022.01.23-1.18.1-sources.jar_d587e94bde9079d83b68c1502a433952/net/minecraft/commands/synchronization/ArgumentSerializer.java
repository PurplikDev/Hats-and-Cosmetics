package net.minecraft.commands.synchronization;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.ArgumentType;
import net.minecraft.network.FriendlyByteBuf;

public interface ArgumentSerializer<T extends ArgumentType<?>> {
   void serializeToNetwork(T pArgument, FriendlyByteBuf pBuffer);

   T deserializeFromNetwork(FriendlyByteBuf pBuffer);

   void serializeToJson(T pArgument, JsonObject pJson);
}