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
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.OwaspCharacterEscapes;
import com.verapi.portal.oapi.AbstractApiController;
import com.verapi.portal.oapi.AbyssApiController;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.Json;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class OpenApiServerVerticle extends AbyssAbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(OpenApiServerVerticle.class);


    /**
     * This demonstrates how to configure a Spring Java Config application to encode JSON rather than simply escaping it as recommended by OWASP XSS cheat sheet.
     * Specifically it states JavaScript should be encoded as
     * "Except for alphanumeric characters, escape all characters with the \\uXXXX unicode escaping format (X = Integer)"
     */
    static {
        Json.mapper.getFactory().setCharacterEscapes(new OwaspCharacterEscapes());
        logger.debug("OwaspCharacterEscapes has been set");
    }


    @Override
    protected Single<HttpServer> createHttpServer() {
        logger.trace("createHttpServer() running");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setCompressionSupported(Config.getInstance().getConfigJsonObject().getBoolean(Constants.HTTP_OPENAPI_SERVER_ENABLE_COMPRESSION_SUPPORT))
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY))
                .setAcceptBacklog(1000000);
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .requestHandler(abyssRouter)
                //.requestHandler(verticleRouter::accept)
                .rxListen(serverPort, verticleHost);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("OpenApiServerVerticle.start invoked");

        super.setVerticleHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_OPENAPI_SERVER_HOST));
        super.setServerPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_OPENAPI_SERVER_PORT));
        super.setVerticleType(Constants.VERTICLE_TYPE_API);

        setAbyssJDBCService(new AbyssJDBCService(vertx));
        Disposable disposable
                =
                initializeJdbcClient(Constants.API_DATA_SOURCE_SERVICE)
                        .flatMap(jdbcClient1 -> createRouters())
                        .flatMap(this::enableCorsSupport)
                        .flatMap(abyssRouter -> configureRouter())
                        .flatMap(this::enableCorsSupport)
                        .flatMap(verticleRouter -> createHttpServer())
                        .subscribe(httpServer -> {
                            super.start(startFuture);
                            logger.trace("OpenApiServerVerticle httpServer started " + httpServer.toString());
                        }, t -> {
                            logger.error("OpenApiServerVerticle httpServer unable to start", t);
                            startFuture.fail(t);
                        });
    }

    Single<Router> enableCorsSupport(Router router) {
        logger.trace("enableCorsSupport() running");
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("Access-Control-Allow-Credentials");
        allowHeaders.add("origin");
        allowHeaders.add(HttpHeaders.CONTENT_TYPE.toString());
        allowHeaders.add("accept");
        allowHeaders.add("Cookie");
        // CORS support
        router.route().handler(CorsHandler.create("http(s)?:\\/\\/(.+\\.)?(192\\.168\\..*|apiportal\\.com|local\\.abyss\\.com|localhost)(:\\d{1,5})?$")
                .allowCredentials(true)
                .allowedHeaders(allowHeaders)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.PUT)
                .allowedMethod(HttpMethod.OPTIONS)
        );
        return Single.just(router);
    }

    private Single<Router> configureRouter() {
        logger.trace("configureRouter() running");

        //create instances for each Api Controller annotated by @AbyssApiController
        new FastClasspathScanner("com.verapi")
                //.verbose()
                .matchClassesWithAnnotation(AbyssApiController.class, classWithAnnotation -> {
                    logger.trace("creating a new instance of {} which has an annonation of {}", classWithAnnotation.getName(), AbyssApiController.class.getName());
                    try {
                        AbstractApiController apiControllerInstance = (AbstractApiController) classWithAnnotation
                                .getConstructor(Vertx.class, Router.class, JDBCAuth.class)
                                .newInstance(vertx, abyssRouter, jdbcAuth);
                    } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        logger.error(e.getLocalizedMessage());
                        logger.error(Arrays.toString(e.getStackTrace()));
                    }
                })
                .scan();

        return Single.just(verticleRouter);
    }

}
