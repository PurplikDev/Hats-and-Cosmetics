package net.minecraft.world.level.pathfinder;

import net.minecraft.network.FriendlyByteBuf;

public class Target extends Node {
   private float bestHeuristic = Float.MAX_VALUE;
   /** The nearest path point of the path that is constructed */
   private Node bestNode;
   private boolean reached;

   public Target(Node p_77502_) {
      super(p_77502_.x, p_77502_.y, p_77502_.z);
   }

   public Target(int p_77498_, int p_77499_, int p_77500_) {
      super(p_77498_, p_77499_, p_77500_);
   }

   public void updateBest(float p_77504_, Node p_77505_) {
      if (p_77504_ < this.bestHeuristic) {
         this.bestHeuristic = p_77504_;
         this.bestNode = p_77505_;
      }

   }

   /**
    * Gets the nearest path point of the path that is constructed
    */
   public Node getBestNode() {
      return this.bestNode;
   }

   public void setReached() {
      this.reached = true;
   }

   public boolean isReached() {
      return this.reached;
   }

   public static Target createFromStream(FriendlyByteBuf p_77507_) {
      Target target = new Target(p_77507_.readInt(), p_77507_.readInt(), p_77507_.readInt());
      target.walkedDistance = p_77507_.readFloat();
      target.costMalus = p_77507_.readFloat();
      target.closed = p_77507_.readBoolean();
      target.type = BlockPathTypes.values()[p_77507_.readInt()];
      target.f = p_77507_.readFloat();
      return target;
   }
}