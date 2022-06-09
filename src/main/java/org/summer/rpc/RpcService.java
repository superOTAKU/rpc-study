package org.summer.rpc;

import org.summer.rpc.netty.NettyRequestProcessor;

import java.util.concurrent.ExecutorService;

public interface RpcService {

    void start();

    void shutdown();

    void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor);

    ProcessorPair getDefaultProcessorPair();

    void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor);

    void setCallbackExecutor(ExecutorService executor);

    ExecutorService getCallbackExecutor();

}
