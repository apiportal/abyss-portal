/*
 * Copyright 2019 Verapi Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verapi.portal.verticle;

//import io.reactivex.Single;
//import io.reactivex.disposables.Disposable;

import com.verapi.abyss.cassandra.impl.verticle.CassandraLoggerVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.reactivex.core.AbstractVerticle;
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
                .flatMap(id -> vertx.rxDeployVerticle(EchoServerVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .flatMap(id -> vertx.rxDeployVerticle(GatewayHttpServerVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .flatMap(id -> vertx.rxDeployVerticle(CassandraLoggerVerticle.class.getName(), new DeploymentOptions().setHa(true)))
                .subscribe(id -> {
                    logger.info(System.getProperty("abyss-jar.name") + " InitVerticle : All verticles successfully deployed");
                    super.start(startFuture);
                }, t -> {
                    logger.error(System.getProperty("abyss-jar.name") + " InitVerticle : Deploying verticles failed " + t);
                    startFuture.fail(t);
                });
    }
}
