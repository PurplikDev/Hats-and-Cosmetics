package net.minecraft.world.level.pathfinder;

import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class Node {
   public final int x;
   public final int y;
   public final int z;
   private final int hash;
   /** The index in the PathHeap. -1 if not assigned. */
   public int heapIdx = -1;
   /** The total cost of all path points up to this one. Corresponds to the A* g-score. */
   public float g;
   /** The estimated cost from this path point to the target. Corresponds to the A* h-score. */
   public float h;
   /**
    * The total cost of the path containing this path point. Used as sort criteria in PathHeap. Corresponds to the A* f-
    * score.
    */
   public float f;
   @Nullable
   public Node cameFrom;
   public boolean closed;
   public float walkedDistance;
   /** The additional cost of the path point. If negative, the path point will be sorted out by NodeProcessors. */
   public float costMalus;
   public BlockPathTypes type = BlockPathTypes.BLOCKED;

   public Node(int p_77285_, int p_77286_, int p_77287_) {
      this.x = p_77285_;
      this.y = p_77286_;
      this.z = p_77287_;
      this.hash = createHash(p_77285_, p_77286_, p_77287_);
   }

   public Node cloneAndMove(int pX, int pY, int pZ) {
      Node node = new Node(pX, pY, pZ);
      node.heapIdx = this.heapIdx;
      node.g = this.g;
      node.h = this.h;
      node.f = this.f;
      node.cameFrom = this.cameFrom;
      node.closed = this.closed;
      node.walkedDistance = this.walkedDistance;
      node.costMalus = this.costMalus;
      node.type = this.type;
      return node;
   }

   public static int createHash(int pX, int pY, int pZ) {
      return pY & 255 | (pX & 32767) << 8 | (pZ & 32767) << 24 | (pX < 0 ? Integer.MIN_VALUE : 0) | (pZ < 0 ? '\u8000' : 0);
   }

   /**
    * Returns the linear distance to another path point
    */
   public float distanceTo(Node pPathpoint) {
      float f = (float)(pPathpoint.x - this.x);
      float f1 = (float)(pPathpoint.y - this.y);
      float f2 = (float)(pPathpoint.z - this.z);
      return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
   }

   public float distanceTo(BlockPos p_164698_) {
      float f = (float)(p_164698_.getX() - this.x);
      float f1 = (float)(p_164698_.getY() - this.y);
      float f2 = (float)(p_164698_.getZ() - this.z);
      return Mth.sqrt(f * f + f1 * f1 + f2 * f2);
   }

   /**
    * Returns the squared distance to another path point
    */
   public float distanceToSqr(Node pPathpoint) {
      float f = (float)(pPathpoint.x - this.x);
      float f1 = (float)(pPathpoint.y - this.y);
      float f2 = (float)(pPathpoint.z - this.z);
      return f * f + f1 * f1 + f2 * f2;
   }

   public float distanceToSqr(BlockPos p_164703_) {
      float f = (float)(p_164703_.getX() - this.x);
      float f1 = (float)(p_164703_.getY() - this.y);
      float f2 = (float)(p_164703_.getZ() - this.z);
      return f * f + f1 * f1 + f2 * f2;
   }

   public float distanceManhattan(Node p_77305_) {
      float f = (float)Math.abs(p_77305_.x - this.x);
      float f1 = (float)Math.abs(p_77305_.y - this.y);
      float f2 = (float)Math.abs(p_77305_.z - this.z);
      return f + f1 + f2;
   }

   public float distanceManhattan(BlockPos p_77307_) {
      float f = (float)Math.abs(p_77307_.getX() - this.x);
      float f1 = (float)Math.abs(p_77307_.getY() - this.y);
      float f2 = (float)Math.abs(p_77307_.getZ() - this.z);
      return f + f1 + f2;
   }

   public BlockPos asBlockPos() {
      return new BlockPos(this.x, this.y, this.z);
   }

   public Vec3 asVec3() {
      return new Vec3((double)this.x, (double)this.y, (double)this.z);
   }

   public boolean equals(Object p_77309_) {
      if (!(p_77309_ instanceof Node)) {
         return false;
      } else {
         Node node = (Node)p_77309_;
         return this.hash == node.hash && this.x == node.x && this.y == node.y && this.z == node.z;
      }
   }

   public int hashCode() {
      return this.hash;
   }

   /**
    * Returns true if this point has already been assigned to a path
    */
   public boolean inOpenSet() {
      return this.heapIdx >= 0;
   }

   public String toString() {
      return "Node{x=" + this.x + ", y=" + this.y + ", z=" + this.z + "}";
   }

   public void writeToStream(FriendlyByteBuf p_164700_) {
      p_164700_.writeInt(this.x);
      p_164700_.writeInt(this.y);
      p_164700_.writeInt(this.z);
      p_164700_.writeFloat(this.walkedDistance);
      p_164700_.writeFloat(this.costMalus);
      p_164700_.writeBoolean(this.closed);
      p_164700_.writeInt(this.type.ordinal());
      p_164700_.writeFloat(this.f);
   }

   public static Node createFromStream(FriendlyByteBuf pBuf) {
      Node node = new Node(pBuf.readInt(), pBuf.readInt(), pBuf.readInt());
      node.walkedDistance = pBuf.readFloat();
      node.costMalus = pBuf.readFloat();
      node.closed = pBuf.readBoolean();
      node.type = BlockPathTypes.values()[pBuf.readInt()];
      node.f = pBuf.readFloat();
      return node;
   }
}