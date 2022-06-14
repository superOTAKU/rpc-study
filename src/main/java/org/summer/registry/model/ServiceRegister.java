package org.summer.registry.model;

/**
 * 一个远程服务的元数据信息
 */
public class ServiceRegister {
    private String host;
    private int port;
    private String name;
    private String group;
    private String version;

    public ServiceKey toKey() {
        return null;
    }

    public HostInfo toHost() {
        return null;
    }

}
