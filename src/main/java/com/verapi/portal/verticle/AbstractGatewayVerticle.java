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
import com.verapi.abyss.common.OpenAPIUtil;
import com.verapi.abyss.exception.AbyssApiException;
import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.abyss.exception.NotFound404Exception;
import com.verapi.abyss.exception.UnAuthorized401Exception;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.service.idam.AuthenticationService;
import com.verapi.portal.service.idam.AuthorizationService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import io.vertx.core.Future;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.http.HttpVersion;
import io.vertx.core.http.RequestOptions;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.auth.jdbc.JDBCHashStrategy;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpClientRequest;
import io.vertx.reactivex.core.http.HttpClientResponse;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.CookieHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
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
import java.nio.charset.StandardCharsets;
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
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGatewayVerticle.class);

    static Router gatewayRouter;
    JDBCAuth jdbcAuth;
    VerticleConf verticleConf;
    private AbyssJDBCService abyssJDBCService;
    private JDBCClient jdbcClient;

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.trace("---start invoked");
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        LOGGER.trace("---stop invoked");
        super.stop(stopFuture);
    }

    private Single<Router> createRouter() {
        LOGGER.trace("---createRouter invoked");
        gatewayRouter = Router.router(vertx);
        return configureRouter(gatewayRouter);
    }

    private Single<Router> configureRouter(Router router) {
        LOGGER.trace("---configureRouter invoked");

        //log HTTP requests
        //router.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

        // 1: install cookie handler
        //A handler which decodes cookies from the request, makes them available in the RoutingContext and writes them back in the response
        router.route().handler(CookieHandler.create());

        // 2: install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        //router.route().handler(BodyHandler.create()); //TODO: Web API Contract - Router Factory automatically mounts a BodyHandler to manage request bodies.

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

        LOGGER.trace("router route list: {}", router.getRoutes());

        // jdbcAuth is only prepared for UserSessionHandler usage inside openAPI routers
        jdbcAuth = JDBCAuth.create(vertx, jdbcClient);

        jdbcAuth.getDelegate().setHashStrategy(JDBCHashStrategy.createPBKDF2(vertx.getDelegate()));

        jdbcAuth.setAuthenticationQuery("select password, passwordsalt from subject where isdeleted = false and isactivated = true and subjectname = ?");

        jdbcAuth.setPermissionsQuery("select permission from subject_permission up, subject u where um.subjectname = ? and up.subjectid = u.id");

        jdbcAuth.setRolesQuery("select groupname from subject_group ug, subject_membership um, subject u where u.subjectname = ? and um.subjectid = u.id and um.groupId = ug.id");

        return Single.just(router);
    }

