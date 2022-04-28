package net.minecraft.network.protocol.game;

import java.util.Random;
import java.util.UUID;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public class DebugEntityNameGenerator {
   private static final String[] NAMES_FIRST_PART = new String[]{"Slim", "Far", "River", "Silly", "Fat", "Thin", "Fish", "Bat", "Dark", "Oak", "Sly", "Bush", "Zen", "Bark", "Cry", "Slack", "Soup", "Grim", "Hook", "Dirt", "Mud", "Sad", "Hard", "Crook", "Sneak", "Stink", "Weird", "Fire", "Soot", "Soft", "Rough", "Cling", "Scar"};
   private static final String[] NAMES_SECOND_PART = new String[]{"Fox", "Tail", "Jaw", "Whisper", "Twig", "Root", "Finder", "Nose", "Brow", "Blade", "Fry", "Seek", "Wart", "Tooth", "Foot", "Leaf", "Stone", "Fall", "Face", "Tongue", "Voice", "Lip", "Mouth", "Snail", "Toe", "Ear", "Hair", "Beard", "Shirt", "Fist"};

   public static String getEntityName(Entity pEntity) {
      if (pEntity instanceof Player) {
         return pEntity.getName().getString();
      } else {
         Component component = pEntity.getCustomName();
         return component != null ? component.getString() : getEntityName(pEntity.getUUID());
      }
   }

   public static String getEntityName(UUID pUuid) {
      Random random = getRandom(pUuid);
      return getRandomString(random, NAMES_FIRST_PART) + getRandomString(random, NAMES_SECOND_PART);
   }

   private static String getRandomString(Random pRand, String[] pStrings) {
      return Util.getRandom(pStrings, pRand);
   }

   private static Random getRandom(UUID pUuid) {
      return new Random((long)(pUuid.hashCode() >> 2));
   }
}