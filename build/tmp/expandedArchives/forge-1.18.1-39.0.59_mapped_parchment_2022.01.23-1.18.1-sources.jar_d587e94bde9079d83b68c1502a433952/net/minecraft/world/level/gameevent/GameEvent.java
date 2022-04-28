package net.minecraft.world.level.gameevent;

import net.minecraft.core.Registry;

/**
 * Describes an in game event or action that can be detected by listeners such as the Sculk Sensor block.
 */
public class GameEvent {
   /**
    * This event is broadcast when a block is attached to another. For example when the tripwire is attached to a
    * tripwire hook.
    */
   public static final GameEvent BLOCK_ATTACH = register("block_attach");
   /** This event is broadcast when a block is changed. For example when a flower is removed from a flower pot. */
   public static final GameEvent BLOCK_CHANGE = register("block_change");
   /** This event is broadcast when a block such as a door, drap door, or gate is closed. */
   public static final GameEvent BLOCK_CLOSE = register("block_close");
   /** This event is broadcast when a block is destroyed or picked up by an enderman. */
   public static final GameEvent BLOCK_DESTROY = register("block_destroy");
   /**
    * This event is broadcast when a block is detached from another block. For example when the tripwire is removed from
    * the hook.
    */
   public static final GameEvent BLOCK_DETACH = register("block_detach");
   /** This event is broadcast when a block such as a door, trap door, or gate has been opened. */
   public static final GameEvent BLOCK_OPEN = register("block_open");
   /** This event is broadcast when a block is placed in the world. */
   public static final GameEvent BLOCK_PLACE = register("block_place");
   /** This event is broadcast when a block such as a button or pressure plate has been activated. */
   public static final GameEvent BLOCK_PRESS = register("block_press");
   /** This event is broadcast when a block such as a lever is switched on. */
   public static final GameEvent BLOCK_SWITCH = register("block_switch");
   /** This event is broadcast when a block such as a button or pressure plate returns to its unactivated state. */
   public static final GameEvent BLOCK_UNPRESS = register("block_unpress");
   /** This event is broadcast when a block such as a lever is turned off. */
   public static final GameEvent BLOCK_UNSWITCH = register("block_unswitch");
   /**
    * This event is broadcast when a block with a storage inventory such as a chest or barrel is closed. Some entities
    * like a minecart with chest may also cause this event to be broadcast.
    */
   public static final GameEvent CONTAINER_CLOSE = register("container_close");
   /**
    * This event is broadcast when a block with a storage inventory such as a chest or barrel is opened. Some entities
    * like a minecart with chest may also cause this event to be broadcast.
    */
   public static final GameEvent CONTAINER_OPEN = register("container_open");
   /** This event is broadcast when a dispenser fails to dispense an item. */
   public static final GameEvent DISPENSE_FAIL = register("dispense_fail");
   /** This event is broadcast when a drinkable item such as a potion has been fully consumed. */
   public static final GameEvent DRINKING_FINISH = register("drinking_finish");
   /**
    * This event is broadcast when an entity consumes food. This includes animals eating grass and other sources of
    * food.
    */
   public static final GameEvent EAT = register("eat");
   /**
    * This event is broadcast while an entity wearing an elytra is flying. This is generally broadcasted once every ten
    * ticks.
    */
   public static final GameEvent ELYTRA_FREE_FALL = register("elytra_free_fall");
   /** This event is broadcast when an entity is damaged. */
   public static final GameEvent ENTITY_DAMAGED = register("entity_damaged");
   /** This event is broadcast when an entity is killed. */
   public static final GameEvent ENTITY_KILLED = register("entity_killed");
   /**
    * This event is broadcast when an entity is artificially placed in the world using an item. For example when a spawn
    * egg is used.
    */
   public static final GameEvent ENTITY_PLACE = register("entity_place");
   /** This event is broadcast when an item is equipped to an entity or armor stand. */
   public static final GameEvent EQUIP = register("equip");
   /** This event is broadcast when an entity such as a creeper, tnt, or a firework explodes. */
   public static final GameEvent EXPLODE = register("explode");
   /** This event is broadcast when a fishing rod is casted out. */
   public static final GameEvent FISHING_ROD_CAST = register("fishing_rod_cast");
   /** This event is broadcast when a fishing rod is reeled in. */
   public static final GameEvent FISHING_ROD_REEL_IN = register("fishing_rod_reel_in");
   /** This event is broadcast when a flying entity such as the ender dragon flaps its wings. */
   public static final GameEvent FLAP = register("flap");
   /**
    * This event is broadcast when a fluid is picked up. This includes using a bucket, harvesting honey, filling a
    * bottle, and removing fluid from a cauldron.
    */
   public static final GameEvent FLUID_PICKUP = register("fluid_pickup");
   /**
    * This event is broadcast when fluid is placed. This includes adding fluid to a cauldron and placing a bucket of
    * fluid.
    */
   public static final GameEvent FLUID_PLACE = register("fluid_place");
   /** This event is broadcast when an entity falls far enough to take fall damage. */
   public static final GameEvent HIT_GROUND = register("hit_ground");
   /**
    * This event is broadcast when a player interacts with a mob. For example when the player heals a tamed wolf or
    * feeds an animal.
    */
   public static final GameEvent MOB_INTERACT = register("mob_interact");
   /** This event is broadcast when lightning strikes a block. */
   public static final GameEvent LIGHTNING_STRIKE = register("lightning_strike");
   public static final GameEvent MINECART_MOVING = register("minecart_moving");
   /** This event is broadcast when a piston head is retracted. */
   public static final GameEvent PISTON_CONTRACT = register("piston_contract");
   /** This event is broadcast when a piston head is extended. */
   public static final GameEvent PISTON_EXTEND = register("piston_extend");
   /** This event is broadcast when an entity such as a creeper or TNT begins exploding. */
   public static final GameEvent PRIME_FUSE = register("prime_fuse");
   /** This event is broadcast when a projectile hits something. */
   public static final GameEvent PROJECTILE_LAND = register("projectile_land");
   /** This event is broadcast when a projectile is fired. */
   public static final GameEvent PROJECTILE_SHOOT = register("projectile_shoot");
   /** This event is broadcast when the ravager uses its roar ability. */
   public static final GameEvent RAVAGER_ROAR = register("ravager_roar");
   /** This event is broadcast when the bell block is rung. */
   public static final GameEvent RING_BELL = register("ring_bell");
   /**
    * This event is broadcast when a shear is used. This includes disarming tripwires, harvesting honeycombs, carving
    * pumpkins, etc.
    */
   public static final GameEvent SHEAR = register("shear");
   /** This event is broadcast when a shulker closes its shell. */
   public static final GameEvent SHULKER_CLOSE = register("shulker_close");
   /** This event is broadcast when a shulker opens its shell. */
   public static final GameEvent SHULKER_OPEN = register("shulker_open");
   /**
    * This event is broadcast wen an entity splashes in the water. This includes boats paddling or hitting bubble
    * columns.
    */
   public static final GameEvent SPLASH = register("splash");
   /** This event is broadcast when an entity moves on the ground. This includes entities such as minecarts. */
   public static final GameEvent STEP = register("step");
   /** This event is broadcast as an entity swims around in water. */
   public static final GameEvent SWIM = register("swim");
   /** This event is broadcast when a wolf shakes off water after getting wet. */
   public static final GameEvent WOLF_SHAKING = register("wolf_shaking");
   /**
    * The default notification radius for events to be broadcasted. @see
    * net.minecraft.world.level.gameevent.GameEvent#register
    */
   public static final int DEFAULT_NOTIFICATION_RADIUS = 16;
   /**
    * The name of the event. This is primarily used for debugging game events. @see
    * net.minecraft.client.renderer.debug.GameEventListenerRenderer#render
    */
   private final String name;
   /**
    * The radius around an event source to broadcast this event. Any listeners within this radius will be notified when
    * the event happens.
    */
   private final int notificationRadius;
   private final net.minecraftforge.common.util.ReverseTagWrapper<GameEvent> reverseTags = new net.minecraftforge.common.util.ReverseTagWrapper<>(this, net.minecraft.tags.GameEventTags::getAllTags);

