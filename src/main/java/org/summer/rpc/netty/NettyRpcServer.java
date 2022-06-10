package org.summer.rpc.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summer.rpc.InvokeCallback;
import org.summer.rpc.RpcServer;
import org.summer.rpc.protocol.RemoteCommand;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NettyRpcServer extends AbstractNettyRpcService implements RpcServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyRpcServer.class);

    private final NettyServerConfig config;
    private NioEventLoopGroup bossGroup;
    private NioEventLoopGroup workerGroup;
    private DefaultEventExecutorGroup eventExecutorGroup;

    public NettyRpcServer(NettyServerConfig config) {
        this.config = config;
    }

    @Override
    public void start() {
        Objects.requireNonNull(defaultProcessorPair, "default process pair can't be null");
        callbackExecutor = Executors.newFixedThreadPool(config.getCallbackThreadCount());
        bossGroup = new NioEventLoopGroup(config.getBossThreadCount());
        workerGroup = new NioEventLoopGroup(config.getWorkerThreadCount());
        eventExecutorGroup = new DefaultEventExecutorGroup(config.getEventThreadCount());
        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childOption(ChannelOption.SO_KEEPALIVE, false)
                .childHandler(new ChannelInitializer<NioSocketChannel>() {
                    @Override
                    protected void initChannel(NioSocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(eventExecutorGroup, new NettyDecoder(), new NettyEncoder(),
                                new NettyChannelEventHandler(),
                                new NettyServerHandler());
                    }
                });
        bootstrap.bind(config.getBindAddress()).syncUninterruptibly();
        LOGGER.info("nettyServer started at {}", config.getBindAddress());
    }

    @Override
    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
        eventExecutorGroup.shutdownGracefully();
        callbackExecutor.shutdown();
        for (var pair : requestProcessorMap.values()) {
            pair.getExecutor().shutdown();
        }
        if (defaultProcessorPair != null) {
            defaultProcessorPair.getExecutor().shutdown();
        }
        if (channelEventExecutor != null) {
            channelEventExecutor.shutdown();
        }
    }

    @Override
    public void invokeAsync(Channel channel, RemoteCommand request, InvokeCallback callback) {
        doInvokeAsync(channel, request, callback);
    }

    @Override
    public RemoteCommand invokeSync(Channel channel, RemoteCommand request) {
        return doInvokeAsync(channel, request, null).awaitResponse(30, TimeUnit.SECONDS);
    }

    class NettyServerHandler extends SimpleChannelInboundHandler<RemoteCommand> {

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, RemoteCommand msg) throws Exception {
            handleReceivedCommand(ctx, msg);
        }
    }


}
