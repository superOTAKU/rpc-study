package org.summer.rpc.protocol;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import io.netty.buffer.ByteBuf;

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
    private boolean oneWay;
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

    public String getBodyUtf8() {
        return new String(body, StandardCharsets.UTF_8);
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public boolean isOneWay() {
        return oneWay;
    }

    public void setOneWay(boolean oneWay) {
        this.oneWay = oneWay;
    }

    @Override
    public String toString() {
        return "RemoteCommand{" +
                "type=" + type +
                ", requestId=" + requestId +
                ", code=" + code +
                ", remark='" + remark + '\'' +
                ", oneWay=" + oneWay +
                '}';
    }

    //便捷的编解码body方法

    public <T> T decodeJsonBody(Class<T> type) {
        return JSON.parseObject(new String(body, StandardCharsets.UTF_8), type);
    }

    public <T> T decodeJsonBody(TypeReference<T> typeReference) {
        return JSON.parseObject(new String(body, StandardCharsets.UTF_8), typeReference);
    }

    public void setJsonBody(Object body) {
        this.body = JSON.toJSONBytes(body);
    }

    //便捷的工厂方法

    public static RemoteCommand newRequest(int code, Supplier<byte[]> bodySupplier) {
        return newRequest(code, bodySupplier);
    }

    public static RemoteCommand newJsonRequest(int code, Object body) {
        return newRequest(code, JSON.toJSONBytes(body));
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
        response.setCode(code);
        return response;
    }

    //----------------------编解码--------------------------

    public static RemoteCommand decode(ByteBuf byteBuf) {
        RemoteCommand command = new RemoteCommand();
        int length = byteBuf.readInt();
        command.setType(RemoteCommandType.values()[byteBuf.readInt()]);
        command.setRequestId(byteBuf.readInt());
        command.setCode(byteBuf.readInt());
        int remarkLen = byteBuf.readInt();
        byte[] remark = new byte[remarkLen];
        byteBuf.readBytes(remark);
        command.setRemark(new String(remark, StandardCharsets.UTF_8));
        command.setOneWay(byteBuf.readBoolean());
        int bodyLen = length - 21 - remarkLen;
        byte[] body  = new byte[bodyLen];
        byteBuf.readBytes(body);
        command.setBody(body);
        return command;
    }

    public static void encode(ByteBuf byteBuf, RemoteCommand command) {
        byte[] commandBytes = command.getRemark() == null ? new byte[0] : command.getRemark().getBytes(StandardCharsets.UTF_8);
        int totalLen = 21 + commandBytes.length + command.getBody().length;
        byteBuf.writeInt(totalLen);
        byteBuf.writeInt(command.getType().ordinal());
        byteBuf.writeInt(command.getRequestId());
        byteBuf.writeInt(command.getCode());
        byteBuf.writeInt(commandBytes.length);
        byteBuf.writeBytes(commandBytes);
        byteBuf.writeBoolean(command.isOneWay());
        byteBuf.writeBytes(command.getBody());
    }

}
