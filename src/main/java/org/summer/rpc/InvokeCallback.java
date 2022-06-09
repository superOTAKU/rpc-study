package org.summer.rpc;

import org.summer.rpc.netty.ResponseFuture;

public interface InvokeCallback {

    void onCompleted(ResponseFuture future);

}
