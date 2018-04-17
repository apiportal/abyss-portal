package com.verapi.portal.common;

import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbyssServiceDiscovery implements AutoCloseable {

    private static Logger logger = LoggerFactory.getLogger(AbyssServiceDiscovery.class);

    private static AbyssServiceDiscovery instance = null;
    private static ServiceDiscovery serviceDiscovery;

    private AbyssServiceDiscovery(Vertx vertx) {
        serviceDiscovery = ServiceDiscovery.create(vertx,
                new ServiceDiscoveryOptions()
                        .setAnnounceAddress("abyss-services-announce")
                        .setName("abyss-service-discovery"));
    }

    public static AbyssServiceDiscovery getInstance(Vertx vertx) {
        if (instance == null)
            instance = new AbyssServiceDiscovery(vertx);
        return instance;
    }

    public ServiceDiscovery getServiceDiscovery() {
        return serviceDiscovery;
    }

    @Override
    public void close() throws Exception {
        if (instance != null) {
            instance.getServiceDiscovery().close();
        }
    }
}
