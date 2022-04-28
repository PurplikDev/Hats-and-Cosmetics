package net.minecraft.network.protocol.game;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.mojang.brigadier.tree.RootCommandNode;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import javax.annotation.Nullable;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypes;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;

public class ClientboundCommandsPacket implements Packet<ClientGamePacketListener> {
   private static final byte MASK_TYPE = 3;
   private static final byte FLAG_EXECUTABLE = 4;
   private static final byte FLAG_REDIRECT = 8;
   private static final byte FLAG_CUSTOM_SUGGESTIONS = 16;
   private static final byte TYPE_ROOT = 0;
   private static final byte TYPE_LITERAL = 1;
   private static final byte TYPE_ARGUMENT = 2;
   private final RootCommandNode<SharedSuggestionProvider> root;

   public ClientboundCommandsPacket(RootCommandNode<SharedSuggestionProvider> pRoot) {
      this.root = pRoot;
   }

   public ClientboundCommandsPacket(FriendlyByteBuf pBuffer) {
      List<ClientboundCommandsPacket.Entry> list = pBuffer.readList(ClientboundCommandsPacket::readNode);
      resolveEntries(list);
      int i = pBuffer.readVarInt();
      this.root = (RootCommandNode)(list.get(i)).node;
   }

   /**
    * Writes the raw packet data to the data stream.
    */
   public void write(FriendlyByteBuf pBuffer) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> object2intmap = enumerateNodes(this.root);
      List<CommandNode<SharedSuggestionProvider>> list = getNodesInIdOrder(object2intmap);
      pBuffer.writeCollection(list, (p_178810_, p_178811_) -> {
         writeNode(p_178810_, p_178811_, object2intmap);
      });
      pBuffer.writeVarInt(object2intmap.get(this.root));
   }

   private static void resolveEntries(List<ClientboundCommandsPacket.Entry> pEntries) {
      List<ClientboundCommandsPacket.Entry> list = Lists.newArrayList(pEntries);

      while(!list.isEmpty()) {
         boolean flag = list.removeIf((p_178816_) -> {
            return p_178816_.build(pEntries);
         });
         if (!flag) {
            throw new IllegalStateException("Server sent an impossible command tree");
         }
      }

   }

   private static Object2IntMap<CommandNode<SharedSuggestionProvider>> enumerateNodes(RootCommandNode<SharedSuggestionProvider> pRootNode) {
      Object2IntMap<CommandNode<SharedSuggestionProvider>> object2intmap = new Object2IntOpenHashMap<>();
      Queue<CommandNode<SharedSuggestionProvider>> queue = Queues.newArrayDeque();
      queue.add(pRootNode);

      CommandNode<SharedSuggestionProvider> commandnode;
      while((commandnode = queue.poll()) != null) {
         if (!object2intmap.containsKey(commandnode)) {
            int i = object2intmap.size();
            object2intmap.put(commandnode, i);
            queue.addAll(commandnode.getChildren());
            if (commandnode.getRedirect() != null) {
               queue.add(commandnode.getRedirect());
            }
         }
      }

      return object2intmap;
   }

   private static List<CommandNode<SharedSuggestionProvider>> getNodesInIdOrder(Object2IntMap<CommandNode<SharedSuggestionProvider>> pNodeToIdMap) {
      ObjectArrayList<CommandNode<SharedSuggestionProvider>> objectarraylist = new ObjectArrayList<>(pNodeToIdMap.size());
      objectarraylist.size(pNodeToIdMap.size());

      for(Object2IntMap.Entry<CommandNode<SharedSuggestionProvider>> entry : Object2IntMaps.fastIterable(pNodeToIdMap)) {
         objectarraylist.set(entry.getIntValue(), entry.getKey());
      }

      return objectarraylist;
   }

   private static ClientboundCommandsPacket.Entry readNode(FriendlyByteBuf p_131888_) {
      byte b0 = p_131888_.readByte();
      int[] aint = p_131888_.readVarIntArray();
      int i = (b0 & 8) != 0 ? p_131888_.readVarInt() : 0;
      ArgumentBuilder<SharedSuggestionProvider, ?> argumentbuilder = createBuilder(p_131888_, b0);
      return new ClientboundCommandsPacket.Entry(argumentbuilder, b0, i, aint);
   }

   @Nullable
   private static ArgumentBuilder<SharedSuggestionProvider, ?> createBuilder(FriendlyByteBuf pBuffer, byte pFlags) {
      int i = pFlags & 3;
      if (i == 2) {
         String s = pBuffer.readUtf();
         ArgumentType<?> argumenttype = ArgumentTypes.deserialize(pBuffer);
         if (argumenttype == null) {
            return null;
         } else {
            RequiredArgumentBuilder<SharedSuggestionProvider, ?> requiredargumentbuilder = RequiredArgumentBuilder.argument(s, argumenttype);
            if ((pFlags & 16) != 0) {
               requiredargumentbuilder.suggests(SuggestionProviders.getProvider(pBuffer.readResourceLocation()));
            }

            return requiredargumentbuilder;
         }
      } else {
         return i == 1 ? LiteralArgumentBuilder.literal(pBuffer.readUtf()) : null;
      }
   }

   private static void writeNode(FriendlyByteBuf pBuffer, CommandNode<SharedSuggestionProvider> pNode, Map<CommandNode<SharedSuggestionProvider>, Integer> pNodeIds) {
      byte b0 = 0;
      if (pNode.getRedirect() != null) {
         b0 = (byte)(b0 | 8);
      }

      if (pNode.getCommand() != null) {
         b0 = (byte)(b0 | 4);
      }

      if (pNode instanceof RootCommandNode) {
         b0 = (byte)(b0 | 0);
      } else if (pNode instanceof ArgumentCommandNode) {
         b0 = (byte)(b0 | 2);
         if (((ArgumentCommandNode)pNode).getCustomSuggestions() != null) {
            b0 = (byte)(b0 | 16);
         }
      } else {
         if (!(pNode instanceof LiteralCommandNode)) {
            throw new UnsupportedOperationException("Unknown node type " + pNode);
         }

         b0 = (byte)(b0 | 1);
      }

      pBuffer.writeByte(b0);
      pBuffer.writeVarInt(pNode.getChildren().size());

      for(CommandNode<SharedSuggestionProvider> commandnode : pNode.getChildren()) {
         pBuffer.writeVarInt(pNodeIds.get(commandnode));
      }

      if (pNode.getRedirect() != null) {
         pBuffer.writeVarInt(pNodeIds.get(pNode.getRedirect()));
      }

      if (pNode instanceof ArgumentCommandNode) {
         ArgumentCommandNode<SharedSuggestionProvider, ?> argumentcommandnode = (ArgumentCommandNode)pNode;
         pBuffer.writeUtf(argumentcommandnode.getName());
         ArgumentTypes.serialize(pBuffer, argumentcommandnode.getType());
         if (argumentcommandnode.getCustomSuggestions() != null) {
            pBuffer.writeResourceLocation(SuggestionProviders.getName(argumentcommandnode.getCustomSuggestions()));
         }
      } else if (pNode instanceof LiteralCommandNode) {
         pBuffer.writeUtf(((LiteralCommandNode)pNode).getLiteral());
      }

   }

   /**
    * Passes this Packet on to the NetHandler for processing.
    */
   public void handle(ClientGamePacketListener pHandler) {
      pHandler.handleCommands(this);
   }

   public RootCommandNode<SharedSuggestionProvider> getRoot() {
      return this.root;
   }

   static class Entry {
      @Nullable
      private final ArgumentBuilder<SharedSuggestionProvider, ?> builder;
      private final byte flags;
      private final int redirect;
      private final int[] children;
      @Nullable
      CommandNode<SharedSuggestionProvider> node;

      Entry(@Nullable ArgumentBuilder<SharedSuggestionProvider, ?> pBuilder, byte pFlags, int pRedirect, int[] pChildren) {
         this.builder = pBuilder;
         this.flags = pFlags;
         this.redirect = pRedirect;
         this.children = pChildren;
      }

      public boolean build(List<ClientboundCommandsPacket.Entry> pEntries) {
         if (this.node == null) {
            if (this.builder == null) {
               this.node = new RootCommandNode<>();
            } else {
               if ((this.flags & 8) != 0) {
                  if ((pEntries.get(this.redirect)).node == null) {
                     return false;
                  }

                  this.builder.redirect((pEntries.get(this.redirect)).node);
               }

               if ((this.flags & 4) != 0) {
                  this.builder.executes((p_131906_) -> {
                     return 0;
                  });
               }

               this.node = this.builder.build();
            }
         }

         for(int i : this.children) {
            if ((pEntries.get(i)).node == null) {
               return false;
            }
         }

         for(int j : this.children) {
            CommandNode<SharedSuggestionProvider> commandnode = (pEntries.get(j)).node;
            if (!(commandnode instanceof RootCommandNode)) {
               this.node.addChild(commandnode);
            }
         }

         return true;
      }
   }
}