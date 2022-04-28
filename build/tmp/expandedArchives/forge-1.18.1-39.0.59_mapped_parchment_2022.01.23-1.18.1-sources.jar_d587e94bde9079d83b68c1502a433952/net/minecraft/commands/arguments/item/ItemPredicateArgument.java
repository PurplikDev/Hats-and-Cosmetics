package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.Tag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemPredicateArgument implements ArgumentType<ItemPredicateArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("stick", "minecraft:stick", "#stick", "#stick{foo=bar}");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((p_121047_) -> {
      return new TranslatableComponent("arguments.item.tag.unknown", p_121047_);
   });

   public static ItemPredicateArgument itemPredicate() {
      return new ItemPredicateArgument();
   }

   public ItemPredicateArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
      ItemParser itemparser = (new ItemParser(pReader, true)).parse();
      if (itemparser.getItem() != null) {
         ItemPredicateArgument.ItemPredicate itempredicateargument$itempredicate = new ItemPredicateArgument.ItemPredicate(itemparser.getItem(), itemparser.getNbt());
         return (p_121045_) -> {
            return itempredicateargument$itempredicate;
         };
      } else {
         ResourceLocation resourcelocation = itemparser.getTag();
         return (p_175098_) -> {
            Tag<Item> tag = p_175098_.getSource().getServer().getTags().getTagOrThrow(Registry.ITEM_REGISTRY, resourcelocation, (p_175094_) -> {
               return ERROR_UNKNOWN_TAG.create(p_175094_.toString());
            });
            return new ItemPredicateArgument.TagPredicate(tag, itemparser.getNbt());
         };
      }
   }

   public static Predicate<ItemStack> getItemPredicate(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, ItemPredicateArgument.Result.class).create(pContext);
   }

   public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> pContext, SuggestionsBuilder pBuilder) {
      StringReader stringreader = new StringReader(pBuilder.getInput());
      stringreader.setCursor(pBuilder.getStart());
      ItemParser itemparser = new ItemParser(stringreader, true);

      try {
         itemparser.parse();
      } catch (CommandSyntaxException commandsyntaxexception) {
      }

      return itemparser.fillSuggestions(pBuilder, ItemTags.getAllTags());
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   static class ItemPredicate implements Predicate<ItemStack> {
      private final Item item;
      @Nullable
      private final CompoundTag nbt;

      public ItemPredicate(Item pItem, @Nullable CompoundTag pTag) {
         this.item = pItem;
         this.nbt = pTag;
      }

      public boolean test(ItemStack pStack) {
         return pStack.is(this.item) && NbtUtils.compareNbt(this.nbt, pStack.getTag(), true);
      }
   }

   public interface Result {
      Predicate<ItemStack> create(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
   }

   static class TagPredicate implements Predicate<ItemStack> {
      private final Tag<Item> tag;
      @Nullable
      private final CompoundTag nbt;

      public TagPredicate(Tag<Item> pTag, @Nullable CompoundTag pNbt) {
         this.tag = pTag;
         this.nbt = pNbt;
      }

      public boolean test(ItemStack pStack) {
         return pStack.is(this.tag) && NbtUtils.compareNbt(this.nbt, pStack.getTag(), true);
      }
   }
}