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
import com.verapi.portal.controller.FailureController;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.Future;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public abstract class AbyssAbstractVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(AbyssAbstractVerticle.class);
    //####### static #######
    static Router abyssRouter;
    //##### static - end #####

    private AbyssJDBCService abyssJDBCService;
    JDBCClient jdbcClient;
    AuthHandler authHandler;
    JDBCAuth jdbcAuth;
    String verticleHost;
    int serverPort;
    Router verticleRouter;
    private String verticleType = "";


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

    void setVerticleType(String verticleType) {
        this.verticleType = verticleType;
    }

    //#########################################################

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("AbyssAbstractVerticle.start invoked");
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        logger.trace("AbyssAbstractVerticle.stop invoked");
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

        logger.trace("AbyssAbstractVerticle.initializeJdbcClient() running for " + dataSourceName);

        return abyssJDBCService.publishDataSource(dataSourceName)
                .flatMap(rec -> {
                    logger.trace("AbyssAbstractVerticle - getting Jdbc Data Service ...");
                    return abyssJDBCService.getJDBCServiceObject(dataSourceName);
                })
                .flatMap(jdbcClient1 -> {
                    this.jdbcClient = jdbcClient1;
                    logger.trace("AbyssAbstractVerticle - Got jdbcClient successfully - " + jdbcClient1.toString());
                    return Single.just(jdbcClient1);
                });
    }

    protected abstract Single<HttpServer> createHttpServer();

    private Single<Router> configureAbyssRouter() {

        logger.trace("createRouter() running");

        //log HTTP requests
        abyssRouter.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

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
        abyssRouter.route().handler(SessionHandler.create(LocalSessionStore.create(vertx, "abyss.session")).setSessionCookieName("abyss.session").setSessionTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.BROWSER_SESSION_TIMEOUT) * 60 * 1000));

        //This handler should be used if you want to store the User object in the Session so it's available between different requests, without you having re-authenticate each time
        //It requires that the session handler is already present on previous matching routes
        //It requires an Auth provider so, if the user is deserialized from a clustered session it knows which Auth provider to associate the session with.

        logger.debug("createRouter() - " + jdbcClient.toString());
        jdbcAuth = JDBCAuth.create(vertx, jdbcClient);

        jdbcAuth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

        jdbcAuth.setAuthenticationQuery("select password, passwordsalt from subject where isdeleted = false and isactivated = true and subjectname = ?");

        jdbcAuth.setPermissionsQuery("select permission from subject_permission up, subject u where um.subjectname = ? and up.subjectid = u.id");

        jdbcAuth.setRolesQuery("select groupname from subject_group ug, subject_membership um, subject u where u.subjectname = ? and um.subjectid = u.id and um.groupId = ug.id");

        abyssRouter.route().handler(UserSessionHandler.create(jdbcAuth));

        //An jdbcAuth handler that's used to handle jdbcAuth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
        authHandler = RedirectAuthHandler.create(jdbcAuth, Constants.ABYSS_ROOT + "/login");

        //If a request times out before the response is written a 503 response will be returned to the client, timeout 5 secs
        abyssRouter.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_TIMEOUT)));

        //Handler which adds a header `x-response-time` in the response of matching requests containing the time taken in ms to process the request.
        abyssRouter.route().handler(ResponseTimeHandler.create());

        abyssRouter.get("/dist/*").handler(StaticHandler.create("webroot/dist"));

        abyssRouter.get("/data/*").handler(StaticHandler.create("webroot/data"));

        abyssRouter.get(Constants.ABYSS_ROOT + "/swagger-ui/*").handler(StaticHandler.create("webroot/swagger-ui"));

        abyssRouter.get(Constants.ABYSS_ROOT + "/swagger-editor/*").handler(StaticHandler.create("webroot/swagger-editor"));

        abyssRouter.get(Constants.ABYSS_ROOT + "/openapi/*").handler(StaticHandler.create("openapi"));

        abyssRouter.get("/global.js").handler(this::globalJavascript);

        FailureController failureController = new FailureController(jdbcAuth, jdbcClient);

        abyssRouter.routeWithRegex("^" + Constants.ABYSS_ROOT + "/[4|5][0|1]\\d$").handler(failureController).failureHandler(this::failureHandler);

        abyssRouter.get(Constants.ABYSS_ROOT + "/failure").handler(failureController).failureHandler(this::failureHandler);

        //if verticle is Portal type verticle then handle failures with Abyss error pages
        if (verticleType.equals(Constants.VERTICLE_TYPE_PORTAL))
            //only rendering page routings' failures shall be handled by using regex
            //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
            abyssRouter.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);

        return Single.just(abyssRouter);
    }

    Single<Router> enableCorsSupport(Router router) {
        logger.trace("enableCorsSupport() running");
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
        router.route().handler(CorsHandler.create("http(s)?:\\/\\/(.+\\.)?(192\\.168\\..*|apiportal\\.com|local\\.abyss\\.com)(:\\d{1,5})?$")
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
        logger.trace("failureHandler invoked.. statusCode: " + context.statusCode());

        //Use user's session for storage
        context.session().put(Constants.HTTP_STATUSCODE, context.statusCode());
        logger.trace(Constants.HTTP_STATUSCODE + " is put in context session:" + context.session().get(Constants.HTTP_STATUSCODE));

        context.session().put(Constants.HTTP_URL, context.request().path());
        logger.trace(Constants.HTTP_URL + " is put in context session:" + context.session().get(Constants.HTTP_URL));

        context.session().put(Constants.HTTP_ERRORMESSAGE, context.statusCode() > 0 ? HttpResponseStatus.valueOf(context.statusCode()).reasonPhrase() : "0");
        logger.trace(Constants.HTTP_ERRORMESSAGE + " is put in context session:" + context.session().get(Constants.HTTP_ERRORMESSAGE));

        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, "-");//context.failed()?context.failure().getLocalizedMessage():"-");
        logger.trace(Constants.CONTEXT_FAILURE_MESSAGE + " is put in context session:" + context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));

        context.response().putHeader("location", Constants.ABYSS_ROOT + "/failure").setStatusCode(302).end();
    }

    void globalJavascript(RoutingContext context) {
        String filecontent =
                "var hostProtocol='" + Config.getInstance().getConfigJsonObject().getString(Constants.HOST_PROTOCOL) + "';" +
                        "var host='" + Config.getInstance().getConfigJsonObject().getString(Constants.HOST) + "';" +
                        "var hostPort='" + Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_PROXY_API_SERVER_PORT) + "';" +
                        "var hostJsonPort='" + Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_PROXY_SERVER_PORT) + "';" +
                        "var abyssSandbox=" + Config.getInstance().getConfigJsonObject().getBoolean(Constants.ISSANDBOX) + ";";
        context.response().putHeader("Content-Type", "application/javascript");
        context.response().setStatusCode(200);
        context.response().end(filecontent);
    }

}
