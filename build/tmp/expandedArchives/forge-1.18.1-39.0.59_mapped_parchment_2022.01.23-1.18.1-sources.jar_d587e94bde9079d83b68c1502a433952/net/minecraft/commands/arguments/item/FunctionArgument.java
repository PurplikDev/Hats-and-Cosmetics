package net.minecraft.commands.arguments.item;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.datafixers.util.Either;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.commands.CommandFunction;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.Tag;

public class FunctionArgument implements ArgumentType<FunctionArgument.Result> {
   private static final Collection<String> EXAMPLES = Arrays.asList("foo", "foo:bar", "#foo");
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_TAG = new DynamicCommandExceptionType((p_120927_) -> {
      return new TranslatableComponent("arguments.function.tag.unknown", p_120927_);
   });
   private static final DynamicCommandExceptionType ERROR_UNKNOWN_FUNCTION = new DynamicCommandExceptionType((p_120917_) -> {
      return new TranslatableComponent("arguments.function.unknown", p_120917_);
   });

   public static FunctionArgument functions() {
      return new FunctionArgument();
   }

   public FunctionArgument.Result parse(StringReader pReader) throws CommandSyntaxException {
      if (pReader.canRead() && pReader.peek() == '#') {
         pReader.skip();
         final ResourceLocation resourcelocation1 = ResourceLocation.read(pReader);
         return new FunctionArgument.Result() {
            public Collection<CommandFunction> create(CommandContext<CommandSourceStack> p_120943_) throws CommandSyntaxException {
               Tag<CommandFunction> tag = FunctionArgument.getFunctionTag(p_120943_, resourcelocation1);
               return tag.getValues();
            }

            public Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> p_120945_) throws CommandSyntaxException {
               return Pair.of(resourcelocation1, Either.right(FunctionArgument.getFunctionTag(p_120945_, resourcelocation1)));
            }
         };
      } else {
         final ResourceLocation resourcelocation = ResourceLocation.read(pReader);
         return new FunctionArgument.Result() {
            public Collection<CommandFunction> create(CommandContext<CommandSourceStack> p_120952_) throws CommandSyntaxException {
               return Collections.singleton(FunctionArgument.getFunction(p_120952_, resourcelocation));
            }

            public Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> p_120954_) throws CommandSyntaxException {
               return Pair.of(resourcelocation, Either.left(FunctionArgument.getFunction(p_120954_, resourcelocation)));
            }
         };
      }
   }

   static CommandFunction getFunction(CommandContext<CommandSourceStack> pContext, ResourceLocation pId) throws CommandSyntaxException {
      return pContext.getSource().getServer().getFunctions().get(pId).orElseThrow(() -> {
         return ERROR_UNKNOWN_FUNCTION.create(pId.toString());
      });
   }

   static Tag<CommandFunction> getFunctionTag(CommandContext<CommandSourceStack> pContext, ResourceLocation pId) throws CommandSyntaxException {
      Tag<CommandFunction> tag = pContext.getSource().getServer().getFunctions().getTag(pId);
      if (tag == null) {
         throw ERROR_UNKNOWN_TAG.create(pId.toString());
      } else {
         return tag;
      }
   }

   public static Collection<CommandFunction> getFunctions(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, FunctionArgument.Result.class).create(pContext);
   }

   public static Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> getFunctionOrTag(CommandContext<CommandSourceStack> pContext, String pName) throws CommandSyntaxException {
      return pContext.getArgument(pName, FunctionArgument.Result.class).unwrap(pContext);
   }

   public Collection<String> getExamples() {
      return EXAMPLES;
   }

   public interface Result {
      Collection<CommandFunction> create(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;

      Pair<ResourceLocation, Either<CommandFunction, Tag<CommandFunction>>> unwrap(CommandContext<CommandSourceStack> pContext) throws CommandSyntaxException;
   }
}