package org.summer.rpc.netty;

import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.summer.rpc.InvokeCallback;
import org.summer.rpc.protocol.RemoteCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class ResponseFuture {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResponseFuture.class);

    private final RemoteCommand request;
    private final Channel channel;
    private final InvokeCallback callback;
    private final ExecutorService callbackExecutor;
    private volatile boolean sendRequestOk;
    private final CompletableFuture<RemoteCommand> future = new CompletableFuture<>();

    public ResponseFuture(RemoteCommand request, Channel channel, InvokeCallback callback, ExecutorService callbackExecutor) {
        this.request = request;
        this.channel = channel;
        this.callback = callback;
        this.callbackExecutor = callbackExecutor;
    }

    public void completeFuture(RemoteCommand response) {
        future.complete(response);
    }

    public void setSendRequestOk(boolean sendRequestOk) {
        this.sendRequestOk = sendRequestOk;
    }

    public boolean isSendRequestOk() {
        return sendRequestOk;
    }

    public void executeCallback() {
        if (callback != null) {
            callbackExecutor.execute(() -> callback.onCompleted(this));
        }
    }

    public RemoteCommand awaitResponse(long timeout, TimeUnit timeUnit) {
        RemoteCommand response = null;
        try {
            response = future.get(timeout, timeUnit);
        } catch (Exception e) {
            LOGGER.error("get response future error", e);
        }
        if (response != null) {
            return response;
        } else {
            throw new RuntimeException("rpc invoke error");
        }
    }

    public RemoteCommand getResponseNow() {
        return future.getNow(null);
    }

}
