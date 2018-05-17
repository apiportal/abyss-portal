/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.verticle;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.oapi.AbstractApiController;
import com.verapi.portal.oapi.AbyssApiController;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
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

    @Override
    protected Single<HttpServer> createHttpServer() {
        logger.info("createHttpServer() running");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setCompressionSupported(true)
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY));
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .requestHandler(abyssRouter::accept)
                //.requestHandler(verticleRouter::accept)
                .rxListen(serverPort, verticleHost);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("OpenApiServerVerticle.start invoked");

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
                            logger.info("OpenApiServerVerticle httpServer started " + httpServer.toString());
                        }, t -> {
                            logger.error("OpenApiServerVerticle httpServer unable to start", t);
                            startFuture.fail(t);
                        });
    }

    Single<Router> enableCorsSupport(Router router) {
        logger.info("enableCorsSupport() running");
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("Access-Control-Allow-Credentials");
        allowHeaders.add("origin");
        allowHeaders.add("Vary : Origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");
        allowHeaders.add("Cookie");
        // CORS support
        router.route().handler(CorsHandler.create("http(s)?:\\/\\/(.+\\.)?(192\\.168\\..*|apiportal\\.com)(:\\d{1,5})?$")
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
        logger.info("configureRouter() running");

        //create instances for each Api Controller annotated by @AbyssApiController
        new FastClasspathScanner("com.verapi")
                //.verbose()
                .matchClassesWithAnnotation(AbyssApiController.class, classWithAnnotation -> {
                    logger.info("AbyssApiController annotated class found and mounted : " + classWithAnnotation);
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
