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
import com.verapi.portal.common.BuildProperties;
import com.verapi.portal.controller.FailurePortalController;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.servicediscovery.Record;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public abstract class AbyssAbstractVerticle extends AbstractVerticle {

    private static final int ONE_MINUTE_IS_60_SECS = 60;
    private static final int ONE_SEC_IS_1000_MILLISECS = 1000;
    private static final Logger LOGGER = LoggerFactory.getLogger(AbyssAbstractVerticle.class);
    private static final String IS_PUT_IN_CONTEXT_SESSION = "{} is put in context session: {}";
    //####### static #######
    static Router abyssRouter;
    //##### static - end #####
    JDBCClient jdbcClient;
    AuthHandler authHandler;
    JDBCAuth jdbcAuth;
    String verticleHost;
    int serverPort;
    Router verticleRouter;
    private AbyssJDBCService abyssJDBCService;
    private String verticleType = "";

    private void globalJavascript(RoutingContext context) {
        String filecontent =
                "var hostProtocol='" + Config.getInstance().getConfigJsonObject().getString(Constants.HOST_PROTOCOL) + "';\n" +
                        "var host='" + Config.getInstance().getConfigJsonObject().getString(Constants.HOST) + "';\n" +
                        "var hostPort='" + Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_PROXY_OPENAPI_SERVER_PORT) + "';\n" +
                        "var hostJsonPort='" + Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_PROXY_SERVER_PORT) + "';\n" +
                        "var abyssSandbox=" + Config.getInstance().getConfigJsonObject().getBoolean(Constants.ISSANDBOX) + ";\n" +
                        "var version='" + BuildProperties.getInstance().getConfigJsonObject().getString(Constants.ABYSS_BUILD_TIMESTAMP) + "';\n" +
                        "var gatewayContext='" + Constants.ABYSS_GW + "';\n" +
                        "var gatewayPort='" + Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_PROXY_GATEWAY_SERVER_PORT) + "';\n" +
                        "var httpBinUrl='" + Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_BIN_URL) + "';\n" +
                        "var searchAllUrl='" + Config.getInstance().getConfigJsonObject().getString(Constants.ES_SERVER_URL) /*+ "/_search"*/ + "';\n";
        context.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/javascript");
        context.response().setStatusCode(HttpStatus.SC_OK);
        context.response().end(filecontent);
    }

    //#########################################################
    public Router getVerticleRouter() {
        return verticleRouter;
    }

    public void setVerticleRouter(Router verticleRouter) {
        this.verticleRouter = verticleRouter;
    }

    void setVerticleHost(String verticleHost) {
        this.verticleHost = verticleHost;
    }

    void setServerPort(int serverPort) {
        this.serverPort = serverPort;
    }

    public AbyssJDBCService getAbyssJDBCService() {
        return abyssJDBCService;
    }

    void setAbyssJDBCService(AbyssJDBCService abyssJDBCService) {
        this.abyssJDBCService = abyssJDBCService;
    }

    //#########################################################

    void setVerticleType(String verticleType) {
        this.verticleType = verticleType;
    }

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.trace("AbyssAbstractVerticle.start invoked");
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        LOGGER.trace("AbyssAbstractVerticle.stop invoked");
        jdbcClient.close();
        abyssJDBCService.releaseJDBCServiceObject(jdbcClient);
        abyssJDBCService.unpublishDataSource();
        super.stop(stopFuture);
    }

    //#########################################################
    Single<Router> createRouters() {
        abyssRouter = Router.router(vertx);
        verticleRouter = Router.router(vertx);
        return configureAbyssRouter();
    }

    Single<JDBCClient> initializeJdbcClient(String dataSourceName) {

        LOGGER.trace("AbyssAbstractVerticle.initializeJdbcClient() running for {}", dataSourceName);

        return abyssJDBCService.publishDataSource(dataSourceName)
                .flatMap((Record rec) -> {
                    LOGGER.trace("AbyssAbstractVerticle - getting Jdbc Data Service ...");
                    return abyssJDBCService.getJDBCServiceObject(dataSourceName);
                })
                .flatMap((JDBCClient jdbcClient1) -> {
                    this.jdbcClient = jdbcClient1;
                    LOGGER.trace("AbyssAbstractVerticle - Got jdbcClient successfully - {}", jdbcClient1);
                    return Single.just(jdbcClient1);
                });
    }

    protected abstract Single<HttpServer> createHttpServer();

    private Single<Router> configureAbyssRouter() {

        LOGGER.trace("configureAbyssRouter() running");

        //log HTTP requests
        abyssRouter.route().handler(io.vertx.reactivex.ext.web.handler.LoggerHandler.create(LoggerFormat.DEFAULT));
        //abyssRouter.route().handler(LoggerHandler.create());

        //firstly install cookie handler
        //A handler which decodes cookies from the request, makes them available in the RoutingContext and writes them back in the response
        abyssRouter.route().handler(CookieHandler.create());

        //secondly install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        abyssRouter.route().handler(BodyHandler.create());

        //thirdly install session handler
        //A handler that maintains a Session for each browser session
        //The session is available on the routing context with RoutingContext.session()
        //The session handler requires a CookieHandler to be on the routing chain before it
        abyssRouter.route().handler(SessionHandler
                .create(LocalSessionStore.create(vertx, "abyss.session"))
                .setSessionCookieName("abyss.session")
                .setSessionTimeout((long) Config
                        .getInstance()
                        .getConfigJsonObject()
                        .getInteger(Constants.SESSION_IDLE_TIMEOUT) * ONE_MINUTE_IS_60_SECS * ONE_SEC_IS_1000_MILLISECS
                )
        );

        //This handler should be used if you want to store the User object in the Session so it's available between different requests, without you having re-authenticate each time
        //It requires that the session handler is already present on previous matching routes
        //It requires an Auth provider so, if the user is deserialized from a clustered session it knows which Auth provider to associate the session with.

        //fourthly install Cassandra LOGGER
        Boolean isCassandraLoggerEnabled = Config.getInstance().getConfigJsonObject().getBoolean(Constants.CASSANDRA_LOGGER_ENABLED);
        if (isCassandraLoggerEnabled) {
            abyssRouter.route().handler(LoggerHandler.create());
        }

        LOGGER.debug("createRouter() - {}", jdbcClient);
        jdbcAuth = JDBCAuth.create(vertx, jdbcClient);

        jdbcAuth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

        jdbcAuth.setAuthenticationQuery("select password, passwordsalt from subject where isdeleted = false and isactivated = true and subjectname = ?");

        jdbcAuth.setPermissionsQuery("select permission from subject_permission up, subject u where um.subjectname = ? and up.subjectid = u.id");

        jdbcAuth.setRolesQuery("select groupname from subject_group ug, subject_membership um, subject u " +
                "where u.subjectname = ? and um.subjectid = u.id and um.groupId = ug.id");

        abyssRouter.route().handler(UserSessionHandler.create(jdbcAuth));

        //An jdbcAuth handler that's used to handle jdbcAuth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
        authHandler = RedirectAuthHandler.create(jdbcAuth, Constants.ABYSS_ROOT + "/login");

        //If a request times out before the response is written a 503 response will be returned to the client, timeout 5 secs
        abyssRouter.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_TIMEOUT)));

        //Handler which adds a header `x-response-time` in the response of matching requests containing the time taken in ms to process the request.
        abyssRouter.route().handler(ResponseTimeHandler.create());

        abyssRouter.get("/app").handler(StaticHandler.create("webroot/index-new.html"));

        abyssRouter.get("/static/*").handler(StaticHandler.create("webroot/static"));

        abyssRouter.get("/dist/*").handler(StaticHandler.create("webroot/dist"));

        abyssRouter.get("/data/*").handler(StaticHandler.create("webroot/data"));

        abyssRouter.get(Constants.ABYSS_ROOT + "/swagger-ui/*").handler(StaticHandler.create("webroot/swagger-ui"));

        abyssRouter.get(Constants.ABYSS_ROOT + "/swagger-editor/*").handler(StaticHandler.create("webroot/swagger-editor"));

        abyssRouter.get(Constants.ABYSS_ROOT + "/openapi/*").handler(StaticHandler.create("openapi"));

        abyssRouter.get("/global.js").handler(this::globalJavascript);

        FailurePortalController failureController = new FailurePortalController(jdbcAuth, jdbcClient);

        //abyssRouter.routeWithRegex("^" + Constants.ABYSS_ROOT + "/[4|5][0|1]\\d$").handler(failureController).failureHandler(this::failureHandler);
        abyssRouter.routeWithRegex("^" + Constants.ABYSS_ROOT + "/[4|5][0|1]\\d$").handler(failureController);

        //abyssRouter.get(Constants.ABYSS_ROOT + "/failure").handler(failureController).failureHandler(this::failureHandler);
        abyssRouter.get(Constants.ABYSS_ROOT + "/failure").handler(failureController);

        //if verticle is Portal type verticle then handle failures with Abyss error pages
        if (verticleType.equals(Constants.VERTICLE_TYPE_PORTAL)) {
            //only rendering page routings' failures shall be handled by using regex
            //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
            abyssRouter.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);
        }

        return Single.just(abyssRouter);
    }

    Single<Router> enableCorsSupport(Router router) {
        LOGGER.trace("enableCorsSupport() running");
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

    void failureHandler(RoutingContext context) {
        if (verticleType.equals(Constants.VERTICLE_TYPE_PORTAL)) {

            LOGGER.trace("VERTICLE_TYPE_PORTAL failureHandler invoked.. statusCode: {}", context.statusCode());

            //Use user's session for storage
            context.session().put(Constants.HTTP_STATUSCODE, context.statusCode());
            LOGGER.trace(IS_PUT_IN_CONTEXT_SESSION, Constants.HTTP_STATUSCODE, context.session().get(Constants.HTTP_STATUSCODE));

            context.session().put(Constants.HTTP_URL, context.request().path());
            LOGGER.trace(IS_PUT_IN_CONTEXT_SESSION, Constants.HTTP_URL, context.session().get(Constants.HTTP_URL));

            context.session().put(Constants.HTTP_ERRORMESSAGE, context.statusCode() > 0 ? HttpResponseStatus.valueOf(context.statusCode()).reasonPhrase() : "0");
            LOGGER.trace(IS_PUT_IN_CONTEXT_SESSION, Constants.HTTP_ERRORMESSAGE, context.session().get(Constants.HTTP_ERRORMESSAGE));

            context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, "-");//context.failed()?context.failure().getLocalizedMessage():"-");
            LOGGER.trace(IS_PUT_IN_CONTEXT_SESSION, Constants.CONTEXT_FAILURE_MESSAGE, context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));

            context.response().putHeader("location", Constants.ABYSS_ROOT + "/failure").setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY).end();
        }
    }

}
