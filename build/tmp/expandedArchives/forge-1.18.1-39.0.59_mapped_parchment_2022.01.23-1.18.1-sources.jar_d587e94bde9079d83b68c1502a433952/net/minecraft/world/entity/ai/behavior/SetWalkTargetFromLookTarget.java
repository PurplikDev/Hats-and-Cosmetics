package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Function;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class SetWalkTargetFromLookTarget extends Behavior<LivingEntity> {
   private final Function<LivingEntity, Float> speedModifier;
   private final int closeEnoughDistance;
   private final Predicate<LivingEntity> canSetWalkTargetPredicate;

   public SetWalkTargetFromLookTarget(float p_24084_, int p_24085_) {
      this((p_182369_) -> {
         return true;
      }, (p_182364_) -> {
         return p_24084_;
      }, p_24085_);
   }

   public SetWalkTargetFromLookTarget(Predicate<LivingEntity> p_182359_, Function<LivingEntity, Float> p_182360_, int p_182361_) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.VALUE_PRESENT));
      this.speedModifier = p_182360_;
      this.closeEnoughDistance = p_182361_;
      this.canSetWalkTargetPredicate = p_182359_;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, LivingEntity pOwner) {
      return this.canSetWalkTargetPredicate.test(pOwner);
   }

   protected void start(ServerLevel pLevel, LivingEntity pEntity, long pGameTime) {
      Brain<?> brain = pEntity.getBrain();
      PositionTracker positiontracker = brain.getMemory(MemoryModuleType.LOOK_TARGET).get();
      brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(positiontracker, this.speedModifier.apply(pEntity), this.closeEnoughDistance));
   }
}