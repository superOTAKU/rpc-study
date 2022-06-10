package org.summer.rpc.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summer.rpc.InvokeCallback;
import org.summer.rpc.RpcClient;
import org.summer.rpc.protocol.RemoteCommand;

import java.net.InetSocketAddress;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class NettyRpcClient extends AbstractNettyRpcService implements RpcClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcClient.class);

    private final NettyClientConfig config;
    private Bootstrap bootstrap;
    private final ConcurrentMap<InetSocketAddress, ChannelWrapper> channelMap = new ConcurrentHashMap<>();
    private final Lock channelMapLock = new ReentrantLock();
    private NioEventLoopGroup bossGroup;
    private DefaultEventExecutorGroup eventExecutorGroup;

    public NettyRpcClient(NettyClientConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        Objects.requireNonNull(defaultProcessorPair, "default process pair can't be null");
        bossGroup = new NioEventLoopGroup();
        eventExecutorGroup = new DefaultEventExecutorGroup(config.getWorkerThreadCount());
        callbackExecutor = Executors.newFixedThreadPool(config.getCallbackThreadCount());
        bootstrap = new Bootstrap();
        bootstrap.group(bossGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.SO_KEEPALIVE, false)
                .handler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(eventExecutorGroup, new NettyDecoder(), new NettyEncoder(),
                                new NettyChannelEventHandler(), new NettyClientHandler());
                    }
                });
    }

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        eventExecutorGroup.shutdownGracefully();
        callbackExecutor.shutdown();
        for (var pair : requestProcessorMap.values()) {
            pair.getExecutor().shutdown();
        }
        if (defaultProcessorPair != null) {
            defaultProcessorPair.getExecutor().shutdown();
        }
        for (var cw : channelMap.values()) {
            cw.closeChannel();
        }
    }

    @Override
    public void invokeAsync(InetSocketAddress remoteAddr, RemoteCommand request, InvokeCallback callback) {
        doInvokeAsync(Objects.requireNonNull(getOrCreateChannel(remoteAddr)), request, callback);
    }

    @Override
    public RemoteCommand invokeSync(InetSocketAddress remoteAddr, RemoteCommand request) {
        return doInvokeAsync(Objects.requireNonNull(getOrCreateChannel(remoteAddr)), request, null).awaitResponse(30, TimeUnit.SECONDS);
    }

    private Channel getOrCreateChannel(InetSocketAddress remoteAddr) {
        ChannelWrapper cw = channelMap.get(remoteAddr);
        if (cw != null && cw.isOk()) {
            return cw.channel();
        }
        return doCreateChannel(remoteAddr);
    }

    private Channel doCreateChannel(InetSocketAddress remoteAddr) {
        try {
            if (channelMapLock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    ChannelWrapper cw = channelMap.get(remoteAddr);
                    boolean needCreate = false;
                    boolean needRemove = false;
                    if (cw == null) {
                        needCreate = true;
                    } else if (!cw.isOk()) {
                        needCreate = true;
                        needRemove = true;
                    } else if (!cw.isDone()) {
                        if (!cw.awaitDone(3, TimeUnit.SECONDS)) {
                            needRemove = true;
                            needCreate = true;
                        } else if (!cw.isOk()) {
                            needRemove = true;
                            needCreate = true;
                        }
                    }
                    if (needRemove) {
                        channelMap.remove(remoteAddr);
                        cw.closeChannel();
                    }
                    if (needCreate) {
                        ChannelFuture channelFuture = bootstrap.connect(remoteAddr);
                        cw = new ChannelWrapper(channelFuture);
                        channelMap.put(remoteAddr, cw);
                        if (!cw.awaitDone(3, TimeUnit.SECONDS) || !cw.isOk()) {
                            channelMap.remove(remoteAddr, cw);
                            cw.closeChannel();
                            return null;
                        }
                    }
                    return cw.channel();
                } finally {
                    channelMapLock.unlock();
                }
            } else {
                LOGGER.warn("create channel get lock timeout");
                return null;
            }
        } catch (InterruptedException e) {
            LOGGER.warn("create channel interrupted", e);
            return null;
        }
    }

    static class ChannelWrapper {
        private final ChannelFuture channelFuture;

        ChannelWrapper(ChannelFuture channelFuture) {
            this.channelFuture = channelFuture;
        }

        boolean isOk() {
            return channelFuture.isSuccess() && channelFuture.channel().isActive();
        }

        boolean isDone() {
            return channelFuture.isDone();
        }

        Channel channel() {
            return channelFuture.channel();
        }

        boolean awaitDone(long timeout, TimeUnit unit) {
            return channelFuture.awaitUninterruptibly(timeout, unit);
        }

        void closeChannel() {
            channelFuture.channel().close();
        }

    }

    @ChannelHandler.Sharable
    class NettyClientHandler extends SimpleChannelInboundHandler<RemoteCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception {
            handleReceivedCommand(ctx, msg);
        }
    }
}
