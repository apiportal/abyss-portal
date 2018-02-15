package com.verapi.portal.common;

import io.vertx.core.Vertx;
import io.vertx.servicediscovery.ServiceDiscovery;
import io.vertx.servicediscovery.ServiceDiscoveryOptions;

public class AbyssServiceDiscovery {

    public static AbyssServiceDiscovery instance = null;
    private static ServiceDiscovery serviceDiscovery;
    private static Vertx vertx;

    private AbyssServiceDiscovery(Vertx vertx) {
        this.vertx = vertx;
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

}
