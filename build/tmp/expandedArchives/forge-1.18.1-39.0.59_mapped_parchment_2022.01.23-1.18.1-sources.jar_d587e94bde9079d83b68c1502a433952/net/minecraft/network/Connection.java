package net.minecraft.network;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoopGroup;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.TimeoutException;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Queue;
import javax.annotation.Nullable;
import javax.crypto.Cipher;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ClientboundDisconnectPacket;
import net.minecraft.network.protocol.login.ClientboundLoginDisconnectPacket;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.util.Mth;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

public class Connection extends SimpleChannelInboundHandler<Packet<?>> {
   private static final float AVERAGE_PACKETS_SMOOTHING = 0.75F;
   private static final Logger LOGGER = LogManager.getLogger();
   public static final Marker ROOT_MARKER = MarkerManager.getMarker("NETWORK");
   public static final Marker PACKET_MARKER = MarkerManager.getMarker("NETWORK_PACKETS", ROOT_MARKER);
   public static final AttributeKey<ConnectionProtocol> ATTRIBUTE_PROTOCOL = AttributeKey.valueOf("protocol");
   public static final LazyLoadedValue<NioEventLoopGroup> NETWORK_WORKER_GROUP = new LazyLoadedValue<>(() -> {
      return new NioEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Client IO #%d").setDaemon(true).build());
   });
   public static final LazyLoadedValue<EpollEventLoopGroup> NETWORK_EPOLL_WORKER_GROUP = new LazyLoadedValue<>(() -> {
      return new EpollEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Epoll Client IO #%d").setDaemon(true).build());
   });
   public static final LazyLoadedValue<DefaultEventLoopGroup> LOCAL_WORKER_GROUP = new LazyLoadedValue<>(() -> {
      return new DefaultEventLoopGroup(0, (new ThreadFactoryBuilder()).setNameFormat("Netty Local Client IO #%d").setDaemon(true).build());
   });
   private final PacketFlow receiving;
   /**
    * The queue for packets that get sent before the channel is connected.
    * Every tick or whenever a new packet is sent the connection will try to flush this queue, if the channel has since
    * finished connecting.
    */
   private final Queue<Connection.PacketHolder> queue = Queues.newConcurrentLinkedQueue();
   /** The active channel */
   private Channel channel;
   /** The address of the remote party */
   private SocketAddress address;
   /** The PacketListener instance responsible for processing received packets */
   private PacketListener packetListener;
   /** A Component indicating why the network has shutdown. */
   private Component disconnectedReason;
   private boolean encrypted;
   private boolean disconnectionHandled;
   private int receivedPackets;
   private int sentPackets;
   private float averageReceivedPackets;
   private float averageSentPackets;
   private int tickCount;
   private boolean handlingFault;
   private java.util.function.Consumer<Connection> activationHandler;

   public Connection(PacketFlow p_129482_) {
      this.receiving = p_129482_;
   }

   public void channelActive(ChannelHandlerContext p_129525_) throws Exception {
      super.channelActive(p_129525_);
      this.channel = p_129525_.channel();
      this.address = this.channel.remoteAddress();
      if (activationHandler != null) activationHandler.accept(this);

      try {
         this.setProtocol(ConnectionProtocol.HANDSHAKING);
      } catch (Throwable throwable) {
         LOGGER.fatal(throwable);
      }

   }

   /**
    * Sets the new connection state and registers which packets this channel may send and receive
    */
   public void setProtocol(ConnectionProtocol pNewState) {
      this.channel.attr(ATTRIBUTE_PROTOCOL).set(pNewState);
      this.channel.config().setAutoRead(true);
      LOGGER.debug("Enabled auto read");
   }

   public void channelInactive(ChannelHandlerContext p_129527_) {
      this.disconnect(new TranslatableComponent("disconnect.endOfStream"));
   }

   public void exceptionCaught(ChannelHandlerContext p_129533_, Throwable p_129534_) {
      if (p_129534_ instanceof SkipPacketException) {
         LOGGER.debug("Skipping packet due to errors", p_129534_.getCause());
      } else {
         boolean flag = !this.handlingFault;
         this.handlingFault = true;
         if (this.channel.isOpen()) {
            if (p_129534_ instanceof TimeoutException) {
               LOGGER.debug("Timeout", p_129534_);
               this.disconnect(new TranslatableComponent("disconnect.timeout"));
            } else {
               Component component = new TranslatableComponent("disconnect.genericReason", "Internal Exception: " + p_129534_);
               if (flag) {
                  LOGGER.debug("Failed to sent packet", p_129534_);
                  ConnectionProtocol connectionprotocol = this.getCurrentProtocol();
                  Packet<?> packet = (Packet<?>)(connectionprotocol == ConnectionProtocol.LOGIN ? new ClientboundLoginDisconnectPacket(component) : new ClientboundDisconnectPacket(component));
                  this.send(packet, (p_129511_) -> {
                     this.disconnect(component);
                  });
                  this.setReadOnly();
               } else {
                  LOGGER.debug("Double fault", p_129534_);
                  this.disconnect(component);
               }
            }

         }
      }
   }