   public GameEvent(String pName, int pNotificationRadius) {
      this.name = pName;
      this.notificationRadius = pNotificationRadius;
   }

   public java.util.Set<net.minecraft.resources.ResourceLocation> getTags() {
      return reverseTags.getTagNames();
   }

   /**
    * Gets the name of the event. This is primarily used for debugging game events.
    * @see net.minecraft.client.renderer.debug.GameEventListenerRenderer#render
    */
   public String getName() {
      return this.name;
   }

   /**
    * Gets the radius around an event source to broadcast the event. Any valid listeners within this radius will be
    * notified when the event happens.
    */
   public int getNotificationRadius() {
      return this.notificationRadius;
   }

   /**
    * Creates a new game event with the default notification radius and then registers it with the game registry.
    * @see net.minecraft.core.Registry#GAME_EVENT
    * @return The newly registered game event.
    * @param pName The name of the event. This will be used to generate the namespaced identifier for the event.
    */
   private static GameEvent register(String pName) {
      return register(pName, 16);
   }

   /**
    * Creates a new game event and then registers it with the game registry.
    * @see net.minecraft.core.Registry#GAME_EVENT
    * @return The newly registered game event.
    * @param pName The name of the event. This will be used to generate the namespaced identifier for the event.
    * @param pNotificationRadius The radius around an event source to broadcast the event. Any valid listeners within
    * this radius will be notified when the event happens.
    */
   private static GameEvent register(String pName, int pNotificationRadius) {
      return Registry.register(Registry.GAME_EVENT, pName, new GameEvent(pName, pNotificationRadius));
   }

   public String toString() {
      return "Game Event{ " + this.name + " , " + this.notificationRadius + "}";
   }
}
