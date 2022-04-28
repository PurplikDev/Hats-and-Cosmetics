package net.minecraft.world.level.storage.loot.predicates;

/**
 * Base interface for builders that can accept loot conditions.
 * 
 * @see LootItemCondition
 */
public interface ConditionUserBuilder<T> {
   T when(LootItemCondition.Builder pConditionBuilder);

   T unwrap();
}