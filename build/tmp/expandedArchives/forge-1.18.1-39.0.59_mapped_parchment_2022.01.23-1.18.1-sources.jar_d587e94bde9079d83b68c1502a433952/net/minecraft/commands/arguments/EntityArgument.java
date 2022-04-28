package net.minecraft.commands.arguments;

import com.google.common.collect.Iterables;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

public class EntityArgument implements ArgumentType<EntitySelector> {
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "@e", "@e[type=foo]", "dd12be42-52a9-4a91-a8a1-11c01849e498");
   public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_ENTITY = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.toomany"));
   public static final SimpleCommandExceptionType ERROR_NOT_SINGLE_PLAYER = new SimpleCommandExceptionType(new TranslatableComponent("argument.player.toomany"));
   public static final SimpleCommandExceptionType ERROR_ONLY_PLAYERS_ALLOWED = new SimpleCommandExceptionType(new TranslatableComponent("argument.player.entities"));
   public static final SimpleCommandExceptionType NO_ENTITIES_FOUND = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.notfound.entity"));
   public static final SimpleCommandExceptionType NO_PLAYERS_FOUND = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.notfound.player"));
   public static final SimpleCommandExceptionType ERROR_SELECTORS_NOT_ALLOWED = new SimpleCommandExceptionType(new TranslatableComponent("argument.entity.selector.not_allowed"));
   private static final byte FLAG_SINGLE = 1;
   private static final byte FLAG_PLAYERS_ONLY = 2;
   final boolean single;
   final boolean playersOnly;

   protected EntityArgument(boolean pSingle, boolean pPlayersOnly) {
      this.single = pSingle;
      this.playersOnly = pPlayersOnly;
   }

   public static EntityArgument entity() {
      return new EntityArgument(true, false);
   }

   public static Entity getEntity(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, EntitySelector.class).findSingleEntity(pContext.getSource());
   }

   public static EntityArgument entities() {
      return new EntityArgument(false, false);
   }

   public static Collection<? extends Entity> getEntities(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      Collection<? extends Entity> collection = getOptionalEntities(pContext, pName);
      if (collection.isEmpty()) {
         throw NO_ENTITIES_FOUND.create();
      } else {
         return collection;
      }
   }

   public static Collection<? extends Entity> getOptionalEntities(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, EntitySelector.class).findEntities(pContext.getSource());
   }

   public static Collection<ServerPlayer> getOptionalPlayers(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, EntitySelector.class).findPlayers(pContext.getSource());
   }

   public static EntityArgument player() {
      return new EntityArgument(true, true);
   }

   public static ServerPlayer getPlayer(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, EntitySelector.class).findSinglePlayer(pContext.getSource());
   }

   public static EntityArgument players() {
      return new EntityArgument(false, true);
   }

   public static Collection<ServerPlayer> getPlayers(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      List<ServerPlayer> list = pContext.getArgument(pName, EntitySelector.class).findPlayers(pContext.getSource());
      if (list.isEmpty()) {
         throw NO_PLAYERS_FOUND.create();
      } else {
         return list;
      }
   }

   public EntitySelector parse(StringReader pReader) throws CommandSyntaxException {
      int i = 0;
      EntitySelectorParser entityselectorparser = new EntitySelectorParser(pReader);
      EntitySelector entityselector = entityselectorparser.parse();
      if (entityselector.getMaxResults() > 1 && this.single) {
         if (this.playersOnly) {
            pReader.setCursor(0);
            throw ERROR_NOT_SINGLE_PLAYER.createWithContext(pReader);
         } else {
            pReader.setCursor(0);
            throw ERROR_NOT_SINGLE_ENTITY.createWithContext(pReader);
         }
      } else if (entityselector.includesEntities() && this.playersOnly && !entityselector.isSelfSelector()) {
         pReader.setCursor(0);
         throw ERROR_ONLY_PLAYERS_ALLOWED.createWithContext(pReader);
      } else {
         return entityselector;
      }
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      if (pContext.getSource() instanceof SharedSuggestionProvider) {
         StringReader stringreader = new StringReader(pBuilder.getInput());
         stringreader.setCursor(pBuilder.getStart());
         SharedSuggestionProvider sharedsuggestionprovider = (SharedSuggestionProvider)pContext.getSource();
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(stringreader, sharedsuggestionprovider.hasPermission(2));

         try {
            entityselectorparser.parse();
         } catch (CommandSyntaxException commandsyntaxexception) {
         }

         return entityselectorparser.fillSuggestions(pBuilder, (p_91457_) -> {
            Collection<String> collection = sharedsuggestionprovider.getOnlinePlayerNames();
            Iterable<String> iterable = (Iterable<String>)(this.playersOnly ? collection : Iterables.concat(collection, sharedsuggestionprovider.getSelectedEntities()));
            SharedSuggestionProvider.suggest(iterable, p_91457_);
         });
      } else {
         return Suggestions.empty();
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public static class Serializer implements ArgumentSerializer<EntityArgument> {
      public void serializeToNetwork(EntityArgument pArgument, FriendlyByteBuf pBuffer) {
         byte b0 = 0;
         if (pArgument.single) {
            b0 = (byte)(b0 | 1);
         }

         if (pArgument.playersOnly) {
            b0 = (byte)(b0 | 2);
         }

         pBuffer.writeByte(b0);
      }

      public EntityArgument deserializeFromNetwork(FriendlyByteBuf pBuffer) {
         byte b0 = pBuffer.readByte();
         return new EntityArgument((b0 & 1) != 0, (b0 & 2) != 0);
      }

      public void serializeToJson(EntityArgument pArgument, JsonObject pJson) {
         pJson.addProperty("amount", pArgument.single ? "single" : "multiple");
         pJson.addProperty("type", pArgument.playersOnly ? "players" : "entities");
      }
   }
}