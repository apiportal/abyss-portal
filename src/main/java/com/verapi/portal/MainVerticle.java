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
import com.verapi.portal.handler.ActivateAccount;
import com.verapi.portal.handler.ChangePassword;
import com.verapi.portal.handler.ForgotPassword;
import com.verapi.portal.handler.Index;
import com.verapi.portal.handler.Login;
import com.verapi.portal.handler.Signup;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CookieHandler;
import io.vertx.reactivex.ext.web.handler.LoggerHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import io.vertx.reactivex.ext.web.handler.RedirectAuthHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.servicediscovery.Record;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.JDBCDataSource;
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
    public void start(Future<Void> startFuture) {
        logger.info("MainVerticle is starting");

        Record record = JDBCDataSource.createRecord(
                Constants.PORTAL_DATA_SOURCE_SERVICE,
                new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
                new JsonObject().put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
        );

        AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().publish(record, asyncResult -> {
            if (asyncResult.succeeded()) {
                logger.info("serviceDiscovery.publish OK..." + asyncResult.succeeded());
            } else {
                logger.error("serviceDiscovery.publish failed..." + asyncResult.cause());
                startFuture.fail(asyncResult.cause());
            }
        });

//        JsonObject jdbcConfig = new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL))
//        .put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
//        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
//        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
//        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE));
//        
//        JDBCClient jdbcClient = JDBCClient.createShared(vertx, jdbcConfig, Constants.PORTAL_DATA_SOURCE_SERVICE);
//        logger.info("JDBCClient created... " + jdbcClient.toString());

        JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), new JsonObject().put("name", Constants.PORTAL_DATA_SOURCE_SERVICE)).subscribe((jdbcClient -> {

            //JDBCClient jdbcClient = resultHandler.result();
            this.jdbcClient = jdbcClient;
            logger.info("JDBCClient created... " + jdbcClient.toString());

            //io.vertx.ext.auth.jdbc.JDBCAuth.create(vertx, jdbcClient);

            auth = JDBCAuth.create(vertx, jdbcClient);
            logger.info("JDBCAuthProvider created... " + auth.toString());

            auth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

            auth.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM portalschema.SUBJECT WHERE IS_DELETED = 0 AND is_activated = 1 AND SUBJECT_NAME = ?");

            //"SELECT PERM FROM portalschema.ROLES_PERMS RP, portalschema.USER_ROLES UR WHERE UR.USERNAME = ? AND UR.ROLE = RP.ROLE"
            //auth.setPermissionsQuery("SELECT PERM FROM portalschema.GROUP_PERMISSION GP, portalschema.USER_MEMBERSHIP UM, portalschema.USER U WHERE UM.USERNAME = ? AND UM.ROLE = UP.ROLE");
            auth.setPermissionsQuery("SELECT PERMISSION FROM portalschema.SUBJECT_PERMISSION UP, portalschema.SUBJECT U WHERE UM.SUBJECT_NAME = ? AND UP.SUBJECT_ID = U.ID");

            //"SELECT ROLE FROM portalschema.USER_ROLES WHERE USERNAME = ?"
            auth.setRolesQuery("SELECT GROUP_NAME FROM portalschema.SUBJECT_GROUP UG, portalschema.SUBJECT_MEMBERSHIP UM, portalschema.SUBJECT U WHERE U.SUBJECT_NAME = ? AND UM.SUBJECT_ID = U.ID AND UM.GROUP_ID = UG.ID");

            //TODO: authProvider.setNonces();
            logger.info("JDBCAuthProvider configuration done... ");

            // To simplify the development of the web components we use a Router to route all HTTP requests
            // to organize our code in a reusable way.
            Router abyssRouter = Router.router(vertx);

            Router router = Router.router(vertx);

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
            abyssRouter.route().handler(UserSessionHandler.create(auth));

            //An auth handler that's used to handle auth (provided by Shiro Auth prodiver) by redirecting user to a custom login page
            AuthHandler authHandler = RedirectAuthHandler.create(auth, "/abyss/login");

            //router.get("/create_user").handler(this::createUser).failureHandler(this::failureHandler);

            Signup signup = new Signup(auth, jdbcClient);
            router.get("/signup").handler(signup::pageRender).failureHandler(this::failureHandler);
            router.post("/sign-up").handler(signup).failureHandler(this::failureHandler);

            ForgotPassword forgotPassword = new ForgotPassword(auth, jdbcClient);
            router.get("/forgot-password").handler(forgotPassword::pageRender).failureHandler(this::failureHandler);
            router.post("/forgot-password").handler(forgotPassword).failureHandler(this::failureHandler);

            ChangePassword changePassword = new ChangePassword(auth, jdbcClient);
            router.route("/change-password").handler(authHandler).failureHandler(this::failureHandler);
            router.get("/change-password").handler(changePassword::pageRender).failureHandler(this::failureHandler);
            router.post("/change-password").handler(changePassword).failureHandler(this::failureHandler);

            ActivateAccount activateAccount = new ActivateAccount(jdbcClient);
            router.get(Constants.ACTIVATION_PATH).handler(activateAccount).failureHandler(this::failureHandler);
            router.get(Constants.RESET_PASSWORD_PATH).handler(activateAccount).failureHandler(this::failureHandler);//TODO: Is same handler ok?

            //install authHandler for all routes where authentication is required
            //router.route("/").handler(authHandler);
            //router.route("/index").handler(authHandler.addAuthority("okumaz")).failureHandler(this::failureHandler);
            router.route("/index").handler(authHandler).failureHandler(this::failureHandler);

            //If a request times out before the response is written a 503 response will be returned to the client, timeout 5 secs
            router.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_TIMEOUT)));

            // Entry point to the application, this will render a custom Thymeleaf template
            //router.get("/login").handler(this::loginHandler);
            Login login = new Login(auth);
            router.get("/login").handler(login::pageRender).failureHandler(this::failureHandler);
            router.post("/login-auth").handler(login).failureHandler(this::failureHandler);

            Index index = new Index(auth);
            router.get("/index").handler(index::pageRender).failureHandler(this::failureHandler);

            router.route("/logout").handler(context -> {
                context.clearUser();
                context.response().putHeader("location", "/abyss/index").setStatusCode(302).end();
            });

            router.route("/").handler(context -> {
                context.response().putHeader("location", "/abyss/index").setStatusCode(302).end();
            });

            //router.post("/login-auth").handler(new SpecialLoginHandler(auth));

            //router.post("/login-auth2").handler(FormLoginHandler.create(auth));


            //router.get("/img/*").handler(StaticHandler.create("webroot/img"));
            //router.get("/vendors/*").handler(StaticHandler.create("webroot/vendors"));
            abyssRouter.get("/dist/*").handler(StaticHandler.create("webroot/dist"));

            abyssRouter.mountSubRouter("/abyss", router);

            abyssRouter.routeWithRegex("^/abyss/[4|5][0|1]\\d$").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

            abyssRouter.get("/abyss/httperror").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

            //only rendering page routings' failures shall be handled by using regex
            //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
            abyssRouter.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);

            abyssRouter.route().handler(ctx -> {
                logger.info("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
                ctx.fail(404);
            });


            logger.info("starting http server");
            HttpServerOptions httpServerOptions = new HttpServerOptions();

            logger.warn("http server is running in plaintext mode. Enable SSL in config for production deployments.");
            vertx.createHttpServer(httpServerOptions.setCompressionSupported(true))
                    .requestHandler(abyssRouter::accept)
                    .listen(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_PORT)
                            , Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_SERVER_HOST)
                            , result -> {
                                if (result.succeeded()) {
                                    logger.info("http server started..." + result.succeeded());
                                    startFuture.complete();
                                } else {
                                    logger.error("http server starting failed..." + result.cause());
                                    startFuture.fail(result.cause());
                                }
                            });
        }), t -> {
            logger.error("serviceDiscovery.getJDBCClient failed..." + t);
            startFuture.fail(t);
        });


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
        context.response().putHeader("location", "/abyss/httperror").setStatusCode(302).end();
//        }
    }
/*
    private void configureThymeleafEngine(ThymeleafTemplateEngine engine) {
        ClassLoaderTemplateResolver templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setPrefix(Constants.TEMPLATE_PREFIX);
        templateResolver.setSuffix(Constants.TEMPLATE_SUFFIX);
        engine.getThymeleafTemplateEngine().setTemplateResolver(templateResolver);

//        CustomMessageResolver customMessageResolver = new CustomMessageResolver();
//        engine.getThymeleafTemplateEngine().setMessageResolver(customMessageResolver);
    }
*/
    /* (non-Javadoc)
     * @see io.vertx.reactivex.core.AbstractVerticle#stop()
     */
    @Override
    public void stop() throws Exception {
        ServiceDiscovery.releaseServiceObject(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), jdbcClient);
        jdbcClient.close();
        AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().close();
        super.stop();
    }
}
