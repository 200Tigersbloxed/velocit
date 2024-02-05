package gay.tigers.velocit;

import gg.playit.minecraft.PlayitConnectionTracker;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;

public class VelocitTcpTunnel {
    private final EventLoopGroup group;
    private final String connectionKey;
    private final PlayitConnectionTracker tracker;
    private final InetSocketAddress minecraftServerAddress;
    private final InetSocketAddress tunnelClaimAddress;
    private final byte[] tunnelClaimToken;
    private final org.slf4j.Logger log;

    public VelocitTcpTunnel(
            EventLoopGroup group,
            PlayitConnectionTracker tracker,
            String connectionKey,
            InetSocketAddress minecraftServerAddress,
            InetSocketAddress tunnelClaimAddress,
            byte[] tunnelClaimToken,
            org.slf4j.Logger log
    ) {
        this.group = group;
        this.tracker = tracker;
        this.connectionKey = connectionKey;
        this.minecraftServerAddress = minecraftServerAddress;
        this.tunnelClaimAddress = tunnelClaimAddress;
        this.tunnelClaimToken = tunnelClaimToken;
        this.log = log;
    }

    private SocketChannel minecraftChannel;
    private SocketChannel tunnelChannel;

    public void start() {
        Bootstrap clientBootstrap = new Bootstrap();
        clientBootstrap.group(group);
        clientBootstrap.channel(NioSocketChannel.class);
        clientBootstrap.remoteAddress(this.tunnelClaimAddress);

        clientBootstrap.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel socketChannel) {
                tunnelChannel = socketChannel;
                socketChannel.pipeline().addLast(new TunnelConnectionHandler());
            }
        });

        log.info("start connection to " + tunnelClaimAddress + " to claim client");
        clientBootstrap.connect().addListener((ChannelFutureListener) future -> {
            if (!future.isSuccess()) {
                log.warn("failed to establish connection to tunnel claim" + tunnelClaimAddress);
                disconnected();
                return;
            }

            log.info("connected to tunnel server, sending claim token");

            future.channel().writeAndFlush(Unpooled.wrappedBuffer(tunnelClaimToken)).addListener(f -> {
                if (!f.isSuccess()) {
                    log.warn("failed to send claim token");
                } else {
                    log.info("claim token sent");
                }
            });
        });
    }

    private void disconnected() {
        this.tracker.removeConnection(connectionKey);
    }

    @ChannelHandler.Sharable
    private class TunnelConnectionHandler extends SimpleChannelInboundHandler<ByteBuf> {
        TunnelConnectionHandler() {
            super(false);
        }

        private int confirmBytesRemaining = 8;

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf byteBuf) throws Exception {
            if (confirmBytesRemaining > 0) {
                if (byteBuf.readableBytes() < confirmBytesRemaining) {
                    confirmBytesRemaining -= byteBuf.readableBytes();
                    byteBuf.readBytes(byteBuf.readableBytes());
                    byteBuf.release();
                    ctx.read();
                    return;
                }

                byteBuf.readBytes(confirmBytesRemaining);
                confirmBytesRemaining = 0;

                log.info("connection to tunnel server has been established");

                // Not necessary apparently
                /*if (addChannelToMinecraftServer()) {
                    log.info("added channel to minecraft server");
                    return;
                }*/

                var minecraftClient = new Bootstrap();
                minecraftClient.group(group);
                minecraftClient.option(ChannelOption.TCP_NODELAY, true);
                minecraftClient.channel(NioSocketChannel.class);
                minecraftClient.remoteAddress(minecraftServerAddress);

                minecraftClient.handler(new ChannelInitializer<SocketChannel>() {
                    protected void initChannel(SocketChannel socketChannel) {
                        minecraftChannel = socketChannel;
                        socketChannel.pipeline().addLast(new MinecraftConnectionHandler());
                    }
                });

                log.info("connecting to minecraft server at " + minecraftServerAddress);
                minecraftClient.connect().addListener((ChannelFutureListener) future -> {
                    if (!future.isSuccess()) {
                        log.warn("failed to connect to local minecraft server");
                        ctx.disconnect();
                        disconnected();
                        return;
                    }

                    log.info("connected to local minecraft server");

                    if (byteBuf.readableBytes() == 0) {
                        byteBuf.release();
                        ctx.read();
                    } else {
                        future.channel().writeAndFlush(byteBuf).addListener(f -> {
                            if (!f.isSuccess()) {
                                log.warn("failed to send data to minecraft server");
                                future.channel().disconnect();
                                ctx.disconnect();
                                disconnected();
                                return;
                            }

                            ctx.read();
                        });
                    }
                });

                return;
            }

            /* proxy data */
            minecraftChannel.writeAndFlush(byteBuf).addListener(f -> {
                if (!f.isSuccess()) {
                    log.warn("failed to send data to minecraft server");
                    minecraftChannel.disconnect();
                    tunnelChannel.disconnect();
                    disconnected();
                    return;
                }

                ctx.read();
            });
        }

        /*private boolean addChannelToMinecraftServer() {
            try {
                Object channelManager = server.getClass().getField("cm").get(server);
                Object serverChannelManager = channelManager.getClass().getField("serverChannelInitializer").get(channelManager);
                serverChannelManager.getClass().getMethod("initChannel", Channel.class).invoke(serverChannelManager, tunnelChannel);
                return true;
            } catch (NoSuchFieldException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                log.error("Failed to register channel! " + e);
                return false;
            }
        }*/

        private class MinecraftConnectionHandler extends SimpleChannelInboundHandler<ByteBuf> {
            MinecraftConnectionHandler() {
                super(false);
            }

            @Override
            protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                tunnelChannel.writeAndFlush(msg).addListener(f -> {
                    if (!f.isSuccess()) {
                        log.warn("failed to send data to tunnel");
                        minecraftChannel.disconnect();
                        tunnelChannel.disconnect();
                        return;
                    }

                    ctx.read();
                });
            }
        }
    }
}
