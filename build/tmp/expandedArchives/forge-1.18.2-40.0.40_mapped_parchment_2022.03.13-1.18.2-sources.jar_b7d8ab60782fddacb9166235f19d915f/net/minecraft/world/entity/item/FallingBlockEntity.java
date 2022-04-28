package net.minecraft.world.entity.item;

import com.mojang.logging.LogUtils;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.CrashReportCategory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ConcretePowderBlock;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

public class FallingBlockEntity extends Entity {
   private static final Logger LOGGER = LogUtils.getLogger();
   private BlockState blockState = Blocks.SAND.defaultBlockState();
   public int time;
   public boolean dropItem = true;
   private boolean cancelDrop;
   private boolean hurtEntities;
   private int fallDamageMax = 40;
   private float fallDamagePerDistance;
   @Nullable
   public CompoundTag blockData;
   protected static final EntityDataAccessor<BlockPos> DATA_START_POS = SynchedEntityData.defineId(FallingBlockEntity.class, EntityDataSerializers.BLOCK_POS);

   public FallingBlockEntity(EntityType<? extends FallingBlockEntity> p_31950_, Level p_31951_) {
      super(p_31950_, p_31951_);
   }

   private FallingBlockEntity(Level p_31953_, double p_31954_, double p_31955_, double p_31956_, BlockState p_31957_) {
      this(EntityType.FALLING_BLOCK, p_31953_);
      this.blockState = p_31957_;
      this.blocksBuilding = true;
      this.setPos(p_31954_, p_31955_, p_31956_);
      this.setDeltaMovement(Vec3.ZERO);
      this.xo = p_31954_;
      this.yo = p_31955_;
      this.zo = p_31956_;
      this.setStartPos(this.blockPosition());
   }

