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

package com.verapi.portal;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.handler.ActivateAccount;
import com.verapi.portal.handler.ChangePassword;
import com.verapi.portal.handler.ForgotPassword;
import com.verapi.portal.handler.Index;
import com.verapi.portal.handler.Login;
import com.verapi.portal.handler.Signup;
import com.verapi.portal.handler.UserGroups;
import com.verapi.portal.handler.Users;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.AuthHandler;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CSRFHandler;
import io.vertx.reactivex.ext.web.handler.CookieHandler;
import io.vertx.reactivex.ext.web.handler.RedirectAuthHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.handler.StaticHandler;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import io.vertx.reactivex.ext.web.handler.UserSessionHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.JDBCDataSource;
import io.vertx.servicediscovery.Record;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);
    private static final String LOCATION = "location";

    private JDBCClient jdbcClient;

    private JDBCAuth auth;

    @SuppressWarnings("findbugs:RV_RETURN_VALUE_IGNORED")
    @Override
    public void start(Future<Void> startFuture) {
        LOGGER.info("MainVerticle is starting");

        Record record = JDBCDataSource.createRecord(
                Constants.PORTAL_DATA_SOURCE_SERVICE,
                new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
                new JsonObject().put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
        );

        AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().publish(record, (AsyncResult<Record> asyncResult) -> {
            if (asyncResult.succeeded()) {
                LOGGER.info("serviceDiscovery.publish OK... {}", asyncResult.succeeded());
            } else {
                LOGGER.error("serviceDiscovery.publish failed... {}", asyncResult.cause().getLocalizedMessage());
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
//        LOGGER.info("JDBCClient created... " + jdbcClient.toString());
//        @SuppressFBWarnings("RV_RETURN_VALUE_IGNORED")

        JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery()
                , new JsonObject().put("name", Constants.PORTAL_DATA_SOURCE_SERVICE))
                .subscribe(((JDBCClient jdbcClient) -> {

                    //JDBCClient jdbcClient = resultHandler.result();
                    this.jdbcClient = jdbcClient;
                    LOGGER.info("JDBCClient created... {}", jdbcClient);

                    //io.vertx.ext.auth.jdbc.JDBCAuth.create(vertx, jdbcClient);

                    auth = JDBCAuth.create(vertx, jdbcClient);
                    LOGGER.info("JDBCAuthProvider created... {}", auth);

                    auth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

                    auth.setAuthenticationQuery("SELECT PASSWORD, PASSWORD_SALT FROM portalschema.SUBJECT " +
                            "WHERE IS_DELETED = false AND is_activated = true AND SUBJECT_NAME = ?");

                    //"SELECT PERM FROM portalschema.ROLES_PERMS RP, portalschema.USER_ROLES UR WHERE UR.USERNAME = ? AND UR.ROLE = RP.ROLE"
                    //auth.setPermissionsQuery("SELECT PERM FROM portalschema.GROUP_PERMISSION GP, portalschema.USER_MEMBERSHIP UM, portalschema.USER U WHERE UM.USERNAME = ? AND UM.ROLE = UP.ROLE");
                    auth.setPermissionsQuery("SELECT PERMISSION FROM portalschema.SUBJECT_PERMISSION UP, portalschema.SUBJECT U " +
                            "WHERE UM.SUBJECT_NAME = ? AND UP.SUBJECT_ID = U.ID");

                    //"SELECT ROLE FROM portalschema.USER_ROLES WHERE USERNAME = ?"
                    auth.setRolesQuery("SELECT GROUP_NAME FROM portalschema.SUBJECT_GROUP UG, portalschema.SUBJECT_MEMBERSHIP UM, portalschema.SUBJECT U " +
                            "WHERE U.SUBJECT_NAME = ? AND UM.SUBJECT_ID = U.ID AND UM.GROUP_ID = UG.ID");

                    //TODO: authProvider.setNonces();
                    LOGGER.info("JDBCAuthProvider configuration done... ");

                    // To simplify the development of the web components we use a Router to route all HTTP requests
                    // to organize our code in a reusable way.
                    Router abyssRouter = Router.router(vertx);

                    Router router = Router.router(vertx);

                    //log HTTP requests
                    //abyssRouter.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

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
                    abyssRouter.route()
                            .handler(SessionHandler
                                    .create(LocalSessionStore
                                            .create(vertx, "abyss.session"))
                                    .setSessionCookieName(Constants.AUTH_ABYSS_PORTAL_SESSION_COOKIE_NAME));

                    //TODO: CSRF Handler for OWASP
                    abyssRouter.route().handler(CSRFHandler.create("cok.cok.gizli"));

                    //This handler should be used if you want to store the User object in the Session so it's available between different requests,
                    // without you having re-authenticate each time
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

                    Users users = new Users(jdbcClient);
                    router.route("/users").handler(authHandler).failureHandler(this::failureHandler);
                    router.get("/users/management").handler(users).failureHandler(this::failureHandler);
                    router.get("/users").handler(users::pageRender).failureHandler(this::failureHandler);

                    UserGroups userGroups = new UserGroups(jdbcClient);
                    router.route("/user-groups").handler(authHandler).failureHandler(this::failureHandler);
                    router.get("/user-groups/management").handler(userGroups).failureHandler(this::failureHandler);
                    router.get("/user-groups").handler(userGroups::pageRender).failureHandler(this::failureHandler);

                    router.route("/user-directories").handler(authHandler).failureHandler(this::failureHandler);
                    router.get("/user-directories").handler(userGroups::dirPageRender).failureHandler(this::failureHandler);

                    //TEST - router.get("/my-apis").handler(userGroups::apiPageRender).failureHandler(this::failureHandler);


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

                    router.route("/logout").handler((RoutingContext context) -> {
                        context.user().clearCache();
                        context.clearUser();

                        context.session().remove(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME);
                        context.session().remove(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME);
                        context.session().remove(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME);
                        context.session().remove(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);

                        context.session().destroy();

                        context.removeCookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME);
                        context.removeCookie(Constants.AUTH_ABYSS_PORTAL_SESSION_COOKIE_NAME);
                        context.removeCookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_COOKIE_NAME); //TODO: Bunu kim koyuyor?
                        context.removeCookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME);
                        context.removeCookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);

                        context.response()
                                .putHeader(LOCATION, "/abyss/index")
                                .setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                                .end();
                    });

                    router.route("/").handler((RoutingContext context) -> context.response()
                            .putHeader(LOCATION, "/abyss/index")
                            .setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY)
                            .end());

                    //router.post("/login-auth").handler(new SpecialLoginHandler(auth));

                    //router.post("/login-auth2").handler(FormLoginHandler.create(auth));


                    //router.get("/img/*").handler(StaticHandler.create("webroot/img"));
                    //router.get("/vendors/*").handler(StaticHandler.create("webroot/vendors"));
                    abyssRouter.get("/dist/*").handler(StaticHandler.create("webroot/dist"));

                    abyssRouter.mountSubRouter("/abyss", router);

                    abyssRouter.routeWithRegex("^/abyss/[4|5][0|1]\\d$").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

                    abyssRouter.get("/abyss/httperror").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

                    abyssRouter.get("/abyss/success").handler(this::pGenericHttpStatusCodeHandler).failureHandler(this::failureHandler);

                    //only rendering page routings' failures shall be handled by using regex
                    //The regex below will match any string, or line without a line break, not containing the (sub)string '.'
                    abyssRouter.routeWithRegex("^((?!\\.).)*$").failureHandler(this::failureHandler);

                    abyssRouter.route().handler((RoutingContext ctx) -> {
                        LOGGER.info("router.route().handler invoked... the last bus stop, no any bus stop more, so it is firing 404 now...!.");
                        ctx.fail(HttpStatus.SC_NOT_FOUND);
                    });


                    LOGGER.info("starting http server");
                    HttpServerOptions httpServerOptions = new HttpServerOptions();

                    LOGGER.warn("http server is running in plaintext mode. Enable SSL in config for production deployments.");
                    vertx.createHttpServer(httpServerOptions.setCompressionSupported(true))
                            .requestHandler(abyssRouter)
                            .listen(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_PORT)
                                    , Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_SERVER_HOST)
                                    , (AsyncResult<HttpServer> result) -> {
                                        if (result.succeeded()) {
                                            LOGGER.info("http server started... {}", result.succeeded());
                                            startFuture.complete();
                                        } else {
                                            LOGGER.error("http server starting failed... {}", result.cause().getLocalizedMessage());
                                            startFuture.fail(result.cause());
                                        }
                                    });
                }), (Throwable t) -> {
                    LOGGER.error("serviceDiscovery.getJDBCClient failed... {}", t.getLocalizedMessage());
                    startFuture.fail(t);
                });


    }

    /**
     * @param routingContext vertx routing context
     */
    private void createUser(RoutingContext routingContext) {

        LOGGER.info("executing createUser...");

        String username = routingContext.request().getParam("username");
        String password = routingContext.request().getParam("password");

        LOGGER.info("Received user: {}", username);


        jdbcClient.getConnection((AsyncResult<SQLConnection> resConn) -> {
            if (resConn.succeeded()) {

                SQLConnection connection = resConn.result();

                connection.queryWithParams("SELECT * FROM portalschema.USER WHERE USERNAME = ?", new JsonArray().add(username)
                        , (AsyncResult<ResultSet> resQuery) -> {
                            if (resQuery.succeeded()) {
                                ResultSet rs = resQuery.result();
                                // Do something with results
                                if (rs.getNumRows() > 0) {
                                    LOGGER.info("user found: {} ", rs.toJson().encodePrettily());
                                } else {
                                    LOGGER.info("user NOT found, creating ...");
                                    String salt = auth.generateSalt();
                                    String hash = auth.computeHash(password, salt);
                                    // save to the database
                                    connection.updateWithParams("INSERT INTO portalschema.user VALUES (?, ?, ?)"
                                            , new JsonArray().add(username).add(hash).add(salt), (AsyncResult<UpdateResult> resUpdate) -> {
                                                if (resUpdate.succeeded()) {
                                                    LOGGER.info("user created successfully");
                                                } else {
                                                    LOGGER.error("user create error: {}", resUpdate.cause().getLocalizedMessage());
                                                    resUpdate.failed();
                                                }
                                            });
                                }
                            } else {
                                LOGGER.error("SELECT user failed: {}", resQuery.cause().getLocalizedMessage());
                                connection.close();
                            }
                        });
            } else {
                // Failed to get connection - deal with it
                LOGGER.error("JDBC getConnection failed: {}", resConn.cause().getLocalizedMessage());
                resConn.failed();
            }
        });
    }


    private void pGenericHttpStatusCodeHandler(RoutingContext context) {

        LOGGER.info("pGenericHttpStatusCodeHandler invoked...");
        Integer statusCode = context.session().get(Constants.HTTP_STATUSCODE);
        LOGGER.info("pGenericHttpStatusCodeHandler - status code: {}", statusCode);

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(context.vertx());

        context.put(Constants.HTTP_STATUSCODE, statusCode);
        context.put(Constants.HTTP_URL, context.session().get(Constants.HTTP_URL));
        context.put(Constants.HTTP_ERRORMESSAGE, context.session().get(Constants.HTTP_ERRORMESSAGE));
        context.put(Constants.CONTEXT_FAILURE_MESSAGE, context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));

        JsonObject templateContext = new JsonObject()
                .put(Constants.HTTP_STATUSCODE, statusCode)
                .put(Constants.HTTP_URL, (String) context.session().get(Constants.HTTP_URL))
                .put(Constants.HTTP_ERRORMESSAGE, (String) context.session().get(Constants.HTTP_ERRORMESSAGE))
                .put(Constants.CONTEXT_FAILURE_MESSAGE, (String) context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));


        String templateFileName;
        if (statusCode == HttpStatus.SC_OK) {
            templateFileName = Constants.HTML_SUCCESS;
        } else {
            templateFileName = Constants.HTML_FAILURE;
        }

