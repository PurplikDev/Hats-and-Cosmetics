package net.minecraft.network.chat;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.DataFixUtils;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.advancements.critereon.NbtPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.NbtPathArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.commands.arguments.coordinates.Coordinates;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.commands.arguments.selector.EntitySelectorParser;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class NbtComponent extends BaseComponent implements ContextAwareComponent {
   private static final Logger LOGGER = LogManager.getLogger();
   protected final boolean interpreting;
   protected final Optional<Component> separator;
   protected final String nbtPathPattern;
   @Nullable
   protected final NbtPathArgument.NbtPath compiledNbtPath;

   @Nullable
   private static NbtPathArgument.NbtPath compileNbtPath(String p_130978_) {
      try {
         return (new NbtPathArgument()).parse(new StringReader(p_130978_));
      } catch (CommandSyntaxException commandsyntaxexception) {
         return null;
      }
   }

   public NbtComponent(String pNbtPathPattern, boolean pInterpreting, Optional<Component> pSeparator) {
      this(pNbtPathPattern, compileNbtPath(pNbtPathPattern), pInterpreting, pSeparator);
   }

   protected NbtComponent(String pNbtPathPattern, @Nullable NbtPathArgument.NbtPath pCompiledNbtPath, boolean pInterpreting, Optional<Component> pSeparator) {
      this.nbtPathPattern = pNbtPathPattern;
      this.compiledNbtPath = pCompiledNbtPath;
      this.interpreting = pInterpreting;
      this.separator = pSeparator;
   }

   protected abstract Stream<CompoundTag> getData(CommandSourceStack pCommandSourceStack) throws CommandSyntaxException;

   public String getNbtPath() {
      return this.nbtPathPattern;
   }

   public boolean isInterpreting() {
      return this.interpreting;
   }

   public MutableComponent resolve(@Nullable CommandSourceStack pCommandSourceStack, @Nullable Entity pEntity, int pRecursionDepth) throws CommandSyntaxException {
      if (pCommandSourceStack != null && this.compiledNbtPath != null) {
         Stream<String> stream = this.getData(pCommandSourceStack).flatMap((p_130973_) -> {
            try {
               return this.compiledNbtPath.get(p_130973_).stream();
            } catch (CommandSyntaxException commandsyntaxexception) {
               return Stream.empty();
            }
         }).map(Tag::getAsString);
         if (this.interpreting) {
            Component component = DataFixUtils.orElse(ComponentUtils.updateForEntity(pCommandSourceStack, this.separator, pEntity, pRecursionDepth), ComponentUtils.DEFAULT_NO_STYLE_SEPARATOR);
            return stream.flatMap((p_130971_) -> {
               try {
                  MutableComponent mutablecomponent = Component.Serializer.fromJson(p_130971_);
                  return Stream.of(ComponentUtils.updateForEntity(pCommandSourceStack, mutablecomponent, pEntity, pRecursionDepth));
               } catch (Exception exception) {
                  LOGGER.warn("Failed to parse component: {}", p_130971_, exception);
                  return Stream.of();
               }
            }).reduce((p_178464_, p_178465_) -> {
               return p_178464_.append(component).append(p_178465_);
            }).orElseGet(() -> {
               return new TextComponent("");
            });
         } else {
            return ComponentUtils.updateForEntity(pCommandSourceStack, this.separator, pEntity, pRecursionDepth).map((p_178461_) -> {
               return stream.map((p_178471_) -> {
                  return (net.minecraft.network.chat.MutableComponent)new TextComponent(p_178471_);
               }).reduce((p_178468_, p_178469_) -> {
                  return p_178468_.append(p_178461_).append(p_178469_);
               }).orElseGet(() -> {
                  return new TextComponent("");
               });
            }).orElseGet(() -> {
               return new TextComponent(stream.collect(Collectors.joining(", ")));
            });
         }
      } else {
         return new TextComponent("");
      }
   }

   public static class BlockNbtComponent extends NbtComponent {
      private final String posPattern;
      @Nullable
      private final Coordinates compiledPos;

      public BlockNbtComponent(String pNbtPathPattern, boolean pInterpreting, String pPosPattern, Optional<Component> pSeparator) {
         super(pNbtPathPattern, pInterpreting, pSeparator);
         this.posPattern = pPosPattern;
         this.compiledPos = this.compilePos(this.posPattern);
      }

      @Nullable
      private Coordinates compilePos(String pPosPattern) {
         try {
            return BlockPosArgument.blockPos().parse(new StringReader(pPosPattern));
         } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
         }
      }

      private BlockNbtComponent(String pNbtPathPattern, @Nullable NbtPathArgument.NbtPath pCompiledNbtPath, boolean pInterpreting, String pPosPattern, @Nullable Coordinates pCompiledPos, Optional<Component> pSeparator) {
         super(pNbtPathPattern, pCompiledNbtPath, pInterpreting, pSeparator);
         this.posPattern = pPosPattern;
         this.compiledPos = pCompiledPos;
      }

      @Nullable
      public String getPos() {
         return this.posPattern;
      }

      /**
       * Creates a copy of this component, losing any style or siblings.
       */
      public NbtComponent.BlockNbtComponent plainCopy() {
         return new NbtComponent.BlockNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.posPattern, this.compiledPos, this.separator);
      }

      protected Stream<CompoundTag> getData(CommandSourceStack pCommandSourceStack) {
         if (this.compiledPos != null) {
            ServerLevel serverlevel = pCommandSourceStack.getLevel();
            BlockPos blockpos = this.compiledPos.getBlockPos(pCommandSourceStack);
            if (serverlevel.isLoaded(blockpos)) {
               BlockEntity blockentity = serverlevel.getBlockEntity(blockpos);
               if (blockentity != null) {
                  return Stream.of(blockentity.saveWithFullMetadata());
               }
            }
         }

         return Stream.empty();
      }

      public boolean equals(Object p_130999_) {
         if (this == p_130999_) {
            return true;
         } else if (!(p_130999_ instanceof NbtComponent.BlockNbtComponent)) {
            return false;
         } else {
            NbtComponent.BlockNbtComponent nbtcomponent$blocknbtcomponent = (NbtComponent.BlockNbtComponent)p_130999_;
            return Objects.equals(this.posPattern, nbtcomponent$blocknbtcomponent.posPattern) && Objects.equals(this.nbtPathPattern, nbtcomponent$blocknbtcomponent.nbtPathPattern) && super.equals(p_130999_);
         }
      }

      public String toString() {
         return "BlockPosArgument{pos='" + this.posPattern + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
      }
   }

   public static class EntityNbtComponent extends NbtComponent {
      private final String selectorPattern;
      @Nullable
      private final EntitySelector compiledSelector;

      public EntityNbtComponent(String pNbtPathPattern, boolean pInterpreting, String pSelectorPattern, Optional<Component> pSeparator) {
         super(pNbtPathPattern, pInterpreting, pSeparator);
         this.selectorPattern = pSelectorPattern;
         this.compiledSelector = compileSelector(pSelectorPattern);
      }

      @Nullable
      private static EntitySelector compileSelector(String pSelectorPattern) {
         try {
            EntitySelectorParser entityselectorparser = new EntitySelectorParser(new StringReader(pSelectorPattern));
            return entityselectorparser.parse();
         } catch (CommandSyntaxException commandsyntaxexception) {
            return null;
         }
      }

      private EntityNbtComponent(String pNbtPathPattern, @Nullable NbtPathArgument.NbtPath pCompiledNbtPath, boolean pInterpreting, String pSelectorPattern, @Nullable EntitySelector pCompiledSelector, Optional<Component> pSeparator) {
         super(pNbtPathPattern, pCompiledNbtPath, pInterpreting, pSeparator);
         this.selectorPattern = pSelectorPattern;
         this.compiledSelector = pCompiledSelector;
      }

      public String getSelector() {
         return this.selectorPattern;
      }

      /**
       * Creates a copy of this component, losing any style or siblings.
       */
      public NbtComponent.EntityNbtComponent plainCopy() {
         return new NbtComponent.EntityNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.selectorPattern, this.compiledSelector, this.separator);
      }

      protected Stream<CompoundTag> getData(CommandSourceStack pCommandSourceStack) throws CommandSyntaxException {
         if (this.compiledSelector != null) {
            List<? extends Entity> list = this.compiledSelector.findEntities(pCommandSourceStack);
            return list.stream().map(NbtPredicate::getEntityTagToCompare);
         } else {
            return Stream.empty();
         }
      }

      public boolean equals(Object p_131022_) {
         if (this == p_131022_) {
            return true;
         } else if (!(p_131022_ instanceof NbtComponent.EntityNbtComponent)) {
            return false;
         } else {
            NbtComponent.EntityNbtComponent nbtcomponent$entitynbtcomponent = (NbtComponent.EntityNbtComponent)p_131022_;
            return Objects.equals(this.selectorPattern, nbtcomponent$entitynbtcomponent.selectorPattern) && Objects.equals(this.nbtPathPattern, nbtcomponent$entitynbtcomponent.nbtPathPattern) && super.equals(p_131022_);
         }
      }

      public String toString() {
         return "EntityNbtComponent{selector='" + this.selectorPattern + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
      }
   }

   public static class StorageNbtComponent extends NbtComponent {
      private final ResourceLocation id;

      public StorageNbtComponent(String pNbtPathPattern, boolean pInterpreting, ResourceLocation pId, Optional<Component> pSeparator) {
         super(pNbtPathPattern, pInterpreting, pSeparator);
         this.id = pId;
      }

      public StorageNbtComponent(String pNbtPathPattern, @Nullable NbtPathArgument.NbtPath pCompiledNbtPath, boolean pInterpreting, ResourceLocation pId, Optional<Component> pSeparator) {
         super(pNbtPathPattern, pCompiledNbtPath, pInterpreting, pSeparator);
         this.id = pId;
      }

      public ResourceLocation getId() {
         return this.id;
      }

      /**
       * Creates a copy of this component, losing any style or siblings.
       */
      public NbtComponent.StorageNbtComponent plainCopy() {
         return new NbtComponent.StorageNbtComponent(this.nbtPathPattern, this.compiledNbtPath, this.interpreting, this.id, this.separator);
      }

      protected Stream<CompoundTag> getData(CommandSourceStack pCommandSourceStack) {
         CompoundTag compoundtag = pCommandSourceStack.getServer().getCommandStorage().get(this.id);
         return Stream.of(compoundtag);
      }

      public boolean equals(Object p_131041_) {
         if (this == p_131041_) {
            return true;
         } else if (!(p_131041_ instanceof NbtComponent.StorageNbtComponent)) {
            return false;
         } else {
            NbtComponent.StorageNbtComponent nbtcomponent$storagenbtcomponent = (NbtComponent.StorageNbtComponent)p_131041_;
            return Objects.equals(this.id, nbtcomponent$storagenbtcomponent.id) && Objects.equals(this.nbtPathPattern, nbtcomponent$storagenbtcomponent.nbtPathPattern) && super.equals(p_131041_);
         }
      }

      public String toString() {
         return "StorageNbtComponent{id='" + this.id + "'path='" + this.nbtPathPattern + "', siblings=" + this.siblings + ", style=" + this.getStyle() + "}";
      }
   }
}