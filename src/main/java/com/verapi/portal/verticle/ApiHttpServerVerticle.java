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

import com.verapi.portal.api.RestRouterHttpServerRequest;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.shareddata.LocalMap;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.ResponseTimeHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

public class ApiHttpServerVerticle extends AbyssAbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ApiHttpServerVerticle.class);
    private VertxRequestHandler vertxRequestHandler;

    @Override
    protected Single<HttpServer> createHttpServer() {
        logger.info("createHttpServer() running");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setCompressionSupported(true)
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY));
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .requestHandler(abyssRouter::accept)
                .rxListen(serverPort, verticleHost);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("ApiHttpServerVerticle.start invoked");
        super.setVerticleHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_API_SERVER_HOST));
        super.setServerPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_API_SERVER_PORT));

        // Build the Jax-RS controller deployment
        //ResteasyProviderFactory.pushContext(Vertx.class, io.vertx.reactivex.core.Vertx.newInstance(vertx));
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();
        //deployment.getRegistry().addPerInstanceResource(ApiController.class);
        //deployment.getRegistry().addPerInstanceResource(SubjectController.class);
        new FastClasspathScanner("com.verapi")
                //.verbose()
                .matchClassesWithAnnotation(Path.class, new ClassAnnotationMatchProcessor() {
                    @Override
                    public void processMatch(Class<?> classWithAnnotation) {
                        logger.info("Path annotated class found : " + classWithAnnotation);
                        deployment.getRegistry().addPerInstanceResource(classWithAnnotation);
                    }
                })
                .scan();
        //deployment.getRegistry().addPerRequestResource(ResteasyWadlDefaultResource.class);
        //ResteasyWadlDefaultResource.getServices().put("/", ResteasyWadlGenerator.generateServiceRegistry(deployment));
        vertxRequestHandler = new VertxRequestHandler(vertx.getDelegate(), deployment);

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
                            logger.info("ApiHttpServerVerticle httpServer started " + httpServer.toString());
                        }, t -> {
                            logger.error("ApiHttpServerVerticle httpServer unable to start", t);
                            startFuture.fail(t);
                        });

    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        logger.info("ApiHttpServerVerticle.stop invoked");
        super.stop(stopFuture);
    }

    private Single<Router> configureRouter() {
        verticleRouter.route("/api/*").handler(authHandler).failureHandler(this::failureHandler);

        verticleRouter.route().handler(ResponseTimeHandler.create());

        verticleRouter.route("/api/*").handler(context -> {
            vertxRequestHandler.handle(new RestRouterHttpServerRequest(context.getDelegate()));
        });

        abyssRouter.mountSubRouter(Constants.ABYSS_ROOT, verticleRouter);

        abyssRouter.route().handler(ctx -> {
            logger.info("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
            ctx.fail(404);
        });

        logger.trace("verticle routers : " + verticleRouter.getRoutes().toString());
        logger.trace("abyss routers : " + abyssRouter.getRoutes().toString());

        return Single.just(verticleRouter);
    }

}
