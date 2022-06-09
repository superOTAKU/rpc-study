package org.summer.rpc.netty;

import io.netty.channel.Channel;
import org.summer.rpc.InvokeCallback;
import org.summer.rpc.protocol.RemoteCommand;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

public class ResponseFuture {
    private final RemoteCommand request;
    private final Channel channel;
    private final InvokeCallback callback;
    private final ExecutorService callbackExecutor;
    private volatile boolean sendRequestOk;
    private CompletableFuture<RemoteCommand> future = new CompletableFuture<>();

    public ResponseFuture(RemoteCommand request, Channel channel, InvokeCallback callback, ExecutorService callbackExecutor) {
        this.request = request;
        this.channel = channel;
        this.callback = callback;
        this.callbackExecutor = callbackExecutor;
    }

    public void completeFuture(RemoteCommand response) {
        future.complete(response);
    }

    public void completeFutureExceptionally(Throwable e) {
        future.completeExceptionally(e);
    }

    public Channel getChannel() {
        return channel;
    }

    public InvokeCallback getCallback() {
        return callback;
    }

    public void setSendRequestOk(boolean sendRequestOk) {
        this.sendRequestOk = sendRequestOk;
    }

    public void executeCallback() {
        callbackExecutor.execute(() -> callback.onCompleted(this));
    }

}
