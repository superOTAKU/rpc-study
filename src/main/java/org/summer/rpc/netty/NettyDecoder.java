package org.summer.rpc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.summer.rpc.protocol.RemoteCommand;

public class NettyDecoder extends LengthFieldBasedFrameDecoder {

    public NettyDecoder() {
        super(65535, 0, 4, -4, 0);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf buf = (ByteBuf)super.decode(ctx, in);
        if (buf == null) {
            return null;
        }
        return RemoteCommand.decode(buf);
    }
}
