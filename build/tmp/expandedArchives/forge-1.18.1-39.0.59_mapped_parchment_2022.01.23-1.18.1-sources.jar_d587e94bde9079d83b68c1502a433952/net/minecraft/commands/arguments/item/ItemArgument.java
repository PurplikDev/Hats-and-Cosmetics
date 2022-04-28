package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import net.minecraft.tags.ItemTags;

public class ItemArgument implements ArgumentType<ItemInput> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "stick{foo=bar}");

   public static ItemArgument item() {
      return new ItemArgument();
   }

   public ItemInput parse(StringReader pReader) throws CommandSyntaxException {
      ItemParser itemparser = (new ItemParser(pReader, false)).parse();
      return new ItemInput(itemparser.getItem(), itemparser.getNbt());
   }

   public static <S> ItemInput getItem(CommandContext<S> pContext, String pName) {
      return pContext.getArgument(pName, ItemInput.class);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      StringReader stringreader = new StringReader(pBuilder.getInput());
      stringreader.setCursor(pBuilder.getStart());
      ItemParser itemparser = new ItemParser(stringreader, false);

      try {
         itemparser.parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
      }

      return itemparser.fillSuggestions(pBuilder, ItemTags.getAllTags());
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}