   protected void channelRead0(ChannelHandlerContext p_129487_, Packet<?> p_129488_) {
      if (this.channel.isOpen()) {
         try {
            genericsFtw(p_129488_, this.packetListener);
         } catch (RunningOnDifferentThreadException runningondifferentthreadexception) {
         } catch (ClassCastException classcastexception) {
            LOGGER.error("Received {} that couldn't be processed", p_129488_.getClass(), classcastexception);
            this.disconnect(new TranslatableComponent("multiplayer.disconnect.invalid_packet"));
         }

         ++this.receivedPackets;
      }

   }

   private static <T extends PacketListener> void genericsFtw(Packet<T> p_129518_, PacketListener p_129519_) {
      p_129518_.handle((T)p_129519_);
   }

   /**
    * Sets the NetHandler for this NetworkManager, no checks are made if this handler is suitable for the particular
    * connection state (protocol)
    */
   public void setListener(PacketListener pHandler) {
      Validate.notNull(pHandler, "packetListener");
      this.packetListener = pHandler;
   }

   public void send(Packet<?> pPacket) {
      this.send(pPacket, (GenericFutureListener<? extends Future<? super Void>>)null);
   }

   public void send(Packet<?> p_129515_, @Nullable GenericFutureListener<? extends Future<? super Void>> p_129516_) {
      if (this.isConnected()) {
         this.flushQueue();
         this.sendPacket(p_129515_, p_129516_);
      } else {
         this.queue.add(new Connection.PacketHolder(p_129515_, p_129516_));
      }

   }

   /**
    * Will commit the packet to the channel. If the current thread 'owns' the channel it will write and flush the
    * packet, otherwise it will add a task for the channel eventloop thread to do that.
    */
   private void sendPacket(Packet<?> pInPacket, @Nullable GenericFutureListener<? extends Future<? super Void>> pFutureListeners) {
      ConnectionProtocol connectionprotocol = ConnectionProtocol.getProtocolForPacket(pInPacket);
      ConnectionProtocol connectionprotocol1 = this.getCurrentProtocol();
      ++this.sentPackets;
      if (connectionprotocol1 != connectionprotocol) {
         LOGGER.debug("Disabled auto read");
         this.channel.eventLoop().execute(()->this.channel.config().setAutoRead(false));
      }

      if (this.channel.eventLoop().inEventLoop()) {
         this.doSendPacket(pInPacket, pFutureListeners, connectionprotocol, connectionprotocol1);
      } else {
         this.channel.eventLoop().execute(() -> {
            this.doSendPacket(pInPacket, pFutureListeners, connectionprotocol, connectionprotocol1);
         });
      }

   }

   private void doSendPacket(Packet<?> p_178304_, @Nullable GenericFutureListener<? extends Future<? super Void>> p_178305_, ConnectionProtocol p_178306_, ConnectionProtocol p_178307_) {
      if (p_178306_ != p_178307_) {
         this.setProtocol(p_178306_);
      }

      ChannelFuture channelfuture = this.channel.writeAndFlush(p_178304_);
      if (p_178305_ != null) {
         channelfuture.addListener(p_178305_);
      }

      channelfuture.addListener(ChannelFutureListener.FIRE_EXCEPTION_ON_FAILURE);
   }

   private ConnectionProtocol getCurrentProtocol() {
      return this.channel.attr(ATTRIBUTE_PROTOCOL).get();
   }

   /**
    * Will iterate through the outboundPacketQueue and dispatch all Packets
    */
   private void flushQueue() {
      if (this.channel != null && this.channel.isOpen()) {
         synchronized(this.queue) {
            Connection.PacketHolder connection$packetholder;
            while((connection$packetholder = this.queue.poll()) != null) {
               this.sendPacket(connection$packetholder.packet, connection$packetholder.listener);
            }

         }
      }
   }

