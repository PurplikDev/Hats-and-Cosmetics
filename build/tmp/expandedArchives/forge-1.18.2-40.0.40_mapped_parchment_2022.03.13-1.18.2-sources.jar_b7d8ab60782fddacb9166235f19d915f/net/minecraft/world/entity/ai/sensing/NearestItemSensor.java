package net.minecraft.world.entity.ai.sensing;

import com.google.common.collect.ImmutableSet;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;

public class NearestItemSensor extends Sensor<Mob> {
   private static final long XZ_RANGE = 8L;
   private static final long Y_RANGE = 4L;
   public static final int MAX_DISTANCE_TO_WANTED_ITEM = 9;

   public Set<MemoryModuleType<?>> requires() {
      return ImmutableSet.of(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM);
   }

   protected void doTick(ServerLevel pLevel, Mob pEntity) {
      Brain<?> brain = pEntity.getBrain();
      List<ItemEntity> list = pLevel.getEntitiesOfClass(ItemEntity.class, pEntity.getBoundingBox().inflate(8.0D, 4.0D, 8.0D), (p_26703_) -> {
         return true;
      });
      list.sort(Comparator.comparingDouble(pEntity::distanceToSqr));
      Optional<ItemEntity> optional = list.stream().filter((p_26706_) -> {
         return pEntity.wantsToPickUp(p_26706_.getItem());
      }).filter((p_26701_) -> {
         return p_26701_.closerThan(pEntity, 9.0D);
      }).filter(pEntity::hasLineOfSight).findFirst();
      brain.setMemory(MemoryModuleType.NEAREST_VISIBLE_WANTED_ITEM, optional);
   }
}