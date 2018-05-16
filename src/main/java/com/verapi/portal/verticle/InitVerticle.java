/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.portal.verticle;

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

        vertx.rxDeployVerticle(PortalVerticle.class.getName(), new DeploymentOptions().setHa(true))
                .flatMap(id -> vertx.rxDeployVerticle(MailVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .flatMap(id -> vertx.rxDeployVerticle(ApiHttpServerVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                //.flatMap(id -> vertx.rxDeployVerticle(ApiBusServerVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .flatMap(id -> vertx.rxDeployVerticle(OpenApiServerVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .subscribe(id -> {
                    logger.info(System.getProperty("abyss-jar.name") + " InitVerticle : All verticles successfully deployed");
                    super.start(startFuture);
                }, t -> {
                    logger.error(System.getProperty("abyss-jar.name") + " InitVerticle : Deploying verticles failed " + t);
                    startFuture.fail(t);
                });
    }
}
