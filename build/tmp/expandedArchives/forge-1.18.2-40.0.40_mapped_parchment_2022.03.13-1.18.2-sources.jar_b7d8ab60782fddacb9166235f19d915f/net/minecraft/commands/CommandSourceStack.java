package net.minecraft.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.ResultConsumer;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BinaryOperator;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.Util;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class CommandSourceStack implements SharedSuggestionProvider, net.minecraftforge.common.extensions.IForgeCommandSourceStack {
   public static final SimpleCommandExceptionType ERROR_NOT_PLAYER = new SimpleCommandExceptionType(new TranslatableComponent("permissions.requires.player"));
   public static final SimpleCommandExceptionType ERROR_NOT_ENTITY = new SimpleCommandExceptionType(new TranslatableComponent("permissions.requires.entity"));
   private final CommandSource source;
   private final Vec3 worldPosition;
   private final ServerLevel level;
   private final int permissionLevel;
   private final String textName;
   private final Component displayName;
   private final MinecraftServer server;
   private final boolean silent;
   @Nullable
   private final Entity entity;
   @Nullable
   private final ResultConsumer<CommandSourceStack> consumer;
   private final EntityAnchorArgument.Anchor anchor;
   private final Vec2 rotation;

   public CommandSourceStack(CommandSource pSource, Vec3 pWorldPosition, Vec2 pRotation, ServerLevel pLevel, int pPermissionLevel, String pTextName, Component pDisplayName, MinecraftServer pServer, @Nullable Entity pEntity) {
      this(pSource, pWorldPosition, pRotation, pLevel, pPermissionLevel, pTextName, pDisplayName, pServer, pEntity, false, (p_81361_, p_81362_, p_81363_) -> {
      }, EntityAnchorArgument.Anchor.FEET);
   }

   protected CommandSourceStack(CommandSource pSource, Vec3 pWorldPosition, Vec2 pRotation, ServerLevel pLevel, int pPermissionLevel, String pTextName, Component pDisplayName, MinecraftServer pServer, @Nullable Entity pEntity, boolean pSilent, @Nullable ResultConsumer<CommandSourceStack> pConsumer, EntityAnchorArgument.Anchor pAnchor) {
      this.source = pSource;
      this.worldPosition = pWorldPosition;
      this.level = pLevel;
      this.silent = pSilent;
      this.entity = pEntity;
      this.permissionLevel = pPermissionLevel;
      this.textName = pTextName;
      this.displayName = pDisplayName;
      this.server = pServer;
      this.consumer = pConsumer;
      this.anchor = pAnchor;
      this.rotation = pRotation;
   }

   public CommandSourceStack withSource(CommandSource pSource) {
      return this.source == pSource ? this : new CommandSourceStack(pSource, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
   }

   public CommandSourceStack withEntity(Entity pEntity) {
      return this.entity == pEntity ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, pEntity.getName().getString(), pEntity.getDisplayName(), this.server, pEntity, this.silent, this.consumer, this.anchor);
   }

   public CommandSourceStack withPosition(Vec3 pPos) {
      return this.worldPosition.equals(pPos) ? this : new CommandSourceStack(this.source, pPos, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
   }

   public CommandSourceStack withRotation(Vec2 pRotation) {
      return this.rotation.equals(pRotation) ? this : new CommandSourceStack(this.source, this.worldPosition, pRotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
   }

   public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> pConsumer) {
      return Objects.equals(this.consumer, pConsumer) ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, pConsumer, this.anchor);
   }

   public CommandSourceStack withCallback(ResultConsumer<CommandSourceStack> pResultConsumer, BinaryOperator<ResultConsumer<CommandSourceStack>> pResultConsumerSelector) {
      ResultConsumer<CommandSourceStack> resultconsumer = pResultConsumerSelector.apply(this.consumer, pResultConsumer);
      return this.withCallback(resultconsumer);
   }

   public CommandSourceStack withSuppressedOutput() {
      return !this.silent && !this.source.alwaysAccepts() ? new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, true, this.consumer, this.anchor) : this;
   }

   public CommandSourceStack withPermission(int pPermissionLevel) {
      return pPermissionLevel == this.permissionLevel ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, pPermissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
   }

   public CommandSourceStack withMaximumPermission(int pPermissionLevel) {
      return pPermissionLevel <= this.permissionLevel ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, pPermissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
   }

   public CommandSourceStack withAnchor(EntityAnchorArgument.Anchor pAnchor) {
      return pAnchor == this.anchor ? this : new CommandSourceStack(this.source, this.worldPosition, this.rotation, this.level, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, pAnchor);
   }

   public CommandSourceStack withLevel(ServerLevel pLevel) {
      if (pLevel == this.level) {
         return this;
      } else {
         double d0 = DimensionType.getTeleportationScale(this.level.dimensionType(), pLevel.dimensionType());
         Vec3 vec3 = new Vec3(this.worldPosition.x * d0, this.worldPosition.y, this.worldPosition.z * d0);
         return new CommandSourceStack(this.source, vec3, this.rotation, pLevel, this.permissionLevel, this.textName, this.displayName, this.server, this.entity, this.silent, this.consumer, this.anchor);
      }
   }

   public CommandSourceStack facing(Entity pEntity, EntityAnchorArgument.Anchor pAnchor) {
      return this.facing(pAnchor.apply(pEntity));
   }

   public CommandSourceStack facing(Vec3 pLookPos) {
      Vec3 vec3 = this.anchor.apply(this);
      double d0 = pLookPos.x - vec3.x;
      double d1 = pLookPos.y - vec3.y;
      double d2 = pLookPos.z - vec3.z;
      double d3 = Math.sqrt(d0 * d0 + d2 * d2);
      float f = Mth.wrapDegrees((float)(-(Mth.atan2(d1, d3) * (double)(180F / (float)Math.PI))));
      float f1 = Mth.wrapDegrees((float)(Mth.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F);
      return this.withRotation(new Vec2(f, f1));
   }

   public Component getDisplayName() {
      return this.displayName;
   }

   public String getTextName() {
      return this.textName;
   }

   public boolean hasPermission(int pLevel) {
      return this.permissionLevel >= pLevel;
   }

   public Vec3 getPosition() {
      return this.worldPosition;
   }

   public ServerLevel getLevel() {
      return this.level;
   }

   @Nullable
   public Entity getEntity() {
      return this.entity;
   }

   public Entity getEntityOrException() throws CommandSyntaxException {
      if (this.entity == null) {
         throw ERROR_NOT_ENTITY.create();
      } else {
         return this.entity;
      }
   }

   public ServerPlayer getPlayerOrException() throws CommandSyntaxException {
      if (!(this.entity instanceof ServerPlayer)) {
         throw ERROR_NOT_PLAYER.create();
      } else {
         return (ServerPlayer)this.entity;
      }
   }

   public Vec2 getRotation() {
      return this.rotation;
   }

   public MinecraftServer getServer() {
      return this.server;
   }

   public EntityAnchorArgument.Anchor getAnchor() {
      return this.anchor;
   }

   public void sendSuccess(Component pMessage, boolean pAllowLogging) {
      if (this.source.acceptsSuccess() && !this.silent) {
         this.source.sendMessage(pMessage, Util.NIL_UUID);
      }

      if (pAllowLogging && this.source.shouldInformAdmins() && !this.silent) {
         this.broadcastToAdmins(pMessage);
      }

   }

   private void broadcastToAdmins(Component pMessage) {
      Component component = (new TranslatableComponent("chat.type.admin", this.getDisplayName(), pMessage)).withStyle(new ChatFormatting[]{ChatFormatting.GRAY, ChatFormatting.ITALIC});
      if (this.server.getGameRules().getBoolean(GameRules.RULE_SENDCOMMANDFEEDBACK)) {
         for(ServerPlayer serverplayer : this.server.getPlayerList().getPlayers()) {
            if (serverplayer != this.source && this.server.getPlayerList().isOp(serverplayer.getGameProfile())) {
               serverplayer.sendMessage(component, Util.NIL_UUID);
            }
         }
      }

      if (this.source != this.server && this.server.getGameRules().getBoolean(GameRules.RULE_LOGADMINCOMMANDS)) {
         this.server.sendMessage(component, Util.NIL_UUID);
      }

   }

   public void sendFailure(Component pMessage) {
      if (this.source.acceptsFailure() && !this.silent) {
         this.source.sendMessage((new TextComponent("")).append(pMessage).withStyle(ChatFormatting.RED), Util.NIL_UUID);
      }

   }

   public void onCommandComplete(CommandContext<CommandSourceStack> pContext, boolean pSuccess, int pResult) {
      if (this.consumer != null) {
         this.consumer.onCommandComplete(pContext, pSuccess, pResult);
      }

   }

   public Collection<String> getOnlinePlayerNames() {
      return Lists.newArrayList(this.server.getPlayerNames());
   }

   public Collection<String> getAllTeams() {
      return this.server.getScoreboard().getTeamNames();
   }

   public Collection<ResourceLocation> getAvailableSoundEvents() {
      return Registry.SOUND_EVENT.keySet();
   }

   public Stream<ResourceLocation> getRecipeNames() {
      return this.server.getRecipeManager().getRecipeIds();
   }

   public CompletableFuture<Suggestions> customSuggestion(CommandContext<?> pContext) {
      return Suggestions.empty();
   }

   public CompletableFuture<Suggestions> suggestRegistryElements(ResourceKey<? extends Registry<?>> p_212330_, SharedSuggestionProvider.ElementSuggestionType p_212331_, SuggestionsBuilder p_212332_, CommandContext<?> p_212333_) {
      return this.registryAccess().registry(p_212330_).map((p_212328_) -> {
         this.suggestRegistryElements(p_212328_, p_212331_, p_212332_);
         return p_212332_.buildFuture();
      }).orElseGet(Suggestions::empty);
   }

   public Set<ResourceKey<Level>> levels() {
      return this.server.levelKeys();
   }

   public RegistryAccess registryAccess() {
      return this.server.registryAccess();
   }
}