   public static FallingBlockEntity fall(Level p_201972_, BlockPos p_201973_, BlockState p_201974_) {
      FallingBlockEntity fallingblockentity = new FallingBlockEntity(p_201972_, (double)p_201973_.getX() + 0.5D, (double)p_201973_.getY(), (double)p_201973_.getZ() + 0.5D, p_201974_.hasProperty(BlockStateProperties.WATERLOGGED) ? p_201974_.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(false)) : p_201974_);
      p_201972_.setBlock(p_201973_, p_201974_.getFluidState().createLegacyBlock(), 3);
      p_201972_.addFreshEntity(fallingblockentity);
      return fallingblockentity;
   }

   /**
    * Returns true if it's possible to attack this entity with an item.
    */
   public boolean isAttackable() {
      return false;
   }

   public void setStartPos(BlockPos pOrigin) {
      this.entityData.set(DATA_START_POS, pOrigin);
   }

   public BlockPos getStartPos() {
      return this.entityData.get(DATA_START_POS);
   }

   protected Entity.MovementEmission getMovementEmission() {
      return Entity.MovementEmission.NONE;
   }

   protected void defineSynchedData() {
      this.entityData.define(DATA_START_POS, BlockPos.ZERO);
   }

   /**
    * Returns true if other Entities should be prevented from moving through this Entity.
    */
   public boolean isPickable() {
      return !this.isRemoved();
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      if (this.blockState.isAir()) {
         this.discard();
      } else {
         Block block = this.blockState.getBlock();
         ++this.time;
         if (!this.isNoGravity()) {
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.04D, 0.0D));
         }

         this.move(MoverType.SELF, this.getDeltaMovement());
         if (!this.level.isClientSide) {
            BlockPos blockpos = this.blockPosition();
            boolean flag = this.blockState.getBlock() instanceof ConcretePowderBlock;
            boolean flag1 = flag && this.level.getFluidState(blockpos).is(FluidTags.WATER);
            double d0 = this.getDeltaMovement().lengthSqr();
            if (flag && d0 > 1.0D) {
               BlockHitResult blockhitresult = this.level.clip(new ClipContext(new Vec3(this.xo, this.yo, this.zo), this.position(), ClipContext.Block.COLLIDER, ClipContext.Fluid.SOURCE_ONLY, this));
               if (blockhitresult.getType() != HitResult.Type.MISS && this.level.getFluidState(blockhitresult.getBlockPos()).is(FluidTags.WATER)) {
                  blockpos = blockhitresult.getBlockPos();
                  flag1 = true;
               }
            }

            if (!this.onGround && !flag1) {
               if (!this.level.isClientSide && (this.time > 100 && (blockpos.getY() <= this.level.getMinBuildHeight() || blockpos.getY() > this.level.getMaxBuildHeight()) || this.time > 600)) {
                  if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                     this.spawnAtLocation(block);
                  }

                  this.discard();
               }
            } else {
               BlockState blockstate = this.level.getBlockState(blockpos);
               this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));
               if (!blockstate.is(Blocks.MOVING_PISTON)) {
                  if (!this.cancelDrop) {
                     boolean flag2 = blockstate.canBeReplaced(new DirectionalPlaceContext(this.level, blockpos, Direction.DOWN, ItemStack.EMPTY, Direction.UP));
                     boolean flag3 = FallingBlock.isFree(this.level.getBlockState(blockpos.below())) && (!flag || !flag1);
                     boolean flag4 = this.blockState.canSurvive(this.level, blockpos) && !flag3;
                     if (flag2 && flag4) {
                        if (this.blockState.hasProperty(BlockStateProperties.WATERLOGGED) && this.level.getFluidState(blockpos).getType() == Fluids.WATER) {
                           this.blockState = this.blockState.setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true));
                        }

                        if (this.level.setBlock(blockpos, this.blockState, 3)) {
                           ((ServerLevel)this.level).getChunkSource().chunkMap.broadcast(this, new ClientboundBlockUpdatePacket(blockpos, this.level.getBlockState(blockpos)));
                           this.discard();
                           if (block instanceof Fallable) {
                              ((Fallable)block).onLand(this.level, blockpos, this.blockState, blockstate, this);
                           }

                           if (this.blockData != null && this.blockState.hasBlockEntity()) {
                              BlockEntity blockentity = this.level.getBlockEntity(blockpos);
                              if (blockentity != null) {
                                 CompoundTag compoundtag = blockentity.saveWithoutMetadata();

                                 for(String s : this.blockData.getAllKeys()) {
                                    compoundtag.put(s, this.blockData.get(s).copy());
                                 }

                                 try {
                                    blockentity.load(compoundtag);
                                 } catch (Exception exception) {
                                    LOGGER.error("Failed to load block entity from falling block", (Throwable)exception);
                                 }

                                 blockentity.setChanged();
                              }
                           }
                        } else if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                           this.discard();
                           this.callOnBrokenAfterFall(block, blockpos);
                           this.spawnAtLocation(block);
                        }
                     } else {
                        this.discard();
                        if (this.dropItem && this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)) {
                           this.callOnBrokenAfterFall(block, blockpos);
                           this.spawnAtLocation(block);
                        }
                     }
                  } else {
                     this.discard();
                     this.callOnBrokenAfterFall(block, blockpos);
                  }
               }
            }
         }

         this.setDeltaMovement(this.getDeltaMovement().scale(0.98D));
      }
   }

   public void callOnBrokenAfterFall(Block p_149651_, BlockPos p_149652_) {
      if (p_149651_ instanceof Fallable) {
         ((Fallable)p_149651_).onBrokenAfterFall(this.level, p_149652_, this);
      }

   }

   public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource) {
      if (!this.hurtEntities) {
         return false;
      } else {
         int i = Mth.ceil(pFallDistance - 1.0F);
         if (i < 0) {
            return false;
         } else {
            Predicate<Entity> predicate;
            DamageSource damagesource;
            if (this.blockState.getBlock() instanceof Fallable) {
               Fallable fallable = (Fallable)this.blockState.getBlock();
               predicate = fallable.getHurtsEntitySelector();
               damagesource = fallable.getFallDamageSource();
            } else {
               predicate = EntitySelector.NO_SPECTATORS;
               damagesource = DamageSource.FALLING_BLOCK;
            }

            float f = (float)Math.min(Mth.floor((float)i * this.fallDamagePerDistance), this.fallDamageMax);
            this.level.getEntities(this, this.getBoundingBox(), predicate).forEach((p_149649_) -> {
               p_149649_.hurt(damagesource, f);
            });
            boolean flag = this.blockState.is(BlockTags.ANVIL);
            if (flag && f > 0.0F && this.random.nextFloat() < 0.05F + (float)i * 0.05F) {
               BlockState blockstate = AnvilBlock.damage(this.blockState);
               if (blockstate == null) {
                  this.cancelDrop = true;
               } else {
                  this.blockState = blockstate;
               }
            }

            return false;
         }
      }
   }

   protected void addAdditionalSaveData(CompoundTag pCompound) {
      pCompound.put("BlockState", NbtUtils.writeBlockState(this.blockState));
      pCompound.putInt("Time", this.time);
      pCompound.putBoolean("DropItem", this.dropItem);
      pCompound.putBoolean("HurtEntities", this.hurtEntities);
      pCompound.putFloat("FallHurtAmount", this.fallDamagePerDistance);
      pCompound.putInt("FallHurtMax", this.fallDamageMax);
      if (this.blockData != null) {
         pCompound.put("TileEntityData", this.blockData);
      }

   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   protected void readAdditionalSaveData(CompoundTag pCompound) {
      this.blockState = NbtUtils.readBlockState(pCompound.getCompound("BlockState"));
      this.time = pCompound.getInt("Time");
      if (pCompound.contains("HurtEntities", 99)) {
         this.hurtEntities = pCompound.getBoolean("HurtEntities");
         this.fallDamagePerDistance = pCompound.getFloat("FallHurtAmount");
         this.fallDamageMax = pCompound.getInt("FallHurtMax");
      } else if (this.blockState.is(BlockTags.ANVIL)) {
         this.hurtEntities = true;
      }

      if (pCompound.contains("DropItem", 99)) {
         this.dropItem = pCompound.getBoolean("DropItem");
      }

      if (pCompound.contains("TileEntityData", 10)) {
         this.blockData = pCompound.getCompound("TileEntityData");
      }

      if (this.blockState.isAir()) {
         this.blockState = Blocks.SAND.defaultBlockState();
      }

   }

   public void setHurtsEntities(float p_149657_, int p_149658_) {
      this.hurtEntities = true;
      this.fallDamagePerDistance = p_149657_;
      this.fallDamageMax = p_149658_;
   }

   /**
    * Return whether this entity should be rendered as on fire.
    */
   public boolean displayFireAnimation() {
      return false;
   }

   public void fillCrashReportCategory(CrashReportCategory pCategory) {
      super.fillCrashReportCategory(pCategory);
      pCategory.setDetail("Immitating BlockState", this.blockState.toString());
   }

   public BlockState getBlockState() {
      return this.blockState;
   }

   /**
    * Checks if players can use this entity to access operator (permission level 2) commands either directly or
    * indirectly, such as give or setblock. A similar method exists for entities at {@link
    * net.minecraft.tileentity.TileEntity#onlyOpsCanSetNbt()}.<p>For example, {@link
    * net.minecraft.entity.item.EntityMinecartCommandBlock#ignoreItemEntityData() command block minecarts} and {@link
    * net.minecraft.entity.item.EntityMinecartMobSpawner#ignoreItemEntityData() mob spawner minecarts} (spawning command
    * block minecarts or drops) are considered accessible.</p>@return true if this entity offers ways for unauthorized
    * players to use restricted commands
    */
   public boolean onlyOpCanSetNbt() {
      return true;
   }

   public Packet<?> getAddEntityPacket() {
      return new ClientboundAddEntityPacket(this, Block.getId(this.getBlockState()));
   }

   public void recreateFromPacket(ClientboundAddEntityPacket pPacket) {
      super.recreateFromPacket(pPacket);
      this.blockState = Block.stateById(pPacket.getData());
      this.blocksBuilding = true;
      double d0 = pPacket.getX();
      double d1 = pPacket.getY();
      double d2 = pPacket.getZ();
      this.setPos(d0, d1, d2);
      this.setStartPos(this.blockPosition());
   }
}