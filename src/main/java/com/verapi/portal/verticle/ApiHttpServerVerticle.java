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

import com.verapi.portal.api.RestRouterHttpServerRequest;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.ResponseTimeHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxRequestHandler;
import org.jboss.resteasy.plugins.server.vertx.VertxResteasyDeployment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;

public class ApiHttpServerVerticle extends AbyssAbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ApiHttpServerVerticle.class);
    private VertxRequestHandler vertxRequestHandler;

    @Override
    protected Single<HttpServer> createHttpServer() {
        logger.trace("createHttpServer() running");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setCompressionSupported(Config.getInstance().getConfigJsonObject().getBoolean(Constants.HTTP_API_SERVER_ENABLE_COMPRESSION_SUPPORT))
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY))
                .setAcceptBacklog(1000000);
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .requestHandler(abyssRouter::accept)
                .rxListen(serverPort, verticleHost);
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("ApiHttpServerVerticle.start invoked");
        super.setVerticleHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_API_SERVER_HOST));
        super.setServerPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_API_SERVER_PORT));
        super.setVerticleType(Constants.VERTICLE_TYPE_API);

        // Build the Jax-RS controller deployment
        //ResteasyProviderFactory.pushContext(Vertx.class, io.vertx.reactivex.core.Vertx.newInstance(vertx));
        VertxResteasyDeployment deployment = new VertxResteasyDeployment();
        deployment.start();
        //deployment.getRegistry().addPerInstanceResource(ApiController.class);
        //deployment.getRegistry().addPerInstanceResource(SubjectController.class);
        //deployment.getRegistry().addPerRequestResource(io.swagger.v3.jaxrs2.integration.resources.OpenApiResource.class);
//        deployment.getRegistry().addPerRequestResource(io.swagger.jaxrs.listing.ApiListingResource.class);
//        deployment.getRegistry().addPerRequestResource(io.swagger.jaxrs.listing.SwaggerSerializers.class);
/*
        deployment.setProviderClasses(Lists.newArrayList("io.swagger.v3.jaxrs2.integration.resources.BaseOpenApiResource.class",
                "io.swagger.v3.jaxrs2.integration.resources.OpenApiResource.class"));
        deployment.setResourceClasses(Lists.newArrayList("io.swagger.v3.jaxrs2.integration.resources.OpenApiResource.class"));
*/
//        deployment.setProviderClasses(Lists.newArrayList(
//                "com.wordnik.swagger.jaxrs.listing.ResourceListingProvider",
//                "com.wordnik.swagger.jaxrs.listing.ApiDeclarationProvider"));


        new FastClasspathScanner("com.verapi")
                //.verbose()
                .matchClassesWithAnnotation(Path.class, new ClassAnnotationMatchProcessor() {
                    @Override
                    public void processMatch(Class<?> classWithAnnotation) {
                        logger.trace("Path annotated class found : " + classWithAnnotation);
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
                            logger.trace("ApiHttpServerVerticle httpServer started " + httpServer.toString());
                        }, t -> {
                            logger.trace("ApiHttpServerVerticle httpServer unable to start", t);
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
