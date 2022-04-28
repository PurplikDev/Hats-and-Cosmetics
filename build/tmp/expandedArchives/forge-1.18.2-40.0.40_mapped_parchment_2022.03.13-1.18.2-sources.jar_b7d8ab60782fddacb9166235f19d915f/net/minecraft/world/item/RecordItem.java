package net.minecraft.world.item;

import com.google.common.collect.Maps;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.JukeboxBlock;
import net.minecraft.world.level.block.state.BlockState;

public class RecordItem extends Item {
   @Deprecated // Forge: refer to WorldRender#playRecord. Modders: there's no need to reflectively modify this map!
   private static final Map<SoundEvent, RecordItem> BY_NAME = Maps.newHashMap();
   private final int analogOutput;
   @Deprecated // Forge: refer to soundSupplier
   private final SoundEvent sound;
   private final java.util.function.Supplier<SoundEvent> soundSupplier;

   @Deprecated // Forge: Use the constructor that takes a supplier instead
   public RecordItem(int pAnalogOutput, SoundEvent pSound, Item.Properties pProperties) {
      super(pProperties);
      this.analogOutput = pAnalogOutput;
      this.sound = pSound;
      BY_NAME.put(this.sound, this);
      this.soundSupplier = this.sound.delegate;
   }

   /**
    * For mod use, allows to create a music disc without having to create a new
    * SoundEvent before their registry event is fired.
    *
    * @param comparatorValue The value this music disc should output on the comparator. Must be between 0 and 15.
    * @param soundSupplier A supplier that provides the sound that should be played. Use a
    *                      {@link net.minecraftforge.registries.RegistryObject<SoundEvent>} or a
    *                      {@link net.minecraftforge.registries.IRegistryDelegate} for this parameter.
    * @param builder A set of {@link Item.Properties} that describe this item.
    */
   public RecordItem(int comparatorValue, java.util.function.Supplier<SoundEvent> soundSupplier, Item.Properties builder)
   {
      super(builder);
      this.analogOutput = comparatorValue;
      this.sound = null;
      this.soundSupplier = soundSupplier;
   }

   /**
    * Called when this item is used when targetting a Block
    */
   public InteractionResult useOn(UseOnContext pContext) {
      Level level = pContext.getLevel();
      BlockPos blockpos = pContext.getClickedPos();
      BlockState blockstate = level.getBlockState(blockpos);
      if (blockstate.is(Blocks.JUKEBOX) && !blockstate.getValue(JukeboxBlock.HAS_RECORD)) {
         ItemStack itemstack = pContext.getItemInHand();
         if (!level.isClientSide) {
            ((JukeboxBlock)Blocks.JUKEBOX).setRecord(level, blockpos, blockstate, itemstack);
            level.levelEvent((Player)null, 1010, blockpos, Item.getId(this));
            itemstack.shrink(1);
            Player player = pContext.getPlayer();
            if (player != null) {
               player.awardStat(Stats.PLAY_RECORD);
            }
         }

         return InteractionResult.sidedSuccess(level.isClientSide);
      } else {
         return InteractionResult.PASS;
      }
   }

   public int getAnalogOutput() {
      return this.analogOutput;
   }

   /**
    * allows items to add custom lines of information to the mouseover description
    */
   public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltip, TooltipFlag pFlag) {
      pTooltip.add(this.getDisplayName().withStyle(ChatFormatting.GRAY));
   }

   public MutableComponent getDisplayName() {
      return new TranslatableComponent(this.getDescriptionId() + ".desc");
   }

   @Nullable
   public static RecordItem getBySound(SoundEvent pSound) {
      return BY_NAME.get(pSound);
   }

   public SoundEvent getSound() {
      return this.soundSupplier.get();
   }
}
