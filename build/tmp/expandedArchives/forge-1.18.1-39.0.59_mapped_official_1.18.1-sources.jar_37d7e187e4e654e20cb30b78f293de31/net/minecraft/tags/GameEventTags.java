package net.minecraft.tags;

import net.minecraft.core.Registry;
import net.minecraft.world.level.gameevent.GameEvent;

public class GameEventTags {
   protected static final StaticTagHelper<GameEvent> HELPER = StaticTags.create(Registry.GAME_EVENT_REGISTRY, "tags/game_events");
   public static final Tag.Named<GameEvent> VIBRATIONS = bind("vibrations");
   public static final Tag.Named<GameEvent> IGNORE_VIBRATIONS_SNEAKING = bind("ignore_vibrations_sneaking");

   public static Tag.Named<GameEvent> bind(String p_144308_) {
      return HELPER.bind(p_144308_);
   }

   public static net.minecraftforge.common.Tags.IOptionalNamedTag<GameEvent> createOptional(net.minecraft.resources.ResourceLocation name) {
      return createOptional(name, null);
   }

   public static net.minecraftforge.common.Tags.IOptionalNamedTag<GameEvent> createOptional(net.minecraft.resources.ResourceLocation name, @javax.annotation.Nullable java.util.Set<java.util.function.Supplier<GameEvent>> defaults) {
      return HELPER.createOptional(name, defaults);
   }

   public static TagCollection<GameEvent> getAllTags() {
      return HELPER.getAllTags();
   }
}
