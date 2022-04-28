package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.phys.Vec3;

public class StrollAroundPoi extends Behavior<PathfinderMob> {
   private static final int MIN_TIME_BETWEEN_STROLLS = 180;
   private static final int STROLL_MAX_XZ_DIST = 8;
   private static final int STROLL_MAX_Y_DIST = 6;
   private final MemoryModuleType<GlobalPos> memoryType;
   private long nextOkStartTime;
   private final int maxDistanceFromPoi;
   private final float speedModifier;

   public StrollAroundPoi(MemoryModuleType<GlobalPos> p_24308_, float p_24309_, int p_24310_) {
      super(ImmutableMap.of(MemoryModuleType.WALK_TARGET, MemoryStatus.REGISTERED, p_24308_, MemoryStatus.VALUE_PRESENT));
      this.memoryType = p_24308_;
      this.speedModifier = p_24309_;
      this.maxDistanceFromPoi = p_24310_;
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, PathfinderMob pOwner) {
      Optional<GlobalPos> optional = pOwner.getBrain().getMemory(this.memoryType);
      return optional.isPresent() && pLevel.dimension() == optional.get().dimension() && optional.get().pos().closerThan(pOwner.position(), (double)this.maxDistanceFromPoi);
   }

   protected void start(ServerLevel pLevel, PathfinderMob pEntity, long pGameTime) {
      if (pGameTime > this.nextOkStartTime) {
         Optional<Vec3> optional = Optional.ofNullable(LandRandomPos.getPos(pEntity, 8, 6));
         pEntity.getBrain().setMemory(MemoryModuleType.WALK_TARGET, optional.map((p_24326_) -> {
            return new WalkTarget(p_24326_, this.speedModifier, 1);
         }));
         this.nextOkStartTime = pGameTime + 180L;
      }

   }
}