/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 2 2018
 *
 */

package com.verapi.portal;

import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.handler.Index;
import com.verapi.portal.handler.Login;
import com.verapi.portal.handler.Signup;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.*;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.web.templ.ThymeleafTemplateEngine;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MainVerticle extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    private JDBCClient jdbcClient;

    private JDBCAuth auth;

    @Override
    public void start(Future<Void> start) {

        Record record = JDBCDataSource.createRecord(
                Constants.PORTAL_DATA_SOURCE_SERVICE,
                new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
                new JsonObject().put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
        );

        AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().publish(record, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info("serviceDiscovery.publish OK..." + asyncResult.succeeded());
            } else {
                logger.error("serviceDiscovery.publish failed..." + asyncResult.cause());
                start.fail(asyncResult.cause());
            }
        });

        JDBCClient jdbcClient = JDBCClient.createShared(vertx, new JsonObject(), Constants.PORTAL_DATA_SOURCE_SERVICE);
        logger.info("JDBCClient created... " + jdbcClient.toString());

        auth = JDBCAuth.create(vertx, jdbcClient);

        logger.info("JDBCAuthProvider created... " + auth.toString());

        auth.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM portalschema.USER WHERE USERNAME = ?");
        auth.setPermissionsQuery("SELECT PERM FROM portalschema.ROLES_PERMS RP, portalschema.USER_ROLES UR WHERE UR.USERNAME = ? AND UR.ROLE = RP.ROLE");
        auth.setRolesQuery("SELECT ROLE FROM portalschema.USER_ROLES WHERE USERNAME = ?");
        auth.setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx));
        //TODO: authProvider.setNonces();
        logger.info("JDBCAuthProvider configuration done... ");


        // To simplify the development of the web components we use a Router to route all HTTP requests
        // to organize our code in a reusable way.
        Router router = Router.router(vertx);

        //log HTTP requests
        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

        //firstly install cookie handler
        //A handler which decodes cookies from the request, makes them available in the RoutingContext and writes them back in the response
        router.route().handler(CookieHandler.create());

        //secondly install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        router.route().handler(BodyHandler.create());

        //thirdly install session handler
        //A handler that maintains a Session for each browser session
        //The session is available on the routing context with RoutingContext.session()
        //The session handler requires a CookieHandler to be on the routing chain before it
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx, "abyss.session")).setSessionCookieName("abyss.session"));

        //This handler should be used if you want to store the User object in the Session so it's available between different requests, without you having re-authenticate each time
        //It requires that the session handler is already present on previous matching routes
        //It requires an Auth provider so, if the user is deserialized from a clustered session it knows which Auth provider to associate the session with.
        router.route().handler(UserSessionHandler.create(auth));

        //An auth handler that's used to handle auth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
        AuthHandler authHandler = RedirectAuthHandler.create(auth, "/full-width-light/login");

        router.get("/create_user").handler(this::createUser).failureHandler(this::failureHandler);
        
        Signup signup = new Signup(auth, jdbcClient);
        router.get("/full-width-light/signup").handler(signup::pageRender).failureHandler(this::failureHandler);
        router.post("/sign-up").handler(signup).failureHandler(this::failureHandler);
        
        //install authHandler for all routes where authentication is required
        //router.route("/full-width-light/").handler(authHandler);
        router.route("/full-width-light/index").handler(authHandler.addAuthority("okumaz")).failureHandler(this::failureHandler);

        // Entry point to the application, this will render a custom Thymeleaf template
        //router.get("/full-width-light/login").handler(this::loginHandler);
        Login login = new Login(auth);
        router.get("/full-width-light/login").handler(login::pageRender).failureHandler(this::failureHandler);
        router.post("/login-auth").handler(login).failureHandler(this::failureHandler);

        Index index = new Index(auth);
        router.get("/full-width-light/index").handler(index::pageRender).failureHandler(this::failureHandler);
        //router.post("/login-auth").handler(new SpecialLoginHandler(auth));

        //router.post("/login-auth2").handler(FormLoginHandler.create(auth));


        router.get("/img/*").handler(StaticHandler.create("/img").setWebRoot("webroot/img"));
        router.get("/vendors/*").handler(StaticHandler.create("/vendors").setWebRoot("webroot/vendors"));
        router.get("/full-width-light/dist/*").handler(StaticHandler.create("/full-width-light/dist").setWebRoot("webroot/full-width-light/dist"));

        router.routeWithRegex("^/full-width-light/[4|5][0|1]\\d$").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

        router.get("/full-width-light/httperror").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

        //only rendering page routings' failures shall be handled by using regex
        //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
        router.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);

        router.route().handler(ctx -> {
            logger.info("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
            ctx.fail(404);
        });

        logger.info("starting http server");
        HttpServerOptions httpServerOptions = new HttpServerOptions();

        logger.warn("http server is running in plaintext mode. Enable SSL in config for production deployments.");
        vertx.createHttpServer(httpServerOptions.setCompressionSupported(true))
                .requestHandler(router::accept)
                .listen(Config.getInstance().getConfigJsonObject().getInteger("port")
                        , Config.getInstance().getConfigJsonObject().getString("host")
                        , result -> {
                            if (result.succeeded()) {
                                logger.info("http server started..." + result.succeeded());
                                start.complete();
                            } else {
                                logger.error("http server starting failed..." + result.cause());
                                start.fail(result.cause());
                            }
                        });

        logger.debug("loaded config : " + Config.getInstance().getConfigJsonObject().encodePrettily());


    }

    /**
     * @param routingContext
     */
    private void createUser(RoutingContext routingContext) {

        logger.info("executing createUser...");

        String username = routingContext.request().getParam("username");
        String password = routingContext.request().getParam("password");

        logger.info("Received user:" + username);
        logger.trace("Received pass:" + password);


        jdbcClient.getConnection(resConn -> {
            if (resConn.succeeded()) {

                SQLConnection connection = resConn.result();

                connection.queryWithParams("SELECT * FROM portalschema.USER WHERE USERNAME = ?", new JsonArray().add(username), resQuery -> {
                    if (resQuery.succeeded()) {
                        ResultSet rs = resQuery.result();
                        // Do something with results
                        if (rs.getNumRows() > 0) {
                            logger.info("user found: " + rs.toJson().encodePrettily());
                        } else {
                            logger.info("user NOT found, creating ...");
                            String salt = auth.generateSalt();
                            String hash = auth.computeHash(password, salt);
                            // save to the database
                            connection.updateWithParams("INSERT INTO portalschema.user VALUES (?, ?, ?)", new JsonArray().add(username).add(hash).add(salt), resUpdate -> {
                                if (resUpdate.succeeded()) {
                                    logger.info("user created successfully");
                                } else {
                                    logger.error("user create error: " + resUpdate.cause().getLocalizedMessage());
                                    resUpdate.failed();
                                }
                            });
                        }
                    } else {
                        logger.error("SELECT user failed: " + resQuery.cause().getLocalizedMessage());
                        connection.close();
                        //jdbcClient.close();
                    }
                });
            } else {
                // Failed to get connection - deal with it
                logger.error("JDBC getConnection failed: " + resConn.cause().getLocalizedMessage());
                resConn.failed();
                //jdbcClient.close();
            }
        });
    }


    private void pGenericHttpStatusCodeHandler(RoutingContext context) {

        logger.info("pGenericHttpStatusCodeHandler invoked...");
        Integer statusCode = context.session().get(Constants.HTTP_STATUSCODE);
        logger.info("pGenericHttpStatusCodeHandler - status code: " + statusCode);

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        context.put(Constants.HTTP_STATUSCODE, statusCode);
        context.put(Constants.HTTP_URL, context.session().get(Constants.HTTP_URL));
        context.put(Constants.HTTP_ERRORMESSAGE, context.session().get(Constants.HTTP_ERRORMESSAGE));
        context.put(Constants.CONTEXT_FAILURE_MESSAGE, context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));


        String templateFileName = Constants.HTTPERROR_HTML;

        if (String.valueOf(statusCode).matches("400|401|403|404|500")) {
            templateFileName = statusCode + ".html";
        }

        // and now delegate to the engine to render it.
        engine.render(context, "src/main/resources/webroot/full-width-light/", templateFileName, res -> {
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
        if (strStatusCode.matches("400|401|403|404|500")) {
            context.response().putHeader("location", "/full-width-light/" + strStatusCode).setStatusCode(302).end();
        } else {
            context.response().putHeader("location", "/full-width-light/httperror").setStatusCode(302).end();
        }
    }

    /* (non-Javadoc)
     * @see io.vertx.core.AbstractVerticle#stop()
     */
    @Override
    public void stop() throws Exception {
        jdbcClient.close();
        AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().close();
        super.stop();
    }
}
