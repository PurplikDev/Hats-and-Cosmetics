package net.minecraft.commands.synchronization.brigadier;

import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;

public class DoubleArgumentSerializer implements ArgumentSerializer<DoubleArgumentType> {
   public void serializeToNetwork(DoubleArgumentType pArgument, FriendlyByteBuf pBuffer) {
      boolean flag = pArgument.getMinimum() != -Double.MAX_VALUE;
      boolean flag1 = pArgument.getMaximum() != Double.MAX_VALUE;
      pBuffer.writeByte(BrigadierArgumentSerializers.createNumberFlags(flag, flag1));
      if (flag) {
         pBuffer.writeDouble(pArgument.getMinimum());
      }

      if (flag1) {
         pBuffer.writeDouble(pArgument.getMaximum());
      }

   }

   public DoubleArgumentType deserializeFromNetwork(FriendlyByteBuf pBuffer) {
      byte b0 = pBuffer.readByte();
      double d0 = BrigadierArgumentSerializers.numberHasMin(b0) ? pBuffer.readDouble() : -Double.MAX_VALUE;
      double d1 = BrigadierArgumentSerializers.numberHasMax(b0) ? pBuffer.readDouble() : Double.MAX_VALUE;
      return DoubleArgumentType.doubleArg(d0, d1);
   }

   public void serializeToJson(DoubleArgumentType pArgument, JsonObject pJson) {
      if (pArgument.getMinimum() != -Double.MAX_VALUE) {
         pJson.addProperty("min", pArgument.getMinimum());
      }

      if (pArgument.getMaximum() != Double.MAX_VALUE) {
         pJson.addProperty("max", pArgument.getMaximum());
      }

   }
}