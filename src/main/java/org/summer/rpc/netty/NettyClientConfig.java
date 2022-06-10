package org.summer.rpc.netty;

public class NettyClientConfig {
    private Integer workerThreadCount;
    private Integer callbackThreadCount;

    public Integer getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount(Integer workerThreadCount) {
        this.workerThreadCount = workerThreadCount;
    }

    public Integer getCallbackThreadCount() {
        return callbackThreadCount;
    }

    public void setCallbackThreadCount(Integer callbackThreadCount) {
        this.callbackThreadCount = callbackThreadCount;
    }

}
