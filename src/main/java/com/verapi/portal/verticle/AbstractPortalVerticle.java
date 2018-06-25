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


import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.controller.Controllers;
import com.verapi.portal.controller.IController;
import com.verapi.portal.controller.PortalAbstractController;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.net.PemKeyCertOptions;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.ResponseTimeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractPortalVerticle extends AbyssAbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(AbstractPortalVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("AbstractPortalVerticle.start invoked");
        setAbyssJDBCService(new AbyssJDBCService(vertx));
        Disposable disposable
                =
                initializeJdbcClient(Constants.PORTAL_DATA_SOURCE_SERVICE)
                        .flatMap(jdbcClient1 -> createRouters())
                        .flatMap(this::enableCorsSupport)
                        .flatMap(abyssRouter -> configureRouter())
                        .flatMap(this::enableCorsSupport)
                        .flatMap(verticleRouter -> createHttpServer())
                        .subscribe(httpServer -> {
                            super.start(startFuture);
                            logger.trace("AbstractPortalVerticle httpServer started " + httpServer.toString());
                        }, t -> {
                            logger.error("AbstractPortalVerticle httpServer unable to start", t);
                            startFuture.fail(t);
                        });
    }

    private Single<Router> configureRouter() {
        logger.trace("AbstractPortalVerticle.configureRouter() invoked");
        verticleRouter.route().handler(ResponseTimeHandler.create());

        verticleRouter.route("/logout").handler(context -> {
            context.session().remove("user.uuid");
            context.remove("username");
            context.user().clearCache();
            context.clearUser();


            logger.trace("Cookie list before logout:");
            for (Cookie c : context.cookies()) {
                logger.debug(c.getName() + ":" + c.getValue());
            }

            context.removeCookie("abyss.principal.uuid");
            context.removeCookie("abyss.session");
            context.removeCookie("abyss_principal"); //TODO: Bunu kim koyuyor?

            logger.trace("Cookie list after logout:");
            for (Cookie c : context.cookies()) {
                logger.debug(c.getName() + ":" + c.getValue());
            }

            context.response().putHeader("location", Constants.ABYSS_ROOT + "/index").setStatusCode(302).end();
            //todo: use redirect method
        });

        verticleRouter.route("/").handler(context -> {
            context.response().putHeader("location", Constants.ABYSS_ROOT + "/index").setStatusCode(302).end();
            //todo: use redirect method
        });

        //lastly mount all controllers
        mountControllerRouters();
        abyssRouter.mountSubRouter(Constants.ABYSS_ROOT, verticleRouter);

        abyssRouter.route().handler(ctx -> {
            logger.trace("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
            ctx.fail(404);
        });

        logger.trace("verticle routers : " + verticleRouter.getRoutes().toString());
        logger.trace("abyss routers : " + abyssRouter.getRoutes().toString());

        return Single.just(verticleRouter);
    }

    //protected <T extends PortalAbstractController> void mountControllerRouter(JDBCAuth jdbcAuth, Controllers.ControllerDef controllerDef, IController<T> requestHandler) throws IllegalAccessException, InstantiationException {
    void mountControllerRouter(JDBCAuth jdbcAuth, Controllers.ControllerDef controllerDef) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        logger.trace("AbstractPortalVerticle.mountControllerRouter invoked : " + controllerDef.className.getName());
        IController<PortalAbstractController> requestHandlerInstance = (IController<PortalAbstractController>) controllerDef.className.getConstructor(JDBCAuth.class, JDBCClient.class).newInstance(jdbcAuth, jdbcClient);
        if (!controllerDef.isPublic)
            verticleRouter.route("/" + controllerDef.routePathGET).handler(authHandler).failureHandler(this::failureHandler);
        verticleRouter.route(HttpMethod.GET, "/" + controllerDef.routePathGET).handler(requestHandlerInstance::defaultGetHandler).failureHandler(this::failureHandler);
        verticleRouter.route(HttpMethod.POST, "/" + controllerDef.routePathPOST).handler(requestHandlerInstance).failureHandler(this::failureHandler);
    }

    protected abstract void mountControllerRouters();

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        logger.trace("AbstractPortalVerticle.stop invoked");
        super.stop(stopFuture);
    }

    protected Single<HttpServer> createHttpServer() {
        logger.trace("createHttpServer() running");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
/*
                // to enable http/2 support; setSSL true, set pem key cert, set use alpn true
                .setSsl(true)
                .setPemKeyCertOptions(new PemKeyCertOptions().setCertPath("tls/server-cert.pem").setKeyPath("tls/server-key.pem"))
                .setUseAlpn(true)
*/
                .setCompressionSupported(Config.getInstance().getConfigJsonObject().getBoolean(Constants.HTTP_SERVER_ENABLE_COMPRESSION_SUPPORT))
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY));
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .requestHandler(abyssRouter::accept)
                .rxListen(serverPort, verticleHost);
    }

}
