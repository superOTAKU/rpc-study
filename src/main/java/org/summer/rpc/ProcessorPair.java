package org.summer.rpc;

import org.summer.rpc.netty.NettyRequestProcessor;

import java.util.concurrent.ExecutorService;

public class ProcessorPair {
    private final NettyRequestProcessor processor;
    private final ExecutorService executor;

    public ProcessorPair(NettyRequestProcessor processor, ExecutorService executor) {
        this.processor = processor;
        this.executor = executor;
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public NettyRequestProcessor getProcessor() {
        return processor;
    }
}
