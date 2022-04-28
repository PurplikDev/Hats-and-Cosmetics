package net.minecraft.commands.arguments;

import com.google.common.collect.Lists;
import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.commands.synchronization.ArgumentSerializer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.world.entity.Entity;

public class ScoreHolderArgument implements ArgumentType<ScoreHolderArgument.Result> {
   public static final SuggestionProvider<CommandSourceStack> SUGGEST_SCORE_HOLDERS = (p_108221_, p_108222_) -> {
      StringReader stringreader = new StringReader(p_108222_.getInput());
      stringreader.setCursor(p_108222_.getStart());
      EntitySelectorParser entityselectorparser = new EntitySelectorParser(stringreader);

      try {
         entityselectorparser.parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
      }

      return entityselectorparser.fillSuggestions(p_108222_, (p_171606_) -> {
         SharedSuggestionProvider.suggest(p_108221_.getSource().getOnlinePlayerNames(), p_171606_);
      });
   };
   private static final Collection<String> EXAMPLES = Arrays.asList("Player", "0123", "*", "@e");
   private static final SimpleCommandExceptionType ERROR_NO_RESULTS = new SimpleCommandExceptionType(new TranslatableComponent("argument.scoreHolder.empty"));
   private static final byte FLAG_MULTIPLE = 1;
   final boolean multiple;

   public ScoreHolderArgument(boolean pMultiple) {
      this.multiple = pMultiple;
   }

   /**
    * Gets a single score holder, with no objectives list.
    */
   public static String getName(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getNames(pContext, pName).iterator().next();
   }

   /**
    * Gets one or more score holders, with no objectives list.
    */
   public static Collection<String> getNames(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getNames(pContext, pName, Collections::emptyList);
   }

   /**
    * Gets one or more score holders, using the server's complete list of objectives.
    */
   public static Collection<String> getNamesWithDefaultWildcard(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return getNames(pContext, pName, pContext.getSource().getServer().getScoreboard()::getTrackedPlayers);
   }

   /**
    * Gets one or more score holders.
    */
   public static Collection<String> getNames(CommandContext<CommandSourceStack> pContext, String pName, Supplier<Collection<String>> pObjectives) throws CommandSyntaxException {
      Collection<String> collection = pContext.getArgument(pName, ScoreHolderArgument.Result.class).getNames(pContext.getSource(), pObjectives);
      if (collection.isEmpty()) {
         throw EntityArgument.NO_ENTITIES_FOUND.create();
      } else {
         return collection;
      }
   }

   public static ScoreHolderArgument scoreHolder() {
      return new ScoreHolderArgument(false);
   }

   public static ScoreHolderArgument scoreHolders() {
      return new ScoreHolderArgument(true);
   }

   public ScoreHolderArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '@') {
         EntitySelectorParser entityselectorparser = new EntitySelectorParser(pReader);
         EntitySelector entityselector = entityselectorparser.parse();
         if (!this.multiple && entityselector.getMaxResults() > 1) {
            throw EntityArgument.ERROR_NOT_SINGLE_ENTITY.create();
         } else {
            return new ScoreHolderArgument.SelectorResult(entityselector);
         }
      } else {
         int i = pReader.getCursor();

         while(pReader.canRead() && pReader.peek() != ' ') {
            pReader.skip();
         }

         String s = pReader.getString().substring(i, pReader.getCursor());
         if (s.equals("*")) {
            return (p_108231_, p_108232_) -> {
               Collection<String> collection1 = p_108232_.get();
               if (collection1.isEmpty()) {
                  throw ERROR_NO_RESULTS.create();
               } else {
                  return collection1;
               }
            };
         } else {
            Collection<String> collection = Collections.singleton(s);
            return (p_108237_, p_108238_) -> {
               return collection;
            };
         }
      }
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   @FunctionalInterface
   public interface Result {
      Collection<String> getNames(CommandSourceStack pSource, Supplier<Collection<String>> pObjectives) throws CommandSyntaxException;
   }

   public static class SelectorResult implements ScoreHolderArgument.Result {
      private final EntitySelector selector;

      public SelectorResult(EntitySelector pSelector) {
         this.selector = pSelector;
      }

      public Collection<String> getNames(CommandSourceStack pSource, Supplier<Collection<String>> pObjectives) throws CommandSyntaxException {
         List<? extends Entity> list = this.selector.findEntities(pSource);
         if (list.isEmpty()) {
            throw EntityArgument.NO_ENTITIES_FOUND.create();
         } else {
            List<String> list1 = Lists.newArrayList();

            for(Entity entity : list) {
               list1.add(entity.getScoreboardName());
            }

            return list1;
         }
      }
   }

   public static class Serializer implements ArgumentSerializer<ScoreHolderArgument> {
      public void serializeToNetwork(ScoreHolderArgument pArgument, FriendlyByteBuf pBuffer) {
         byte b0 = 0;
         if (pArgument.multiple) {
            b0 = (byte)(b0 | 1);
         }

         pBuffer.writeByte(b0);
      }

      public ScoreHolderArgument deserializeFromNetwork(FriendlyByteBuf pBuffer) {
         byte b0 = pBuffer.readByte();
         boolean flag = (b0 & 1) != 0;
         return new ScoreHolderArgument(flag);
      }

      public void serializeToJson(ScoreHolderArgument pArgument, JsonObject pJson) {
         pJson.addProperty("amount", pArgument.multiple ? "multiple" : "single");
      }
   }
}