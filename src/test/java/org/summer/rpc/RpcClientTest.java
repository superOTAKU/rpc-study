package org.summer.rpc;

import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summer.rpc.netty.NettyClientConfig;
import org.summer.rpc.netty.NettyRequestProcessor;
import org.summer.rpc.netty.NettyRpcClient;
import org.summer.rpc.protocol.RemoteCommand;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class RpcClientTest {

    public static class RequestCode {
        public static final int SAY_HELLO = 1;
    }

    public static class DefaultProcessor implements NettyRequestProcessor {
        private static final Logger LOGGER = LoggerFactory.getLogger(DefaultProcessor.class);
        @Override
        public RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request) {
            LOGGER.info("request: {}, {}", request, request.getBodyUtf8());
            RemoteCommand response = RemoteCommand.newResponse();
            response.setJsonBody("Hello Server");
            return response;
        }
    }

    public static void main(String[] args) throws Exception {
        NettyClientConfig config = new NettyClientConfig();
        config.setWorkerThreadCount(4);
        config.setCallbackThreadCount(4);
        NettyRpcClient client = new NettyRpcClient(config);
        client.registerDefaultProcessor(new DefaultProcessor(), Executors.newFixedThreadPool(4));
        client.start();
        RemoteCommand request = RemoteCommand.newJsonRequest(RequestCode.SAY_HELLO, "hello server");
        client.invokeAsync(new InetSocketAddress("localhost", 8888), request, f -> {
            System.out.println("response: " + f.getResponseNow() + ", " + f.getResponseNow().getBodyUtf8());
        });
        Thread.sleep(3000);
        client.shutdown();
    }

}
