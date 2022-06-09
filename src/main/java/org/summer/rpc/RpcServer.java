package org.summer.rpc;

import io.netty.channel.Channel;
import org.summer.rpc.protocol.RemoteCommand;

public interface RpcServer extends RpcService {

    void invokeAsync(Channel channel, RemoteCommand request, InvokeCallback callback);

    RemoteCommand invokeSync(Channel channel, RemoteCommand request);

}
