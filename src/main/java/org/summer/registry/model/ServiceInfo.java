package org.summer.registry.model;

import java.util.List;

public class ServiceInfo {
    private String group;
    private String version;
    private String name;
    private List<HostInfo> hosts;

    public void setGroup(String group) {
        this.group = group;
    }

    public String getGroup() {
        return group;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getVersion() {
        return version;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setHosts(List<HostInfo> hosts) {
        this.hosts = hosts;
    }

    public List<HostInfo> getHosts() {
        return hosts;
    }
}
