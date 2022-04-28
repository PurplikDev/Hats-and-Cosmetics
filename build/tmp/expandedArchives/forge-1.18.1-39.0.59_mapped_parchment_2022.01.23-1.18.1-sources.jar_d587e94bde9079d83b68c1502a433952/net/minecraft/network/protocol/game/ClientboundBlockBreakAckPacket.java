package net.minecraft.network.protocol.game;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public record ClientboundBlockBreakAckPacket(BlockPos pos, BlockState state, ServerboundPlayerActionPacket.Action action, boolean allGood) implements Packet<ClientGamePacketListener> {
   /** Unused (probably related to the unused parameter in the constructor) */
   private static final Logger LOGGER = LogManager.getLogger();

   public ClientboundBlockBreakAckPacket(BlockPos pPos, BlockState pState, ServerboundPlayerActionPacket.Action pAction, boolean pAllGood, String pReason) {
      this(pPos, pState, pAction, pAllGood);
   }

   public ClientboundBlockBreakAckPacket(BlockPos pos, BlockState state, ServerboundPlayerActionPacket.Action action, boolean allGood) {
      pos = pos.immutable();
      this.pos = pos;
      this.state = state;
      this.action = action;
      this.allGood = allGood;
   }

   public ClientboundBlockBreakAckPacket(FriendlyByteBuf pBuffer) {
      this(pBuffer.readBlockPos(), Block.BLOCK_STATE_REGISTRY.byId(pBuffer.readVarInt()), pBuffer.readEnum(ServerboundPlayerActionPacket.Action.class), pBuffer.readBoolean());
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      pBuffer.writeBlockPos(this.pos);
      pBuffer.writeVarInt(Block.getId(this.state));
      pBuffer.writeEnum(this.action);
      pBuffer.writeBoolean(this.allGood);
   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleBlockBreakAck(this);
   }
}