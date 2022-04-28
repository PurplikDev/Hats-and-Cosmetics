package net.minecraft.server.level;

import com.google.common.collect.Lists;
import com.mojang.authlib.GameProfile;
import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Random;
import java.util.UUID;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.Util;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.SectionPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddPlayerPacket;
import net.minecraft.network.protocol.game.ClientboundAnimatePacket;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundChangeDifficultyPacket;
import net.minecraft.network.protocol.game.ClientboundChatPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetContentPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetDataPacket;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.network.protocol.game.ClientboundEntityEventPacket;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.network.protocol.game.ClientboundHorseScreenOpenPacket;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenBookPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundOpenSignEditorPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEndPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatEnterPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerCombatKillPacket;
import net.minecraft.network.protocol.game.ClientboundPlayerLookAtPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveMobEffectPacket;
import net.minecraft.network.protocol.game.ClientboundResourcePackPacket;
import net.minecraft.network.protocol.game.ClientboundRespawnPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetExperiencePacket;
import net.minecraft.network.protocol.game.ClientboundSetHealthPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundUpdateMobEffectPacket;
import net.minecraft.network.protocol.game.ServerboundClientInformationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.TextFilter;
import net.minecraft.server.players.PlayerList;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.ServerRecipeBook;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.EntityDamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.NeutralMob;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.ChatVisiblity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import net.minecraft.world.inventory.ContainerSynchronizer;
import net.minecraft.world.inventory.HorseInventoryMenu;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ComplexItem;
import net.minecraft.world.item.ItemCooldowns;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ServerItemCooldowns;
import net.minecraft.world.item.WrittenBookItem;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.CommandBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Score;
import net.minecraft.world.scores.Team;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerPlayer extends Player {
   private static final Logger LOGGER = LogManager.getLogger();
   private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_XZ = 32;
   private static final int NEUTRAL_MOB_DEATH_NOTIFICATION_RADII_Y = 10;
   public ServerGamePacketListenerImpl connection;
   public final MinecraftServer server;
   public final ServerPlayerGameMode gameMode;
   private final PlayerAdvancements advancements;
   private final ServerStatsCounter stats;
   /** the total health of the player, includes actual health and absorption health. Updated every tick. */
   private float lastRecordedHealthAndAbsorption = Float.MIN_VALUE;
   private int lastRecordedFoodLevel = Integer.MIN_VALUE;
   private int lastRecordedAirLevel = Integer.MIN_VALUE;
   private int lastRecordedArmor = Integer.MIN_VALUE;
   private int lastRecordedLevel = Integer.MIN_VALUE;
   private int lastRecordedExperience = Integer.MIN_VALUE;
   private float lastSentHealth = -1.0E8F;
   private int lastSentFood = -99999999;
   private boolean lastFoodSaturationZero = true;
   private int lastSentExp = -99999999;
   private int spawnInvulnerableTime = 60;
   private ChatVisiblity chatVisibility = ChatVisiblity.FULL;
   private boolean canChatColor = true;
   private long lastActionTime = Util.getMillis();
   /** The entity the player is currently spectating through. */
   @Nullable
   private Entity camera;
   private boolean isChangingDimension;
   private boolean seenCredits;
   private final ServerRecipeBook recipeBook = new ServerRecipeBook();
   @Nullable
   private Vec3 levitationStartPos;
   private int levitationStartTime;
   private boolean disconnected;
   @Nullable
   private Vec3 startingToFallPosition;
   @Nullable
   private Vec3 enteredNetherPosition;
   @Nullable
   private Vec3 enteredLavaOnVehiclePosition;
   /** Player section position as last updated by TicketManager, used by ChunkManager */
   private SectionPos lastSectionPos = SectionPos.of(0, 0, 0);
   private ResourceKey<Level> respawnDimension = Level.OVERWORLD;
   @Nullable
   private BlockPos respawnPosition;
   private boolean respawnForced;
   private float respawnAngle;
   private final TextFilter textFilter;
   private boolean textFilteringEnabled;
   private boolean allowsListing = true;
   private final ContainerSynchronizer containerSynchronizer = new ContainerSynchronizer() {
      public void sendInitialData(AbstractContainerMenu p_143448_, NonNullList<ItemStack> p_143449_, ItemStack p_143450_, int[] p_143451_) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetContentPacket(p_143448_.containerId, p_143448_.incrementStateId(), p_143449_, p_143450_));

         for(int i = 0; i < p_143451_.length; ++i) {
            this.broadcastDataValue(p_143448_, i, p_143451_[i]);
         }

      }

      public void sendSlotChange(AbstractContainerMenu p_143441_, int p_143442_, ItemStack p_143443_) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(p_143441_.containerId, p_143441_.incrementStateId(), p_143442_, p_143443_));
      }

      public void sendCarriedChange(AbstractContainerMenu p_143445_, ItemStack p_143446_) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetSlotPacket(-1, p_143445_.incrementStateId(), -1, p_143446_));
      }

      public void sendDataChange(AbstractContainerMenu p_143437_, int p_143438_, int p_143439_) {
         this.broadcastDataValue(p_143437_, p_143438_, p_143439_);
      }

      private void broadcastDataValue(AbstractContainerMenu p_143455_, int p_143456_, int p_143457_) {
         ServerPlayer.this.connection.send(new ClientboundContainerSetDataPacket(p_143455_.containerId, p_143456_, p_143457_));
      }
   };
   private final ContainerListener containerListener = new ContainerListener() {
      /**
       * Sends the contents of an inventory slot to the client-side Container. This doesn't have to match the actual
       * contents of that slot.
       */
      public void slotChanged(AbstractContainerMenu p_143466_, int p_143467_, ItemStack p_143468_) {
         Slot slot = p_143466_.getSlot(p_143467_);
         if (!(slot instanceof ResultSlot)) {
            if (slot.container == ServerPlayer.this.getInventory()) {
               CriteriaTriggers.INVENTORY_CHANGED.trigger(ServerPlayer.this, ServerPlayer.this.getInventory(), p_143468_);
            }

         }
      }

      public void dataChanged(AbstractContainerMenu p_143462_, int p_143463_, int p_143464_) {
      }
   };
   public int containerCounter;
   public int latency;
   public boolean wonGame;

   public ServerPlayer(MinecraftServer p_143384_, ServerLevel p_143385_, GameProfile p_143386_) {
      super(p_143385_, p_143385_.getSharedSpawnPos(), p_143385_.getSharedSpawnAngle(), p_143386_);
      this.textFilter = p_143384_.createTextFilterForPlayer(this);
      this.gameMode = p_143384_.createGameModeForPlayer(this);
      this.server = p_143384_;
      this.stats = p_143384_.getPlayerList().getPlayerStats(this);
      this.advancements = p_143384_.getPlayerList().getPlayerAdvancements(this);
      this.maxUpStep = 1.0F;
      this.fudgeSpawnLocation(p_143385_);
   }

   private void fudgeSpawnLocation(ServerLevel p_9202_) {
      BlockPos blockpos = p_9202_.getSharedSpawnPos();
      if (p_9202_.dimensionType().hasSkyLight() && p_9202_.getServer().getWorldData().getGameType() != GameType.ADVENTURE) {
         int i = Math.max(0, this.server.getSpawnRadius(p_9202_));
         int j = Mth.floor(p_9202_.getWorldBorder().getDistanceToBorder((double)blockpos.getX(), (double)blockpos.getZ()));
         if (j < i) {
            i = j;
         }

         if (j <= 1) {
            i = 1;
         }

         long k = (long)(i * 2 + 1);
         long l = k * k;
         int i1 = l > 2147483647L ? Integer.MAX_VALUE : (int)l;
         int j1 = this.getCoprime(i1);
         int k1 = (new Random()).nextInt(i1);

         for(int l1 = 0; l1 < i1; ++l1) {
            int i2 = (k1 + j1 * l1) % i1;
            int j2 = i2 % (i * 2 + 1);
            int k2 = i2 / (i * 2 + 1);
            BlockPos blockpos1 = PlayerRespawnLogic.getOverworldRespawnPos(p_9202_, blockpos.getX() + j2 - i, blockpos.getZ() + k2 - i);
            if (blockpos1 != null) {
               this.moveTo(blockpos1, 0.0F, 0.0F);
               if (p_9202_.noCollision(this)) {
                  break;
               }
            }
         }
      } else {
         this.moveTo(blockpos, 0.0F, 0.0F);

         while(!p_9202_.noCollision(this) && this.getY() < (double)(p_9202_.getMaxBuildHeight() - 1)) {
            this.setPos(this.getX(), this.getY() + 1.0D, this.getZ());
         }
      }

   }

   private int getCoprime(int p_9238_) {
      return p_9238_ <= 16 ? p_9238_ - 1 : 17;
   }

   /**
    * (abstract) Protected helper method to read subclass entity data from NBT.
    */
   public void readAdditionalSaveData(CompoundTag pCompound) {
      super.readAdditionalSaveData(pCompound);
      if (pCompound.contains("enteredNetherPosition", 10)) {
         CompoundTag compoundtag = pCompound.getCompound("enteredNetherPosition");
         this.enteredNetherPosition = new Vec3(compoundtag.getDouble("x"), compoundtag.getDouble("y"), compoundtag.getDouble("z"));
      }

      this.seenCredits = pCompound.getBoolean("seenCredits");
      if (pCompound.contains("recipeBook", 10)) {
         this.recipeBook.fromNbt(pCompound.getCompound("recipeBook"), this.server.getRecipeManager());
      }

      if (this.isSleeping()) {
         this.stopSleeping();
      }

      if (pCompound.contains("SpawnX", 99) && pCompound.contains("SpawnY", 99) && pCompound.contains("SpawnZ", 99)) {
         this.respawnPosition = new BlockPos(pCompound.getInt("SpawnX"), pCompound.getInt("SpawnY"), pCompound.getInt("SpawnZ"));
         this.respawnForced = pCompound.getBoolean("SpawnForced");
         this.respawnAngle = pCompound.getFloat("SpawnAngle");
         if (pCompound.contains("SpawnDimension")) {
            this.respawnDimension = Level.RESOURCE_KEY_CODEC.parse(NbtOps.INSTANCE, pCompound.get("SpawnDimension")).resultOrPartial(LOGGER::error).orElse(Level.OVERWORLD);
         }
      }

   }

   public void addAdditionalSaveData(CompoundTag pCompound) {
      super.addAdditionalSaveData(pCompound);
      this.storeGameTypes(pCompound);
      pCompound.putBoolean("seenCredits", this.seenCredits);
      if (this.enteredNetherPosition != null) {
         CompoundTag compoundtag = new CompoundTag();
         compoundtag.putDouble("x", this.enteredNetherPosition.x);
         compoundtag.putDouble("y", this.enteredNetherPosition.y);
         compoundtag.putDouble("z", this.enteredNetherPosition.z);
         pCompound.put("enteredNetherPosition", compoundtag);
      }

      Entity entity1 = this.getRootVehicle();
      Entity entity = this.getVehicle();
      if (entity != null && entity1 != this && entity1.hasExactlyOnePlayerPassenger()) {
         CompoundTag compoundtag1 = new CompoundTag();
         CompoundTag compoundtag2 = new CompoundTag();
         entity1.save(compoundtag2);
         compoundtag1.putUUID("Attach", entity.getUUID());
         compoundtag1.put("Entity", compoundtag2);
         pCompound.put("RootVehicle", compoundtag1);
      }

      pCompound.put("recipeBook", this.recipeBook.toNbt());
      pCompound.putString("Dimension", this.level.dimension().location().toString());
      if (this.respawnPosition != null) {
         pCompound.putInt("SpawnX", this.respawnPosition.getX());
         pCompound.putInt("SpawnY", this.respawnPosition.getY());
         pCompound.putInt("SpawnZ", this.respawnPosition.getZ());
         pCompound.putBoolean("SpawnForced", this.respawnForced);
         pCompound.putFloat("SpawnAngle", this.respawnAngle);
         ResourceLocation.CODEC.encodeStart(NbtOps.INSTANCE, this.respawnDimension.location()).resultOrPartial(LOGGER::error).ifPresent((p_9134_) -> {
            pCompound.put("SpawnDimension", p_9134_);
         });
      }

   }

   public void setExperiencePoints(int p_8986_) {
      float f = (float)this.getXpNeededForNextLevel();
      float f1 = (f - 1.0F) / f;
      this.experienceProgress = Mth.clamp((float)p_8986_ / f, 0.0F, f1);
      this.lastSentExp = -1;
   }

   public void setExperienceLevels(int pLevel) {
      this.experienceLevel = pLevel;
      this.lastSentExp = -1;
   }

   /**
    * Add experience levels to this player.
    */
   public void giveExperienceLevels(int pLevels) {
      super.giveExperienceLevels(pLevels);
      this.lastSentExp = -1;
   }

   public void onEnchantmentPerformed(ItemStack pEnchantedItem, int pCost) {
      super.onEnchantmentPerformed(pEnchantedItem, pCost);
      this.lastSentExp = -1;
   }

   public void initMenu(AbstractContainerMenu p_143400_) {
      p_143400_.addSlotListener(this.containerListener);
      p_143400_.setSynchronizer(this.containerSynchronizer);
   }

   public void initInventoryMenu() {
      this.initMenu(this.inventoryMenu);
   }

   /**
    * Sends an ENTER_COMBAT packet to the client
    */
   public void onEnterCombat() {
      super.onEnterCombat();
      this.connection.send(new ClientboundPlayerCombatEnterPacket());
   }

   /**
    * Sends an END_COMBAT packet to the client
    */
   public void onLeaveCombat() {
      super.onLeaveCombat();
      this.connection.send(new ClientboundPlayerCombatEndPacket(this.getCombatTracker()));
   }

   protected void onInsideBlock(BlockState pState) {
      CriteriaTriggers.ENTER_BLOCK.trigger(this, pState);
   }

   protected ItemCooldowns createItemCooldowns() {
      return new ServerItemCooldowns(this);
   }

   /**
    * Called to update the entity's position/logic.
    */
   public void tick() {
      this.gameMode.tick();
      --this.spawnInvulnerableTime;
      if (this.invulnerableTime > 0) {
         --this.invulnerableTime;
      }

      this.containerMenu.broadcastChanges();
      if (!this.level.isClientSide && !this.containerMenu.stillValid(this)) {
         this.closeContainer();
         this.containerMenu = this.inventoryMenu;
      }

      Entity entity = this.getCamera();
      if (entity != this) {
         if (entity.isAlive()) {
            this.absMoveTo(entity.getX(), entity.getY(), entity.getZ(), entity.getYRot(), entity.getXRot());
            this.getLevel().getChunkSource().move(this);
            if (this.wantsToStopRiding()) {
               this.setCamera(this);
            }
         } else {
            this.setCamera(this);
         }
      }

      CriteriaTriggers.TICK.trigger(this);
      if (this.levitationStartPos != null) {
         CriteriaTriggers.LEVITATION.trigger(this, this.levitationStartPos, this.tickCount - this.levitationStartTime);
      }

      this.trackStartFallingPosition();
      this.trackEnteredOrExitedLavaOnVehicle();
      this.advancements.flushDirty(this);
   }

   public void doTick() {
      try {
         if (!this.isSpectator() || !this.touchingUnloadedChunk()) {
            super.tick();
         }

         for(int i = 0; i < this.getInventory().getContainerSize(); ++i) {
            ItemStack itemstack = this.getInventory().getItem(i);
            if (itemstack.getItem().isComplex()) {
               Packet<?> packet = ((ComplexItem)itemstack.getItem()).getUpdatePacket(itemstack, this.level, this);
               if (packet != null) {
                  this.connection.send(packet);
               }
            }
         }

         if (this.getHealth() != this.lastSentHealth || this.lastSentFood != this.foodData.getFoodLevel() || this.foodData.getSaturationLevel() == 0.0F != this.lastFoodSaturationZero) {
            this.connection.send(new ClientboundSetHealthPacket(this.getHealth(), this.foodData.getFoodLevel(), this.foodData.getSaturationLevel()));
            this.lastSentHealth = this.getHealth();
            this.lastSentFood = this.foodData.getFoodLevel();
            this.lastFoodSaturationZero = this.foodData.getSaturationLevel() == 0.0F;
         }

         if (this.getHealth() + this.getAbsorptionAmount() != this.lastRecordedHealthAndAbsorption) {
            this.lastRecordedHealthAndAbsorption = this.getHealth() + this.getAbsorptionAmount();
            this.updateScoreForCriteria(ObjectiveCriteria.HEALTH, Mth.ceil(this.lastRecordedHealthAndAbsorption));
         }

         if (this.foodData.getFoodLevel() != this.lastRecordedFoodLevel) {
            this.lastRecordedFoodLevel = this.foodData.getFoodLevel();
            this.updateScoreForCriteria(ObjectiveCriteria.FOOD, Mth.ceil((float)this.lastRecordedFoodLevel));
         }

         if (this.getAirSupply() != this.lastRecordedAirLevel) {
            this.lastRecordedAirLevel = this.getAirSupply();
            this.updateScoreForCriteria(ObjectiveCriteria.AIR, Mth.ceil((float)this.lastRecordedAirLevel));
         }

         if (this.getArmorValue() != this.lastRecordedArmor) {
            this.lastRecordedArmor = this.getArmorValue();
            this.updateScoreForCriteria(ObjectiveCriteria.ARMOR, Mth.ceil((float)this.lastRecordedArmor));
         }

         if (this.totalExperience != this.lastRecordedExperience) {
            this.lastRecordedExperience = this.totalExperience;
            this.updateScoreForCriteria(ObjectiveCriteria.EXPERIENCE, Mth.ceil((float)this.lastRecordedExperience));
         }

         if (this.experienceLevel != this.lastRecordedLevel) {
            this.lastRecordedLevel = this.experienceLevel;
            this.updateScoreForCriteria(ObjectiveCriteria.LEVEL, Mth.ceil((float)this.lastRecordedLevel));
         }

         if (this.totalExperience != this.lastSentExp) {
            this.lastSentExp = this.totalExperience;
            this.connection.send(new ClientboundSetExperiencePacket(this.experienceProgress, this.totalExperience, this.experienceLevel));
         }

         if (this.tickCount % 20 == 0) {
            CriteriaTriggers.LOCATION.trigger(this);
         }

      } catch (Throwable throwable) {
         CrashReport crashreport = CrashReport.forThrowable(throwable, "Ticking player");
         CrashReportCategory crashreportcategory = crashreport.addCategory("Player being ticked");
         this.fillCrashReportCategory(crashreportcategory);
         throw new ReportedException(crashreport);
      }
   }

   public void resetFallDistance() {
      if (this.getHealth() > 0.0F && this.startingToFallPosition != null) {
         CriteriaTriggers.FALL_FROM_HEIGHT.trigger(this, this.startingToFallPosition);
      }

      this.startingToFallPosition = null;
      super.resetFallDistance();
   }

   public void trackStartFallingPosition() {
      if (this.fallDistance > 0.0F && this.startingToFallPosition == null) {
         this.startingToFallPosition = this.position();
      }

   }

   public void trackEnteredOrExitedLavaOnVehicle() {
      if (this.getVehicle() != null && this.getVehicle().isInLava()) {
         if (this.enteredLavaOnVehiclePosition == null) {
            this.enteredLavaOnVehiclePosition = this.position();
         } else {
            CriteriaTriggers.RIDE_ENTITY_IN_LAVA_TRIGGER.trigger(this, this.enteredLavaOnVehiclePosition);
         }
      }

      if (this.enteredLavaOnVehiclePosition != null && (this.getVehicle() == null || !this.getVehicle().isInLava())) {
         this.enteredLavaOnVehiclePosition = null;
      }

   }

   private void updateScoreForCriteria(ObjectiveCriteria pCriteria, int pPoints) {
      this.getScoreboard().forAllObjectives(pCriteria, this.getScoreboardName(), (p_9178_) -> {
         p_9178_.setScore(pPoints);
      });
   }

   /**
    * Called when the mob's health reaches 0.
    */
   public void die(DamageSource pCause) {
      if (net.minecraftforge.common.ForgeHooks.onLivingDeath(this, pCause)) return;
      boolean flag = this.level.getGameRules().getBoolean(GameRules.RULE_SHOWDEATHMESSAGES);
      if (flag) {
         Component component = this.getCombatTracker().getDeathMessage();
         this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), component), (p_9142_) -> {
            if (!p_9142_.isSuccess()) {
               int i = 256;
               String s = component.getString(256);
               Component component1 = new TranslatableComponent("death.attack.message_too_long", (new TextComponent(s)).withStyle(ChatFormatting.YELLOW));
               Component component2 = (new TranslatableComponent("death.attack.even_more_magic", this.getDisplayName())).withStyle((p_143420_) -> {
                  return p_143420_.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, component1));
               });
               this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), component2));
            }

         });
         Team team = this.getTeam();
         if (team != null && team.getDeathMessageVisibility() != Team.Visibility.ALWAYS) {
            if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OTHER_TEAMS) {
               this.server.getPlayerList().broadcastToTeam(this, component);
            } else if (team.getDeathMessageVisibility() == Team.Visibility.HIDE_FOR_OWN_TEAM) {
               this.server.getPlayerList().broadcastToAllExceptTeam(this, component);
            }
         } else {
            this.server.getPlayerList().broadcastMessage(component, ChatType.SYSTEM, Util.NIL_UUID);
         }
      } else {
         this.connection.send(new ClientboundPlayerCombatKillPacket(this.getCombatTracker(), TextComponent.EMPTY));
      }

      this.removeEntitiesOnShoulder();
      if (this.level.getGameRules().getBoolean(GameRules.RULE_FORGIVE_DEAD_PLAYERS)) {
         this.tellNeutralMobsThatIDied();
      }

      if (!this.isSpectator()) {
         this.dropAllDeathLoot(pCause);
      }

      this.getScoreboard().forAllObjectives(ObjectiveCriteria.DEATH_COUNT, this.getScoreboardName(), Score::increment);
      LivingEntity livingentity = this.getKillCredit();
      if (livingentity != null) {
         this.awardStat(Stats.ENTITY_KILLED_BY.get(livingentity.getType()));
         livingentity.awardKillScore(this, this.deathScore, pCause);
         this.createWitherRose(livingentity);
      }

      this.level.broadcastEntityEvent(this, (byte)3);
      this.awardStat(Stats.DEATHS);
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_DEATH));
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      this.clearFire();
      this.setTicksFrozen(0);
      this.setSharedFlagOnFire(false);
      this.getCombatTracker().recheckStatus();
   }

   private void tellNeutralMobsThatIDied() {
      AABB aabb = (new AABB(this.blockPosition())).inflate(32.0D, 10.0D, 32.0D);
      this.level.getEntitiesOfClass(Mob.class, aabb, EntitySelector.NO_SPECTATORS).stream().filter((p_9188_) -> {
         return p_9188_ instanceof NeutralMob;
      }).forEach((p_9057_) -> {
         ((NeutralMob)p_9057_).playerDied(this);
      });
   }

   public void awardKillScore(Entity pKilled, int pScoreValue, DamageSource pDamageSource) {
      if (pKilled != this) {
         super.awardKillScore(pKilled, pScoreValue, pDamageSource);
         this.increaseScore(pScoreValue);
         String s = this.getScoreboardName();
         String s1 = pKilled.getScoreboardName();
         this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_ALL, s, Score::increment);
         if (pKilled instanceof Player) {
            this.awardStat(Stats.PLAYER_KILLS);
            this.getScoreboard().forAllObjectives(ObjectiveCriteria.KILL_COUNT_PLAYERS, s, Score::increment);
         } else {
            this.awardStat(Stats.MOB_KILLS);
         }

         this.handleTeamKill(s, s1, ObjectiveCriteria.TEAM_KILL);
         this.handleTeamKill(s1, s, ObjectiveCriteria.KILLED_BY_TEAM);
         CriteriaTriggers.PLAYER_KILLED_ENTITY.trigger(this, pKilled, pDamageSource);
      }
   }

   private void handleTeamKill(String p_9125_, String p_9126_, ObjectiveCriteria[] p_9127_) {
      PlayerTeam playerteam = this.getScoreboard().getPlayersTeam(p_9126_);
      if (playerteam != null) {
         int i = playerteam.getColor().getId();
         if (i >= 0 && i < p_9127_.length) {
            this.getScoreboard().forAllObjectives(p_9127_[i], p_9125_, Score::increment);
         }
      }

   }

   /**
    * Called when the entity is attacked.
    */
   public boolean hurt(DamageSource pSource, float pAmount) {
      if (this.isInvulnerableTo(pSource)) {
         return false;
      } else {
         boolean flag = this.server.isDedicatedServer() && this.isPvpAllowed() && "fall".equals(pSource.msgId);
         if (!flag && this.spawnInvulnerableTime > 0 && pSource != DamageSource.OUT_OF_WORLD) {
            return false;
         } else {
            if (pSource instanceof EntityDamageSource) {
               Entity entity = pSource.getEntity();
               if (entity instanceof Player && !this.canHarmPlayer((Player)entity)) {
                  return false;
               }

               if (entity instanceof AbstractArrow) {
                  AbstractArrow abstractarrow = (AbstractArrow)entity;
                  Entity entity1 = abstractarrow.getOwner();
                  if (entity1 instanceof Player && !this.canHarmPlayer((Player)entity1)) {
                     return false;
                  }
               }
            }

            return super.hurt(pSource, pAmount);
         }
      }
   }

   public boolean canHarmPlayer(Player pOther) {
      return !this.isPvpAllowed() ? false : super.canHarmPlayer(pOther);
   }

   /**
    * Returns if other players can attack this player
    */
   private boolean isPvpAllowed() {
      return this.server.isPvpAllowed();
   }

   @Nullable
   protected PortalInfo findDimensionEntryPoint(ServerLevel pDestination) {
      PortalInfo portalinfo = super.findDimensionEntryPoint(pDestination);
      if (portalinfo != null && this.level.dimension() == Level.OVERWORLD && pDestination.dimension() == Level.END) {
         Vec3 vec3 = portalinfo.pos.add(0.0D, -1.0D, 0.0D);
         return new PortalInfo(vec3, Vec3.ZERO, 90.0F, 0.0F);
      } else {
         return portalinfo;
      }
   }

   @Nullable
   public Entity changeDimension(ServerLevel pServer, net.minecraftforge.common.util.ITeleporter teleporter) {
      if (!net.minecraftforge.common.ForgeHooks.onTravelToDimension(this, pServer.dimension())) return null;
      this.isChangingDimension = true;
      ServerLevel serverlevel = this.getLevel();
      ResourceKey<Level> resourcekey = serverlevel.dimension();
      if (resourcekey == Level.END && pServer.dimension() == Level.OVERWORLD && teleporter.isVanilla()) { //Forge: Fix non-vanilla teleporters triggering end credits
         this.unRide();
         this.getLevel().removePlayer(this, true); //Forge: The player entity is cloned so keep the data until after cloning calls copyFrom
         if (!this.wonGame) {
            this.wonGame = true;
            this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.WIN_GAME, this.seenCredits ? 0.0F : 1.0F));
            this.seenCredits = true;
         }

         return this;
      } else {
         LevelData leveldata = pServer.getLevelData();
         this.connection.send(new ClientboundRespawnPacket(pServer.dimensionType(), pServer.dimension(), BiomeManager.obfuscateSeed(pServer.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), pServer.isDebug(), pServer.isFlat(), true));
         this.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
         PlayerList playerlist = this.server.getPlayerList();
         playerlist.sendPlayerPermissionLevel(this);
         serverlevel.removeEntity(this, true); //Forge: the player entity is moved to the new world, NOT cloned. So keep the data alive with no matching invalidate call.
         this.revive();
         PortalInfo portalinfo = teleporter.getPortalInfo(this, pServer, this::findDimensionEntryPoint);
         if (portalinfo != null) {
            Entity e = teleporter.placeEntity(this, serverlevel, pServer, this.getYRot(), spawnPortal -> {//Forge: Start vanilla logic
            serverlevel.getProfiler().push("moving");
            if (resourcekey == Level.OVERWORLD && pServer.dimension() == Level.NETHER) {
               this.enteredNetherPosition = this.position();
            } else if (spawnPortal && pServer.dimension() == Level.END) {
               this.createEndPlatform(pServer, new BlockPos(portalinfo.pos));
            }

            serverlevel.getProfiler().pop();
            serverlevel.getProfiler().push("placing");
            this.setLevel(pServer);
            pServer.addDuringPortalTeleport(this);
            this.setRot(portalinfo.yRot, portalinfo.xRot);
            this.moveTo(portalinfo.pos.x, portalinfo.pos.y, portalinfo.pos.z);
            serverlevel.getProfiler().pop();
            this.triggerDimensionChangeTriggers(serverlevel);
            return this;//forge: this is part of the ITeleporter patch
            });//Forge: End vanilla logic
            if (e != this) throw new java.lang.IllegalArgumentException(String.format("Teleporter %s returned not the player entity but instead %s, expected PlayerEntity %s", teleporter, e, this));
            this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
            playerlist.sendLevelInfo(this, pServer);
            playerlist.sendAllPlayerInfo(this);

            for(MobEffectInstance mobeffectinstance : this.getActiveEffects()) {
               this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), mobeffectinstance));
            }

            if (teleporter.playTeleportSound(this, serverlevel, pServer))
            this.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
            this.lastSentExp = -1;
            this.lastSentHealth = -1.0F;
            this.lastSentFood = -1;
            net.minecraftforge.event.ForgeEventFactory.firePlayerChangedDimensionEvent(this, resourcekey, pServer.dimension());
         }

         return this;
      }
   }

   private void createEndPlatform(ServerLevel p_9007_, BlockPos p_9008_) {
      BlockPos.MutableBlockPos blockpos$mutableblockpos = p_9008_.mutable();

      for(int i = -2; i <= 2; ++i) {
         for(int j = -2; j <= 2; ++j) {
            for(int k = -1; k < 3; ++k) {
               BlockState blockstate = k == -1 ? Blocks.OBSIDIAN.defaultBlockState() : Blocks.AIR.defaultBlockState();
               p_9007_.setBlockAndUpdate(blockpos$mutableblockpos.set(p_9008_).move(j, k, i), blockstate);
            }
         }
      }

   }

   /**
    * 
    * @param pFindFrom Position where searching starts from
    */
   protected Optional<BlockUtil.FoundRectangle> getExitPortal(ServerLevel pDestination, BlockPos pFindFrom, boolean pIsToNether, WorldBorder pWorldBorder) {
      Optional<BlockUtil.FoundRectangle> optional = super.getExitPortal(pDestination, pFindFrom, pIsToNether, pWorldBorder);
      if (optional.isPresent()) {
         return optional;
      } else {
         Direction.Axis direction$axis = this.level.getBlockState(this.portalEntrancePos).getOptionalValue(NetherPortalBlock.AXIS).orElse(Direction.Axis.X);
         Optional<BlockUtil.FoundRectangle> optional1 = pDestination.getPortalForcer().createPortal(pFindFrom, direction$axis);
         if (!optional1.isPresent()) {
            LOGGER.error("Unable to create a portal, likely target out of worldborder");
         }

         return optional1;
      }
   }

   private void triggerDimensionChangeTriggers(ServerLevel p_9210_) {
      ResourceKey<Level> resourcekey = p_9210_.dimension();
      ResourceKey<Level> resourcekey1 = this.level.dimension();
      CriteriaTriggers.CHANGED_DIMENSION.trigger(this, resourcekey, resourcekey1);
      if (resourcekey == Level.NETHER && resourcekey1 == Level.OVERWORLD && this.enteredNetherPosition != null) {
         CriteriaTriggers.NETHER_TRAVEL.trigger(this, this.enteredNetherPosition);
      }

      if (resourcekey1 != Level.NETHER) {
         this.enteredNetherPosition = null;
      }

   }

   public boolean broadcastToPlayer(ServerPlayer pPlayer) {
      if (pPlayer.isSpectator()) {
         return this.getCamera() == this;
      } else {
         return this.isSpectator() ? false : super.broadcastToPlayer(pPlayer);
      }
   }

   /**
    * Called when the entity picks up an item.
    */
   public void take(Entity pEntity, int pQuantity) {
      super.take(pEntity, pQuantity);
      this.containerMenu.broadcastChanges();
   }

   public Either<Player.BedSleepingProblem, Unit> startSleepInBed(BlockPos pAt) {
      java.util.Optional<BlockPos> optAt = java.util.Optional.of(pAt);
      Player.BedSleepingProblem ret = net.minecraftforge.event.ForgeEventFactory.onPlayerSleepInBed(this, optAt);
      if (ret != null) return Either.left(ret);
      Direction direction = this.level.getBlockState(pAt).getValue(HorizontalDirectionalBlock.FACING);
      if (!this.isSleeping() && this.isAlive()) {
         if (!this.level.dimensionType().natural()) {
            return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_HERE);
         } else if (!this.bedInRange(pAt, direction)) {
            return Either.left(Player.BedSleepingProblem.TOO_FAR_AWAY);
         } else if (this.bedBlocked(pAt, direction)) {
            return Either.left(Player.BedSleepingProblem.OBSTRUCTED);
         } else {
            this.setRespawnPosition(this.level.dimension(), pAt, this.getYRot(), false, true);
            if (!net.minecraftforge.event.ForgeEventFactory.fireSleepingTimeCheck(this, optAt)) {
               return Either.left(Player.BedSleepingProblem.NOT_POSSIBLE_NOW);
            } else {
               if (!this.isCreative()) {
                  double d0 = 8.0D;
                  double d1 = 5.0D;
                  Vec3 vec3 = Vec3.atBottomCenterOf(pAt);
                  List<Monster> list = this.level.getEntitiesOfClass(Monster.class, new AABB(vec3.x() - 8.0D, vec3.y() - 5.0D, vec3.z() - 8.0D, vec3.x() + 8.0D, vec3.y() + 5.0D, vec3.z() + 8.0D), (p_9062_) -> {
                     return p_9062_.isPreventingPlayerRest(this);
                  });
                  if (!list.isEmpty()) {
                     return Either.left(Player.BedSleepingProblem.NOT_SAFE);
                  }
               }

               Either<Player.BedSleepingProblem, Unit> either = super.startSleepInBed(pAt).ifRight((p_9029_) -> {
                  this.awardStat(Stats.SLEEP_IN_BED);
                  CriteriaTriggers.SLEPT_IN_BED.trigger(this);
               });
               if (!this.getLevel().canSleepThroughNights()) {
                  this.displayClientMessage(new TranslatableComponent("sleep.not_possible"), true);
               }

               ((ServerLevel)this.level).updateSleepingPlayerList();
               return either;
            }
         }
      } else {
         return Either.left(Player.BedSleepingProblem.OTHER_PROBLEM);
      }
   }

   public void startSleeping(BlockPos pPos) {
      this.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));
      super.startSleeping(pPos);
   }

   private boolean bedInRange(BlockPos p_9117_, Direction p_9118_) {
      if (p_9118_ == null) return false;
      return this.isReachableBedBlock(p_9117_) || this.isReachableBedBlock(p_9117_.relative(p_9118_.getOpposite()));
   }

   private boolean isReachableBedBlock(BlockPos p_9223_) {
      Vec3 vec3 = Vec3.atBottomCenterOf(p_9223_);
      return Math.abs(this.getX() - vec3.x()) <= 3.0D && Math.abs(this.getY() - vec3.y()) <= 2.0D && Math.abs(this.getZ() - vec3.z()) <= 3.0D;
   }

   private boolean bedBlocked(BlockPos p_9192_, Direction p_9193_) {
      BlockPos blockpos = p_9192_.above();
      return !this.freeAt(blockpos) || !this.freeAt(blockpos.relative(p_9193_.getOpposite()));
   }

   public void stopSleepInBed(boolean p_9165_, boolean p_9166_) {
      if (this.isSleeping()) {
         this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(this, 2));
      }

      super.stopSleepInBed(p_9165_, p_9166_);
      if (this.connection != null) {
         this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
      }

   }

   public boolean startRiding(Entity pEntity, boolean pForce) {
      Entity entity = this.getVehicle();
      if (!super.startRiding(pEntity, pForce)) {
         return false;
      } else {
         Entity entity1 = this.getVehicle();
         if (entity1 != entity && this.connection != null) {
            this.connection.teleport(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
         }

         return true;
      }
   }

   /**
    * Dismounts this entity from the entity it is riding.
    */
   public void stopRiding() {
      Entity entity = this.getVehicle();
      super.stopRiding();
      Entity entity1 = this.getVehicle();
      if (entity1 != entity && this.connection != null) {
         this.connection.dismount(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
      }

   }

   public void dismountTo(double pX, double pY, double pZ) {
      this.removeVehicle();
      if (this.connection != null) {
         this.connection.dismount(pX, pY, pZ, this.getYRot(), this.getXRot());
      }

   }

   /**
    * Returns whether this Entity is invulnerable to the given DamageSource.
    */
   public boolean isInvulnerableTo(DamageSource pSource) {
      return super.isInvulnerableTo(pSource) || this.isChangingDimension() || this.getAbilities().invulnerable && pSource == DamageSource.WITHER;
   }

   protected void checkFallDamage(double pY, boolean pOnGround, BlockState pState, BlockPos pPos) {
   }

   protected void onChangedBlock(BlockPos pPos) {
      if (!this.isSpectator()) {
         super.onChangedBlock(pPos);
      }

   }

   /**
    * process player falling based on movement packet
    */
   public void doCheckFallDamage(double pY, boolean pOnGround) {
      if (!this.touchingUnloadedChunk()) {
         BlockPos blockpos = this.getOnPos();
         super.checkFallDamage(pY, pOnGround, this.level.getBlockState(blockpos), blockpos);
      }
   }

   public void openTextEdit(SignBlockEntity pSignTile) {
      pSignTile.setAllowedPlayerEditor(this.getUUID());
      this.connection.send(new ClientboundBlockUpdatePacket(this.level, pSignTile.getBlockPos()));
      this.connection.send(new ClientboundOpenSignEditorPacket(pSignTile.getBlockPos()));
   }

   /**
    * get the next window id to use
    */
   public void nextContainerCounter() {
      this.containerCounter = this.containerCounter % 100 + 1;
   }

   public OptionalInt openMenu(@Nullable MenuProvider p_9033_) {
      if (p_9033_ == null) {
         return OptionalInt.empty();
      } else {
         if (this.containerMenu != this.inventoryMenu) {
            this.closeContainer();
         }

         this.nextContainerCounter();
         AbstractContainerMenu abstractcontainermenu = p_9033_.createMenu(this.containerCounter, this.getInventory(), this);
         if (abstractcontainermenu == null) {
            if (this.isSpectator()) {
               this.displayClientMessage((new TranslatableComponent("container.spectatorCantOpen")).withStyle(ChatFormatting.RED), true);
            }

            return OptionalInt.empty();
         } else {
            this.connection.send(new ClientboundOpenScreenPacket(abstractcontainermenu.containerId, abstractcontainermenu.getType(), p_9033_.getDisplayName()));
            this.initMenu(abstractcontainermenu);
            this.containerMenu = abstractcontainermenu;
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open(this, this.containerMenu));
            return OptionalInt.of(this.containerCounter);
         }
      }
   }

   public void sendMerchantOffers(int p_8988_, MerchantOffers p_8989_, int p_8990_, int p_8991_, boolean p_8992_, boolean p_8993_) {
      this.connection.send(new ClientboundMerchantOffersPacket(p_8988_, p_8989_, p_8990_, p_8991_, p_8992_, p_8993_));
   }

   public void openHorseInventory(AbstractHorse pHorse, Container pInventory) {
      if (this.containerMenu != this.inventoryMenu) {
         this.closeContainer();
      }

      this.nextContainerCounter();
      this.connection.send(new ClientboundHorseScreenOpenPacket(this.containerCounter, pInventory.getContainerSize(), pHorse.getId()));
      this.containerMenu = new HorseInventoryMenu(this.containerCounter, this.getInventory(), pInventory, pHorse);
      this.initMenu(this.containerMenu);
      net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Open(this, this.containerMenu));
   }

   public void openItemGui(ItemStack pStack, InteractionHand pHand) {
      if (pStack.is(Items.WRITTEN_BOOK)) {
         if (WrittenBookItem.resolveBookComponents(pStack, this.createCommandSourceStack(), this)) {
            this.containerMenu.broadcastChanges();
         }

         this.connection.send(new ClientboundOpenBookPacket(pHand));
      }

   }

   public void openCommandBlock(CommandBlockEntity pCommandBlock) {
      this.connection.send(ClientboundBlockEntityDataPacket.create(pCommandBlock, BlockEntity::saveWithoutMetadata));
   }

   /**
    * set current crafting inventory back to the 2x2 square
    */
   public void closeContainer() {
      this.connection.send(new ClientboundContainerClosePacket(this.containerMenu.containerId));
      this.doCloseContainer();
   }

   /**
    * Closes the container the player currently has open.
    */
   public void doCloseContainer() {
      this.containerMenu.removed(this);
      this.inventoryMenu.transferState(this.containerMenu);
      net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerContainerEvent.Close(this, this.containerMenu));
      this.containerMenu = this.inventoryMenu;
   }

   public void setPlayerInput(float pStrafe, float pForward, boolean pJumping, boolean pSneaking) {
      if (this.isPassenger()) {
         if (pStrafe >= -1.0F && pStrafe <= 1.0F) {
            this.xxa = pStrafe;
         }

         if (pForward >= -1.0F && pForward <= 1.0F) {
            this.zza = pForward;
         }

         this.jumping = pJumping;
         this.setShiftKeyDown(pSneaking);
      }

   }

   /**
    * Adds a value to a statistic field.
    */
   public void awardStat(Stat<?> pStat, int pAmount) {
      this.stats.increment(this, pStat, pAmount);
      this.getScoreboard().forAllObjectives(pStat, this.getScoreboardName(), (p_8996_) -> {
         p_8996_.add(pAmount);
      });
   }

   public void resetStat(Stat<?> pStat) {
      this.stats.setValue(this, pStat, 0);
      this.getScoreboard().forAllObjectives(pStat, this.getScoreboardName(), Score::reset);
   }

   public int awardRecipes(Collection<Recipe<?>> p_9129_) {
      return this.recipeBook.addRecipes(p_9129_, this);
   }

   public void awardRecipesByKey(ResourceLocation[] p_9168_) {
      List<Recipe<?>> list = Lists.newArrayList();

      for(ResourceLocation resourcelocation : p_9168_) {
         this.server.getRecipeManager().byKey(resourcelocation).ifPresent(list::add);
      }

      this.awardRecipes(list);
   }

   public int resetRecipes(Collection<Recipe<?>> p_9195_) {
      return this.recipeBook.removeRecipes(p_9195_, this);
   }

   public void giveExperiencePoints(int p_9208_) {
      super.giveExperiencePoints(p_9208_);
      this.lastSentExp = -1;
   }

   public void disconnect() {
      this.disconnected = true;
      this.ejectPassengers();
      if (this.isSleeping()) {
         this.stopSleepInBed(true, false);
      }

   }

   public boolean hasDisconnected() {
      return this.disconnected;
   }

   /**
    * this function is called when a players inventory is sent to him, lastHealth is updated on any dimension
    * transitions, then reset.
    */
   public void resetSentInfo() {
      this.lastSentHealth = -1.0E8F;
   }

   public void displayClientMessage(Component pChatComponent, boolean pActionBar) {
      this.sendMessage(pChatComponent, pActionBar ? ChatType.GAME_INFO : ChatType.CHAT, Util.NIL_UUID);
   }

   /**
    * Used for when item use count runs out, ie: eating completed
    */
   protected void completeUsingItem() {
      if (!this.useItem.isEmpty() && this.isUsingItem()) {
         this.connection.send(new ClientboundEntityEventPacket(this, (byte)9));
         super.completeUsingItem();
      }

   }

   public void lookAt(EntityAnchorArgument.Anchor pAnchor, Vec3 pTarget) {
      super.lookAt(pAnchor, pTarget);
      this.connection.send(new ClientboundPlayerLookAtPacket(pAnchor, pTarget.x, pTarget.y, pTarget.z));
   }

   public void lookAt(EntityAnchorArgument.Anchor p_9108_, Entity p_9109_, EntityAnchorArgument.Anchor p_9110_) {
      Vec3 vec3 = p_9110_.apply(p_9109_);
      super.lookAt(p_9108_, vec3);
      this.connection.send(new ClientboundPlayerLookAtPacket(p_9108_, p_9109_, p_9110_));
   }

   public void restoreFrom(ServerPlayer pThat, boolean pKeepEverything) {
      this.textFilteringEnabled = pThat.textFilteringEnabled;
      this.gameMode.setGameModeForPlayer(pThat.gameMode.getGameModeForPlayer(), pThat.gameMode.getPreviousGameModeForPlayer());
      if (pKeepEverything) {
         this.getInventory().replaceWith(pThat.getInventory());
         this.setHealth(pThat.getHealth());
         this.foodData = pThat.foodData;
         this.experienceLevel = pThat.experienceLevel;
         this.totalExperience = pThat.totalExperience;
         this.experienceProgress = pThat.experienceProgress;
         this.setScore(pThat.getScore());
         this.portalEntrancePos = pThat.portalEntrancePos;
      } else if (this.level.getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || pThat.isSpectator()) {
         this.getInventory().replaceWith(pThat.getInventory());
         this.experienceLevel = pThat.experienceLevel;
         this.totalExperience = pThat.totalExperience;
         this.experienceProgress = pThat.experienceProgress;
         this.setScore(pThat.getScore());
      }

      this.enchantmentSeed = pThat.enchantmentSeed;
      this.enderChestInventory = pThat.enderChestInventory;
      this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, pThat.getEntityData().get(DATA_PLAYER_MODE_CUSTOMISATION));
      this.lastSentExp = -1;
      this.lastSentHealth = -1.0F;
      this.lastSentFood = -1;
      this.recipeBook.copyOverData(pThat.recipeBook);
      this.seenCredits = pThat.seenCredits;
      this.enteredNetherPosition = pThat.enteredNetherPosition;
      this.setShoulderEntityLeft(pThat.getShoulderEntityLeft());
      this.setShoulderEntityRight(pThat.getShoulderEntityRight());

      //Copy over a section of the Entity Data from the old player.
      //Allows mods to specify data that persists after players respawn.
      CompoundTag old = pThat.getPersistentData();
      if (old.contains(PERSISTED_NBT_TAG))
          getPersistentData().put(PERSISTED_NBT_TAG, old.get(PERSISTED_NBT_TAG));
      net.minecraftforge.event.ForgeEventFactory.onPlayerClone(this, pThat, !pKeepEverything);
   }

   protected void onEffectAdded(MobEffectInstance p_143393_, @Nullable Entity p_143394_) {
      super.onEffectAdded(p_143393_, p_143394_);
      this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_143393_));
      if (p_143393_.getEffect() == MobEffects.LEVITATION) {
         this.levitationStartTime = this.tickCount;
         this.levitationStartPos = this.position();
      }

      CriteriaTriggers.EFFECTS_CHANGED.trigger(this, p_143394_);
   }

   protected void onEffectUpdated(MobEffectInstance p_143396_, boolean p_143397_, @Nullable Entity p_143398_) {
      super.onEffectUpdated(p_143396_, p_143397_, p_143398_);
      this.connection.send(new ClientboundUpdateMobEffectPacket(this.getId(), p_143396_));
      CriteriaTriggers.EFFECTS_CHANGED.trigger(this, p_143398_);
   }

   protected void onEffectRemoved(MobEffectInstance pEffect) {
      super.onEffectRemoved(pEffect);
      this.connection.send(new ClientboundRemoveMobEffectPacket(this.getId(), pEffect.getEffect()));
      if (pEffect.getEffect() == MobEffects.LEVITATION) {
         this.levitationStartPos = null;
      }

      CriteriaTriggers.EFFECTS_CHANGED.trigger(this, (Entity)null);
   }

   /**
    * Sets the position of the entity and updates the 'last' variables
    */
   public void teleportTo(double pX, double pY, double pZ) {
      this.connection.teleport(pX, pY, pZ, this.getYRot(), this.getXRot());
   }

   public void moveTo(double pX, double pY, double pZ) {
      this.teleportTo(pX, pY, pZ);
      this.connection.resetPosition();
   }

   /**
    * Called when the entity is dealt a critical hit.
    */
   public void crit(Entity pEntityHit) {
      this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(pEntityHit, 4));
   }

   public void magicCrit(Entity pEntityHit) {
      this.getLevel().getChunkSource().broadcastAndSend(this, new ClientboundAnimatePacket(pEntityHit, 5));
   }

   /**
    * Sends the player's abilities to the server (if there is one).
    */
   public void onUpdateAbilities() {
      if (this.connection != null) {
         this.connection.send(new ClientboundPlayerAbilitiesPacket(this.getAbilities()));
         this.updateInvisibilityStatus();
      }
   }

   public ServerLevel getLevel() {
      return (ServerLevel)this.level;
   }

   public boolean setGameMode(GameType p_143404_) {
      if (!net.minecraftforge.common.ForgeHooks.onChangeGameMode(this, this.gameMode.getGameModeForPlayer(), p_143404_)) return false;
      if (!this.gameMode.changeGameModeForPlayer(p_143404_)) {
         return false;
      } else {
         this.connection.send(new ClientboundGameEventPacket(ClientboundGameEventPacket.CHANGE_GAME_MODE, (float)p_143404_.getId()));
         if (p_143404_ == GameType.SPECTATOR) {
            this.removeEntitiesOnShoulder();
            this.stopRiding();
         } else {
            this.setCamera(this);
         }

         this.onUpdateAbilities();
         this.updateEffectVisibility();
         return true;
      }
   }

   /**
    * Returns true if the player is in spectator mode.
    */
   public boolean isSpectator() {
      return this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR;
   }

   public boolean isCreative() {
      return this.gameMode.getGameModeForPlayer() == GameType.CREATIVE;
   }

   /**
    * Send a chat message to the CommandSender
    */
   public void sendMessage(Component pComponent, UUID pSenderUUID) {
      this.sendMessage(pComponent, ChatType.SYSTEM, pSenderUUID);
   }

   public void sendMessage(Component p_9147_, ChatType p_9148_, UUID p_9149_) {
      if (this.acceptsChat(p_9148_)) {
         this.connection.send(new ClientboundChatPacket(p_9147_, p_9148_, p_9149_), (p_9139_) -> {
            if (!p_9139_.isSuccess() && (p_9148_ == ChatType.GAME_INFO || p_9148_ == ChatType.SYSTEM) && this.acceptsChat(ChatType.SYSTEM)) {
               int i = 256;
               String s = p_9147_.getString(256);
               Component component = (new TextComponent(s)).withStyle(ChatFormatting.YELLOW);
               this.connection.send(new ClientboundChatPacket((new TranslatableComponent("multiplayer.message_not_delivered", component)).withStyle(ChatFormatting.RED), ChatType.SYSTEM, p_9149_));
            }

         });
      }
   }

   /**
    * Gets the player's IP address. Used in /banip.
    */
   public String getIpAddress() {
      String s = this.connection.connection.getRemoteAddress().toString();
      s = s.substring(s.indexOf("/") + 1);
      return s.substring(0, s.indexOf(":"));
   }

   public void updateOptions(ServerboundClientInformationPacket pPacket) {
      this.chatVisibility = pPacket.chatVisibility();
      this.canChatColor = pPacket.chatColors();
      this.textFilteringEnabled = pPacket.textFilteringEnabled();
      this.allowsListing = pPacket.allowsListing();
      this.getEntityData().set(DATA_PLAYER_MODE_CUSTOMISATION, (byte)pPacket.modelCustomisation());
      this.getEntityData().set(DATA_PLAYER_MAIN_HAND, (byte)(pPacket.mainHand() == HumanoidArm.LEFT ? 0 : 1));
      this.language = pPacket.language();
   }

   public boolean canChatInColor() {
      return this.canChatColor;
   }

   public ChatVisiblity getChatVisibility() {
      return this.chatVisibility;
   }

   private boolean acceptsChat(ChatType p_143417_) {
      switch(this.chatVisibility) {
      case HIDDEN:
         return p_143417_ == ChatType.GAME_INFO;
      case SYSTEM:
         return p_143417_ == ChatType.SYSTEM || p_143417_ == ChatType.GAME_INFO;
      case FULL:
      default:
         return true;
      }
   }

   public void sendTexturePack(String p_143409_, String p_143410_, boolean p_143411_, @Nullable Component p_143412_) {
      this.connection.send(new ClientboundResourcePackPacket(p_143409_, p_143410_, p_143411_, p_143412_));
   }

   protected int getPermissionLevel() {
      return this.server.getProfilePermissions(this.getGameProfile());
   }

   public void resetLastActionTime() {
      this.lastActionTime = Util.getMillis();
   }

   public ServerStatsCounter getStats() {
      return this.stats;
   }

   public ServerRecipeBook getRecipeBook() {
      return this.recipeBook;
   }

   /**
    * Clears potion metadata values if the entity has no potion effects. Otherwise, updates potion effect color,
    * ambience, and invisibility metadata values
    */
   protected void updateInvisibilityStatus() {
      if (this.isSpectator()) {
         this.removeEffectParticles();
         this.setInvisible(true);
      } else {
         super.updateInvisibilityStatus();
      }

   }

   public Entity getCamera() {
      return (Entity)(this.camera == null ? this : this.camera);
   }

   public void setCamera(@Nullable Entity pEntityToSpectate) {
      Entity entity = this.getCamera();
      this.camera = (Entity)(pEntityToSpectate == null ? this : pEntityToSpectate);
      while (this.camera instanceof net.minecraftforge.entity.PartEntity<?> partEntity) this.camera = partEntity.getParent(); // FORGE: fix MC-46486
      if (entity != this.camera) {
         this.connection.send(new ClientboundSetCameraPacket(this.camera));
         this.teleportTo(this.camera.getX(), this.camera.getY(), this.camera.getZ());
      }

   }

   /**
    * Decrements the counter for the remaining time until the entity may use a portal again.
    */
   protected void processPortalCooldown() {
      if (!this.isChangingDimension) {
         super.processPortalCooldown();
      }

   }

   /**
    * Attacks for the player the targeted entity with the currently equipped item.  The equipped item has hitEntity
    * called on it. Args: targetEntity
    */
   public void attack(Entity pTargetEntity) {
      if (this.gameMode.getGameModeForPlayer() == GameType.SPECTATOR) {
         this.setCamera(pTargetEntity);
      } else {
         super.attack(pTargetEntity);
      }

   }

   public long getLastActionTime() {
      return this.lastActionTime;
   }

   /**
    * Returns null which indicates the tab list should just display the player's name, return a different value to
    * display the specified text instead of the player's name
    */
   @Nullable
   public Component getTabListDisplayName() {
      if (!this.hasTabListName) {
         this.tabListDisplayName = net.minecraftforge.event.ForgeEventFactory.getPlayerTabListDisplayName(this);
         this.hasTabListName = true;
      }
      return this.tabListDisplayName;
   }

   public void swing(InteractionHand pHand) {
      super.swing(pHand);
      this.resetAttackStrengthTicker();
   }

   public boolean isChangingDimension() {
      return this.isChangingDimension;
   }

   public void hasChangedDimension() {
      this.isChangingDimension = false;
   }

   public PlayerAdvancements getAdvancements() {
      return this.advancements;
   }

   public void teleportTo(ServerLevel pNewLevel, double pX, double pY, double pZ, float pYaw, float pPitch) {
      this.setCamera(this);
      this.stopRiding();
      if (pNewLevel == this.level) {
         this.connection.teleport(pX, pY, pZ, pYaw, pPitch);
      } else if (net.minecraftforge.common.ForgeHooks.onTravelToDimension(this, pNewLevel.dimension())) {
         ServerLevel serverlevel = this.getLevel();
         LevelData leveldata = pNewLevel.getLevelData();
         this.connection.send(new ClientboundRespawnPacket(pNewLevel.dimensionType(), pNewLevel.dimension(), BiomeManager.obfuscateSeed(pNewLevel.getSeed()), this.gameMode.getGameModeForPlayer(), this.gameMode.getPreviousGameModeForPlayer(), pNewLevel.isDebug(), pNewLevel.isFlat(), true));
         this.connection.send(new ClientboundChangeDifficultyPacket(leveldata.getDifficulty(), leveldata.isDifficultyLocked()));
         this.server.getPlayerList().sendPlayerPermissionLevel(this);
         serverlevel.removePlayer(this, true); //Forge: The player entity itself is moved, and not cloned. So we need to keep the data alive with no matching invalidate call later.
         this.revive();
         this.moveTo(pX, pY, pZ, pYaw, pPitch);
         this.setLevel(pNewLevel);
         pNewLevel.addDuringCommandTeleport(this);
         this.triggerDimensionChangeTriggers(serverlevel);
         this.connection.teleport(pX, pY, pZ, pYaw, pPitch);
         this.gameMode.setLevel(pNewLevel);
         this.server.getPlayerList().sendLevelInfo(this, pNewLevel);
         this.server.getPlayerList().sendAllPlayerInfo(this);
         net.minecraftforge.event.ForgeEventFactory.firePlayerChangedDimensionEvent(this, serverlevel.dimension(), pNewLevel.dimension());
      }

   }

   @Nullable
   public BlockPos getRespawnPosition() {
      return this.respawnPosition;
   }

   public float getRespawnAngle() {
      return this.respawnAngle;
   }

   public ResourceKey<Level> getRespawnDimension() {
      return this.respawnDimension;
   }

   public boolean isRespawnForced() {
      return this.respawnForced;
   }

   public void setRespawnPosition(ResourceKey<Level> p_9159_, @Nullable BlockPos p_9160_, float p_9161_, boolean p_9162_, boolean p_9163_) {
      if (net.minecraftforge.event.ForgeEventFactory.onPlayerSpawnSet(this, p_9160_ == null ? Level.OVERWORLD : p_9159_, p_9160_, p_9162_)) return;
      if (p_9160_ != null) {
         boolean flag = p_9160_.equals(this.respawnPosition) && p_9159_.equals(this.respawnDimension);
         if (p_9163_ && !flag) {
            this.sendMessage(new TranslatableComponent("block.minecraft.set_spawn"), Util.NIL_UUID);
         }

         this.respawnPosition = p_9160_;
         this.respawnDimension = p_9159_;
         this.respawnAngle = p_9161_;
         this.respawnForced = p_9162_;
      } else {
         this.respawnPosition = null;
         this.respawnDimension = Level.OVERWORLD;
         this.respawnAngle = 0.0F;
         this.respawnForced = false;
      }

   }

   public void trackChunk(ChunkPos p_184136_, Packet<?> p_184137_) {
      this.connection.send(p_184137_);
   }

   public void untrackChunk(ChunkPos p_9089_) {
      if (this.isAlive()) {
         this.connection.send(new ClientboundForgetLevelChunkPacket(p_9089_.x, p_9089_.z));
      }

   }

   public SectionPos getLastSectionPos() {
      return this.lastSectionPos;
   }

   public void setLastSectionPos(SectionPos pSectionPos) {
      this.lastSectionPos = pSectionPos;
   }

   public void playNotifySound(SoundEvent p_9019_, SoundSource p_9020_, float p_9021_, float p_9022_) {
      this.connection.send(new ClientboundSoundPacket(p_9019_, p_9020_, this.getX(), this.getY(), this.getZ(), p_9021_, p_9022_));
   }

   public Packet<?> getAddEntityPacket() {
      return new ClientboundAddPlayerPacket(this);
   }

   /**
    * Creates and drops the provided item. Depending on the dropAround, it will drop teh item around the player, instead
    * of dropping the item from where the player is pointing at. Likewise, if traceItem is true, the dropped item entity
    * will have the thrower set as the player.
    */
   public ItemEntity drop(ItemStack pDroppedItem, boolean pDropAround, boolean pTraceItem) {
      ItemEntity itementity = super.drop(pDroppedItem, pDropAround, pTraceItem);
      if (itementity == null) {
         return null;
      } else {
         if (captureDrops() != null) captureDrops().add(itementity);
         else
         this.level.addFreshEntity(itementity);
         ItemStack itemstack = itementity.getItem();
         if (pTraceItem) {
            if (!itemstack.isEmpty()) {
               this.awardStat(Stats.ITEM_DROPPED.get(itemstack.getItem()), pDroppedItem.getCount());
            }

            this.awardStat(Stats.DROP);
         }

         return itementity;
      }
   }

   private String language = "en_us";
   /**
    * Returns the language last reported by the player as their local language.
    * Defaults to en_us if the value is unknown.
    */
   public String getLanguage() {
      return this.language;
   }

   // We need this as tablistDisplayname may be null even if the the event was fired.
   private boolean hasTabListName = false;
   private Component tabListDisplayName = null;
   /**
    * Force the name displayed in the tab list to refresh, by firing {@link net.minecraftforge.event.entity.player.PlayerEvent.TabListNameFormat}.
    */
   public void refreshTabListName() {
      Component oldName = this.tabListDisplayName;
      this.tabListDisplayName = net.minecraftforge.event.ForgeEventFactory.getPlayerTabListDisplayName(this);
      if (!java.util.Objects.equals(oldName, this.tabListDisplayName)) {
         this.getServer().getPlayerList().broadcastAll(new net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket(net.minecraft.network.protocol.game.ClientboundPlayerInfoPacket.Action.UPDATE_DISPLAY_NAME, this));
      }
   }

   public TextFilter getTextFilter() {
      return this.textFilter;
   }

   public void setLevel(ServerLevel p_143426_) {
      this.level = p_143426_;
      this.gameMode.setLevel(p_143426_);
   }

   @Nullable
   private static GameType readPlayerMode(@Nullable CompoundTag p_143414_, String p_143415_) {
      return p_143414_ != null && p_143414_.contains(p_143415_, 99) ? GameType.byId(p_143414_.getInt(p_143415_)) : null;
   }

   private GameType calculateGameModeForNewPlayer(@Nullable GameType p_143424_) {
      GameType gametype = this.server.getForcedGameType();
      if (gametype != null) {
         return gametype;
      } else {
         return p_143424_ != null ? p_143424_ : this.server.getDefaultGameType();
      }
   }

   public void loadGameTypes(@Nullable CompoundTag p_143428_) {
      this.gameMode.setGameModeForPlayer(this.calculateGameModeForNewPlayer(readPlayerMode(p_143428_, "playerGameType")), readPlayerMode(p_143428_, "previousPlayerGameType"));
   }

   private void storeGameTypes(CompoundTag p_143431_) {
      p_143431_.putInt("playerGameType", this.gameMode.getGameModeForPlayer().getId());
      GameType gametype = this.gameMode.getPreviousGameModeForPlayer();
      if (gametype != null) {
         p_143431_.putInt("previousPlayerGameType", gametype.getId());
      }

   }

   public boolean isTextFilteringEnabled() {
      return this.textFilteringEnabled;
   }

   public boolean shouldFilterMessageTo(ServerPlayer p_143422_) {
      if (p_143422_ == this) {
         return false;
      } else {
         return this.textFilteringEnabled || p_143422_.textFilteringEnabled;
      }
   }

   public boolean mayInteract(Level pLevel, BlockPos pPos) {
      return super.mayInteract(pLevel, pPos) && pLevel.mayInteract(this, pPos);
   }

   protected void updateUsingItem(ItemStack p_143402_) {
      CriteriaTriggers.USING_ITEM.trigger(this, p_143402_);
      super.updateUsingItem(p_143402_);
   }

   public boolean drop(boolean p_182295_) {
      Inventory inventory = this.getInventory();
      ItemStack selected = inventory.getSelected();
      if (selected.isEmpty() || !selected.onDroppedByPlayer(this)) return false;
      ItemStack itemstack = inventory.removeFromSelected(p_182295_);
      this.containerMenu.findSlot(inventory, inventory.selected).ifPresent((p_182293_) -> {
         this.containerMenu.setRemoteSlot(p_182293_, inventory.getSelected());
      });
      return net.minecraftforge.common.ForgeHooks.onPlayerTossEvent(this, itemstack, true) != null;
   }

   public boolean allowsListing() {
      return this.allowsListing;
   }
}
