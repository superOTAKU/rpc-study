package org.summer.extension.provider;

import org.summer.extension.spi.HelloService;

public class HelloServiceImpl implements HelloService {

    @Override
    public String sayHello(String name) {
        return "Hello, " + name;
    }

}
