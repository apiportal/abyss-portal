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
import com.verapi.abyss.sql.builder.metadata.AbyssDatabaseMetadataDiscovery;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.controller.AbstractPortalController;
import com.verapi.portal.controller.Controllers;
import com.verapi.portal.controller.IController;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.handler.ResponseTimeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public abstract class AbstractPortalVerticle extends AbyssAbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPortalVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.trace("AbstractPortalVerticle.start invoked");
        setAbyssJDBCService(new AbyssJDBCService(vertx));
        Disposable disposable
                =
                initializeJdbcClient(Constants.PORTAL_DATA_SOURCE_SERVICE)
                        .flatMap(jdbcClient -> AbyssDatabaseMetadataDiscovery.getInstance().populateMetaData(jdbcClient))
                        .flatMap(jdbcClient1 -> createRouters())
                        .flatMap(this::enableCorsSupport)
                        .flatMap(abyssRouter -> configureRouter())
                        .flatMap(this::enableCorsSupport)
                        .flatMap(verticleRouter -> createHttpServer())
                        .subscribe(httpServer -> {
                            super.start(startFuture);
                            LOGGER.trace("AbstractPortalVerticle httpServer started " + httpServer.toString());
                        }, t -> {
                            LOGGER.error("AbstractPortalVerticle httpServer unable to start", t);
                            startFuture.fail(t);
                        });
    }

    private Single<Router> configureRouter() {
        LOGGER.trace("AbstractPortalVerticle.configureRouter() invoked");
        //verticleRouter.route().handler(LoggerHandler.create());
        verticleRouter.route().handler(ResponseTimeHandler.create());

        verticleRouter.route("/logout").handler(context -> {
            context.session().remove(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);
            context.session().remove(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME);
            context.remove("username"); //TODO: ??
            context.user().clearCache();
            context.clearUser();

            context.session().remove(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME);
            context.session().remove(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);

            LOGGER.trace("Cookie list before logout:");
            for (Cookie c : context.cookies()) {
                LOGGER.debug(c.getName() + ":" + c.getValue());
            }

            context.removeCookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME);
            context.removeCookie(Constants.AUTH_ABYSS_PORTAL_SESSION_COOKIE_NAME);
            context.removeCookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_COOKIE_NAME); //TODO: Bunu kim koyuyor?

            context.removeCookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME);
            context.removeCookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);

            LOGGER.trace("Cookie list after logout:");
            for (Cookie c : context.cookies()) {
                LOGGER.debug(c.getName() + ":" + c.getValue());
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
            LOGGER.trace("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
            ctx.fail(404);
        });

        LOGGER.trace("verticle routers : " + verticleRouter.getRoutes().toString());
        LOGGER.trace("abyss routers : " + abyssRouter.getRoutes().toString());

        return Single.just(verticleRouter);
    }

    //protected <T extends AbstractPortalController> void mountControllerRouter(JDBCAuth jdbcAuth, Controllers.ControllerDef controllerDef, IController<T> requestHandler) throws IllegalAccessException, InstantiationException {
    void mountControllerRouter(JDBCAuth jdbcAuth, Controllers.ControllerDef controllerDef) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        LOGGER.trace("AbstractPortalVerticle.mountControllerRouter invoked : " + controllerDef.className.getName());
        IController<AbstractPortalController> requestHandlerInstance = (IController<AbstractPortalController>) controllerDef.className.getConstructor(JDBCAuth.class, JDBCClient.class).newInstance(jdbcAuth, jdbcClient);
        if (!controllerDef.isPublic)
            verticleRouter.route("/" + controllerDef.routePathGET).handler(authHandler).failureHandler(this::failureHandler);
        verticleRouter.route(HttpMethod.GET, "/" + controllerDef.routePathGET).handler(requestHandlerInstance::defaultGetHandler).failureHandler(this::failureHandler);
        verticleRouter.route(HttpMethod.POST, "/" + controllerDef.routePathPOST).handler(requestHandlerInstance).failureHandler(this::failureHandler);
    }

    protected abstract void mountControllerRouters();

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        LOGGER.trace("AbstractPortalVerticle.stop invoked");
        super.stop(stopFuture);
    }

    protected Single<HttpServer> createHttpServer() {
        LOGGER.trace("createHttpServer() running");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
/*
                // to enable http/2 support; setSSL true, set pem key cert, set use alpn true
                .setSsl(true)
                .setPemKeyCertOptions(new PemKeyCertOptions().setCertPath("tls/server-cert.pem").setKeyPath("tls/server-key.pem"))
                .setUseAlpn(true)
*/
                .setCompressionSupported(Config.getInstance().getConfigJsonObject().getBoolean(Constants.HTTP_SERVER_ENABLE_COMPRESSION_SUPPORT))
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY))
                .setAcceptBacklog(1000000);
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> LOGGER.error(event.getLocalizedMessage(), event))
                .requestHandler(abyssRouter)
                .rxListen(serverPort, verticleHost);
    }

}