   /**
    * Checks timeouts and processes all packets received
    */
   public void tick() {
      this.flushQueue();
      if (this.packetListener instanceof ServerLoginPacketListenerImpl) {
         ((ServerLoginPacketListenerImpl)this.packetListener).tick();
      }

      if (this.packetListener instanceof ServerGamePacketListenerImpl) {
         ((ServerGamePacketListenerImpl)this.packetListener).tick();
      }

      if (!this.isConnected() && !this.disconnectionHandled) {
         this.handleDisconnection();
      }

      if (this.channel != null) {
         this.channel.flush();
      }

      if (this.tickCount++ % 20 == 0) {
         this.tickSecond();
      }

   }

   protected void tickSecond() {
      this.averageSentPackets = Mth.lerp(0.75F, (float)this.sentPackets, this.averageSentPackets);
      this.averageReceivedPackets = Mth.lerp(0.75F, (float)this.receivedPackets, this.averageReceivedPackets);
      this.sentPackets = 0;
      this.receivedPackets = 0;
   }

   /**
    * Returns the socket address of the remote side. Server-only.
    */
   public SocketAddress getRemoteAddress() {
      return this.address;
   }

   /**
    * Closes the channel with a given reason. The reason is stored for later and will be used for informational purposes
    * (info log on server,
    * disconnection screen on the client). This method is also called on the client when the server requests
    * disconnection via
    * {@code ClientboundDisconnectPacket}.
    * 
    * Closing the channel this way does not send any disconnection packets, it simply terminates the underlying netty
    * channel.
    */
   public void disconnect(Component pMessage) {
      if (this.channel.isOpen()) {
         this.channel.close().awaitUninterruptibly();
         this.disconnectedReason = pMessage;
      }

   }

   /**
    * True if this NetworkManager uses a memory connection (single player game). False may imply both an active TCP
    * connection or simply no active connection at all
    */
   public boolean isMemoryConnection() {
      return this.channel instanceof LocalChannel || this.channel instanceof LocalServerChannel;
   }

   /**
    * The receiving packet direction (i.e. SERVERBOUND on the server and CLIENTBOUND on the client).
    */
   public PacketFlow getReceiving() {
      return this.receiving;
   }

   /**
    * The sending packet direction (i.e. SERVERBOUND on the client and CLIENTBOUND on the server)
    */
   public PacketFlow getSending() {
      return this.receiving.getOpposite();
   }

