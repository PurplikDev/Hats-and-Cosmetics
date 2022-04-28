package net.minecraft.world.level.block;

import java.util.Optional;
import java.util.Random;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PowderSnowBlock extends Block implements BucketPickup {
   private static final float HORIZONTAL_PARTICLE_MOMENTUM_FACTOR = 0.083333336F;
   private static final float IN_BLOCK_HORIZONTAL_SPEED_MULTIPLIER = 0.9F;
   private static final float IN_BLOCK_VERTICAL_SPEED_MULTIPLIER = 1.5F;
   private static final float NUM_BLOCKS_TO_FALL_INTO_BLOCK = 2.5F;
   private static final VoxelShape FALLING_COLLISION_SHAPE = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, (double)0.9F, 1.0D);
   private static final double MINIMUM_FALL_DISTANCE_FOR_SOUND = 4.0D;
   private static final double MINIMUM_FALL_DISTANCE_FOR_BIG_SOUND = 7.0D;

   public PowderSnowBlock(BlockBehaviour.Properties p_154253_) {
      super(p_154253_);
   }

   public boolean skipRendering(BlockState pState, BlockState pAdjacentBlockState, Direction pDirection) {
      return pAdjacentBlockState.is(this) ? true : super.skipRendering(pState, pAdjacentBlockState, pDirection);
   }

   public VoxelShape getOcclusionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos) {
      return Shapes.empty();
   }

   public void entityInside(BlockState pState, Level pLevel, BlockPos pPos, Entity pEntity) {
      if (!(pEntity instanceof LivingEntity) || pEntity.getFeetBlockState().is(this)) {
         pEntity.makeStuckInBlock(pState, new Vec3((double)0.9F, 1.5D, (double)0.9F));
         if (pLevel.isClientSide) {
            Random random = pLevel.getRandom();
            boolean flag = pEntity.xOld != pEntity.getX() || pEntity.zOld != pEntity.getZ();
            if (flag && random.nextBoolean()) {
               pLevel.addParticle(ParticleTypes.SNOWFLAKE, pEntity.getX(), (double)(pPos.getY() + 1), pEntity.getZ(), (double)(Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F), (double)0.05F, (double)(Mth.randomBetween(random, -1.0F, 1.0F) * 0.083333336F));
            }
         }
      }

      pEntity.setIsInPowderSnow(true);
      if (!pLevel.isClientSide) {
         if (pEntity.isOnFire() && (pLevel.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || pEntity instanceof Player) && pEntity.mayInteract(pLevel, pPos)) {
            pLevel.destroyBlock(pPos, false);
         }

         pEntity.setSharedFlagOnFire(false);
      }

   }

   public void fallOn(Level p_196695_, BlockState p_196696_, BlockPos p_196697_, Entity p_196698_, float p_196699_) {
      if (!((double)p_196699_ < 4.0D) && p_196698_ instanceof LivingEntity) {
         LivingEntity livingentity = (LivingEntity)p_196698_;
         LivingEntity.Fallsounds $$7 = livingentity.getFallSounds();
         SoundEvent soundevent = (double)p_196699_ < 7.0D ? $$7.small() : $$7.big();
         p_196698_.playSound(soundevent, 1.0F, 1.0F);
      }
   }

   public VoxelShape getCollisionShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      if (pContext instanceof EntityCollisionContext) {
         EntityCollisionContext entitycollisioncontext = (EntityCollisionContext)pContext;
         Entity entity = entitycollisioncontext.getEntity();
         if (entity != null) {
            if (entity.fallDistance > 2.5F) {
               return FALLING_COLLISION_SHAPE;
            }

            boolean flag = entity instanceof FallingBlockEntity;
            if (flag || canEntityWalkOnPowderSnow(entity) && pContext.isAbove(Shapes.block(), pPos, false) && !pContext.isDescending()) {
               return super.getCollisionShape(pState, pLevel, pPos, pContext);
            }
         }
      }

      return Shapes.empty();
   }

   public VoxelShape getVisualShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return Shapes.empty();
   }

   public static boolean canEntityWalkOnPowderSnow(Entity pEntity) {
      if (pEntity.getType().is(EntityTypeTags.POWDER_SNOW_WALKABLE_MOBS)) {
         return true;
      } else {
         return pEntity instanceof LivingEntity ? ((LivingEntity)pEntity).getItemBySlot(EquipmentSlot.FEET).is(Items.LEATHER_BOOTS) : false;
      }
   }

   public ItemStack pickupBlock(LevelAccessor pLevel, BlockPos pPos, BlockState pState) {
      pLevel.setBlock(pPos, Blocks.AIR.defaultBlockState(), 11);
      if (!pLevel.isClientSide()) {
         pLevel.levelEvent(2001, pPos, Block.getId(pState));
      }

      return new ItemStack(Items.POWDER_SNOW_BUCKET);
   }

   public Optional<SoundEvent> getPickupSound() {
      return Optional.of(SoundEvents.BUCKET_FILL_POWDER_SNOW);
   }

   public boolean isPathfindable(BlockState pState, BlockGetter pLevel, BlockPos pPos, PathComputationType pType) {
      return true;
   }
}