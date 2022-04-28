package net.minecraft.gametest.framework;

import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class GameTestHelper {
   private final GameTestInfo testInfo;
   private boolean finalCheckAdded;

   public GameTestHelper(GameTestInfo pTestInfo) {
      this.testInfo = pTestInfo;
   }

   public ServerLevel getLevel() {
      return this.testInfo.getLevel();
   }

   public BlockState getBlockState(BlockPos pPos) {
      return this.getLevel().getBlockState(this.absolutePos(pPos));
   }

   @Nullable
   public BlockEntity getBlockEntity(BlockPos pPos) {
      return this.getLevel().getBlockEntity(this.absolutePos(pPos));
   }

   public void killAllEntities() {
      AABB aabb = this.getBounds();
      List<Entity> list = this.getLevel().getEntitiesOfClass(Entity.class, aabb.inflate(1.0D), (p_177131_) -> {
         return !(p_177131_ instanceof Player);
      });
      list.forEach(Entity::kill);
   }

   public ItemEntity spawnItem(Item pItem, float pX, float pY, float pZ) {
      ServerLevel serverlevel = this.getLevel();
      Vec3 vec3 = this.absoluteVec(new Vec3((double)pX, (double)pY, (double)pZ));
      ItemEntity itementity = new ItemEntity(serverlevel, vec3.x, vec3.y, vec3.z, new ItemStack(pItem, 1));
      itementity.setDeltaMovement(0.0D, 0.0D, 0.0D);
      serverlevel.addFreshEntity(itementity);
      return itementity;
   }

   public <E extends Entity> E spawn(EntityType<E> pType, BlockPos pPos) {
      return this.spawn(pType, Vec3.atBottomCenterOf(pPos));
   }

   public <E extends Entity> E spawn(EntityType<E> pType, Vec3 pPos) {
      ServerLevel serverlevel = this.getLevel();
      E e = pType.create(serverlevel);
      if (e instanceof Mob) {
         ((Mob)e).setPersistenceRequired();
      }

      Vec3 vec3 = this.absoluteVec(pPos);
      e.moveTo(vec3.x, vec3.y, vec3.z, e.getYRot(), e.getXRot());
      serverlevel.addFreshEntity(e);
      return e;
   }

   public <E extends Entity> E spawn(EntityType<E> pType, int pX, int pY, int pZ) {
      return this.spawn(pType, new BlockPos(pX, pY, pZ));
   }

   public <E extends Entity> E spawn(EntityType<E> pType, float pX, float pY, float pZ) {
      return this.spawn(pType, new Vec3((double)pX, (double)pY, (double)pZ));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, BlockPos pPos) {
      E e = this.spawn(pType, pPos);
      e.removeFreeWill();
      return e;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, int pX, int pY, int pZ) {
      return this.spawnWithNoFreeWill(pType, new BlockPos(pX, pY, pZ));
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, Vec3 pPos) {
      E e = this.spawn(pType, pPos);
      e.removeFreeWill();
      return e;
   }

   public <E extends Mob> E spawnWithNoFreeWill(EntityType<E> pType, float pX, float pY, float pZ) {
      return this.spawnWithNoFreeWill(pType, new Vec3((double)pX, (double)pY, (double)pZ));
   }

   public GameTestSequence walkTo(Mob pMob, BlockPos pPos, float pSpeed) {
      return this.startSequence().thenExecuteAfter(2, () -> {
         Path path = pMob.getNavigation().createPath(this.absolutePos(pPos), 0);
         pMob.getNavigation().moveTo(path, (double)pSpeed);
      });
   }

   public void pressButton(int pX, int pY, int pZ) {
      this.pressButton(new BlockPos(pX, pY, pZ));
   }

   public void pressButton(BlockPos pPos) {
      this.assertBlockState(pPos, (p_177212_) -> {
         return p_177212_.is(BlockTags.BUTTONS);
      }, () -> {
         return "Expected button";
      });
      BlockPos blockpos = this.absolutePos(pPos);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      ButtonBlock buttonblock = (ButtonBlock)blockstate.getBlock();
      buttonblock.press(blockstate, this.getLevel(), blockpos);
   }

   public void useBlock(BlockPos pPos) {
      BlockPos blockpos = this.absolutePos(pPos);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      blockstate.use(this.getLevel(), this.makeMockPlayer(), InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(blockpos), Direction.NORTH, blockpos, true));
   }

   public LivingEntity makeAboutToDrown(LivingEntity pEntity) {
      pEntity.setAirSupply(0);
      pEntity.setHealth(0.25F);
      return pEntity;
   }

   public Player makeMockPlayer() {
      return new Player(this.getLevel(), BlockPos.ZERO, 0.0F, new GameProfile(UUID.randomUUID(), "test-mock-player")) {
         /**
          * Returns true if the player is in spectator mode.
          */
         public boolean isSpectator() {
            return false;
         }

         public boolean isCreative() {
            return true;
         }
      };
   }

   public void pullLever(int pX, int pY, int pZ) {
      this.pullLever(new BlockPos(pX, pY, pZ));
   }

   public void pullLever(BlockPos pPos) {
      this.assertBlockPresent(Blocks.LEVER, pPos);
      BlockPos blockpos = this.absolutePos(pPos);
      BlockState blockstate = this.getLevel().getBlockState(blockpos);
      LeverBlock leverblock = (LeverBlock)blockstate.getBlock();
      leverblock.pull(blockstate, this.getLevel(), blockpos);
   }

   public void pulseRedstone(BlockPos pPos, long pDelay) {
      this.setBlock(pPos, Blocks.REDSTONE_BLOCK);
      this.runAfterDelay(pDelay, () -> {
         this.setBlock(pPos, Blocks.AIR);
      });
   }

   public void destroyBlock(BlockPos pPos) {
      this.getLevel().destroyBlock(this.absolutePos(pPos), false, (Entity)null);
   }

   public void setBlock(int pX, int pY, int pZ, Block pBlock) {
      this.setBlock(new BlockPos(pX, pY, pZ), pBlock);
   }

   public void setBlock(int pX, int pY, int pZ, BlockState pState) {
      this.setBlock(new BlockPos(pX, pY, pZ), pState);
   }

   public void setBlock(BlockPos pPos, Block pBlock) {
      this.setBlock(pPos, pBlock.defaultBlockState());
   }

   public void setBlock(BlockPos pPos, BlockState pState) {
      this.getLevel().setBlock(this.absolutePos(pPos), pState, 3);
   }

   public void setNight() {
      this.setDayTime(13000);
   }

   public void setDayTime(int pTime) {
      this.getLevel().setDayTime((long)pTime);
   }

   public void assertBlockPresent(Block pBlock, int pX, int pY, int pZ) {
      this.assertBlockPresent(pBlock, new BlockPos(pX, pY, pZ));
   }

   public void assertBlockPresent(Block pBlock, BlockPos pPos) {
      BlockState blockstate = this.getBlockState(pPos);
      this.assertBlock(pPos, (p_177216_) -> {
         return blockstate.is(pBlock);
      }, "Expected " + pBlock.getName().getString() + ", got " + blockstate.getBlock().getName().getString());
   }

   public void assertBlockNotPresent(Block pBlock, int pX, int pY, int pZ) {
      this.assertBlockNotPresent(pBlock, new BlockPos(pX, pY, pZ));
   }

   public void assertBlockNotPresent(Block pBlock, BlockPos pPos) {
      this.assertBlock(pPos, (p_177251_) -> {
         return !this.getBlockState(pPos).is(pBlock);
      }, "Did not expect " + pBlock.getName().getString());
   }

   public void succeedWhenBlockPresent(Block pBlock, int pX, int pY, int pZ) {
      this.succeedWhenBlockPresent(pBlock, new BlockPos(pX, pY, pZ));
   }

   public void succeedWhenBlockPresent(Block pBlock, BlockPos pPos) {
      this.succeedWhen(() -> {
         this.assertBlockPresent(pBlock, pPos);
      });
   }

   public void assertBlock(BlockPos pPos, Predicate<Block> pPredicate, String pExceptionMessage) {
      this.assertBlock(pPos, pPredicate, () -> {
         return pExceptionMessage;
      });
   }

   public void assertBlock(BlockPos p_177276_, Predicate<Block> p_177277_, Supplier<String> p_177278_) {
      this.assertBlockState(p_177276_, (p_177296_) -> {
         return p_177277_.test(p_177296_.getBlock());
      }, p_177278_);
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177256_, Property<T> p_177257_, T p_177258_) {
      this.assertBlockState(p_177256_, (p_177223_) -> {
         return p_177223_.hasProperty(p_177257_) && p_177223_.<T>getValue(p_177257_).equals(p_177258_);
      }, () -> {
         return "Expected property " + p_177257_.getName() + " to be " + p_177258_;
      });
   }

   public <T extends Comparable<T>> void assertBlockProperty(BlockPos p_177260_, Property<T> p_177261_, Predicate<T> p_177262_, String p_177263_) {
      this.assertBlockState(p_177260_, (p_177300_) -> {
         return p_177262_.test(p_177300_.getValue(p_177261_));
      }, () -> {
         return p_177263_;
      });
   }

   public void assertBlockState(BlockPos p_177358_, Predicate<BlockState> p_177359_, Supplier<String> p_177360_) {
      BlockState blockstate = this.getBlockState(p_177358_);
      if (!p_177359_.test(blockstate)) {
         throw new GameTestAssertPosException(p_177360_.get(), this.absolutePos(p_177358_), p_177358_, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> pType) {
      List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), Entity::isAlive);
      if (list.isEmpty()) {
         throw new GameTestAssertException("Expected " + pType.toShortString() + " to exist");
      }
   }

   public void assertEntityPresent(EntityType<?> pType, int pX, int pY, int pZ) {
      this.assertEntityPresent(pType, new BlockPos(pX, pY, pZ));
   }

   public void assertEntityPresent(EntityType<?> pType, BlockPos pPos) {
      BlockPos blockpos = this.absolutePos(pPos);
      List<? extends Entity> list = this.getLevel().getEntities(pType, new AABB(blockpos), Entity::isAlive);
      if (list.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + pType.toShortString(), blockpos, pPos, this.testInfo.getTick());
      }
   }

   public void assertEntityPresent(EntityType<?> pType, BlockPos pPos, double pExpansionAmount) {
      BlockPos blockpos = this.absolutePos(pPos);
      List<? extends Entity> list = this.getLevel().getEntities(pType, (new AABB(blockpos)).inflate(pExpansionAmount), Entity::isAlive);
      if (list.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + pType.toShortString(), blockpos, pPos, this.testInfo.getTick());
      }
   }

   public void assertEntityInstancePresent(Entity pEntity, int pX, int pY, int pZ) {
      this.assertEntityInstancePresent(pEntity, new BlockPos(pX, pY, pZ));
   }

   public void assertEntityInstancePresent(Entity pEntity, BlockPos pPos) {
      BlockPos blockpos = this.absolutePos(pPos);
      List<? extends Entity> list = this.getLevel().getEntities(pEntity.getType(), new AABB(blockpos), Entity::isAlive);
      list.stream().filter((p_177139_) -> {
         return p_177139_ == pEntity;
      }).findFirst().orElseThrow(() -> {
         return new GameTestAssertPosException("Expected " + pEntity.getType().toShortString(), blockpos, pPos, this.testInfo.getTick());
      });
   }

   public void assertItemEntityCountIs(Item pItem, BlockPos pPos, double pExpansionAmount, int pCount) {
      BlockPos blockpos = this.absolutePos(pPos);
      List<ItemEntity> list = this.getLevel().getEntities(EntityType.ITEM, (new AABB(blockpos)).inflate(pExpansionAmount), Entity::isAlive);
      int i = 0;

      for(Entity entity : list) {
         ItemEntity itementity = (ItemEntity)entity;
         if (itementity.getItem().getItem().equals(pItem)) {
            i += itementity.getItem().getCount();
         }
      }

      if (i != pCount) {
         throw new GameTestAssertPosException("Expected " + pCount + " " + pItem.getDescription().getString() + " items to exist (found " + i + ")", blockpos, pPos, this.testInfo.getTick());
      }
   }

   public void assertItemEntityPresent(Item pItem, BlockPos pPos, double pExpansionAmount) {
      BlockPos blockpos = this.absolutePos(pPos);

      for(Entity entity : this.getLevel().getEntities(EntityType.ITEM, (new AABB(blockpos)).inflate(pExpansionAmount), Entity::isAlive)) {
         ItemEntity itementity = (ItemEntity)entity;
         if (itementity.getItem().getItem().equals(pItem)) {
            return;
         }
      }

      throw new GameTestAssertPosException("Expected " + pItem.getDescription().getString() + " item", blockpos, pPos, this.testInfo.getTick());
   }

   public void assertEntityNotPresent(EntityType<?> pType) {
      List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), Entity::isAlive);
      if (!list.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + pType.toShortString() + " to exist");
      }
   }

   public void assertEntityNotPresent(EntityType<?> pType, int pX, int pY, int pZ) {
      this.assertEntityNotPresent(pType, new BlockPos(pX, pY, pZ));
   }

   public void assertEntityNotPresent(EntityType<?> pType, BlockPos pPos) {
      BlockPos blockpos = this.absolutePos(pPos);
      List<? extends Entity> list = this.getLevel().getEntities(pType, new AABB(blockpos), Entity::isAlive);
      if (!list.isEmpty()) {
         throw new GameTestAssertPosException("Did not expect " + pType.toShortString(), blockpos, pPos, this.testInfo.getTick());
      }
   }

   public void assertEntityTouching(EntityType<?> pType, double pX, double pY, double pZ) {
      Vec3 vec3 = new Vec3(pX, pY, pZ);
      Vec3 vec31 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = (p_177346_) -> {
         return p_177346_.getBoundingBox().intersects(vec31, vec31);
      };
      List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), predicate);
      if (list.isEmpty()) {
         throw new GameTestAssertException("Expected " + pType.toShortString() + " to touch " + vec31 + " (relative " + vec3 + ")");
      }
   }

   public void assertEntityNotTouching(EntityType<?> pType, double pX, double pY, double pZ) {
      Vec3 vec3 = new Vec3(pX, pY, pZ);
      Vec3 vec31 = this.absoluteVec(vec3);
      Predicate<? super Entity> predicate = (p_177231_) -> {
         return !p_177231_.getBoundingBox().intersects(vec31, vec31);
      };
      List<? extends Entity> list = this.getLevel().getEntities(pType, this.getBounds(), predicate);
      if (list.isEmpty()) {
         throw new GameTestAssertException("Did not expect " + pType.toShortString() + " to touch " + vec31 + " (relative " + vec3 + ")");
      }
   }

   public <E extends Entity, T> void assertEntityData(BlockPos p_177238_, EntityType<E> p_177239_, Function<? super E, T> p_177240_, @Nullable T p_177241_) {
      BlockPos blockpos = this.absolutePos(p_177238_);
      List<E> list = this.getLevel().getEntities(p_177239_, new AABB(blockpos), Entity::isAlive);
      if (list.isEmpty()) {
         throw new GameTestAssertPosException("Expected " + p_177239_.toShortString(), blockpos, p_177238_, this.testInfo.getTick());
      } else {
         for(E e : list) {
            T t = p_177240_.apply(e);
            if (t == null) {
               if (p_177241_ != null) {
                  throw new GameTestAssertException("Expected entity data to be: " + p_177241_ + ", but was: " + t);
               }
            } else if (!t.equals(p_177241_)) {
               throw new GameTestAssertException("Expected entity data to be: " + p_177241_ + ", but was: " + t);
            }
         }

      }
   }

   public void assertContainerEmpty(BlockPos pPos) {
      BlockPos blockpos = this.absolutePos(pPos);
      BlockEntity blockentity = this.getLevel().getBlockEntity(blockpos);
      if (blockentity instanceof BaseContainerBlockEntity && !((BaseContainerBlockEntity)blockentity).isEmpty()) {
         throw new GameTestAssertException("Container should be empty");
      }
   }

   public void assertContainerContains(BlockPos pPos, Item pItem) {
      BlockPos blockpos = this.absolutePos(pPos);
      BlockEntity blockentity = this.getLevel().getBlockEntity(blockpos);
      if (blockentity instanceof BaseContainerBlockEntity && ((BaseContainerBlockEntity)blockentity).countItem(pItem) != 1) {
         throw new GameTestAssertException("Container should contain: " + pItem);
      }
   }

   public void assertSameBlockStates(BoundingBox pBoundingBox, BlockPos pPos) {
      BlockPos.betweenClosedStream(pBoundingBox).forEach((p_177267_) -> {
         BlockPos blockpos = pPos.offset(p_177267_.getX() - pBoundingBox.minX(), p_177267_.getY() - pBoundingBox.minY(), p_177267_.getZ() - pBoundingBox.minZ());
         this.assertSameBlockState(p_177267_, blockpos);
      });
   }

   public void assertSameBlockState(BlockPos pTestPos, BlockPos pComparisonPos) {
      BlockState blockstate = this.getBlockState(pTestPos);
      BlockState blockstate1 = this.getBlockState(pComparisonPos);
      if (blockstate != blockstate1) {
         this.fail("Incorrect state. Expected " + blockstate1 + ", got " + blockstate, pTestPos);
      }

   }

   public void assertAtTickTimeContainerContains(long pTickTime, BlockPos pPos, Item pItem) {
      this.runAtTickTime(pTickTime, () -> {
         this.assertContainerContains(pPos, pItem);
      });
   }

   public void assertAtTickTimeContainerEmpty(long pTickTime, BlockPos pPos) {
      this.runAtTickTime(pTickTime, () -> {
         this.assertContainerEmpty(pPos);
      });
   }

   public <E extends Entity, T> void succeedWhenEntityData(BlockPos p_177350_, EntityType<E> p_177351_, Function<E, T> p_177352_, T p_177353_) {
      this.succeedWhen(() -> {
         this.assertEntityData(p_177350_, p_177351_, p_177352_, p_177353_);
      });
   }

   public <E extends Entity> void assertEntityProperty(E p_177153_, Predicate<E> p_177154_, String p_177155_) {
      if (!p_177154_.test(p_177153_)) {
         throw new GameTestAssertException("Entity " + p_177153_ + " failed " + p_177155_ + " test");
      }
   }

   public <E extends Entity, T> void assertEntityProperty(E p_177148_, Function<E, T> p_177149_, String p_177150_, T p_177151_) {
      T t = p_177149_.apply(p_177148_);
      if (!t.equals(p_177151_)) {
         throw new GameTestAssertException("Entity " + p_177148_ + " value " + p_177150_ + "=" + t + " is not equal to expected " + p_177151_);
      }
   }

   public void succeedWhenEntityPresent(EntityType<?> pType, int pX, int pY, int pZ) {
      this.succeedWhenEntityPresent(pType, new BlockPos(pX, pY, pZ));
   }

   public void succeedWhenEntityPresent(EntityType<?> pType, BlockPos pPos) {
      this.succeedWhen(() -> {
         this.assertEntityPresent(pType, pPos);
      });
   }

   public void succeedWhenEntityNotPresent(EntityType<?> pType, int pX, int pY, int pZ) {
      this.succeedWhenEntityNotPresent(pType, new BlockPos(pX, pY, pZ));
   }

   public void succeedWhenEntityNotPresent(EntityType<?> pType, BlockPos pPos) {
      this.succeedWhen(() -> {
         this.assertEntityNotPresent(pType, pPos);
      });
   }

   public void succeed() {
      this.testInfo.succeed();
   }

   private void ensureSingleFinalCheck() {
      if (this.finalCheckAdded) {
         throw new IllegalStateException("This test already has final clause");
      } else {
         this.finalCheckAdded = true;
      }
   }

   public void succeedIf(Runnable pCriterion) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(0L, pCriterion).thenSucceed();
   }

   public void succeedWhen(Runnable pCriterion) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil(pCriterion).thenSucceed();
   }

   public void succeedOnTickWhen(int pTick, Runnable pCriterion) {
      this.ensureSingleFinalCheck();
      this.testInfo.createSequence().thenWaitUntil((long)pTick, pCriterion).thenSucceed();
   }

   public void runAtTickTime(long pTickTime, Runnable pTask) {
      this.testInfo.setRunAtTickTime(pTickTime, pTask);
   }

   public void runAfterDelay(long pDelay, Runnable pTask) {
      this.runAtTickTime(this.testInfo.getTick() + pDelay, pTask);
   }

   public void randomTick(BlockPos pPos) {
      BlockPos blockpos = this.absolutePos(pPos);
      ServerLevel serverlevel = this.getLevel();
      serverlevel.getBlockState(blockpos).randomTick(serverlevel, blockpos, serverlevel.random);
   }

   public void fail(String pExceptionMessage, BlockPos pPos) {
      throw new GameTestAssertPosException(pExceptionMessage, this.absolutePos(pPos), pPos, this.getTick());
   }

   public void fail(String pExceptionMessage, Entity pEntity) {
      throw new GameTestAssertPosException(pExceptionMessage, pEntity.blockPosition(), this.relativePos(pEntity.blockPosition()), this.getTick());
   }

   public void fail(String pExceptionMessage) {
      throw new GameTestAssertException(pExceptionMessage);
   }

   public void failIf(Runnable pCriterion) {
      this.testInfo.createSequence().thenWaitUntil(pCriterion).thenFail(() -> {
         return new GameTestAssertException("Fail conditions met");
      });
   }

   public void failIfEver(Runnable pCriterion) {
      LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((p_177365_) -> {
         this.testInfo.setRunAtTickTime(p_177365_, pCriterion::run);
      });
   }

   public GameTestSequence startSequence() {
      return this.testInfo.createSequence();
   }

   public BlockPos absolutePos(BlockPos pPos) {
      BlockPos blockpos = this.testInfo.getStructureBlockPos();
      BlockPos blockpos1 = blockpos.offset(pPos);
      return StructureTemplate.transform(blockpos1, Mirror.NONE, this.testInfo.getRotation(), blockpos);
   }

   public BlockPos relativePos(BlockPos pPos) {
      BlockPos blockpos = this.testInfo.getStructureBlockPos();
      Rotation rotation = this.testInfo.getRotation().getRotated(Rotation.CLOCKWISE_180);
      BlockPos blockpos1 = StructureTemplate.transform(pPos, Mirror.NONE, rotation, blockpos);
      return blockpos1.subtract(blockpos);
   }

   public Vec3 absoluteVec(Vec3 pRelativeVec3) {
      Vec3 vec3 = Vec3.atLowerCornerOf(this.testInfo.getStructureBlockPos());
      return StructureTemplate.transform(vec3.add(pRelativeVec3), Mirror.NONE, this.testInfo.getRotation(), this.testInfo.getStructureBlockPos());
   }

   public long getTick() {
      return this.testInfo.getTick();
   }

   private AABB getBounds() {
      return this.testInfo.getStructureBounds();
   }

   private AABB getRelativeBounds() {
      AABB aabb = this.testInfo.getStructureBounds();
      return aabb.move(BlockPos.ZERO.subtract(this.absolutePos(BlockPos.ZERO)));
   }

   public void forEveryBlockInStructure(Consumer<BlockPos> pConsumer) {
      AABB aabb = this.getRelativeBounds();
      BlockPos.MutableBlockPos.betweenClosedStream(aabb.move(0.0D, 1.0D, 0.0D)).forEach(pConsumer);
   }

   public void onEachTick(Runnable pTask) {
      LongStream.range(this.testInfo.getTick(), (long)this.testInfo.getTimeoutTicks()).forEach((p_177283_) -> {
         this.testInfo.setRunAtTickTime(p_177283_, pTask::run);
      });
   }
}