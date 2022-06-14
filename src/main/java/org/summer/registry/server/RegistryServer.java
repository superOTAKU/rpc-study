package org.summer.registry.server;

import io.netty.channel.ChannelHandlerContext;
import org.summer.registry.model.RegistryRequestCode;
import org.summer.registry.model.ServiceInfo;
import org.summer.registry.model.ServiceRegister;
import org.summer.rpc.netty.NettyRequestProcessor;
import org.summer.rpc.netty.NettyRpcServer;
import org.summer.rpc.netty.NettyServerConfig;
import org.summer.rpc.protocol.RemoteCommand;
import org.summer.rpc.protocol.SystemResponseCode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * 注册中心server
 */
public class RegistryServer {
    final RegistryServerConfig config;
    private NettyRpcServer server;
    final ServiceRegistry serviceRegistry = new ServiceRegistry();
    private RegistrySyncServer syncServer;

    public RegistryServer(RegistryServerConfig config) {
        this.config = config;
    }

    public synchronized void start() {
        NettyServerConfig nettyServerConfig = new NettyServerConfig();
        nettyServerConfig.setBossThreadCount(1);
        nettyServerConfig.setWorkerThreadCount(4);
        nettyServerConfig.setEventThreadCount(1);
        nettyServerConfig.setCallbackThreadCount(1);
        nettyServerConfig.setBindAddress(new InetSocketAddress(config.getHost(), config.getPort()));
        server = new NettyRpcServer(nettyServerConfig);
        server.registerDefaultProcessor(new RegistryProcessor(), Executors.newFixedThreadPool(4));
        serviceRegistry.start();
        if (config.isMaster()) {
            syncServer = new RegistrySyncServer(this);
            syncServer.start();
        } else {
            //TODO sync client when not master
        }
    }

    public synchronized void shutdown() {
        server.shutdown();
        if (syncServer != null) {
            syncServer.shutdown();
        }
    }

    class RegistryProcessor implements NettyRequestProcessor {

        @Override
        public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request) {
            RemoteCommand response;
            switch (request.getCode()) {
                case RegistryRequestCode.SERVICE_REGISTER:
                    ServiceRegister serviceRegister = request.decodeJsonBody(ServiceRegister.class);
                    serviceRegistry.registerService(serviceRegister);
                    response = RemoteCommand.newResponse();
                    break;
                case RegistryRequestCode.GET_SERVICE_LIST:
                    List<ServiceInfo> serviceList = serviceRegistry.getServices();
                    response = RemoteCommand.newResponse();
                    response.setJsonBody(serviceList);
                    break;
                default:
                    response = RemoteCommand.newResponse(SystemResponseCode.REQUEST_CODE_NOT_SUPPORTED);
            }
            return response;
        }
    }

}
