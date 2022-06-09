package org.summer.rpc.protocol;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

/**
 * RPC命令对象
 */
public class RemoteCommand {
    private RemoteCommandType type;
    private Integer requestId;
    //对于request，是请求业务编码；对于response，是处理状态，或者称为错误码
    private Integer code;
    private String remark;
    private byte[] body;

    public void setType(RemoteCommandType type) {
        this.type = type;
    }

    public RemoteCommandType getType() {
        return type;
    }

    public void setRequestId(Integer requestId) {
        this.requestId = requestId;
    }

    public Integer getRequestId() {
        return requestId;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public Integer getCode() {
        return code;
    }

    public void setBody(byte[] body) {
        this.body = body;
    }

    public byte[] getBody() {
        return body;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    //便捷的编解码body方法

    public <T> T decodeJsonBody(Class<T> type) {
        return JSON.parseObject(new String(body, StandardCharsets.UTF_8), type);
    }

    public <T> T decodeJsonBody(TypeReference<T> typeReference) {
        return JSON.parseObject(new String(body, StandardCharsets.UTF_8), typeReference);
    }

    //便捷的工厂方法

    public static RemoteCommand newRequest(int code, Supplier<byte[]> bodySupplier) {
        return newRequest(code, bodySupplier);
    }

    public static RemoteCommand newRequest(int code, byte[] body) {
        RemoteCommand request = new RemoteCommand();
        request.setType(RemoteCommandType.REQUEST);
        request.setCode(code);
        request.setBody(body);
        return request;
    }

    public static RemoteCommand newResponse() {
        return newResponse(SystemResponseCode.SUCCESS);
    }

    public static RemoteCommand newResponse(int code) {
        RemoteCommand response = new RemoteCommand();
        response.setType(RemoteCommandType.RESPONSE);
        return response;
    }

}
