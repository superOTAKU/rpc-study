package org.summer.registry.server;


import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import org.summer.registry.model.ServiceInfo;
import org.summer.registry.model.SyncRequestCode;
import org.summer.rpc.netty.ChannelEventListener;
import org.summer.rpc.netty.NettyRequestProcessor;
import org.summer.rpc.netty.NettyRpcServer;
import org.summer.rpc.netty.NettyServerConfig;
import org.summer.rpc.protocol.RemoteCommand;
import org.summer.rpc.protocol.SystemResponseCode;
import org.summer.util.NamedThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.concurrent.*;

/**
 * 主从同步server
 */
public class RegistrySyncServer {
    private final RegistryServer registryServer;
    private NettyRpcServer server;
    private final ConcurrentMap<SocketAddress, Channel> channelMap = new ConcurrentHashMap<>();
    private ScheduledExecutorService scheduledService;
    public RegistrySyncServer(RegistryServer server) {
        this.registryServer = server;
    }

    public void start() {
        NettyServerConfig serverConfig = new NettyServerConfig();
        serverConfig.setBossThreadCount(1);
        serverConfig.setWorkerThreadCount(1);
        serverConfig.setEventThreadCount(1);
        serverConfig.setCallbackThreadCount(1);
        serverConfig.setBindAddress(new InetSocketAddress(registryServer.config.getHost(), registryServer.config.getSyncPort()));
        server = new NettyRpcServer(serverConfig);
        server.setChannelEventListener(new ChannelEventListenerImpl());
        server.registerDefaultProcessor(new SyncServerProcessor(), Executors.newFixedThreadPool(4));
        server.start();
        scheduledService = Executors.newSingleThreadScheduledExecutor(new NamedThreadFactory("syncScheduler", true));
        scheduledService.scheduleAtFixedRate(this::pushServiceList, 30, 30, TimeUnit.SECONDS);
    }

    public void shutdown() {
        server.shutdown();
    }

    private void pushServiceList() {
        List<ServiceInfo> services = registryServer.serviceRegistry.getServices();
        RemoteCommand request = RemoteCommand.newJsonRequest(SyncRequestCode.PUSH_SERVICE_LIST, services);
        request.setOneWay(true);
        for (var channel : channelMap.values()) {
            try {
                server.invokeAsync(channel, request, f -> {});
            } catch (Exception e) {
                //ignored
            }
        }
    }

    class ChannelEventListenerImpl implements ChannelEventListener {

        @Override
        public void onConnected(Channel channel) {
            channelMap.put(channel.remoteAddress(), channel);
        }

        @Override
        public void onClosed(Channel channel) {
            channelMap.remove(channel.remoteAddress());
        }

        @Override
        public void onIdle(Channel channel, IdleState idleState) {
            channelMap.remove(channel.remoteAddress());
            channel.close();
        }

        @Override
        public void onException(Channel channel, Throwable ex) {
            channelMap.remove(channel.remoteAddress());
        }
    }

    class SyncServerProcessor implements NettyRequestProcessor {

        @Override
        public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request) {
            RemoteCommand response;
            switch (request.getCode()) {
                case SyncRequestCode.HEARTBEAT:
                    response = RemoteCommand.newResponse();
                    break;
                default:
                    response = RemoteCommand.newResponse(SystemResponseCode.REQUEST_CODE_NOT_SUPPORTED);
                    break;
            }
            return response;
        }
    }

}
