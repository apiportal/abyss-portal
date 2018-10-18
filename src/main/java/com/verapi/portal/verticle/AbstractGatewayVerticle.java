/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 6 2018
 *
 */

package com.verapi.portal.verticle;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.common.OpenAPIUtil;
import com.verapi.portal.oapi.exception.AbyssApiException;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.oapi.exception.NotFound404Exception;
import com.verapi.portal.oapi.exception.UnAuthorized401Exception;
import com.verapi.portal.service.idam.AuthenticationService;
import com.verapi.portal.service.idam.AuthorizationService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.ResolverCache;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import io.vertx.ext.web.api.contract.openapi3.impl.OpenAPI3RouterFactoryImpl;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpClientResponse;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CookieHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.LoggerHandler;
import io.vertx.reactivex.ext.web.handler.ResponseTimeHandler;
import io.vertx.reactivex.ext.web.handler.SessionHandler;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import io.vertx.reactivex.ext.web.sstore.LocalSessionStore;
import io.vertx.reactivex.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

//import io.vertx.ext.auth.jdbc.JDBCAuth;

//import io.vertx.core.http.HttpClient;

//public class AbstractGatewayVerticle extends AbstractVerticle implements IGatewayVerticle {
public abstract class AbstractGatewayVerticle extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(AbstractGatewayVerticle.class);

    static Router gatewayRouter;
    private AbyssJDBCService abyssJDBCService;
    private JDBCClient jdbcClient;
    JDBCAuth jdbcAuth;
    VerticleConf verticleConf;


    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("---start invoked");
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        logger.trace("---stop invoked");
        super.stop(stopFuture);
    }

    private Single<Router> createRouter() {
        logger.trace("---createRouter invoked");
        gatewayRouter = Router.router(vertx);
        return configureRouter(gatewayRouter);
    }

    private Single<Router> configureRouter(Router router) {
        logger.trace("---configureRouter invoked");

        //log HTTP requests
        router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

        // 1: install cookie handler
        //A handler which decodes cookies from the request, makes them available in the RoutingContext and writes them back in the response
        router.route().handler(CookieHandler.create());

        // 2: install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        router.route().handler(BodyHandler.create());

        // 3: install session handler
        //A handler that maintains a Session for each browser session
        //The session is available on the routing context with RoutingContext.session()
        //The session handler requires a CookieHandler to be on the routing chain before it
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx, Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)).setSessionCookieName(Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME).setSessionTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60 * 1000));

        //If a request times out before the response is written a 503 response will be returned to the client, default abyss-gw timeout 30 secs
        router.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_GATEWAY_SERVER_TIMEOUT)));

        //Handler which adds a header `x-response-time` in the response of matching requests containing the time taken in ms to process the request.
        router.route().handler(ResponseTimeHandler.create());

        router.route().failureHandler(this::failureHandler);

        //router.route(Constants.ABYSS_GATEWAY_ROOT + "/:apiUUID/:apiPath").handler(this::routingContextHandler);
        router.route(Constants.ABYSS_GATEWAY_ROOT).handler(this::routingContextHandler);

        logger.trace("router route list: {}", router.getRoutes());

        // jdbcAuth is only prepared for UserSessionHandler usage inside openAPI routers
        jdbcAuth = JDBCAuth.create(vertx, jdbcClient);

        jdbcAuth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

        jdbcAuth.setAuthenticationQuery("select password, passwordsalt from subject where isdeleted = false and isactivated = true and subjectname = ?");

        jdbcAuth.setPermissionsQuery("select permission from subject_permission up, subject u where um.subjectname = ? and up.subjectid = u.id");

        jdbcAuth.setRolesQuery("select groupname from subject_group ug, subject_membership um, subject u where u.subjectname = ? and um.subjectid = u.id and um.groupId = ug.id");

        return Single.just(router);
    }

    Single<Router> createSubRouter(String mountPoint) {
        logger.trace("---createSubRouter invoked");

        if (!mountPoint.startsWith("/"))
            mountPoint = "/" + mountPoint;
        Router subRouter = Router.router(vertx);

        //log HTTP requests
        subRouter.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

        //install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        subRouter.route().handler(BodyHandler.create());

        //If a request times out before the response is written a 503 response will be returned to the client, default abyss-gw timeout 30 secs
        subRouter.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_GATEWAY_SERVER_TIMEOUT)));

        //Handler which adds a header `x-response-time` in the response of matching requests containing the time taken in ms to process the request.
        subRouter.route().handler(ResponseTimeHandler.create());

        subRouter.route().failureHandler(this::failureHandler);

        subRouter.route(mountPoint + "/:apipath*").handler(this::routingContextHandler);

        gatewayRouter.mountSubRouter(Constants.ABYSS_GATEWAY_ROOT, subRouter);

        return Single.just(subRouter);
    }

    private void failureHandler(RoutingContext routingContext) {

        // This is the failure handler
        Throwable failure = routingContext.failure();
        if (failure instanceof AbyssApiException) {
            //Handle Abyss Api Exception
            routingContext.response()
                    .setStatusCode(((AbyssApiException) failure).getHttpResponseStatus().code())
                    .setStatusMessage(((AbyssApiException) failure).getHttpResponseStatus().reasonPhrase())
                    .end();
        } else {
            if (routingContext.statusCode() == -1)
                routingContext.response().setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code());
            // Handle other exception
            routingContext.response()
                    .setStatusCode(routingContext.response().getStatusCode())
                    .setStatusMessage(HttpResponseStatus.valueOf(routingContext.response().getStatusCode()).reasonPhrase())
                    .end();
        }
    }

    private Single<HttpServer> createHttpServer(Router router, String serverHost, int serverPort, Boolean isSSL) {
        logger.trace("---createHttpServer invoked");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setLogActivity(Config.getInstance().getConfigJsonObject()
                        .getBoolean((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_LOG_HTTP_ACTIVITY : Constants.HTTP_GATEWAY_SERVER_LOG_HTTP_ACTIVITY))
                .setAcceptBacklog(Config.getInstance().getConfigJsonObject()
                        .getInteger((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_ACCEPT_BACKLOG : Constants.HTTP_GATEWAY_SERVER_ACCEPT_BACKLOG))
                .setCompressionSupported(Config.getInstance().getConfigJsonObject()
                        .getBoolean((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_ENABLE_COMPRESSION_SUPPORT : Constants.HTTP_GATEWAY_SERVER_ENABLE_COMPRESSION_SUPPORT))
                .setIdleTimeout(Config.getInstance().getConfigJsonObject()
                        .getInteger((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_IDLE_TIMEOUT : Constants.HTTP_GATEWAY_SERVER_IDLE_TIMEOUT));
        if (isSSL) {
            httpServerOptions.setSsl(true)
                    .setKeyStoreOptions(
                            new JksOptions()
                                    .setPath(Constants.HTTPS_GATEWAY_SSL_KEYSTORE_PATH)
                                    .setPassword(Constants.HTTPS_GATEWAY_SSL_KEYSTORE_PASSWORD)
                    )
                    .setTrustStoreOptions(
                            new JksOptions()
                                    .setPath(Constants.HTTPS_GATEWAY_SSL_TRUSTSTORE_PATH)
                                    .setPassword(Constants.HTTPS_GATEWAY_SSL_TRUSTSTORE_PASSWORD)
                    );
        }
        HttpServer server = vertx.createHttpServer(httpServerOptions);
        server.requestStream()
                .toFlowable()
                .map(HttpServerRequest::pause)
                .onBackpressureDrop(req -> req.response().setStatusCode(HttpResponseStatus.SERVICE_UNAVAILABLE.code()).end())
                .observeOn(RxHelper.scheduler(vertx.getDelegate()))
                .subscribe(req -> {
                    req.resume();
                    router.accept(req);
                });

        return server
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .rxListen(serverPort, serverHost)
                .flatMap(httpServer -> {
                    logger.trace("http server started | {}:{}", serverHost, serverPort);
                    return Single.just(httpServer);
                });

/*
        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> logger.error(event.getLocalizedMessage(), event))
                .requestHandler(router::accept)
                .rxListen(serverPort, serverHost);
*/
    }

    Single<Router> enableCorsSupport(Router router) {
        logger.trace("---enableCorsSupport invoked");
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
        router.route().handler(CorsHandler.create("http(s)?:\\/\\/(.+\\.)?(192\\.168\\..*|apiportal\\.com)(:\\d{1,5})?$")
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

    private Single<JDBCClient> initializeJdbcClient(String dataSourceName) {
        logger.trace("---initializeJdbcClient[{}] invoked", dataSourceName);
        abyssJDBCService = new AbyssJDBCService(vertx);
        return abyssJDBCService.publishDataSource(dataSourceName)
                .flatMap(rec -> abyssJDBCService.getJDBCServiceObject(dataSourceName))
                .flatMap(jdbcClient -> {
                    this.jdbcClient = jdbcClient;
                    return Single.just(jdbcClient);
                });
    }

    Completable initializeServer() {
        logger.trace("---initializeServer invoked");
        return Completable.fromSingle(initializeJdbcClient(Constants.GATEWAY_DATA_SOURCE_SERVICE)
                .flatMap(jdbcClient -> createRouter())
                .flatMap(this::enableCorsSupport)
                .flatMap(router -> createHttpServer(router,
                        verticleConf.serverHost,
                        verticleConf.serverPort,
                        verticleConf.isSSL)
                )
                .doOnSuccess(httpServer -> logger.info("initializeServer successful"))
                .doOnError(throwable -> logger.error("initializeServer error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace())));
    }

    Completable registerEchoHttpService() {
        logger.trace("---registerEchoHttpService invoked");
        Record record = new Record()
                .setType("http-endpoint")
                .setLocation((new HttpLocation()
                        .setSsl(false)
                        .setHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_ECHO_SERVER_HOST))
                        .setPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_ECHO_SERVER_PORT))
                        .setRoot("/")
                        .toJson()))
                .setName(Constants.ECHO_HTTP_SERVICE)
                .setMetadata(new JsonObject().put("some-label", "some-value"));
        return Completable.fromSingle(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).flatMap(Single::just));
    }

    Completable testEchoHttpService() {
        logger.trace("---testEchoHttpService invoked");
        return Completable.fromSingle(
                AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", Constants.ECHO_HTTP_SERVICE))
                        .flatMap(record -> {
                            ServiceReference serviceReference = AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().getReference(record);
                            try {
                                //WebClient webClient = serviceReference.getAs(WebClient.class);
                                HttpClient httpClient = serviceReference.getAs(io.vertx.reactivex.core.http.HttpClient.class);
                                httpClient.post(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_ECHO_SERVER_PORT),
                                        Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_ECHO_SERVER_HOST),
                                        "/", resp -> {
                                            //System.out.println("Got echo response " + resp.statusCode());
                                            //resp.handler(buf -> System.out.println(buf.toString("UTF-8")));
                                            resp.handler(buf -> logger.trace("status:{} response:{}", resp.statusCode(), buf.toString("UTF-8")));
                                        })
                                        .setChunked(true)
                                        .putHeader("Content-Type", "text/plain")
                                        .write("hello").end();
                            } finally {
                                AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().release(serviceReference);
                                logger.trace("{}service released", serviceReference.record().getName());
                            }
                            return Single.just(serviceReference);
                        })
                        .doOnError(throwable -> {
                            logger.error("testEchoHttpService error");
                            logger.error(throwable.getLocalizedMessage());
                            logger.error(Arrays.toString(throwable.getStackTrace()));
                        })
        );
    }

    public class AbyssServiceReference {
        ServiceReference serviceReference;
        HttpClient httpClient;

        AbyssServiceReference(ServiceReference serviceReference, HttpClient httpClient) {
            this.serviceReference = serviceReference;
            this.httpClient = httpClient;
        }

        public ServiceReference getServiceReference() {
            return serviceReference;
        }

        public HttpClient getHttpClient() {
            return httpClient;
        }
    }

    Single<AbyssServiceReference> lookupHttpService(String serviceName) {
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", serviceName))
                .flatMap(record -> {
                    ServiceReference serviceReference = AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().getReference(record);
                    HttpClient httpClient = serviceReference.getAs(io.vertx.reactivex.core.http.HttpClient.class);
                    return Single.just(new AbyssServiceReference(serviceReference, httpClient));
                })
                .doOnError(throwable -> {
                    logger.error("lookupHttpService error - {} | {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                })
                .doAfterSuccess(serviceReference -> {
                    logger.trace("{} service lookup completed successfully", serviceName);
                });
    }

    Completable releaseHttpService(ServiceReference serviceReference) {
        if (AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().release(serviceReference)) {
            logger.trace("{}service released", serviceReference.record().getName());
            return Completable.complete();
        } else {
            logger.error("releaseHttpService error");
            return Completable.error(Throwable::new);
        }
    }

    public class AbyssHttpRequest {
        AbyssServiceReference abyssServiceReference;
        RoutingContext context;

        public AbyssHttpRequest(AbyssServiceReference abyssServiceReference, RoutingContext context) {
            this.abyssServiceReference = abyssServiceReference;
            this.context = context;
        }
    }

    Flowable<HttpClientResponse> invokeHttpService(AbyssHttpRequest abyssHttpRequest) {
        JsonObject apiSpec = new JsonObject(abyssHttpRequest.abyssServiceReference.serviceReference.record().getMetadata().getString("apiSpec"));
        JsonArray servers = apiSpec.getJsonArray(OpenAPIUtil.OPENAPI_SECTION_SERVERS);
        URL serverURL;
        try {
            serverURL = new URL(servers.getJsonObject(new Random().nextInt(servers.size())).getString("url"));
        } catch (MalformedURLException e) {
            logger.error("malformed server url {}", servers.getJsonObject(new Random().nextInt(servers.size())).getString("url"));
            return Flowable.error(e);
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
                .setSsl("https".equals(serverURL.getProtocol()))
                .setTrustAll(false) //TODO: re-engineering for parametric trust certificate of api
                .setVerifyHost(true)); //TODO: re-engineering for parametric trust certificate of api
//                .setProxyOptions(new ProxyOptions()
//                        .setType(ProxyType.HTTP)
//                        .setHost("localhost")
//                        .setPort(8080)))
        RequestOptions requestOptions = new RequestOptions()
                .setHost(serverURL.getHost())
                .setPort(serverURL.getPort())
                .setSsl("https".equals(serverURL.getProtocol()))
                .setURI(serverURL.getPath());
        HttpClientRequest request = httpClient
                .request(abyssHttpRequest.context.request().method(), requestOptions);
        request.headers().setAll(abyssHttpRequest.context.request().headers());
        return request
                .toFlowable()
                .doOnSubscribe(subscription -> request.end());

/*
*çağrılan yerden aşağıdaki gibi subscribe olunacak ve tüketilecek...
        request.toFlowable().subscribe(httpClientResponse -> {
            logger.trace("httpClientResponse statusCode: {} - headers: {}", httpClientResponse.statusCode(), httpClientResponse.headers());
        });
*/


/*
                .put(8282, "localhost", "/", resp -> {
            System.out.println("Got response " + resp.statusCode());
            resp.bodyHandler(body -> System.out.println("Got data " + body.toString("ISO-8859-1")));
        });
*/


    }


    Completable loadAllProxyApis() {
        logger.trace("---loadAllProxyApis invoked");
        return Completable.complete();
    }

    public void routingContextHandler(RoutingContext context) {
        logger.trace("---routingContextHandler invoked");
        context.response().setStatusCode(HttpResponseStatus.OK.code()).end(HttpResponseStatus.OK.reasonPhrase(), "UTF-8");
    }

    static final class VerticleConf {
        String serverHost;
        int serverPort;
        Boolean isSSL = Boolean.FALSE;
        Boolean isSandbox = Boolean.FALSE;

        VerticleConf(String serverHost, int serverPort, Boolean isSSL, Boolean isSandbox) {
            this.serverHost = serverHost;
            this.serverPort = serverPort;
            this.isSSL = isSSL;
            this.isSandbox = isSandbox;
        }
    }

    void dummySecuritySchemaHandler(SecurityScheme securityScheme, RoutingContext routingContext) {
        logger.trace("---dummySecuritySchemaHandler invoked");
        routingContext.next();
    }

    void genericSecuritySchemaHandler(SecurityScheme securityScheme, RoutingContext routingContext) {
        logger.trace("---genericSecuritySchemaHandler invoked for security schema name {}", securityScheme.getName());
        SecurityScheme.Type securitySchemeType = securityScheme.getType();
        SecurityScheme.In securitySchemeIn = securityScheme.getIn();

        if (securitySchemeType == SecurityScheme.Type.APIKEY) {
            if (securitySchemeIn == SecurityScheme.In.COOKIE) {
                if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)) {
                    //check if this cookie exists in http request headers
                    if (routingContext.getCookie(Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME) == null) {
                        logger.error("platform security scheme cookie [{}] does not exist inside cookies", Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
                        routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                        return;
                    }
                    // Handle security here
                    User user = routingContext.user();
                    if (user == null) {
                        routingContext.fail(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                        return;
                    }
                    if (user.principal().isEmpty()) {
                        routingContext.fail(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                    }
                } else if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                    logger.error("unsupported platform security scheme [{}] in cookie", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                }
            } else if (securitySchemeIn == SecurityScheme.In.HEADER) {
                if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)) {
                    logger.error("unsupported platform security scheme [{}] in header", Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                } else if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                    //check if this access token sent via http request header
                    if (!routingContext.request().headers().names().contains(Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                        logger.error("platform security scheme access token [{}] does not exist inside headers", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                        routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                        return;
                    }
                    String apiAccessToken = routingContext.request().getHeader(Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                    AuthenticationService authenticationService = new AuthenticationService(vertx);
                    authenticationService.validateAccessToken(apiAccessToken).subscribe(accessTokenValidation -> {
                        if (!accessTokenValidation.getBoolean("status")) {
                            logger.error("platform security scheme access token [{}] validation failed: {} \n validation report: {}",
                                    Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME,
                                    accessTokenValidation.getString("error"),
                                    accessTokenValidation.getJsonObject("validationreport"));
                            routingContext.fail(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                        } else {
                            routingContext.put("validationreport", accessTokenValidation.getJsonObject("validationreport"));
                            routingContext.next();
                        }
                    }, throwable -> {
                        logger.error("error occured during platform security scheme access token [{}] validation: {} | {}", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME, throwable.getLocalizedMessage(), throwable.getStackTrace());
                        routingContext.fail(new InternalServerError500Exception(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
                    });
                }
            } else if (securitySchemeIn == SecurityScheme.In.QUERY) {
                if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)) {
                    logger.error("unsupported platform security scheme [{}] as query parameter", Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                } else if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                    logger.error("unsupported platform security scheme [{}] as query parameter", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                }
            }
        } else if (securitySchemeType == SecurityScheme.Type.HTTP) {
            logger.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        } else if (securitySchemeType == SecurityScheme.Type.OAUTH2) {
            logger.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        } else if (securitySchemeType == SecurityScheme.Type.OPENIDCONNECT) {
            logger.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        } else {
            //logger.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        }
    }

    void genericFailureHandler(RoutingContext routingContext) {
        logger.error("failureHandler invoked {} | {} ", routingContext.failure().getLocalizedMessage(), routingContext.failure().getStackTrace());

        // This is the failure handler
        Throwable failure = routingContext.failure();
        if (failure instanceof ValidationException)
            // Handle Validation Exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code())
                    .setStatusMessage(HttpResponseStatus.UNPROCESSABLE_ENTITY.reasonPhrase() + " " + ((ValidationException) failure).type().name() + " " + failure.getLocalizedMessage())
                    .end();
        else if (failure instanceof AbyssApiException)
            //Handle Abyss Api Exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(((AbyssApiException) failure).getHttpResponseStatus().code())
                    .setStatusMessage(((AbyssApiException) failure).getHttpResponseStatus().reasonPhrase())
                    .end(((AbyssApiException) failure).getApiError().toJson().toString(), "UTF-8");
        else
            // Handle other exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(routingContext.statusCode())
                    .setStatusMessage(failure.getLocalizedMessage())
                    .end();
    }

    void genericOperationHandler(RoutingContext routingContext) {
        logger.trace("---genericOperationHandler invoked");
        if (routingContext.get("method") == null)
            routingContext.put("method", routingContext.request().method());
        else {
            logger.trace("genericOperationHandler already executed, so skipping..");
            routingContext.next();
            return;
        }
        String requestUriPath = routingContext.request().path();
        String requestedApi = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length(), ("/" + Constants.ABYSS_GW + "/").length() + 36);
        String pathParameters = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length() + 36, requestUriPath.length());
        logger.trace("captured uri: {} | path parameter: {}", requestUriPath, pathParameters);
        logger.trace("captured mountpoint: {} | method: {}", routingContext.mountPoint(), routingContext.request().method().toString());
        JsonObject validationReport = routingContext.get("validationreport");
        JsonObject apiSpec = new JsonObject(validationReport.getString("businessapiopenapidocument"));

        OpenAPIUtil.openAPIParser(apiSpec)
                .flatMap(swaggerParseResult -> {
                    List<Server> serversList = swaggerParseResult.getOpenAPI().getServers();
                    URL businessApiServerURL;
                    String businessApiServerURLStr = serversList.get(new Random().nextInt(serversList.size())).getUrl();
                    try {
                        businessApiServerURL = new URL(businessApiServerURLStr + pathParameters);
                        return Single.just(businessApiServerURL);
                    } catch (MalformedURLException e) {
                        logger.error("malformed server url {}", businessApiServerURLStr);
                        return Single.error(e);
                    }
                })
                .subscribe(businessApiServerURL -> {
                            logger.trace("Business API Server URL : {} Path: {}", businessApiServerURL, businessApiServerURL.getPath());
                            HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
                                    .setSsl("https".equals(businessApiServerURL.getProtocol()))
                                    .setTrustAll(true) //TODO: re-engineering for parametric trust certificate of api
                                    .setVerifyHost(false)
                                    .setLogActivity(true)
                                    .setMaxPoolSize(50)
                            );
                            RequestOptions requestOptions = new RequestOptions()
                                    .setHost(businessApiServerURL.getHost());
                            if (businessApiServerURL.getPort() != -1) {
                                requestOptions.setPort(businessApiServerURL.getPort());
                            } else {
                                if ("https".equals(businessApiServerURL.getProtocol()))
                                    requestOptions.setPort(443);
                            }
                            requestOptions
                                    .setSsl("https".equals(businessApiServerURL.getProtocol()))
                                    .setURI(businessApiServerURL.getPath());

                            if (routingContext.request().params().size() > 0) {
                                String apiQueryParams = "?";

                                for (Map.Entry<String, String> stringStringEntry : routingContext.request().params().getDelegate()) {
                                    apiQueryParams = (apiQueryParams.equals("?")) ? apiQueryParams : apiQueryParams.concat("&");
                                    apiQueryParams = apiQueryParams.concat(stringStringEntry.getKey()).concat("=").concat(stringStringEntry.getValue());
                                }
                                requestOptions.setURI(businessApiServerURL.getPath().concat(apiQueryParams));
                            }
                            // pass through http request method
                            HttpClientRequest request = httpClient.request(routingContext.request().method(), requestOptions);
                            request.setChunked(true);
                            routingContext.response().setChunked(true);
                            // pass through http request headers
                            request.headers().setAll(routingContext.request().headers());

/*
                            request.endHandler(event -> {
                                logger.trace("request stream ended");
                                routingContext.response().headers().setAll(request.headers());
                                //routingContext.response().end();
                                routingContext.response().setStatusCode(HttpResponseStatus.OK.code()).end();
                            });
*/
                            //routingContext.request().handler(request::write);
/*
                            routingContext.request().endHandler(event -> {
                                request.end();
                            });
*/
                            request
                                    .toFlowable()
                                    .flatMap(httpClientResponse -> {
                                        logger.trace("httpClientResponse statusCode: {} | statusMessage: {}", httpClientResponse.statusCode(), httpClientResponse.statusMessage());
                                        routingContext.response()
                                                .setStatusCode(httpClientResponse.statusCode())
                                                .putHeader("Content-Type", "application/json; charset=utf-8");
                                        return httpClientResponse.toFlowable();
                                    })
                                    .doFinally(() -> {
                                        //the final
                                        logger.trace("finally finished");
                                        routingContext.response().end();
                                        routingContext.next();
                                    })
                                    .subscribe(data -> {
                                        //logger.trace("httpClientResponse subcribe data: {}", data);
                                        //////routingContext.response().write(data);
                                        routingContext.response().headers().setAll(request.headers());
                                        routingContext.response()
                                                .putHeader("Content-Type", "application/json; charset=utf-8")
                                                .write(data);
                                        //routingContext.next();
                                    }, throwable -> {
                                        logger.error("error occured during business api invocation, error: {} | stack: {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                                        routingContext.fail(new InternalServerError500Exception(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
                                    });
                            request.end();
                        },
                        throwable -> {
                            logger.error("error occured during business api spec parsing and server url extraction, error: {} | stack: {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                            routingContext.fail(new InternalServerError500Exception(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
                        });

/*
        OpenAPIUtil.openAPIParser(apiSpec)
                .flatMap(swaggerParseResult -> {
                    List<Server> serversList = swaggerParseResult.getOpenAPI().getServers();
                    URL businessApiServerURL;
                    String businessApiServerURLStr = serversList.get(new Random().nextInt(serversList.size())).getUrl();
                    try {
                        businessApiServerURL = new URL(businessApiServerURLStr);
                        return Single.just(businessApiServerURL);
                    } catch (MalformedURLException e) {
                        logger.error("malformed server url {}", businessApiServerURLStr);
                        return Single.error(e);
                    }
                })
                .flatMap(businessApiServerURL -> {
                    HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
                            .setSsl("https".equals(businessApiServerURL.getProtocol()))
                            .setTrustAll(false) //TODO: re-engineering for parametric trust certificate of api
                            .setVerifyHost(true)); //TODO: re-engineering for parametric trust certificate of api
                    RequestOptions requestOptions = new RequestOptions()
                            .setHost(businessApiServerURL.getHost())
                            .setPort(businessApiServerURL.getPort())
                            .setSsl("https".equals(businessApiServerURL.getProtocol()))
                            .setURI(businessApiServerURL.getPath());
                    // pass through http request method
                    HttpClientRequest request = httpClient.request(routingContext.request().method(), requestOptions);
                    // pass through http request headers
                    request.headers().setAll(routingContext.request().headers());
                    request.end(routingContext.getBody());
                    return Single.just(request);
                })
                .toFlowable()
                .flatMap(HttpClientRequest::toFlowable)
                .subscribe(httpClientResponse -> routingContext.response().setStatusCode(httpClientResponse.statusCode()).end(httpClientResponse.));
*/



/*
                .doOnError(throwable -> logger.error("loading API proxy error {} | {} | {}", apiUUID, throwable.getLocalizedMessage(), throwable.getStackTrace()))
                .doAfterSuccess(swaggerParseResult -> logger.trace("successfully loaded API proxy {}", apiUUID))
                .doFinally(() -> logger.trace("+++++gatewayRouter route list: {}", gatewayRouter.getRoutes()))
                .subscribe();
*/

    }

    void genericAuthorizationHandler(RoutingContext routingContext) {
        logger.trace("---genericAuthorizationHandler invoked");
        String requestUriPath = routingContext.request().path();
        String requestedApi = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length(), ("/" + Constants.ABYSS_GW + "/").length() + 36);
        String pathParameters = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length() + 36, requestUriPath.length());
        logger.trace("captured uri: {} | path parameter: {}", requestUriPath, pathParameters);
        logger.trace("captured mountpoint: {} | method: {}", routingContext.mountPoint(), routingContext.request().method().toString());
        JsonObject validationReport = routingContext.get("validationreport");

        AuthorizationService authorizationService = new AuthorizationService(vertx);
        if (authorizationService.authorize(UUID.fromString(validationReport.getString("apiuuid")), UUID.fromString(requestedApi)))
            routingContext.next();
        else
            routingContext.fail(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));

    }

}
