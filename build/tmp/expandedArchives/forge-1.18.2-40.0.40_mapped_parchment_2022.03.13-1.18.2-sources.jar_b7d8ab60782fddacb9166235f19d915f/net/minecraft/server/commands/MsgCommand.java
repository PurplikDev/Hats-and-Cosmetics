package net.minecraft.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class MsgCommand {
   public static void register(CommandDispatcher<CommandSourceStack> pDispatcher) {
      LiteralCommandNode<CommandSourceStack> literalcommandnode = pDispatcher.register(Commands.literal("msg").then(Commands.argument("targets", EntityArgument.players()).then(Commands.argument("message", MessageArgument.message()).executes((p_138063_) -> {
         return sendMessage(p_138063_.getSource(), EntityArgument.getPlayers(p_138063_, "targets"), MessageArgument.getMessage(p_138063_, "message"));
      }))));
      pDispatcher.register(Commands.literal("tell").redirect(literalcommandnode));
      pDispatcher.register(Commands.literal("w").redirect(literalcommandnode));
   }

   private static int sendMessage(CommandSourceStack pSource, Collection<ServerPlayer> pRecipients, Component pMessage) {
      UUID uuid = pSource.getEntity() == null ? Util.NIL_UUID : pSource.getEntity().getUUID();
      Entity entity = pSource.getEntity();
      Consumer<Component> consumer;
      if (entity instanceof ServerPlayer) {
         ServerPlayer serverplayer = (ServerPlayer)entity;
         consumer = (p_138059_) -> {
            serverplayer.sendMessage((new TranslatableComponent("commands.message.display.outgoing", p_138059_, pMessage)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), serverplayer.getUUID());
         };
      } else {
         consumer = (p_138071_) -> {
            pSource.sendSuccess((new TranslatableComponent("commands.message.display.outgoing", p_138071_, pMessage)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), false);
         };
      }

      for(ServerPlayer serverplayer1 : pRecipients) {
         consumer.accept(serverplayer1.getDisplayName());
         serverplayer1.sendMessage((new TranslatableComponent("commands.message.display.incoming", pSource.getDisplayName(), pMessage)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC}), uuid);
      }

      return pRecipients.size();
   }
}