package org.summer.registry.server;

public class RegistryServerConfig {
    private String host;
    private int port;
    //是否master
    private boolean master;
    //和其他slave节点同步信息的端口
    private int syncPort;

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public void setMaster(boolean master) {
        this.master = master;
    }

    public boolean isMaster() {
        return master;
    }

    public void setSyncPort(int syncPort) {
        this.syncPort = syncPort;
    }

    public int getSyncPort() {
        return syncPort;
    }

}
