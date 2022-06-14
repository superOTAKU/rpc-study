package org.summer.registry.server;

import org.summer.registry.model.HostInfo;
import org.summer.registry.model.ServiceInfo;
import org.summer.registry.model.ServiceKey;
import org.summer.registry.model.ServiceRegister;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ServiceRegistry {
    private final Lock serviceMapLock = new ReentrantLock();
    private final ConcurrentMap<ServiceKey, Set<HostInfo>> serviceMap = new ConcurrentHashMap<>();
    //service列表，每隔几秒从serviceMap构造，避免对serviceMap的重复访问
    private volatile List<ServiceInfo> serviceListCache = new ArrayList<>();
    private final ScheduledExecutorService scheduledService = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        }
    });

    public void start() {
        scheduledService.scheduleAtFixedRate(this::loadServiceList, 5, 5, TimeUnit.SECONDS);
    }

    /**
     * 注册一个远程服务
     */
    public void registerService(ServiceRegister service) {
        try {
            if (serviceMapLock.tryLock(3, TimeUnit.SECONDS)) {
                try {
                    ServiceKey key = service.toKey();
                    HostInfo host = service.toHost();
                    Set<HostInfo> hostList = serviceMap.computeIfAbsent(key, k -> new CopyOnWriteArraySet<>());
                    hostList.add(host);
                } finally {
                    serviceMapLock.unlock();
                }
            } else {
                throw new RuntimeException("lock timeout");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void loadServiceList() {
        List<ServiceInfo> serviceList = new ArrayList<>();
        for (var entry : serviceMap.entrySet()) {
            ServiceKey key = entry.getKey();
            ServiceInfo serviceInfo = new ServiceInfo();
            serviceInfo.setName(key.getName());
            serviceInfo.setGroup(key.getGroup());
            serviceInfo.setVersion(key.getVersion());
            serviceInfo.setHosts(new ArrayList<>(entry.getValue()));
            serviceList.add(serviceInfo);
        }
        this.serviceListCache = serviceList;
    }

    /**
     * 获取所有服务信息
     */
    public List<ServiceInfo> getServices() {
        return serviceListCache;
    }

}
