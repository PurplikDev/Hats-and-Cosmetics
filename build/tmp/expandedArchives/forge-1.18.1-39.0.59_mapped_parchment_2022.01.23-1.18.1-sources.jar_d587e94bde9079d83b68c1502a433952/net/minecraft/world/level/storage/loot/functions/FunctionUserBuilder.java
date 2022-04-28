package net.minecraft.world.level.storage.loot.functions;

/**
 * Base interface for builders that accept loot functions.
 * 
 * @see LootItemFunction
 */
public interface FunctionUserBuilder<T> {
   T apply(LootItemFunction.Builder pFunctionBuilder);

   T unwrap();
}