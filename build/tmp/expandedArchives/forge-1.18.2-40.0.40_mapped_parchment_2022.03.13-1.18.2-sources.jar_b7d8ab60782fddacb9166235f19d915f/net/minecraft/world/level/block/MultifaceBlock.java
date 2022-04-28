package net.minecraft.world.level.block;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.datafixers.util.Pair;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class MultifaceBlock extends Block {
   private static final float AABB_OFFSET = 1.0F;
   private static final VoxelShape UP_AABB = Block.box(0.0D, 15.0D, 0.0D, 16.0D, 16.0D, 16.0D);
   private static final VoxelShape DOWN_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);
   private static final VoxelShape WEST_AABB = Block.box(0.0D, 0.0D, 0.0D, 1.0D, 16.0D, 16.0D);
   private static final VoxelShape EAST_AABB = Block.box(15.0D, 0.0D, 0.0D, 16.0D, 16.0D, 16.0D);
   private static final VoxelShape NORTH_AABB = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 16.0D, 1.0D);
   private static final VoxelShape SOUTH_AABB = Block.box(0.0D, 0.0D, 15.0D, 16.0D, 16.0D, 16.0D);
   private static final Map<Direction, BooleanProperty> PROPERTY_BY_DIRECTION = PipeBlock.PROPERTY_BY_DIRECTION;
   private static final Map<Direction, VoxelShape> SHAPE_BY_DIRECTION = Util.make(Maps.newEnumMap(Direction.class), (p_153923_) -> {
      p_153923_.put(Direction.NORTH, NORTH_AABB);
      p_153923_.put(Direction.EAST, EAST_AABB);
      p_153923_.put(Direction.SOUTH, SOUTH_AABB);
      p_153923_.put(Direction.WEST, WEST_AABB);
      p_153923_.put(Direction.UP, UP_AABB);
      p_153923_.put(Direction.DOWN, DOWN_AABB);
   });
   protected static final Direction[] DIRECTIONS = Direction.values();
   private final ImmutableMap<BlockState, VoxelShape> shapesCache;
   private final boolean canRotate;
   private final boolean canMirrorX;
   private final boolean canMirrorZ;

   public MultifaceBlock(BlockBehaviour.Properties p_153822_) {
      super(p_153822_);
      this.registerDefaultState(getDefaultMultifaceState(this.stateDefinition));
      this.shapesCache = this.getShapeForEachState(MultifaceBlock::calculateMultifaceShape);
      this.canRotate = Direction.Plane.HORIZONTAL.stream().allMatch(this::isFaceSupported);
      this.canMirrorX = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.X).filter(this::isFaceSupported).count() % 2L == 0L;
      this.canMirrorZ = Direction.Plane.HORIZONTAL.stream().filter(Direction.Axis.Z).filter(this::isFaceSupported).count() % 2L == 0L;
   }

   protected boolean isFaceSupported(Direction p_153921_) {
      return true;
   }

   protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
      for(Direction direction : DIRECTIONS) {
         if (this.isFaceSupported(direction)) {
            pBuilder.add(getFaceProperty(direction));
         }
      }

   }

   /**
    * Update the provided state given the provided neighbor direction and neighbor state, returning a new state.
    * For example, fences make their connections to the passed in state if possible, and wet concrete powder immediately
    * returns its solidified counterpart.
    * Note that this method should ideally consider only the specific direction passed in.
    */
   public BlockState updateShape(BlockState pState, Direction pDirection, BlockState pNeighborState, LevelAccessor pLevel, BlockPos pCurrentPos, BlockPos pNeighborPos) {
      if (!hasAnyFace(pState)) {
         return Blocks.AIR.defaultBlockState();
      } else {
         return hasFace(pState, pDirection) && !canAttachTo(pLevel, pDirection, pNeighborPos, pNeighborState) ? removeFace(pState, getFaceProperty(pDirection)) : pState;
      }
   }

   public VoxelShape getShape(BlockState pState, BlockGetter pLevel, BlockPos pPos, CollisionContext pContext) {
      return this.shapesCache.get(pState);
   }

   public boolean canSurvive(BlockState pState, LevelReader pLevel, BlockPos pPos) {
      boolean flag = false;

      for(Direction direction : DIRECTIONS) {
         if (hasFace(pState, direction)) {
            BlockPos blockpos = pPos.relative(direction);
            if (!canAttachTo(pLevel, direction, blockpos, pLevel.getBlockState(blockpos))) {
               return false;
            }

            flag = true;
         }
      }

      return flag;
   }

   public boolean canBeReplaced(BlockState pState, BlockPlaceContext pUseContext) {
      return hasAnyVacantFace(pState);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockPlaceContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      return Arrays.stream(pContext.getNearestLookingDirections()).map((p_153865_) -> {
         return this.getStateForPlacement(blockstate, level, blockpos, p_153865_);
      }).filter(Objects::nonNull).findFirst().orElse((BlockState)null);
   }

   @Nullable
   public BlockState getStateForPlacement(BlockState pCurrentState, BlockGetter pLevel, BlockPos pPos, Direction pLookingDirection) {
      if (!this.isFaceSupported(pLookingDirection)) {
         return null;
      } else {
         BlockState blockstate;
         if (pCurrentState.is(this)) {
            if (hasFace(pCurrentState, pLookingDirection)) {
               return null;
            }

            blockstate = pCurrentState;
         } else if (this.isWaterloggable() && pCurrentState.getFluidState().isSourceOfType(Fluids.WATER)) {
            blockstate = this.defaultBlockState().setValue(BlockStateProperties.WATERLOGGED, Boolean.valueOf(true));
         } else {
            blockstate = this.defaultBlockState();
         }

         BlockPos blockpos = pPos.relative(pLookingDirection);
         return canAttachTo(pLevel, pLookingDirection, blockpos, pLevel.getBlockState(blockpos)) ? blockstate.setValue(getFaceProperty(pLookingDirection), Boolean.valueOf(true)) : null;
      }
   }

   /**
    * Returns the blockstate with the given rotation from the passed blockstate. If inapplicable, returns the passed
    * blockstate.
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#rotate} whenever
    * possible. Implementing/overriding is fine.
    */
   public BlockState rotate(BlockState pState, Rotation pRotation) {
      return !this.canRotate ? pState : this.mapDirections(pState, pRotation::rotate);
   }

   /**
    * Returns the blockstate with the given mirror of the passed blockstate. If inapplicable, returns the passed
    * blockstate.
    * @deprecated call via {@link net.minecraft.world.level.block.state.BlockBehavior.BlockStateBase#mirror} whenever
    * possible. Implementing/overriding is fine.
    */
   public BlockState mirror(BlockState pState, Mirror pMirror) {
      if (pMirror == Mirror.FRONT_BACK && !this.canMirrorX) {
         return pState;
      } else {
         return pMirror == Mirror.LEFT_RIGHT && !this.canMirrorZ ? pState : this.mapDirections(pState, pMirror::mirror);
      }
   }

   private BlockState mapDirections(BlockState p_153911_, Function<Direction, Direction> p_153912_) {
      BlockState blockstate = p_153911_;

      for(Direction direction : DIRECTIONS) {
         if (this.isFaceSupported(direction)) {
            blockstate = blockstate.setValue(getFaceProperty(p_153912_.apply(direction)), p_153911_.getValue(getFaceProperty(direction)));
         }
      }

      return blockstate;
   }

   public boolean spreadFromRandomFaceTowardRandomDirection(BlockState p_153936_, ServerLevel p_153937_, BlockPos p_153938_, Random p_153939_) {
      List<Direction> list = Lists.newArrayList(DIRECTIONS);
      Collections.shuffle(list);
      return list.stream().filter((p_153955_) -> {
         return hasFace(p_153936_, p_153955_);
      }).anyMatch((p_153846_) -> {
         return this.spreadFromFaceTowardRandomDirection(p_153936_, p_153937_, p_153938_, p_153846_, p_153939_, false);
      });
   }

   public boolean spreadFromFaceTowardRandomDirection(BlockState p_153874_, LevelAccessor p_153875_, BlockPos p_153876_, Direction p_153877_, Random p_153878_, boolean p_153879_) {
      List<Direction> list = Arrays.asList(DIRECTIONS);
      Collections.shuffle(list, p_153878_);
      return list.stream().anyMatch((p_153886_) -> {
         return this.spreadFromFaceTowardDirection(p_153874_, p_153875_, p_153876_, p_153877_, p_153886_, p_153879_);
      });
   }

   public boolean spreadFromFaceTowardDirection(BlockState p_153867_, LevelAccessor p_153868_, BlockPos p_153869_, Direction p_153870_, Direction p_153871_, boolean p_153872_) {
      Optional<Pair<BlockPos, Direction>> optional = this.getSpreadFromFaceTowardDirection(p_153867_, p_153868_, p_153869_, p_153870_, p_153871_);
      if (optional.isPresent()) {
         Pair<BlockPos, Direction> pair = optional.get();
         return this.spreadToFace(p_153868_, pair.getFirst(), pair.getSecond(), p_153872_);
      } else {
         return false;
      }
   }

   protected boolean canSpread(BlockState p_153949_, BlockGetter p_153950_, BlockPos p_153951_, Direction p_153952_) {
      return Stream.of(DIRECTIONS).anyMatch((p_153929_) -> {
         return this.getSpreadFromFaceTowardDirection(p_153949_, p_153950_, p_153951_, p_153952_, p_153929_).isPresent();
      });
   }

   private Optional<Pair<BlockPos, Direction>> getSpreadFromFaceTowardDirection(BlockState p_153856_, BlockGetter p_153857_, BlockPos p_153858_, Direction p_153859_, Direction p_153860_) {
      if (p_153860_.getAxis() != p_153859_.getAxis() && hasFace(p_153856_, p_153859_) && !hasFace(p_153856_, p_153860_)) {
         if (this.canSpreadToFace(p_153857_, p_153858_, p_153860_)) {
            return Optional.of(Pair.of(p_153858_, p_153860_));
         } else {
            BlockPos blockpos = p_153858_.relative(p_153860_);
            if (this.canSpreadToFace(p_153857_, blockpos, p_153859_)) {
               return Optional.of(Pair.of(blockpos, p_153859_));
            } else {
               BlockPos blockpos1 = blockpos.relative(p_153859_);
               Direction direction = p_153860_.getOpposite();
               return this.canSpreadToFace(p_153857_, blockpos1, direction) ? Optional.of(Pair.of(blockpos1, direction)) : Optional.empty();
            }
         }
      } else {
         return Optional.empty();
      }
   }

   private boolean canSpreadToFace(BlockGetter p_153826_, BlockPos p_153827_, Direction p_153828_) {
      BlockState blockstate = p_153826_.getBlockState(p_153827_);
      if (!this.canSpreadInto(blockstate)) {
         return false;
      } else {
         BlockState blockstate1 = this.getStateForPlacement(blockstate, p_153826_, p_153827_, p_153828_);
         return blockstate1 != null;
      }
   }

   private boolean spreadToFace(LevelAccessor p_153835_, BlockPos p_153836_, Direction p_153837_, boolean p_153838_) {
      BlockState blockstate = p_153835_.getBlockState(p_153836_);
      BlockState blockstate1 = this.getStateForPlacement(blockstate, p_153835_, p_153836_, p_153837_);
      if (blockstate1 != null) {
         if (p_153838_) {
            p_153835_.getChunk(p_153836_).markPosForPostprocessing(p_153836_);
         }

         return p_153835_.setBlock(p_153836_, blockstate1, 2);
      } else {
         return false;
      }
   }

   private boolean canSpreadInto(BlockState p_153957_) {
      return p_153957_.isAir() || p_153957_.is(this) || p_153957_.is(Blocks.WATER) && p_153957_.getFluidState().isSource();
   }

   private static boolean hasFace(BlockState p_153901_, Direction p_153902_) {
      BooleanProperty booleanproperty = getFaceProperty(p_153902_);
      return p_153901_.hasProperty(booleanproperty) && p_153901_.getValue(booleanproperty);
   }

   private static boolean canAttachTo(BlockGetter p_153830_, Direction p_153831_, BlockPos p_153832_, BlockState p_153833_) {
      return Block.isFaceFull(p_153833_.getCollisionShape(p_153830_, p_153832_), p_153831_.getOpposite());
   }

   private boolean isWaterloggable() {
      return this.stateDefinition.getProperties().contains(BlockStateProperties.WATERLOGGED);
   }

   private static BlockState removeFace(BlockState p_153898_, BooleanProperty p_153899_) {
      BlockState blockstate = p_153898_.setValue(p_153899_, Boolean.valueOf(false));
      return hasAnyFace(blockstate) ? blockstate : Blocks.AIR.defaultBlockState();
   }

   public static BooleanProperty getFaceProperty(Direction p_153934_) {
      return PROPERTY_BY_DIRECTION.get(p_153934_);
   }

   private static BlockState getDefaultMultifaceState(StateDefinition<Block, BlockState> p_153919_) {
      BlockState blockstate = p_153919_.any();

      for(BooleanProperty booleanproperty : PROPERTY_BY_DIRECTION.values()) {
         if (blockstate.hasProperty(booleanproperty)) {
            blockstate = blockstate.setValue(booleanproperty, Boolean.valueOf(false));
         }
      }

      return blockstate;
   }

   private static VoxelShape calculateMultifaceShape(BlockState p_153959_) {
      VoxelShape voxelshape = Shapes.empty();

      for(Direction direction : DIRECTIONS) {
         if (hasFace(p_153959_, direction)) {
            voxelshape = Shapes.or(voxelshape, SHAPE_BY_DIRECTION.get(direction));
         }
      }

      return voxelshape.isEmpty() ? Shapes.block() : voxelshape;
   }

   protected static boolean hasAnyFace(BlockState p_153961_) {
      return Arrays.stream(DIRECTIONS).anyMatch((p_153947_) -> {
         return hasFace(p_153961_, p_153947_);
      });
   }

   private static boolean hasAnyVacantFace(BlockState p_153963_) {
      return Arrays.stream(DIRECTIONS).anyMatch((p_153932_) -> {
         return !hasFace(p_153963_, p_153932_);
      });
   }
}