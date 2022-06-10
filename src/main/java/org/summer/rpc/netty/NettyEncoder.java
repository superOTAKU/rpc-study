package org.summer.rpc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.summer.rpc.protocol.RemoteCommand;

public class NettyEncoder extends MessageToByteEncoder<RemoteCommand> {
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, RemoteCommand remoteCommand, ByteBuf byteBuf) {
        RemoteCommand.encode(byteBuf, remoteCommand);
    }
}
