package net.minecraft.commands.arguments;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import java.util.Arrays;
import java.util.Collection;
import net.minecraft.advancements.Advancement;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.loot.ItemModifierManager;
import net.minecraft.world.level.storage.loot.PredicateManager;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class ResourceLocationArgument implements ArgumentType<ResourceLocation> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "012");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ADVANCEMENT = new DynamicCommandExceptionType((p_107010_) -> {
      return new TranslatableComponent("advancement.advancementNotFound", p_107010_);
   });
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_RECIPE = new DynamicCommandExceptionType((p_107005_) -> {
      return new TranslatableComponent("recipe.notFound", p_107005_);
   });
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_PREDICATE = new DynamicCommandExceptionType((p_106998_) -> {
      return new TranslatableComponent("predicate.unknown", p_106998_);
   });
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ATTRIBUTE = new DynamicCommandExceptionType((p_106991_) -> {
      return new TranslatableComponent("attribute.unknown", p_106991_);
   });
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_ITEM_MODIFIER = new DynamicCommandExceptionType((p_171026_) -> {
      return new TranslatableComponent("item_modifier.unknown", p_171026_);
   });

   public static ResourceLocationArgument id() {
      return new ResourceLocationArgument();
   }

   public static Advancement getAdvancement(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      ResourceLocation resourcelocation = pContext.getArgument(pName, ResourceLocation.class);
      Advancement advancement = pContext.getSource().getAdvancement(resourcelocation);
      if (advancement == null) {
         throw ERROR_UNKNOWN_ADVANCEMENT.create(resourcelocation);
      } else {
         return advancement;
      }
   }

   public static Recipe<?> getRecipe(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      RecipeManager recipemanager = pContext.getSource().getRecipeManager();
      ResourceLocation resourcelocation = pContext.getArgument(pName, ResourceLocation.class);
      return recipemanager.byKey(resourcelocation).orElseThrow(() -> {
         return ERROR_UNKNOWN_RECIPE.create(resourcelocation);
      });
   }

   public static LootItemCondition getPredicate(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      ResourceLocation resourcelocation = pContext.getArgument(pName, ResourceLocation.class);
      PredicateManager predicatemanager = pContext.getSource().getServer().getPredicateManager();
      LootItemCondition lootitemcondition = predicatemanager.get(resourcelocation);
      if (lootitemcondition == null) {
         throw ERROR_UNKNOWN_PREDICATE.create(resourcelocation);
      } else {
         return lootitemcondition;
      }
   }

   public static LootItemFunction getItemModifier(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      ResourceLocation resourcelocation = pContext.getArgument(pName, ResourceLocation.class);
      ItemModifierManager itemmodifiermanager = pContext.getSource().getServer().getItemModifierManager();
      LootItemFunction lootitemfunction = itemmodifiermanager.get(resourcelocation);
      if (lootitemfunction == null) {
         throw ERROR_UNKNOWN_ITEM_MODIFIER.create(resourcelocation);
      } else {
         return lootitemfunction;
      }
   }

   public static Attribute getAttribute(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      ResourceLocation resourcelocation = pContext.getArgument(pName, ResourceLocation.class);
      return Registry.ATTRIBUTE.getOptional(resourcelocation).orElseThrow(() -> {
         return ERROR_UNKNOWN_ATTRIBUTE.create(resourcelocation);
      });
   }

   public static ResourceLocation getId(CommandContext<CommandSourceStack> pContext, String pName) {
      return pContext.getArgument(pName, ResourceLocation.class);
   }

   public ResourceLocation parse(StringReader pReader) throws CommandSyntaxException {
      return ResourceLocation.read(pReader);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }
}
