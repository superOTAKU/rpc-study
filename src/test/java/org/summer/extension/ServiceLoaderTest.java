package org.summer.extension;

import org.summer.extension.spi.HelloService;

import java.util.Optional;
import java.util.ServiceLoader;

public class ServiceLoaderTest {

    public static void main(String[] args) {
        //ServiceLoader提供机制加载SPI实现类
        ServiceLoader<HelloService> loader = ServiceLoader.load(HelloService.class);
        Optional<HelloService> service = loader.findFirst();
        if (service.isEmpty()) {
            throw new NullPointerException();
        }
        System.out.println(service.get().sayHello("Jack"));
    }

}
