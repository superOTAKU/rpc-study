package org.summer.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import org.summer.rpc.protocol.RemoteCommand;

public interface NettyRequestProcessor {

    RemoteCommand processRequest(ChannelHandlerContext ctx, RemoteCommand request);

}
