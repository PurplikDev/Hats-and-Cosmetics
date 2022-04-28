package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.NearestVisibleLivingEntities;
import net.minecraft.world.phys.AABB;

public class NearestLivingEntitySensor extends Sensor<LivingEntity> {
   protected void doTick(ServerLevel pLevel, LivingEntity pEntity) {
      AABB aabb = pEntity.getBoundingBox().inflate(16.0D, 16.0D, 16.0D);
      List<LivingEntity> list = pLevel.getEntitiesOfClass(LivingEntity.class, aabb, (p_26717_) -> {
         return p_26717_ != pEntity && p_26717_.isAlive();
      });
      list.sort(Comparator.comparingDouble(pEntity::distanceToSqr));
      Brain<?> brain = pEntity.getBrain();
      brain.setMemory(MemoryModuleType.NEAREST_LIVING_ENTITIES, list);
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES, new NearestVisibleLivingEntities(pEntity, list));
   }

   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_LIVING_ENTITIES, MemoryModuleType.NEAREST_VISIBLE_LIVING_ENTITIES);
   }
}