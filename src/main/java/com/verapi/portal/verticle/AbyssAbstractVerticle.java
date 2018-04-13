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


import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.common.JDBCService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.*;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

public abstract class AbyssAbstractVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(AbyssAbstractVerticle.class);
    private static final String abyssRootPath = "/" + Config.getInstance().getConfigJsonObject().getString(Constants.ABYSS);
    protected JDBCAuth jdbcAuth;
    private String host;
    private int port;
    private Router router;
    private JDBCService jdbcService;
    protected JDBCClient jdbcClient;

    /*    public AbyssAbstractVerticle(String host, int port, Router router) {
            this.host = host;
            this.port = port;
            this.router = router;
        }
    */
    @Override
    public void start(Future<Void> startFuture) throws Exception {
        initializeJdbcClient().subscribe(() -> {
            createRouter().subscribe(() -> {
                enableCorsSupport(router).subscribe(() -> {
                    createHttpServer().subscribe(startFuture::complete, startFuture::fail);
                }, startFuture::fail);
            }, startFuture::fail);
        }, startFuture::fail);
    }

    protected void mountControllerRouter(HttpMethod method, String path, Handler<RoutingContext> requestHandler) {
        router.route(method, "/" + path).handler(requestHandler).failureHandler(this::failureHandler);
    }

    protected abstract void mountControllerRouters();

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        jdbcClient.close();
        jdbcService.releaseJDBCServiceObject(jdbcClient);
        jdbcService.unpublishDataSource();
        super.stop(stopFuture);
    }

    public void setHostParams(String host, int port, Router router) {
        this.host = host;
        this.port = port;
        this.router = router;
    }

    protected Completable createRouter() {
        Router abyssRouter = Router.router(vertx);

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
        abyssRouter.route().handler(SessionHandler.create(LocalSessionStore.create(vertx, "abyss.session")).setSessionCookieName("abyss.session"));

        //This handler should be used if you want to store the User object in the Session so it's available between different requests, without you having re-authenticate each time
        //It requires that the session handler is already present on previous matching routes
        //It requires an Auth provider so, if the user is deserialized from a clustered session it knows which Auth provider to associate the session with.

        jdbcAuth = JDBCAuth.create(vertx, jdbcClient);

        jdbcAuth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

        jdbcAuth.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM portalschema.SUBJECT WHERE IS_DELETED = 0 AND is_activated = 1 AND SUBJECT_NAME = ?");

        jdbcAuth.setPermissionsQuery("SELECT PERMISSION FROM portalschema.SUBJECT_PERMISSION UP, portalschema.SUBJECT U WHERE UM.SUBJECT_NAME = ? AND UP.SUBJECT_ID = U.ID");

        jdbcAuth.setRolesQuery("SELECT GROUP_NAME FROM portalschema.SUBJECT_GROUP UG, portalschema.SUBJECT_MEMBERSHIP UM, portalschema.SUBJECT U WHERE U.SUBJECT_NAME = ? AND UM.SUBJECT_ID = U.ID AND UM.GROUP_ID = UG.ID");

        abyssRouter.route().handler(UserSessionHandler.create(jdbcAuth));

        //An jdbcAuth handler that's used to handle jdbcAuth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
        AuthHandler authHandler = RedirectAuthHandler.create(jdbcAuth, abyssRootPath + "/login");

        //If a request times out before the response is written a 503 response will be returned to the client, timeout 5 secs
        router.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_TIMEOUT)));

        router.route("/logout").handler(context -> {
            context.clearUser();
            context.response().putHeader("location", abyssRootPath+"/index").setStatusCode(302).end();
        });

        router.route("/").handler(context -> {
            context.response().putHeader("location", abyssRootPath+"/index").setStatusCode(302).end();
        });

        abyssRouter.get("/dist/*").handler(StaticHandler.create("webroot/dist"));

        abyssRouter.mountSubRouter(abyssRootPath, router);

        //lastly mount all controllers
        mountControllerRouters();

        abyssRouter.routeWithRegex("^"+abyssRootPath+"/[4|5][0|1]\\d$").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

        abyssRouter.get(abyssRootPath+"/httperror").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

        //only rendering page routings' failures shall be handled by using regex
        //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
        abyssRouter.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);

        abyssRouter.route().handler(ctx -> {
            logger.info("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
            ctx.fail(404);
        });

        return Completable.complete();
    }

    protected Completable createHttpServer() {
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setCompressionSupported(true)
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY));
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage()))
                .requestHandler(router::accept)
                .rxListen(port, host)
                .toCompletable();
    }

    protected Completable enableCorsSupport(Router router) {
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("origin");
        allowHeaders.add("Content-Type");
        allowHeaders.add("accept");
        // CORS support
        router.route().handler(CorsHandler.create("*")
                .allowedHeaders(allowHeaders)
                .allowedMethod(HttpMethod.GET)
                .allowedMethod(HttpMethod.POST)
                .allowedMethod(HttpMethod.DELETE)
                .allowedMethod(HttpMethod.PATCH)
                .allowedMethod(HttpMethod.PUT)
        );
        return Completable.complete();
    }

    protected Completable initializeJdbcClient() {
        jdbcService.publishDataSource().subscribe(() -> {
            jdbcService.getJDBCServiceObject().subscribe(jdbcClient -> {
                this.jdbcClient = jdbcClient;
            });
        });
        return Completable.complete();
    }

    private void failureHandler(RoutingContext context) {
        logger.info("failureHandler invoked.. statusCode: " + context.statusCode());

        //Use user's session for storage
        context.session().put(Constants.HTTP_STATUSCODE, new Integer(context.statusCode()));
        logger.info(Constants.HTTP_STATUSCODE + " is put in context session:" + context.session().get(Constants.HTTP_STATUSCODE));

        context.session().put(Constants.HTTP_URL, context.request().path());
        logger.info(Constants.HTTP_URL + " is put in context session:" + context.session().get(Constants.HTTP_URL));

        context.session().put(Constants.HTTP_ERRORMESSAGE, HttpResponseStatus.valueOf(context.statusCode()).reasonPhrase());
        logger.info(Constants.HTTP_ERRORMESSAGE + " is put in context session:" + context.session().get(Constants.HTTP_ERRORMESSAGE));

        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, "-");//context.failed()?context.failure().getLocalizedMessage():"-");
        logger.info(Constants.CONTEXT_FAILURE_MESSAGE + " is put in context session:" + context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));


        String strStatusCode = String.valueOf(context.statusCode());

        //if (strStatusCode.matches("[4|5][0|1]\")) //TODO: In the future...
