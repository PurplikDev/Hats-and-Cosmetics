package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class EraseMemoryIf<E extends LivingEntity> extends Behavior<E> {
   private final Predicate<E> predicate;
   private final MemoryModuleType<?> memoryType;

   public EraseMemoryIf(Predicate<E> p_22859_, MemoryModuleType<?> p_22860_) {
      super(ImmutableMap.of(p_22860_, MemoryStatus.VALUE_PRESENT));
      this.predicate = p_22859_;
      this.memoryType = p_22860_;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.predicate.test(pOwner);
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      pEntity.getBrain().eraseMemory(this.memoryType);
   }
}