/*
    Single<Router> createSubRouter(String mountPoint) {
        LOGGER.trace("---createSubRouter invoked");

        if (!mountPoint.startsWith("/"))
            mountPoint = "/" + mountPoint;
        Router subRouter = Router.router(vertx);

        //log HTTP requests
        //subRouter.route().handler(LoggerHandler.create(LoggerFormat.DEFAULT));

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
*/

    private void failureHandler(RoutingContext routingContext) {
        LOGGER.trace("AbstractGatewayVerticle - failureHandler invoked");

        // This is the failure handler
        Throwable failure = routingContext.failure();
        LOGGER.error("AbstractGatewayVerticle - failure: {}\n{}", failure.getLocalizedMessage(), Arrays.toString(failure.getStackTrace()));

        if (routingContext.response().ended()) {
            LOGGER.error("AbstractGatewayVerticle - failureHandler invoked but response has already been ended. So quiting handler...");
            return;
        }


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
        LOGGER.trace("---createHttpServer invoked");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                //.setMaxChunkSize(8192) //TODO: Http Chunked Serving
                .setLogActivity(Config.getInstance().getConfigJsonObject()
                        .getBoolean((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_LOG_HTTP_ACTIVITY : Constants.HTTP_GATEWAY_SERVER_LOG_HTTP_ACTIVITY))
                .setAcceptBacklog(Config.getInstance().getConfigJsonObject()
                        .getInteger((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_ACCEPT_BACKLOG : Constants.HTTP_GATEWAY_SERVER_ACCEPT_BACKLOG))
                .setCompressionSupported(Config.getInstance().getConfigJsonObject()
                        .getBoolean((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_ENABLE_COMPRESSION_SUPPORT : Constants.HTTP_GATEWAY_SERVER_ENABLE_COMPRESSION_SUPPORT))
                .setIdleTimeout(Config.getInstance().getConfigJsonObject()
                        .getInteger((isSSL) ? Constants.HTTPS_GATEWAY_SERVER_IDLE_TIMEOUT : Constants.HTTP_GATEWAY_SERVER_IDLE_TIMEOUT))
                .setAcceptBacklog(1000000);
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
/*
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
                .exceptionHandler(event -> LOGGER.error(event.getLocalizedMessage(), event))
                .rxListen(serverPort, serverHost)
                .flatMap(httpServer -> {
                    LOGGER.trace("http server started | {}:{}", serverHost, serverPort);
                    return Single.just(httpServer);
                });
*/

        return vertx.createHttpServer(httpServerOptions)
                .exceptionHandler(event -> LOGGER.error(event.getLocalizedMessage(), event))
                .requestHandler(router::accept)
                .rxListen(serverPort, serverHost);
    }

    Single<Router> enableCorsSupport(Router router) {
        LOGGER.trace("---enableCorsSupport invoked");
        Set<String> allowHeaders = new HashSet<>();
        allowHeaders.add("x-requested-with");
        allowHeaders.add("Access-Control-Allow-Origin");
        allowHeaders.add("Access-Control-Allow-Credentials");
        allowHeaders.add("origin");
        allowHeaders.add(HttpHeaders.CONTENT_TYPE.toString());
        allowHeaders.add("accept");
        allowHeaders.add("Cookie");
        // CORS support
        router.route().handler(CorsHandler.create("http(s)?:\\/\\/(.+\\.)?(192\\.168\\..*|apiportal\\.com|localhost)(:\\d{1,5})?$")
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
        LOGGER.trace("---initializeJdbcClient[{}] invoked", dataSourceName);
        abyssJDBCService = new AbyssJDBCService(vertx);
        return abyssJDBCService.publishDataSource(dataSourceName)
                .flatMap(rec -> abyssJDBCService.getJDBCServiceObject(dataSourceName))
                .flatMap(jdbcClient -> {
                    this.jdbcClient = jdbcClient;
                    return Single.just(jdbcClient);
                });
    }

    Completable initializeServer() {
        LOGGER.trace("---initializeServer invoked");
        return Completable.fromSingle(initializeJdbcClient(Constants.GATEWAY_DATA_SOURCE_SERVICE)
                .flatMap(jdbcClient -> createRouter())
                .flatMap(this::enableCorsSupport)
                .flatMap(router -> createHttpServer(router,
                        verticleConf.serverHost,
                        verticleConf.serverPort,
                        verticleConf.isSSL)
                )
                .doOnSuccess(httpServer -> LOGGER.info("initializeServer successful"))
                .doOnError(throwable -> LOGGER.error("initializeServer error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace())));
    }

    Completable registerEchoHttpService() {
        LOGGER.trace("---registerEchoHttpService invoked");
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
        LOGGER.trace("---testEchoHttpService invoked");
        return Completable.fromSingle(
                AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", Constants.ECHO_HTTP_SERVICE))
                        .flatMapSingle(record -> {
                            ServiceReference serviceReference = AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().getReference(record);
                            try {
                                //WebClient webClient = serviceReference.getAs(WebClient.class);
                                HttpClient httpClient = serviceReference.getAs(io.vertx.reactivex.core.http.HttpClient.class);
                                httpClient.post(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_ECHO_SERVER_PORT),
                                        Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_ECHO_SERVER_HOST),
                                        "/", resp -> {
                                            //System.out.println("Got echo response " + resp.statusCode());
                                            //resp.handler(buf -> System.out.println(buf.toString("UTF-8")));
                                            resp.handler(buf -> LOGGER.trace("status:{} response:{}", resp.statusCode(), buf.toString(StandardCharsets.UTF_8)));
                                        })
                                        .setChunked(true)
                                        .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                                        .write("hello").end();
                            } finally {
                                AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().release(serviceReference);
                                LOGGER.trace("{}service released", serviceReference.record().getName());
                            }
                            return Single.just(serviceReference);
                        })
                        .doOnError(throwable -> {
                            LOGGER.error("testEchoHttpService error");
                            LOGGER.error(throwable.getLocalizedMessage());
                            LOGGER.error(Arrays.toString(throwable.getStackTrace()));
                        })
        );
    }

    Single<AbyssServiceReference> lookupHttpService(String serviceName) {
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", serviceName))
                .flatMapSingle(record -> {
                    ServiceReference serviceReference = AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().getReference(record);
                    HttpClient httpClient = serviceReference.getAs(io.vertx.reactivex.core.http.HttpClient.class);
                    return Single.just(new AbyssServiceReference(serviceReference, httpClient));
                })
                .doOnError(throwable -> {
                    LOGGER.error("lookupHttpService error - {} | {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                })
                .doAfterSuccess(serviceReference -> {
                    LOGGER.trace("{} service lookup completed successfully", serviceName);
                });
    }

    Completable releaseHttpService(ServiceReference serviceReference) {
        if (AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().release(serviceReference)) {
            LOGGER.trace("{}service released", serviceReference.record().getName());
            return Completable.complete();
        } else {
            LOGGER.error("releaseHttpService error");
            return Completable.error(Throwable::new);
        }
    }

    Flowable<HttpClientResponse> invokeHttpService(AbyssHttpRequest abyssHttpRequest) {
        JsonObject apiSpec = new JsonObject(abyssHttpRequest.abyssServiceReference.serviceReference.record().getMetadata().getString("apiSpec"));
        JsonArray servers = apiSpec.getJsonArray(OpenAPIUtil.OPENAPI_SECTION_SERVERS);
        URL serverURL;
        int serverPosition = new Random().nextInt(servers.size());
        try {
            serverURL = new URL(servers.getJsonObject(serverPosition).getString("url"));
        } catch (MalformedURLException e) {
            LOGGER.error("malformed server url {}", servers.getJsonObject(serverPosition).getString("url"));
            return Flowable.error(e);
        }

        HttpClient httpClient = vertx.createHttpClient(new HttpClientOptions()
                //.setUseAlpn(true).setAlpnVersions().setHttp2MaxPoolSize() //TODO: Support Http/2
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
        request.setChunked(true);
        request.setTimeout(30000);
        return request
                .toFlowable()
                .doOnSubscribe(subscription -> request.end());

/*
*çağrılan yerden aşağıdaki gibi subscribe olunacak ve tüketilecek...
        request.toFlowable().subscribe(httpClientResponse -> {
            LOGGER.trace("httpClientResponse statusCode: {} - headers: {}", httpClientResponse.statusCode(), httpClientResponse.headers());
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
        LOGGER.trace("---loadAllProxyApis invoked");
        return Completable.complete();
    }

    public void routingContextHandler(RoutingContext context) {
        LOGGER.trace("---routingContextHandler invoked");
        context.response().setStatusCode(HttpResponseStatus.OK.code()).end(HttpResponseStatus.OK.reasonPhrase(), StandardCharsets.UTF_8.toString());
    }

    void dummySecuritySchemaHandler(SecurityScheme securityScheme, RoutingContext routingContext) {
        LOGGER.trace("---dummySecuritySchemaHandler invoked");
        routingContext.next();
    }

    void genericSecuritySchemaHandler(SecurityScheme securityScheme, RoutingContext routingContext) {
        LOGGER.trace("---genericSecuritySchemaHandler invoked for security schema name {}", securityScheme.getName());
        SecurityScheme.Type securitySchemeType = securityScheme.getType();
        SecurityScheme.In securitySchemeIn = securityScheme.getIn();

        if (securitySchemeType == SecurityScheme.Type.APIKEY) {
            if (securitySchemeIn == SecurityScheme.In.COOKIE) {
                if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)) {
                    //check if this cookie exists in http request headers
                    if (routingContext.getCookie(Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME) == null) {
                        LOGGER.error("platform security scheme cookie [{}] does not exist inside cookies", Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
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
                    LOGGER.error("unsupported platform security scheme [{}] in cookie", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                }
            } else if (securitySchemeIn == SecurityScheme.In.HEADER) {
                if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)) {
                    LOGGER.error("unsupported platform security scheme [{}] in header", Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                } else if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                    //check if this access token sent via http request header
                    if (!routingContext.request().headers().names().contains(Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                        LOGGER.error("platform security scheme access token [{}] does not exist inside headers", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                        routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                        return;
                    }
                    String apiAccessToken = routingContext.request().getHeader(Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                    AuthenticationService authenticationService = new AuthenticationService(vertx);
                    authenticationService.validateAccessToken(apiAccessToken).subscribe(accessTokenValidation -> {
                        if (!accessTokenValidation.getBoolean("status")) {
                            LOGGER.error("platform security scheme access token [{}] validation failed: {} \n validation report: {}",
                                    Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME,
                                    accessTokenValidation.getString("error"),
                                    accessTokenValidation.getJsonObject("validationreport"));
                            routingContext.fail(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                        } else {
                            routingContext.put("validationreport", accessTokenValidation.getJsonObject("validationreport"));
                            routingContext.next();
                        }
                    }, throwable -> {
                        LOGGER.error("error occured during platform security scheme access token [{}] validation: {} | {}", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME, throwable.getLocalizedMessage(), throwable.getStackTrace());
                        routingContext.fail(new InternalServerError500Exception(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
                    });
                }
            } else if (securitySchemeIn == SecurityScheme.In.QUERY) {
                if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME)) {
                    LOGGER.error("unsupported platform security scheme [{}] as query parameter", Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                } else if (Objects.equals(securityScheme.getName(), Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME)) {
                    LOGGER.error("unsupported platform security scheme [{}] as query parameter", Constants.AUTH_ABYSS_GATEWAY_API_ACCESSTOKEN_NAME);
                    routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
                }
            }
        } else if (securitySchemeType == SecurityScheme.Type.HTTP) {
            LOGGER.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        } else if (securitySchemeType == SecurityScheme.Type.OAUTH2) {
            LOGGER.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        } else if (securitySchemeType == SecurityScheme.Type.OPENIDCONNECT) {
            LOGGER.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        } else {
            //LOGGER.error("unsupported platform security scheme [{}]", securitySchemeType.name());
            routingContext.fail(new NotFound404Exception(HttpResponseStatus.NOT_FOUND.reasonPhrase()));
        }
    }

    void genericFailureHandler(RoutingContext routingContext) {
        LOGGER.error("AbstractGatewayVerticle - genericFailureHandler invoked {} | {} ", routingContext.failure().getLocalizedMessage(), routingContext.failure().getStackTrace());

        // This is the failure handler
        Throwable failure = routingContext.failure();
        if (failure instanceof ValidationException)
            // Handle Validation Exception
            routingContext.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .setStatusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code())
                    .setStatusMessage(HttpResponseStatus.UNPROCESSABLE_ENTITY.reasonPhrase() + " " + ((ValidationException) failure).type().name() + " " + failure.getLocalizedMessage())
                    .end();
        else if (failure instanceof AbyssApiException)
            //Handle Abyss Api Exception
            routingContext.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .setStatusCode(((AbyssApiException) failure).getHttpResponseStatus().code())
                    .setStatusMessage(((AbyssApiException) failure).getHttpResponseStatus().reasonPhrase())
                    .end(((AbyssApiException) failure).getApiError().toJson().toString(), StandardCharsets.UTF_8.toString());
        else
            // Handle other exception
            routingContext.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                    .setStatusCode(routingContext.statusCode())
                    .setStatusMessage(failure.getLocalizedMessage())
                    .end();
    }

    void genericOperationHandler(RoutingContext routingContext) {
        LOGGER.trace("---genericOperationHandler invoked");

        class BusinessApi {
            private URL serverURL;
            private HttpVersion protocolVersion;

            private BusinessApi(URL serverURL, HttpVersion protocolVersion) {
                this.serverURL = serverURL;
                this.protocolVersion = protocolVersion;
            }
        }

        if (routingContext.get("method") == null)
            routingContext.put("method", routingContext.request().method());
        else {
            LOGGER.trace("genericOperationHandler already executed, so skipping..");
            routingContext.next();
            return;
        }
        String requestUriPath = routingContext.request().path();
        String requestedApi = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length(), ("/" + Constants.ABYSS_GW + "/").length() + 36);
        String pathParameters = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length() + 36);
        LOGGER.trace("captured uri: {} | path parameter: {}", requestUriPath, pathParameters);
        LOGGER.trace("captured mountpoint: {} | method: {}", routingContext.mountPoint(), routingContext.request().method().toString());
        JsonObject validationReport = routingContext.get("validationreport");
        JsonObject apiSpec = new JsonObject(validationReport.getString("businessapiopenapidocument"));
        HttpClientOptions httpClientOptions = new HttpClientOptions();

        OpenAPIUtil.openAPIParser(apiSpec)
                .flatMap(swaggerParseResult -> {
                    List<Server> serversList = swaggerParseResult.getOpenAPI().getServers();
                    URL businessApiServerURL;
                    int serverPosition = new Random().nextInt(serversList.size());
                    String businessApiServerURLStr = serversList.get(serverPosition).getUrl();
                    HttpVersion businessApiServerHttpProtocolVersion;

                    if (serversList.get(serverPosition).getExtensions() != null && serversList.get(serverPosition).getExtensions().containsKey(Constants.OPENAPI_HTTP_PROTOCOL_VERSION))
                        businessApiServerHttpProtocolVersion = HttpVersion.valueOf(serversList.get(serverPosition).getExtensions().get(Constants.OPENAPI_HTTP_PROTOCOL_VERSION).toString());
                    else
                        businessApiServerHttpProtocolVersion = HttpVersion.HTTP_1_1;

                    try {
                        businessApiServerURL = new URL(businessApiServerURLStr + pathParameters);
                        return Single.just(new BusinessApi(businessApiServerURL, businessApiServerHttpProtocolVersion));
                    } catch (MalformedURLException e) {
                        LOGGER.error("malformed server url {}", businessApiServerURLStr);
                        return Single.error(e);
                    }
                })
                .subscribe(businessApi -> {
                            LOGGER.trace("Business API Server URL : {} Path: {}", businessApi.serverURL, businessApi.serverURL.getPath());
                            HttpClient httpClient = vertx.createHttpClient(httpClientOptions
                                    .setSsl("https".equals(businessApi.serverURL.getProtocol()))
                                    .setTrustAll(true) //TODO: re-engineering for parametric trust certificate of api
                                    .setVerifyHost(false)
                                    //.setLogActivity(true)
                                    .setMaxPoolSize(50)
                                    .setProtocolVersion(businessApi.protocolVersion)
                            );
                            RequestOptions requestOptions = new RequestOptions()
                                    .setHost(businessApi.serverURL.getHost());
                            if (businessApi.serverURL.getPort() != -1) {
                                requestOptions.setPort(businessApi.serverURL.getPort());
                            } else {
                                if ("https".equals(businessApi.serverURL.getProtocol()))
                                    requestOptions.setPort(443);
                            }
                            requestOptions
                                    .setSsl("https".equals(businessApi.serverURL.getProtocol()))
                                    .setURI(businessApi.serverURL.getPath());

                            if (routingContext.request().params().size() > 0) {
                                String apiQueryParams = "?";

                                for (Map.Entry<String, String> stringStringEntry : routingContext.request().params().getDelegate()) {
                                    apiQueryParams = (apiQueryParams.equals("?")) ? apiQueryParams : apiQueryParams.concat("&");
                                    apiQueryParams = apiQueryParams.concat(stringStringEntry.getKey()).concat("=").concat(stringStringEntry.getValue());
                                }
                                requestOptions.setURI(businessApi.serverURL.getPath().concat(apiQueryParams));
                            }
                            // pass through http request method
                            HttpClientRequest request = httpClient.request(routingContext.request().method(), requestOptions);
                            request.setChunked(true);
                            routingContext.response().setChunked(true);
                            // pass through http request headers
                            request.headers().setAll(routingContext.request().headers());

/*
                            request.endHandler(event -> {
                                LOGGER.trace("request stream ended");
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
                                        LOGGER.trace("httpClientResponse statusCode: {} | statusMessage: {}", httpClientResponse.statusCode(), httpClientResponse.statusMessage());
                                        routingContext.response()
                                                .setStatusCode(httpClientResponse.statusCode())
                                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8");
                                        return httpClientResponse.toFlowable();
                                    })
                                    .doFinally(() -> {
                                        //the final
                                        LOGGER.trace("finally finished");
                                        routingContext.response().end();
                                        routingContext.next();
                                    })
                                    .subscribe(data -> {
                                        //LOGGER.trace("httpClientResponse subcribe data: {}", data);
                                        //////routingContext.response().write(data);
                                        routingContext.response().headers().setAll(request.headers());
                                        routingContext.response().setChunked(true); //TODO: Http Chunked Serving per request
                                        routingContext.response()
                                                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                                                .write(data);
                                        //routingContext.next();
                                    }, throwable -> {
                                        LOGGER.error("error occured during business api invocation, error: {} | stack: {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                                        routingContext.fail(new InternalServerError500Exception(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase()));
                                    });
                            request.end();
                        },
                        throwable -> {
                            LOGGER.error("error occured during business api spec parsing and server url extraction, error: {} | stack: {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
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
                        LOGGER.error("malformed server url {}", businessApiServerURLStr);
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
                .doOnError(throwable -> LOGGER.error("loading API proxy error {} | {} | {}", apiUUID, throwable.getLocalizedMessage(), throwable.getStackTrace()))
                .doAfterSuccess(swaggerParseResult -> LOGGER.trace("successfully loaded API proxy {}", apiUUID))
                .doFinally(() -> LOGGER.trace("+++++gatewayRouter route list: {}", gatewayRouter.getRoutes()))
                .subscribe();
*/

    }

    void genericAuthorizationHandler(RoutingContext routingContext) {
        LOGGER.trace("---genericAuthorizationHandler invoked");
        String requestUriPath = routingContext.request().path();
        String requestedApi = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length(), ("/" + Constants.ABYSS_GW + "/").length() + 36);
        String pathParameters = requestUriPath.substring(("/" + Constants.ABYSS_GW + "/").length() + 36);
        LOGGER.trace("captured uri: {} | path parameter: {}", requestUriPath, pathParameters);
        LOGGER.trace("captured mountpoint: {} | method: {}", routingContext.mountPoint(), routingContext.request().method().toString());
        JsonObject validationReport = routingContext.get("validationreport");

        AuthorizationService authorizationService = new AuthorizationService(vertx);
        if (authorizationService.authorize(UUID.fromString(validationReport.getString("apiuuid")), UUID.fromString(requestedApi)))
            //If (Consumed API is protected by Abyss Access Manager)
            //  Check if subject & organization is received in header request
            //      Permission Check(subject, organization, resource, action)
            //      If Permission GRANTED
            routingContext.next();
            //      Else
            //          Fail
            //  Else
            //      Fail
            //End If
        else
            routingContext.fail(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));

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

    public class AbyssHttpRequest {
        AbyssServiceReference abyssServiceReference;
        RoutingContext context;

        public AbyssHttpRequest(AbyssServiceReference abyssServiceReference, RoutingContext context) {
            this.abyssServiceReference = abyssServiceReference;
            this.context = context;
        }
    }

}
