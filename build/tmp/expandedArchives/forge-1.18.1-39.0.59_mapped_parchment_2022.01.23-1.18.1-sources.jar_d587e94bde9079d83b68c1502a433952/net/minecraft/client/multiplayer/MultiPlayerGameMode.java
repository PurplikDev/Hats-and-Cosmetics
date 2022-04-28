package net.minecraft.client.multiplayer;

import com.google.common.collect.Lists;
import com.mojang.datafixers.util.Pair;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.client.ClientRecipeBook;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.protocol.game.ServerboundContainerButtonClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemPacket;
import net.minecraft.network.protocol.game.ServerboundPlaceRecipePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSetCreativeModeSlotPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class MultiPlayerGameMode {
   private static final Logger LOGGER = LogManager.getLogger();
   private final Minecraft minecraft;
   private final ClientPacketListener connection;
   private BlockPos destroyBlockPos = new BlockPos(-1, -1, -1);
   private ItemStack destroyingItem = ItemStack.EMPTY;
   private float destroyProgress;
   private float destroyTicks;
   private int destroyDelay;
   private boolean isDestroying;
   private GameType localPlayerMode = GameType.DEFAULT_MODE;
   @Nullable
   private GameType previousLocalPlayerMode;
   private final Object2ObjectLinkedOpenHashMap<Pair<BlockPos, ServerboundPlayerActionPacket.Action>, Vec3> unAckedActions = new Object2ObjectLinkedOpenHashMap<>();
   private static final int MAX_ACTIONS_SIZE = 50;
   private int carriedIndex;

   public MultiPlayerGameMode(Minecraft pMinecraft, ClientPacketListener pConnection) {
      this.minecraft = pMinecraft;
      this.connection = pConnection;
   }

   /**
    * Sets player capabilities depending on current gametype. params: player
    */
   public void adjustPlayer(Player pPlayer) {
      this.localPlayerMode.updatePlayerAbilities(pPlayer.getAbilities());
   }

   public void setLocalMode(GameType pLocalPlayerMode, @Nullable GameType pPreviousLocalPlayerMode) {
      this.localPlayerMode = pLocalPlayerMode;
      this.previousLocalPlayerMode = pPreviousLocalPlayerMode;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   /**
    * Sets the game type for the player.
    */
   public void setLocalMode(GameType pType) {
      if (pType != this.localPlayerMode) {
         this.previousLocalPlayerMode = this.localPlayerMode;
      }

      this.localPlayerMode = pType;
      this.localPlayerMode.updatePlayerAbilities(this.minecraft.player.getAbilities());
   }

   public boolean canHurtPlayer() {
      return this.localPlayerMode.isSurvival();
   }

   public boolean destroyBlock(BlockPos pPos) {
      if (minecraft.player.getMainHandItem().onBlockStartBreak(pPos, minecraft.player)) return false;
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pPos, this.localPlayerMode)) {
         return false;
      } else {
         Level level = this.minecraft.level;
         BlockState blockstate = level.getBlockState(pPos);
         if (!this.minecraft.player.getMainHandItem().getItem().canAttackBlock(blockstate, level, pPos, this.minecraft.player)) {
            return false;
         } else {
            Block block = blockstate.getBlock();
            if (block instanceof GameMasterBlock && !this.minecraft.player.canUseGameMasterBlocks()) {
               return false;
            } else if (blockstate.isAir()) {
               return false;
            } else {
               FluidState fluidstate = level.getFluidState(pPos);
               boolean flag = blockstate.onDestroyedByPlayer(level, pPos, minecraft.player, false, fluidstate);
               if (flag) {
                  block.destroy(level, pPos, blockstate);
               }

               return flag;
            }
         }
      }
   }

   /**
    * Called when the player is hitting a block with an item.
    */
   public boolean startDestroyBlock(BlockPos pLoc, Direction pFace) {
      if (this.minecraft.player.blockActionRestricted(this.minecraft.level, pLoc, this.localPlayerMode)) {
         return false;
      } else if (!this.minecraft.level.getWorldBorder().isWithinBounds(pLoc)) {
         return false;
      } else {
         if (this.localPlayerMode.isCreative()) {
            BlockState blockstate = this.minecraft.level.getBlockState(pLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pLoc, blockstate, 1.0F);
            this.sendBlockAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pLoc, pFace);
            if (!net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this.minecraft.player, pLoc, pFace).isCanceled())
            this.destroyBlock(pLoc);
            this.destroyDelay = 5;
         } else if (!this.isDestroying || !this.sameDestroyTarget(pLoc)) {
            if (this.isDestroying) {
               this.sendBlockAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, pFace);
            }
            net.minecraftforge.event.entity.player.PlayerInteractEvent.LeftClickBlock event = net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this.minecraft.player, pLoc, pFace);

            BlockState blockstate1 = this.minecraft.level.getBlockState(pLoc);
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pLoc, blockstate1, 0.0F);
            this.sendBlockAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pLoc, pFace);
            boolean flag = !blockstate1.isAir();
            if (flag && this.destroyProgress == 0.0F) {
               if (event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY)
               blockstate1.attack(this.minecraft.level, pLoc, this.minecraft.player);
            }

            if (event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) return true;
            if (flag && blockstate1.getDestroyProgress(this.minecraft.player, this.minecraft.player.level, pLoc) >= 1.0F) {
               this.destroyBlock(pLoc);
            } else {
               this.isDestroying = true;
               this.destroyBlockPos = pLoc;
               this.destroyingItem = this.minecraft.player.getMainHandItem();
               this.destroyProgress = 0.0F;
               this.destroyTicks = 0.0F;
               this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, (int)(this.destroyProgress * 10.0F) - 1);
            }
         }

         return true;
      }
   }

   /**
    * Resets current block damage
    */
   public void stopDestroyBlock() {
      if (this.isDestroying) {
         BlockState blockstate = this.minecraft.level.getBlockState(this.destroyBlockPos);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, this.destroyBlockPos, blockstate, -1.0F);
         this.sendBlockAction(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, this.destroyBlockPos, Direction.DOWN);
         this.isDestroying = false;
         this.destroyProgress = 0.0F;
         this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, -1);
         this.minecraft.player.resetAttackStrengthTicker();
      }

   }

   public boolean continueDestroyBlock(BlockPos pPosBlock, Direction pDirectionFacing) {
      this.ensureHasSentCarriedItem();
      if (this.destroyDelay > 0) {
         --this.destroyDelay;
         return true;
      } else if (this.localPlayerMode.isCreative() && this.minecraft.level.getWorldBorder().isWithinBounds(pPosBlock)) {
         this.destroyDelay = 5;
         BlockState blockstate1 = this.minecraft.level.getBlockState(pPosBlock);
         this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pPosBlock, blockstate1, 1.0F);
         this.sendBlockAction(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pPosBlock, pDirectionFacing);
         if (!net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this.minecraft.player, pPosBlock, pDirectionFacing).isCanceled())
         this.destroyBlock(pPosBlock);
         return true;
      } else if (this.sameDestroyTarget(pPosBlock)) {
         BlockState blockstate = this.minecraft.level.getBlockState(pPosBlock);
         if (blockstate.isAir()) {
            this.isDestroying = false;
            return false;
         } else {
            this.destroyProgress += blockstate.getDestroyProgress(this.minecraft.player, this.minecraft.player.level, pPosBlock);
            if (this.destroyTicks % 4.0F == 0.0F) {
               SoundType soundtype = blockstate.getSoundType(this.minecraft.level, pPosBlock, this.minecraft.player);
               this.minecraft.getSoundManager().play(new SimpleSoundInstance(soundtype.getHitSound(), SoundSource.BLOCKS, (soundtype.getVolume() + 1.0F) / 8.0F, soundtype.getPitch() * 0.5F, pPosBlock));
            }

            ++this.destroyTicks;
            this.minecraft.getTutorial().onDestroyBlock(this.minecraft.level, pPosBlock, blockstate, Mth.clamp(this.destroyProgress, 0.0F, 1.0F));
            if (net.minecraftforge.common.ForgeHooks.onLeftClickBlock(this.minecraft.player, pPosBlock, pDirectionFacing).getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) return true;
            if (this.destroyProgress >= 1.0F) {
               this.isDestroying = false;
               this.sendBlockAction(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pPosBlock, pDirectionFacing);
               this.destroyBlock(pPosBlock);
               this.destroyProgress = 0.0F;
               this.destroyTicks = 0.0F;
               this.destroyDelay = 5;
            }

            this.minecraft.level.destroyBlockProgress(this.minecraft.player.getId(), this.destroyBlockPos, (int)(this.destroyProgress * 10.0F) - 1);
            return true;
         }
      } else {
         return this.startDestroyBlock(pPosBlock, pDirectionFacing);
      }
   }

   /**
    * player reach distance = 4F
    */
   public float getPickRange() {
      float attrib = (float)minecraft.player.getAttribute(net.minecraftforge.common.ForgeMod.REACH_DISTANCE.get()).getValue();
      return this.localPlayerMode.isCreative() ? attrib : attrib - 0.5F;
   }

   public void tick() {
      this.ensureHasSentCarriedItem();
      if (this.connection.getConnection().isConnected()) {
         this.connection.getConnection().tick();
      } else {
         this.connection.getConnection().handleDisconnection();
      }

   }

   private boolean sameDestroyTarget(BlockPos pPos) {
      ItemStack itemstack = this.minecraft.player.getMainHandItem();
      boolean flag = this.destroyingItem.isEmpty() && itemstack.isEmpty();
      if (!this.destroyingItem.isEmpty() && !itemstack.isEmpty()) {
         flag = !this.destroyingItem.shouldCauseBlockBreakReset(itemstack);
      }

      return pPos.equals(this.destroyBlockPos) && flag;
   }

   /**
    * Syncs the current player item with the server
    */
   private void ensureHasSentCarriedItem() {
      int i = this.minecraft.player.getInventory().selected;
      if (i != this.carriedIndex) {
         this.carriedIndex = i;
         this.connection.send(new ServerboundSetCarriedItemPacket(this.carriedIndex));
      }

   }

   public InteractionResult useItemOn(LocalPlayer pPlayer, ClientLevel pLevel, InteractionHand pHand, BlockHitResult pBlockHitResult) {
      this.ensureHasSentCarriedItem();
      BlockPos blockpos = pBlockHitResult.getBlockPos();
      if (!this.minecraft.level.getWorldBorder().isWithinBounds(blockpos)) {
         return InteractionResult.FAIL;
      } else {
         ItemStack itemstack = pPlayer.getItemInHand(pHand);
         net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event = net.minecraftforge.common.ForgeHooks
                 .onRightClickBlock(pPlayer, pHand, blockpos, pBlockHitResult);
         if (event.isCanceled()) {
            this.connection.send(new ServerboundUseItemOnPacket(pHand, pBlockHitResult));
            return event.getCancellationResult();
         }
         if (this.localPlayerMode == GameType.SPECTATOR) {
            this.connection.send(new ServerboundUseItemOnPacket(pHand, pBlockHitResult));
            return InteractionResult.SUCCESS;
         } else {
            UseOnContext useoncontext = new UseOnContext(pPlayer, pHand, pBlockHitResult);
            if (event.getUseItem() != net.minecraftforge.eventbus.api.Event.Result.DENY) {
               InteractionResult result = itemstack.onItemUseFirst(useoncontext);
               if (result != InteractionResult.PASS) {
                  this.connection.send(new ServerboundUseItemOnPacket(pHand, pBlockHitResult));
                  return result;
               }
            }
            boolean flag = !pPlayer.getMainHandItem().doesSneakBypassUse(pLevel,blockpos,pPlayer) || !pPlayer.getOffhandItem().doesSneakBypassUse(pLevel,blockpos,pPlayer);
            boolean flag1 = pPlayer.isSecondaryUseActive() && flag;
            if (event.getUseBlock() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || (event.getUseBlock() != net.minecraftforge.eventbus.api.Event.Result.DENY && !flag1)) {
               InteractionResult interactionresult = pLevel.getBlockState(blockpos).use(pLevel, pPlayer, pHand, pBlockHitResult);
               if (interactionresult.consumesAction()) {
                  this.connection.send(new ServerboundUseItemOnPacket(pHand, pBlockHitResult));
                  return interactionresult;
               }
            }

            this.connection.send(new ServerboundUseItemOnPacket(pHand, pBlockHitResult));
            if (event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.DENY) return InteractionResult.PASS;
            if (event.getUseItem() == net.minecraftforge.eventbus.api.Event.Result.ALLOW || (!itemstack.isEmpty() && !pPlayer.getCooldowns().isOnCooldown(itemstack.getItem()))) {
               InteractionResult interactionresult1;
               if (this.localPlayerMode.isCreative()) {
                  int i = itemstack.getCount();
                  interactionresult1 = itemstack.useOn(useoncontext);
                  itemstack.setCount(i);
               } else {
                  interactionresult1 = itemstack.useOn(useoncontext);
               }

               return interactionresult1;
            } else {
               return InteractionResult.PASS;
            }
         }
      }
   }

   public InteractionResult useItem(Player pPlayer, Level pLevel, InteractionHand pHand) {
      if (this.localPlayerMode == GameType.SPECTATOR) {
         return InteractionResult.PASS;
      } else {
         this.ensureHasSentCarriedItem();
         this.connection.send(new ServerboundMovePlayerPacket.PosRot(pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), pPlayer.getYRot(), pPlayer.getXRot(), pPlayer.isOnGround()));
         this.connection.send(new ServerboundUseItemPacket(pHand));
         ItemStack itemstack = pPlayer.getItemInHand(pHand);
         if (pPlayer.getCooldowns().isOnCooldown(itemstack.getItem())) {
            return InteractionResult.PASS;
         } else {
            InteractionResult cancelResult = net.minecraftforge.common.ForgeHooks.onItemRightClick(pPlayer, pHand);
            if (cancelResult != null) return cancelResult;
            InteractionResultHolder<ItemStack> interactionresultholder = itemstack.use(pLevel, pPlayer, pHand);
            ItemStack itemstack1 = interactionresultholder.getObject();
            if (itemstack1 != itemstack) {
               pPlayer.setItemInHand(pHand, itemstack1);
               if (itemstack1.isEmpty()) net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(pPlayer, itemstack, pHand);
            }

            return interactionresultholder.getResult();
         }
      }
   }

   public LocalPlayer createPlayer(ClientLevel pLevel, StatsCounter pStatsManager, ClientRecipeBook pRecipes) {
      return this.createPlayer(pLevel, pStatsManager, pRecipes, false, false);
   }

   public LocalPlayer createPlayer(ClientLevel pLevel, StatsCounter pStatsManager, ClientRecipeBook pRecipes, boolean pWasShiftKeyDown, boolean pWasSprinting) {
      return new LocalPlayer(this.minecraft, pLevel, this.connection, pStatsManager, pRecipes, pWasShiftKeyDown, pWasSprinting);
   }

   /**
    * Attacks an entity
    */
   public void attack(Player pPlayer, Entity pTargetEntity) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createAttackPacket(pTargetEntity, pPlayer.isShiftKeyDown()));
      if (this.localPlayerMode != GameType.SPECTATOR) {
         pPlayer.attack(pTargetEntity);
         pPlayer.resetAttackStrengthTicker();
      }

   }

   /**
    * Handles right clicking an entity, sends a packet to the server.
    */
   public InteractionResult interact(Player pPlayer, Entity pTarget, InteractionHand pHand) {
      this.ensureHasSentCarriedItem();
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(pTarget, pPlayer.isShiftKeyDown(), pHand));
      if (this.localPlayerMode == GameType.SPECTATOR) return InteractionResult.PASS; // don't fire for spectators to match non-specific EntityInteract
      InteractionResult cancelResult = net.minecraftforge.common.ForgeHooks.onInteractEntity(pPlayer, pTarget, pHand);
      if(cancelResult != null) return cancelResult;
      return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : pPlayer.interactOn(pTarget, pHand);
   }

   /**
    * Handles right clicking an entity from the entities side, sends a packet to the server.
    */
   public InteractionResult interactAt(Player pPlayer, Entity pTarget, EntityHitResult pRay, InteractionHand pHand) {
      this.ensureHasSentCarriedItem();
      Vec3 vec3 = pRay.getLocation().subtract(pTarget.getX(), pTarget.getY(), pTarget.getZ());
      this.connection.send(ServerboundInteractPacket.createInteractionPacket(pTarget, pPlayer.isShiftKeyDown(), pHand, vec3));
      if (this.localPlayerMode == GameType.SPECTATOR) return InteractionResult.PASS; // don't fire for spectators to match non-specific EntityInteract
      InteractionResult cancelResult = net.minecraftforge.common.ForgeHooks.onInteractEntityAt(pPlayer, pTarget, pRay, pHand);
      if(cancelResult != null) return cancelResult;
      return this.localPlayerMode == GameType.SPECTATOR ? InteractionResult.PASS : pTarget.interactAt(pPlayer, vec3, pHand);
   }

   public void handleInventoryMouseClick(int pContainerId, int pSlotId, int pMouseButton, ClickType pClickType, Player pPlayer) {
      AbstractContainerMenu abstractcontainermenu = pPlayer.containerMenu;
      NonNullList<Slot> nonnulllist = abstractcontainermenu.slots;
      int i = nonnulllist.size();
      List<ItemStack> list = Lists.newArrayListWithCapacity(i);

      for(Slot slot : nonnulllist) {
         list.add(slot.getItem().copy());
      }

      abstractcontainermenu.clicked(pSlotId, pMouseButton, pClickType, pPlayer);
      Int2ObjectMap<ItemStack> int2objectmap = new Int2ObjectOpenHashMap<>();

      for(int j = 0; j < i; ++j) {
         ItemStack itemstack = list.get(j);
         ItemStack itemstack1 = nonnulllist.get(j).getItem();
         if (!ItemStack.matches(itemstack, itemstack1)) {
            int2objectmap.put(j, itemstack1.copy());
         }
      }

      this.connection.send(new ServerboundContainerClickPacket(pContainerId, abstractcontainermenu.getStateId(), pSlotId, pMouseButton, pClickType, abstractcontainermenu.getCarried().copy(), int2objectmap));
   }

   public void handlePlaceRecipe(int pContainerId, Recipe<?> pRecipe, boolean pPlaceAll) {
      this.connection.send(new ServerboundPlaceRecipePacket(pContainerId, pRecipe, pPlaceAll));
   }

   /**
    * GuiEnchantment uses this during multiplayer to tell PlayerControllerMP to send a packet indicating the enchantment
    * action the player has taken.
    */
   public void handleInventoryButtonClick(int pContainerId, int pButtonId) {
      this.connection.send(new ServerboundContainerButtonClickPacket(pContainerId, pButtonId));
   }

   /**
    * Used in PlayerControllerMP to update the server with an ItemStack in a slot.
    */
   public void handleCreativeModeItemAdd(ItemStack pStack, int pSlotId) {
      if (this.localPlayerMode.isCreative()) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(pSlotId, pStack));
      }

   }

   /**
    * Sends a Packet107 to the server to drop the item on the ground
    */
   public void handleCreativeModeItemDrop(ItemStack pStack) {
      if (this.localPlayerMode.isCreative() && !pStack.isEmpty()) {
         this.connection.send(new ServerboundSetCreativeModeSlotPacket(-1, pStack));
      }

   }

   public void releaseUsingItem(Player pPlayer) {
      this.ensureHasSentCarriedItem();
      this.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.RELEASE_USE_ITEM, BlockPos.ZERO, Direction.DOWN));
      pPlayer.releaseUsingItem();
   }

   public boolean hasExperience() {
      return this.localPlayerMode.isSurvival();
   }

   /**
    * Checks if the player is not creative, used for checking if it should break a block instantly
    */
   public boolean hasMissTime() {
      return !this.localPlayerMode.isCreative();
   }

   /**
    * returns true if player is in creative mode
    */
   public boolean hasInfiniteItems() {
      return this.localPlayerMode.isCreative();
   }

   /**
    * true for hitting entities far away.
    */
   public boolean hasFarPickRange() {
      return this.localPlayerMode.isCreative();
   }

   /**
    * Checks if the player is riding a horse, used to chose the GUI to open
    */
   public boolean isServerControlledInventory() {
      return this.minecraft.player.isPassenger() && this.minecraft.player.getVehicle() instanceof AbstractHorse;
   }

   public boolean isAlwaysFlying() {
      return this.localPlayerMode == GameType.SPECTATOR;
   }

   @Nullable
   public GameType getPreviousPlayerMode() {
      return this.previousLocalPlayerMode;
   }

   public GameType getPlayerMode() {
      return this.localPlayerMode;
   }

   /**
    * Return isHittingBlock
    */
   public boolean isDestroying() {
      return this.isDestroying;
   }

   public void handlePickItem(int pIndex) {
      this.connection.send(new ServerboundPickItemPacket(pIndex));
   }

   private void sendBlockAction(ServerboundPlayerActionPacket.Action pAction, BlockPos pPos, Direction pDir) {
      LocalPlayer localplayer = this.minecraft.player;
      this.unAckedActions.put(Pair.of(pPos, pAction), localplayer.position());
      this.connection.send(new ServerboundPlayerActionPacket(pAction, pPos, pDir));
   }

   public void handleBlockBreakAck(ClientLevel pLevel, BlockPos pPos, BlockState pBlock, ServerboundPlayerActionPacket.Action pAction, boolean pSuccessful) {
      Vec3 vec3 = this.unAckedActions.remove(Pair.of(pPos, pAction));
      BlockState blockstate = pLevel.getBlockState(pPos);
      if ((vec3 == null || !pSuccessful || pAction != ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK && blockstate != pBlock) && blockstate != pBlock) {
         pLevel.setKnownState(pPos, pBlock);
         Player player = this.minecraft.player;
         if (vec3 != null && pLevel == player.level && player.isColliding(pPos, pBlock)) {
            player.absMoveTo(vec3.x, vec3.y, vec3.z);
         }
      }

      while(this.unAckedActions.size() >= 50) {
         Pair<BlockPos, ServerboundPlayerActionPacket.Action> pair = this.unAckedActions.firstKey();
         this.unAckedActions.removeFirst();
         LOGGER.error("Too many unacked block actions, dropping {}", (Object)pair);
      }

   }
}
