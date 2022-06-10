package org.summer.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
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

}