   /**
    * Prepares a clientside Connection for a network connection to a remote server.
    * Establishes a connection to the socket supplied and configures the channel pipeline. Returns the newly created
    * instance.
    * @param pUseEpollIfAvailable whether to use an Epoll channel if it is available
    */
   public static Connection connectToServer(InetSocketAddress pAddress, boolean pUseEpollIfAvailable) {
      if (pAddress.getAddress() instanceof java.net.Inet6Address) System.setProperty("java.net.preferIPv4Stack", "false");
      final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      connection.activationHandler = net.minecraftforge.network.NetworkHooks::registerClientLoginChannel;
      Class<? extends SocketChannel> oclass;
      LazyLoadedValue<? extends EventLoopGroup> lazyloadedvalue;
      if (Epoll.isAvailable() && pUseEpollIfAvailable) {
         oclass = EpollSocketChannel.class;
         lazyloadedvalue = NETWORK_EPOLL_WORKER_GROUP;
      } else {
         oclass = NioSocketChannel.class;
         lazyloadedvalue = NETWORK_WORKER_GROUP;
      }

      (new Bootstrap()).group(lazyloadedvalue.get()).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel p_129552_) {
            try {
               p_129552_.config().setOption(ChannelOption.TCP_NODELAY, true);
            } catch (ChannelException channelexception) {
            }

            p_129552_.pipeline().addLast("timeout", new ReadTimeoutHandler(30)).addLast("splitter", new Varint21FrameDecoder()).addLast("decoder", new PacketDecoder(PacketFlow.CLIENTBOUND)).addLast("prepender", new Varint21LengthFieldPrepender()).addLast("encoder", new PacketEncoder(PacketFlow.SERVERBOUND)).addLast("packet_handler", connection);
         }
      }).channel(oclass).connect(pAddress.getAddress(), pAddress.getPort()).syncUninterruptibly();
      return connection;
   }

   /**
    * Prepares a clientside Connection for a local in-memory connection ("single player").
    * Establishes a connection to the socket supplied and configures the channel pipeline (only the packet handler is
    * necessary,
    * since this is for an in-memory connection). Returns the newly created instance.
    */
   public static Connection connectToLocalServer(SocketAddress pAddress) {
      final Connection connection = new Connection(PacketFlow.CLIENTBOUND);
      connection.activationHandler = net.minecraftforge.network.NetworkHooks::registerClientLoginChannel;
      (new Bootstrap()).group(LOCAL_WORKER_GROUP.get()).handler(new ChannelInitializer<Channel>() {
         protected void initChannel(Channel p_129557_) {
            p_129557_.pipeline().addLast("packet_handler", connection);
         }
      }).channel(LocalChannel.class).connect(pAddress).syncUninterruptibly();
      return connection;
   }

   /**
    * Enables encryption for this connection using the given decrypting and encrypting ciphers.
    * This adds new handlers to this connection's pipeline which handle the decrypting and encrypting.
    * This happens as part of the normal network handshake.
    * 
    * @see net.minecraft.network.protocol.login.ClientboundHelloPacket
    * @see net.minecraft.network.protocol.login.ServerboundKeyPacket
    */
   public void setEncryptionKey(Cipher pDecryptingCipher, Cipher pEncryptingCipher) {
      this.encrypted = true;
      this.channel.pipeline().addBefore("splitter", "decrypt", new CipherDecoder(pDecryptingCipher));
      this.channel.pipeline().addBefore("prepender", "encrypt", new CipherEncoder(pEncryptingCipher));
   }

   public boolean isEncrypted() {
      return this.encrypted;
   }

   /**
    * Returns true if this NetworkManager has an active channel, false otherwise
    */
   public boolean isConnected() {
      return this.channel != null && this.channel.isOpen();
   }

   /**
    * Returns true while this connection is still connecting, i.e. {@link #channelActive} has not fired yet.
    */
   public boolean isConnecting() {
      return this.channel == null;
   }

   /**
    * Gets the current handler for processing packets
    */
   public PacketListener getPacketListener() {
      return this.packetListener;
   }

   /**
    * If this channel is closed, returns the exit message, null otherwise.
    */
   @Nullable
   public Component getDisconnectedReason() {
      return this.disconnectedReason;
   }

   /**
    * Switches the channel to manual reading modus
    */
   public void setReadOnly() {
      this.channel.config().setAutoRead(false);
   }

   /**
    * Enables or disables compression for this connection. If {@code threshold} is >= 0 then a {@link
    * CompressionDecoder} and {@link CompressionEncoder}
    * are installed in the pipeline or updated if they already exists. If {@code threshold} is < 0 then any such codec
    * are removed.
    * 
    * Compression is enabled as part of the connection handshake when the server sends {@link
    * net.minecraft.network.protocol.login.ClientboundLoginCompressionPacket}.
    */
   public void setupCompression(int pThreshold, boolean pValidateDecompressed) {
      if (pThreshold >= 0) {
         if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
            ((CompressionDecoder)this.channel.pipeline().get("decompress")).setThreshold(pThreshold, pValidateDecompressed);
         } else {
            this.channel.pipeline().addBefore("decoder", "decompress", new CompressionDecoder(pThreshold, pValidateDecompressed));
         }

         if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
            ((CompressionEncoder)this.channel.pipeline().get("compress")).setThreshold(pThreshold);
         } else {
            this.channel.pipeline().addBefore("encoder", "compress", new CompressionEncoder(pThreshold));
         }
      } else {
         if (this.channel.pipeline().get("decompress") instanceof CompressionDecoder) {
            this.channel.pipeline().remove("decompress");
         }

         if (this.channel.pipeline().get("compress") instanceof CompressionEncoder) {
            this.channel.pipeline().remove("compress");
         }
      }

   }

   /**
    * Checks if the channle is no longer active and if so, processes the disconnection
    * by notifying the current packet listener, which will handle things like removing the player from the world
    * (serverside) or
    * showing the disconnection screen (clientside).
    */
   public void handleDisconnection() {
      if (this.channel != null && !this.channel.isOpen()) {
         if (this.disconnectionHandled) {
            LOGGER.warn("handleDisconnection() called twice");
         } else {
            this.disconnectionHandled = true;
            if (this.getDisconnectedReason() != null) {
               this.getPacketListener().onDisconnect(this.getDisconnectedReason());
            } else if (this.getPacketListener() != null) {
               this.getPacketListener().onDisconnect(new TranslatableComponent("multiplayer.disconnect.generic"));
            }
         }

      }
   }

   public float getAverageReceivedPackets() {
      return this.averageReceivedPackets;
   }

   public float getAverageSentPackets() {
      return this.averageSentPackets;
   }

   public Channel channel() {
      return channel;
   }

   public PacketFlow getDirection() {
      return this.receiving;
   }

   static class PacketHolder {
      final Packet<?> packet;
      @Nullable
      final GenericFutureListener<? extends Future<? super Void>> listener;

      public PacketHolder(Packet<?> p_129561_, @Nullable GenericFutureListener<? extends Future<? super Void>> p_129562_) {
         this.packet = p_129561_;
         this.listener = p_129562_;
      }
   }
}
