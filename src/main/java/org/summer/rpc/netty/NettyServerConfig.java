package org.summer.rpc.netty;

import java.net.InetSocketAddress;

public class NettyServerConfig {
    private Integer bossThreadCount;
    private Integer workerThreadCount;
    private Integer eventThreadCount;
    private InetSocketAddress bindAddress;
    private Integer callbackThreadCount;

    public Integer getBossThreadCount() {
        return bossThreadCount;
    }

    public void setBossThreadCount(Integer bossThreadCount) {
        this.bossThreadCount = bossThreadCount;
    }

    public Integer getWorkerThreadCount() {
        return workerThreadCount;
    }

    public void setWorkerThreadCount(Integer workerThreadCount) {
        this.workerThreadCount = workerThreadCount;
    }

    public Integer getEventThreadCount() {
        return eventThreadCount;
    }

    public void setEventThreadCount(Integer eventThreadCount) {
        this.eventThreadCount = eventThreadCount;
    }

    public InetSocketAddress getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(InetSocketAddress bindAddress) {
        this.bindAddress = bindAddress;
    }

    public Integer getCallbackThreadCount() {
        return callbackThreadCount;
    }

    public void setCallbackThreadCount(Integer callbackThreadCount) {
        this.callbackThreadCount = callbackThreadCount;
    }

}
