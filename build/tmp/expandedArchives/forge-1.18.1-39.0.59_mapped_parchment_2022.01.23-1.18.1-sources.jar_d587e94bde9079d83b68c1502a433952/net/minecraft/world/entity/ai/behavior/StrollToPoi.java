package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;

public class StrollToPoi extends Behavior<PathfinderMob> {
   private final MemoryModuleType<GlobalPos> memoryType;
   private final int closeEnoughDist;
   private final int maxDistanceFromPoi;
   private final float speedModifier;
   private long nextOkStartTime;

   public StrollToPoi(MemoryModuleType<GlobalPos> p_24333_, float p_24334_, int p_24335_, int p_24336_) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, p_24333_, MemoryStatus.VALUE_PRESENT));
      this.memoryType = p_24333_;
      this.speedModifier = p_24334_;
      this.closeEnoughDist = p_24335_;
      this.maxDistanceFromPoi = p_24336_;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      Optional<GlobalPos> optional = pOwner.getBrain().getMemory(this.memoryType);
      return optional.isPresent() && pLevel.dimension() == optional.get().dimension() && optional.get().pos().closerThan(pOwner.position(), (double)this.maxDistanceFromPoi);
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      if (pGameTime > this.nextOkStartTime) {
         Brain<?> brain = pEntity.getBrain();
         Optional<GlobalPos> optional = brain.getMemory(this.memoryType);
         optional.ifPresent((p_24353_) -> {
            brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(p_24353_.pos(), this.speedModifier, this.closeEnoughDist));
         });
         this.nextOkStartTime = pGameTime + 80L;
      }

   }
}