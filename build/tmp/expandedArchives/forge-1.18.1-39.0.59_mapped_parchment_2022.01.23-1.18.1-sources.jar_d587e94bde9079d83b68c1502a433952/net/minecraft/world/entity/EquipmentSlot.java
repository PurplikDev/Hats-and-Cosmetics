package net.minecraft.world.entity;

public enum EquipmentSlot {
   MAINHAND(EquipmentSlot.Type.HAND, 0, 0, "mainhand"),
   OFFHAND(EquipmentSlot.Type.HAND, 1, 5, "offhand"),
   FEET(EquipmentSlot.Type.ARMOR, 0, 1, "feet"),
   LEGS(EquipmentSlot.Type.ARMOR, 1, 2, "legs"),
   CHEST(EquipmentSlot.Type.ARMOR, 2, 3, "chest"),
   HEAD(EquipmentSlot.Type.ARMOR, 3, 4, "head");

   private final EquipmentSlot.Type type;
   private final int index;
   private final int filterFlag;
   private final String name;

   private EquipmentSlot(EquipmentSlot.Type p_20739_, int p_20740_, int p_20741_, String p_20742_) {
      this.type = p_20739_;
      this.index = p_20740_;
      this.filterFlag = p_20741_;
      this.name = p_20742_;
   }

   public EquipmentSlot.Type getType() {
      return this.type;
   }

   public int getIndex() {
      return this.index;
   }

   public int getIndex(int p_147069_) {
      return p_147069_ + this.index;
   }

   /**
    * Gets the actual slot index.
    */
   public int getFilterFlag() {
      return this.filterFlag;
   }

   public String getName() {
      return this.name;
   }

   public static EquipmentSlot byName(String pTargetName) {
      for(EquipmentSlot equipmentslot : values()) {
         if (equipmentslot.getName().equals(pTargetName)) {
            return equipmentslot;
         }
      }

      throw new IllegalArgumentException("Invalid slot '" + pTargetName + "'");
   }

   /**
    * Returns the slot type based on the slot group and the index inside of that group.
    */
   public static EquipmentSlot byTypeAndIndex(EquipmentSlot.Type pSlotType, int pSlotIndex) {
      for(EquipmentSlot equipmentslot : values()) {
         if (equipmentslot.getType() == pSlotType && equipmentslot.getIndex() == pSlotIndex) {
            return equipmentslot;
         }
      }

      throw new IllegalArgumentException("Invalid slot '" + pSlotType + "': " + pSlotIndex);
   }

   public static enum Type {
      HAND,
      ARMOR;
   }
}