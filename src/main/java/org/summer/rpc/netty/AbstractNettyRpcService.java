package org.summer.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summer.rpc.InvokeCallback;
import org.summer.rpc.ProcessorPair;
import org.summer.rpc.RpcService;
import org.summer.rpc.protocol.RemoteCommand;
import org.summer.rpc.protocol.RemoteCommandType;
import org.summer.rpc.protocol.SystemResponseCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractNettyRpcService implements RpcService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNettyRpcService.class);

    protected ProcessorPair defaultProcessorPair;
    protected final Map<Integer, ProcessorPair> requestProcessorMap = new HashMap<>();
    protected final ConcurrentMap<Integer, ResponseFuture> responseFutureMap = new ConcurrentHashMap<>();
    protected ExecutorService callbackExecutor;
    protected final AtomicInteger requestId = new AtomicInteger(0);
    //负责网络连接事件处理
    protected ChannelEventListener channelEventListener;
    protected ExecutorService channelEventExecutor;

    @Override
    public void registerProcessor(int requestCode, NettyRequestProcessor processor, ExecutorService executor) {
        requestProcessorMap.put(requestCode, new ProcessorPair(processor, executor));
    }

    @Override
    public void registerDefaultProcessor(NettyRequestProcessor processor, ExecutorService executor) {
        defaultProcessorPair = new ProcessorPair(processor, executor);
    }

    @Override
    public ProcessorPair getDefaultProcessorPair() {
        return defaultProcessorPair;
    }

    @Override
    public ExecutorService getCallbackExecutor() {
        return callbackExecutor;
    }

    @Override
    public void setCallbackExecutor(ExecutorService callbackExecutor) {
        this.callbackExecutor = callbackExecutor;
    }

    protected void handleReceivedCommand(ChannelHandlerContext ctx, RemoteCommand command) {
        if (command.getType() == RemoteCommandType.REQUEST) {
            handleRequestCommand(ctx, command);
        } else {
            handleResponseCommand(ctx, command);
        }
    }

    /**
     * 分发请求到具体业务线程池
     */
    protected void handleRequestCommand(ChannelHandlerContext ctx, RemoteCommand request) {
        ProcessorPair pair = Optional.ofNullable(requestProcessorMap.get(request.getCode())).orElse(defaultProcessorPair);
        if (pair == null) {
            ctx.writeAndFlush(RemoteCommand.newResponse(SystemResponseCode.REQUEST_CODE_NOT_SUPPORTED));
            return;
        }
        pair.getExecutor().submit(() -> {
            RemoteCommand response = pair.getProcessor().processRequest(ctx, request);
            if (request.isOneWay()) {
                return;
            }
            response.setRequestId(request.getRequestId());
            ctx.writeAndFlush(response);
        });
    }

    /**
     * 处理返回的响应
     */
    protected void handleResponseCommand(ChannelHandlerContext ctx, RemoteCommand response) {
        ResponseFuture responseFuture = responseFutureMap.remove(response.getRequestId());
        if (responseFuture == null) {
            return;
        }
        responseFuture.completeFuture(response);
        responseFuture.executeCallback();
    }

    protected ResponseFuture doInvokeAsync(Channel channel, RemoteCommand request, InvokeCallback callback) {
        request.setRequestId(requestId.incrementAndGet());
        ResponseFuture rf = new ResponseFuture(request, channel, callback, callbackExecutor);
        responseFutureMap.put(request.getRequestId(), rf);
        channel.writeAndFlush(request).addListener(future -> {
            ResponseFuture responseFuture = responseFutureMap.get(request.getRequestId());
            if (responseFuture == null) {
                LOGGER.warn("send request[{}] success, no future found", request.getRequestId());
                return;
            }
            if (future.isSuccess()) {
                responseFuture.setSendRequestOk(true);
            } else {
                responseFuture.completeFuture(null);
                responseFuture.executeCallback();
                responseFutureMap.remove(request.getRequestId());
            }
        });
        return rf;
    }

    public ChannelEventListener getChannelEventListener() {
        return channelEventListener;
    }

    public void setChannelEventListener(ChannelEventListener channelEventListener) {
        this.channelEventListener = channelEventListener;
    }

    public ExecutorService getChannelEventExecutor() {
        return channelEventExecutor;
    }

    public void setChannelEventExecutor(ExecutorService channelEventExecutor) {
        this.channelEventExecutor = channelEventExecutor;
    }

    @ChannelHandler.Sharable
    class NettyChannelEventHandler extends ChannelDuplexHandler {

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            LOGGER.error("channel {} exception", ctx.channel(), cause);
            if (getChannelEventExecutor() != null && getChannelEventListener() != null) {
                getChannelEventExecutor().execute(() -> getChannelEventListener().onException(ctx.channel(), cause));
            }
            //一旦发送异常，这个channel必须被close
            ctx.channel().close();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            super.channelInactive(ctx);
            LOGGER.info("channel {} inactive", ctx.channel());
            if (getChannelEventExecutor() != null && getChannelEventListener() != null) {
                getChannelEventExecutor().execute(() -> getChannelEventListener().onClosed(ctx.channel()));
            }
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            super.channelActive(ctx);
            LOGGER.info("channel {} active", ctx.channel());
            if (getChannelEventExecutor() != null && getChannelEventListener() != null) {
                getChannelEventExecutor().execute(() -> getChannelEventListener().onConnected(ctx.channel()));
            }
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            super.userEventTriggered(ctx, evt);
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
                LOGGER.info("channel {} idle {}", ctx.channel(), idleStateEvent.state());
                if (getChannelEventExecutor() != null && getChannelEventListener() != null) {
                    getChannelEventExecutor().execute(() -> getChannelEventListener().onIdle(ctx.channel(), idleStateEvent.state()));
                }
            }
        }

    }

}