//        if (String.valueOf(statusCode).matches("400|401|403|404|500")) {
//            templateFileName = statusCode + ".html";
//        }

        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_ROOT + templateFileName, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                context.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                context.response().setStatusCode(statusCode);
                context.response().end(res.result());
            } else {
                LOGGER.error("pGenericHttpStatusCodeHandler - engine render failed with cause: {}", res.cause().getLocalizedMessage());
                context.fail(res.cause());
            }
        });
    }

    private void failureHandler(RoutingContext context) {
        LOGGER.info("failureHandler invoked.. statusCode: {}", context.statusCode());
        final String LOGGER_MESSAGE = "{} is put in context session: {}";

        //Use user's session for storage 
        context.session().put(Constants.HTTP_STATUSCODE, context.statusCode());
        LOGGER.info(LOGGER_MESSAGE, Constants.HTTP_STATUSCODE, context.session().get(Constants.HTTP_STATUSCODE));

        context.session().put(Constants.HTTP_URL, context.request().path());
        LOGGER.info(LOGGER_MESSAGE, Constants.HTTP_URL, context.session().get(Constants.HTTP_URL));

        context.session().put(Constants.HTTP_ERRORMESSAGE, HttpResponseStatus.valueOf(context.statusCode()).reasonPhrase());
        LOGGER.info(LOGGER_MESSAGE, Constants.HTTP_ERRORMESSAGE, context.session().get(Constants.HTTP_ERRORMESSAGE));

        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, "-");//context.failed()?context.failure().getLocalizedMessage():"-");
        LOGGER.info("{} is put in context session: {}", Constants.CONTEXT_FAILURE_MESSAGE, context.session().get(Constants.CONTEXT_FAILURE_MESSAGE));


        //if (strStatusCode.matches("[4|5][0|1]\")) //TODO: In the future...
//        if (strStatusCode.matches("400|401|403|404|500")) {
//            context.response().putHeader("location", "/" + strStatusCode).setStatusCode(302).end();
//        } else {
        context.response().putHeader(LOCATION, "/abyss/httperror").setStatusCode(HttpStatus.SC_MOVED_TEMPORARILY).end();
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
     * @see io.vertx.reactivex.core.AbyssAbstractVerticle#stop()
     */
    @Override
    public void stop() throws Exception {
        ServiceDiscovery.releaseServiceObject(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), jdbcClient);
        jdbcClient.close();
        AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().close();
        super.stop();
    }
}
