package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class BecomePassiveIfMemoryPresent extends Behavior<LivingEntity> {
   private final int pacifyDuration;

   public BecomePassiveIfMemoryPresent(MemoryModuleType<?> p_22516_, int p_22517_) {
      super(ImmutableMap.of(MemoryModuleType.ATTACK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.PACIFIED, MemoryStatus.VALUE_ABSENT, p_22516_, MemoryStatus.VALUE_PRESENT));
      this.pacifyDuration = p_22517_;
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      pEntity.getBrain().setMemoryWithExpiry(MemoryModuleType.PACIFIED, true, (long)this.pacifyDuration);
      pEntity.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
   }
}