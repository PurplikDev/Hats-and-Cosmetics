package net.minecraft.client;

import java.util.function.Consumer;
import javax.annotation.Nullable;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ServerboundBlockEntityTagQuery;
import net.minecraft.network.protocol.game.ServerboundEntityTagQuery;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class DebugQueryHandler {
   private final ClientPacketListener connection;
   private int transactionId = -1;
   @Nullable
   private Consumer<CompoundTag> callback;

   public DebugQueryHandler(ClientPacketListener pConnection) {
      this.connection = pConnection;
   }

   public boolean handleResponse(int pTransactionId, @Nullable CompoundTag pTag) {
      if (this.transactionId == pTransactionId && this.callback != null) {
         this.callback.accept(pTag);
         this.callback = null;
         return true;
      } else {
         return false;
      }
   }

   private int startTransaction(Consumer<CompoundTag> pCallback) {
      this.callback = pCallback;
      return ++this.transactionId;
   }

   public void queryEntityTag(int p_90703_, Consumer<CompoundTag> p_90704_) {
      int i = this.startTransaction(p_90704_);
      this.connection.send(new ServerboundEntityTagQuery(i, p_90703_));
   }

   public void queryBlockEntityTag(BlockPos p_90709_, Consumer<CompoundTag> p_90710_) {
      int i = this.startTransaction(p_90710_);
      this.connection.send(new ServerboundBlockEntityTagQuery(i, p_90709_));
   }
}