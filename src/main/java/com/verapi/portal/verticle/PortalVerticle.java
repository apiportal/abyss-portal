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

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.controller.AbstractPortalController;
import com.verapi.portal.controller.AbyssController;
import com.verapi.portal.controller.IController;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class PortalVerticle extends AbstractPortalVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(PortalVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.trace("PortalVerticle.start invoked");
        super.setVerticleHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_SERVER_HOST));
        super.setServerPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_PORT));
        super.setVerticleType(Constants.VERTICLE_TYPE_PORTAL);
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        LOGGER.trace("PortalVerticle.stop invoked");
        super.stop(stopFuture);
    }

    @Override
    protected void mountControllerRouters() {
        LOGGER.trace("PortalVerticle.mountControllerRouters() invoked");
        new FastClasspathScanner("com.verapi") //TODO: refine scan spec to improve performance
                //.verbose()
                .matchClassesWithAnnotation(AbyssController.class, new ClassAnnotationMatchProcessor() {
                    @Override
                    public void processMatch(Class<?> classWithAnnotation) {
                        LOGGER.trace("AbyssController annotated class found and mounted : {}", classWithAnnotation);
                        IController<AbstractPortalController> requestHandlerInstance = null;
                        try {
                            requestHandlerInstance = (IController<AbstractPortalController>) classWithAnnotation
                                    .getConstructor(JDBCAuth.class, JDBCClient.class)
                                    .newInstance(jdbcAuth, jdbcClient);
                            if (!classWithAnnotation.getAnnotation(AbyssController.class).isPublic()) {
                                verticleRouter.route("/" + classWithAnnotation.getAnnotation(AbyssController.class).routePathGET())
                                        .handler(authHandler).failureHandler(PortalVerticle.super::failureHandler);
                            }
                            verticleRouter.route(HttpMethod.GET, "/" + classWithAnnotation.getAnnotation(AbyssController.class).routePathGET())
                                    .handler(requestHandlerInstance::defaultGetHandler).failureHandler(PortalVerticle.super::failureHandler);
                            verticleRouter.route(HttpMethod.POST, "/" + classWithAnnotation.getAnnotation(AbyssController.class).routePathPOST())
                                    .handler(requestHandlerInstance).failureHandler(PortalVerticle.super::failureHandler);

                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            LOGGER.error("PortalVerticle.mountControllerRouters() exception : {}\n{}", e.getLocalizedMessage(), e.getStackTrace());
                        }
                    }
                })
                .scan();
    }

}
