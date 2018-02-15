package com.verapi.portal;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(InitVerticle.class);

    @Override
    public void start(Future<Void> start) {

        vertx.deployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setHa(true), res -> {
            if (res.succeeded()) {
                logger.info(System.getProperty("abyss-jar.name")+" MainVerticle deployVerticle completed..." + res.succeeded());
                start.complete();
            } else {
                logger.error("MainVerticle deployVerticle failed..." + res.cause());
                start.fail(res.cause());
            }
        });
    }

    ;
}
