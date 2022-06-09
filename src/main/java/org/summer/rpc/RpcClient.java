package org.summer.rpc;

import org.summer.rpc.protocol.RemoteCommand;

import java.net.InetSocketAddress;

public interface RpcClient extends RpcService {

    /**
     * 异步调用远程服务
     */
    void invokeAsync(InetSocketAddress remoteAddr, RemoteCommand request, InvokeCallback callback);

    /**
     * 同步调用远程服务
     */
    RemoteCommand invokeSync(InetSocketAddress remoteAddr, RemoteCommand request);

}
