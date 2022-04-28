package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class GoToCelebrateLocation<E extends Mob> extends Behavior<E> {
   private final int closeEnoughDist;
   private final float speedModifier;

   public GoToCelebrateLocation(int p_23057_, float p_23058_) {
      super(ImmutableMap.of(MemoryModuleType.CELEBRATE_LOCATION, MemoryStatus.VALUE_PRESENT, MemoryModuleType.ATTACK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.WALK_TARGET, MemoryStatus.VALUE_ABSENT, MemoryModuleType.LOOK_TARGET, MemoryStatus.REGISTERED));
      this.closeEnoughDist = p_23057_;
      this.speedModifier = p_23058_;
   }

   protected void start(ServerLevel pLevel, Mob pEntity, long pGameTime) {
      BlockPos blockpos = getCelebrateLocation(pEntity);
      boolean flag = blockpos.closerThan(pEntity.blockPosition(), (double)this.closeEnoughDist);
      if (!flag) {
         BehaviorUtils.setWalkAndLookTargetMemories(pEntity, getNearbyPos(pEntity, blockpos), this.speedModifier, this.closeEnoughDist);
      }

   }

   private static BlockPos getNearbyPos(Mob p_23070_, BlockPos p_23071_) {
      Random random = p_23070_.level.random;
      return p_23071_.offset(getRandomOffset(random), 0, getRandomOffset(random));
   }

   private static int getRandomOffset(Random p_23073_) {
      return p_23073_.nextInt(3) - 1;
   }

   private static BlockPos getCelebrateLocation(Mob p_23068_) {
      return p_23068_.getBrain().getMemory(MemoryModuleType.CELEBRATE_LOCATION).get();
   }
}