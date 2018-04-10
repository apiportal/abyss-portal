package com.verapi.portal;

//import io.reactivex.Single;
//import io.reactivex.disposables.Disposable;

import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(InitVerticle.class);

    @Override
    public void start(Future<Void> startFuture) {

        vertx.rxDeployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setHa(true))
                .flatMap(id -> vertx.rxDeployVerticle(MailVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .subscribe(id -> {
                    logger.info(System.getProperty("abyss-jar.name") + " MainVerticle and MailVerticle deployVerticle completed...");
                    startFuture.complete();
                }, t -> {
                    logger.error("MainVerticle or MailVerticle deployVerticle failed..." + t);
                    startFuture.fail(t);
                });
    }
}
