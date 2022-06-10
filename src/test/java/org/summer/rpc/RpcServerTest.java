package org.summer.rpc;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summer.rpc.netty.ChannelEventListener;
import org.summer.rpc.netty.NettyRequestProcessor;
import org.summer.rpc.netty.NettyRpcServer;
import org.summer.rpc.netty.NettyServerConfig;
import org.summer.rpc.protocol.RemoteCommand;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class RpcServerTest {


    public static class DefaultProcessor implements NettyRequestProcessor {
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProcessor.class);
        @Override
        public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request) {
            LOGGER.info("request: {}, {}", request, request.getBodyUtf8());
            RemoteCommand response = RemoteCommand.newResponse();
            response.setJsonBody("Hello Client");
            return response;
        }
    }

    public static class DefaultChannelEventListener implements ChannelEventListener {

        @Override
        public void onConnected(Channel channel) {
            System.err.println("channel connected: " + channel);
        }

        @Override
        public void onClosed(Channel channel) {
            System.err.println("channel closed: " + channel);
        }

        @Override
        public void onIdle(Channel channel, IdleState idleState) {
            System.err.println("channel idle: " + channel);
        }

        @Override
        public void onException(Channel channel, Throwable ex) {
            System.err.println("channel exception: " + channel);
        }
    }


    public static void main(String[] args) {
        NettyServerConfig config = new NettyServerConfig();
        config.setBindAddress(new InetSocketAddress("0.0.0.0", 8888));
        config.setBossThreadCount(1);
        config.setWorkerThreadCount(4);
        config.setCallbackThreadCount(4);
        config.setEventThreadCount(4);
        NettyRpcServer server = new NettyRpcServer(config);
        server.setChannelEventListener(new DefaultChannelEventListener());
        server.setChannelEventExecutor(Executors.newSingleThreadExecutor());
        server.registerDefaultProcessor(new DefaultProcessor(), Executors.newFixedThreadPool(4));
        server.start();
    }

}
