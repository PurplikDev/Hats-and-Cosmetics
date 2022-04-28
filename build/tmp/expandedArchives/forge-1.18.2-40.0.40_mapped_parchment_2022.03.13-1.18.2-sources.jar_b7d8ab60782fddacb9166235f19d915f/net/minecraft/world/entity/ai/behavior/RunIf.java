package net.minecraft.world.entity.ai.behavior;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.MemoryStatus;

public class RunIf<E extends LivingEntity> extends Behavior<E> {
   private final Predicate<E> predicate;
   private final Behavior<? super E> wrappedBehavior;
   private final boolean checkWhileRunningAlso;

   public RunIf(Map<MemoryModuleType<?>, MemoryStatus> p_23799_, Predicate<E> p_23800_, Behavior<? super E> p_23801_, boolean p_23802_) {
      super(mergeMaps(p_23799_, p_23801_.entryCondition));
      this.predicate = p_23800_;
      this.wrappedBehavior = p_23801_;
      this.checkWhileRunningAlso = p_23802_;
   }

   private static Map<MemoryModuleType<?>, MemoryStatus> mergeMaps(Map<MemoryModuleType<?>, MemoryStatus> p_23816_, Map<MemoryModuleType<?>, MemoryStatus> p_23817_) {
      Map<MemoryModuleType<?>, MemoryStatus> map = Maps.newHashMap();
      map.putAll(p_23816_);
      map.putAll(p_23817_);
      return map;
   }

   public RunIf(Predicate<E> p_147868_, Behavior<? super E> p_147869_, boolean p_147870_) {
      this(ImmutableMap.of(), p_147868_, p_147869_, p_147870_);
   }

   public RunIf(Predicate<E> p_23804_, Behavior<? super E> p_23805_) {
      this(ImmutableMap.of(), p_23804_, p_23805_, false);
   }

   public RunIf(Map<MemoryModuleType<?>, MemoryStatus> p_147865_, Behavior<? super E> p_147866_) {
      this(p_147865_, (p_147872_) -> {
         return true;
      }, p_147866_, false);
   }

   protected boolean checkExtraStartConditions(ServerLevel pLevel, E pOwner) {
      return this.predicate.test(pOwner) && this.wrappedBehavior.checkExtraStartConditions(pLevel, pOwner);
   }

   protected boolean canStillUse(ServerLevel pLevel, E pEntity, long pGameTime) {
      return this.checkWhileRunningAlso && this.predicate.test(pEntity) && this.wrappedBehavior.canStillUse(pLevel, pEntity, pGameTime);
   }

   protected boolean timedOut(long pGameTime) {
      return false;
   }

   protected void start(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.wrappedBehavior.start(pLevel, pEntity, pGameTime);
   }

   protected void tick(ServerLevel pLevel, E pOwner, long pGameTime) {
      this.wrappedBehavior.tick(pLevel, pOwner, pGameTime);
   }

   protected void stop(ServerLevel pLevel, E pEntity, long pGameTime) {
      this.wrappedBehavior.stop(pLevel, pEntity, pGameTime);
   }

   public String toString() {
      return "RunIf: " + this.wrappedBehavior;
   }
}