package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.item.ItemEntity;

public class GoToWantedItem<E extends LivingEntity> extends Behavior<E> {
   private final Predicate<E> predicate;
   private final int maxDistToWalk;
   private final float speedModifier;

   public GoToWantedItem(float p_23140_, boolean p_23141_, int p_23142_) {
      this((p_23158_) -> {
         return true;
      }, p_23140_, p_23141_, p_23142_);
   }

   public GoToWantedItem(Predicate<E> p_23144_, float p_23145_, boolean p_23146_, int p_23147_) {
      super(ImmutableMap.of(MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED, MemoryModuleType.WALK_TARGET, p_23146_ ? MemoryStatus.REGISTERED : MemoryStatus.VALUE_ABSENT, MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, MemoryStatus.VALUE_PRESENT));
      this.predicate = p_23144_;
      this.maxDistToWalk = p_23147_;
      this.speedModifier = p_23145_;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.predicate.test(pOwner) && this.getClosestLovedItem(pOwner).closerThan(pOwner, (double)this.maxDistToWalk);
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      BehaviorUtils.setWalkAndLookTargetMemories(pEntity, this.getClosestLovedItem(pEntity), this.speedModifier, 0);
   }

   private ItemEntity getClosestLovedItem(E p_23156_) {
      return p_23156_.getBrain().getMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM).get();
   }
}