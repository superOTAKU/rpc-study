package org.summer.rpc.netty;

import io.netty.channel.Channel;
import io.netty.handler.timeout.IdleState;

public interface ChannelEventListener {

    void onConnected(Channel channel);

    void onClosed(Channel channel);

    void onIdle(Channel channel, IdleState idleState);

    void onException(Channel channel, Throwable ex);

}