//        if (strStatusCode.matches("400|401|403|404|500")) {
//            context.response().putHeader("location", "/" + strStatusCode).setStatusCode(302).end();
//        } else {
        context.response().putHeader("location", abyssRootPath+"/httperror").setStatusCode(302).end();
//        }
    }

    private void pGenericHttpStatusCodeHandler(RoutingContext context) {

        logger.info("pGenericHttpStatusCodeHandler invoked...");
        Integer statusCode = context.session().get(Constants.HTTP_STATUSCODE);
        logger.info("pGenericHttpStatusCodeHandler - status code: " + statusCode);

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();
        //configureThymeleafEngine(engine);

        context.put(Constants.HTTP_STATUSCODE, statusCode);
        context.put(Constants.HTTP_URL, context.session().get(Constants.HTTP_URL));
        context.put(Constants.HTTP_ERRORMESSAGE, context.session().get(Constants.HTTP_ERRORMESSAGE));
        context.put(Constants.CONTEXT_FAILURE_MESSAGE, context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));


        String templateFileName = Constants.HTTPERROR_HTML;

//        if (String.valueOf(statusCode).matches("400|401|403|404|500")) {
//            templateFileName = statusCode + ".html";
//        }

        // and now delegate to the engine to render it.
        engine.render(context, "webroot/", templateFileName, res -> {
            if (res.succeeded()) {
                context.response().putHeader("Content-Type", "text/html");
                context.response().setStatusCode(statusCode);
                context.response().end(res.result());
            } else {
                logger.error("pGenericHttpStatusCodeHandler - engine render failed with cause:" + res.cause().getLocalizedMessage());
                context.fail(res.cause());
            }
        });
    }

}
