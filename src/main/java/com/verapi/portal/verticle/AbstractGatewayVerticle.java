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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.net.JksOptions;
import io.vertx.ext.web.api.contract.RouterFactoryException;
import io.vertx.ext.web.handler.LoggerFormat;
import io.vertx.reactivex.RxHelper;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpClient;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerRequest;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.handler.BodyHandler;
import io.vertx.reactivex.ext.web.handler.CorsHandler;
import io.vertx.reactivex.ext.web.handler.LoggerHandler;
import io.vertx.reactivex.ext.web.handler.ResponseTimeHandler;
import io.vertx.reactivex.ext.web.handler.TimeoutHandler;
import io.vertx.reactivex.servicediscovery.ServiceReference;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpLocation;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLHandshakeException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

//import io.vertx.core.http.HttpClient;

//public class AbstractGatewayVerticle extends AbstractVerticle implements IGatewayVerticle {
public abstract class AbstractGatewayVerticle extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(AbstractGatewayVerticle.class);

    static Router gatewayRouter;
    private AbyssJDBCService abyssJDBCService;
    private JDBCClient jdbcClient;
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

        //install body handler
        //A handler which gathers the entire request body and sets it on the RoutingContext
        //It also handles HTTP file uploads and can be used to limit body sizes
        router.route().handler(BodyHandler.create());

        //If a request times out before the response is written a 503 response will be returned to the client, default abyss-gw timeout 30 secs
        router.route().handler(TimeoutHandler.create(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_GATEWAY_SERVER_TIMEOUT)));

        //Handler which adds a header `x-response-time` in the response of matching requests containing the time taken in ms to process the request.
        router.route().handler(ResponseTimeHandler.create());

        router.route().failureHandler(this::failureHandler);

        //router.route(Constants.ABYSS_GATEWAY_ROOT + "/:apiUUID/:apiPath").handler(this::routingContextHandler);
        router.route(Constants.ABYSS_GATEWAY_ROOT).handler(this::routingContextHandler);

        //router.route().handler(ctx -> ctx.fail(HttpResponseStatus.NOT_FOUND.code()));
        logger.trace("router route list: {}", router.getRoutes());

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

    private void failureHandler(RoutingContext context) {
        context.response().setStatusCode(context.statusCode()).end();
    }

    private Single<HttpServer> createHttpServer(Router router, String serverHost, int serverPort, Boolean isSSL) {
        logger.trace("---createHttpServer invoked");
        HttpServerOptions httpServerOptions = new HttpServerOptions()
                .setCompressionSupported(true)
                .setLogActivity(Config.getInstance().getConfigJsonObject().getBoolean(Constants.LOG_HTTPSERVER_ACTIVITY));
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


    Completable loadAllProxyApis() {
        logger.trace("---loadAllProxyApis invoked");
        return Completable.complete();
    }

    public void routingContextHandler(RoutingContext context) {
        logger.trace("---routingContextHandler invoked");
        context.response().setStatusCode(HttpResponseStatus.OK.code()).end(HttpResponseStatus.OK.reasonPhrase(), "UTF-8");
    }

    public static final class VerticleConf {
        String serverHost;
        int serverPort;
        Boolean isSSL = Boolean.FALSE;
        Boolean isSandbox = Boolean.FALSE;

        public VerticleConf(String serverHost, int serverPort, Boolean isSSL, Boolean isSandbox) {
            this.serverHost = serverHost;
            this.serverPort = serverPort;
            this.isSSL = isSSL;
            this.isSandbox = isSandbox;
        }
    }

    public Single<SwaggerParseResult> openAPIParser(JsonObject apiSpec) {
        logger.trace("---openAPIParser invoked");
        ObjectMapper mapper;
        String data = apiSpec.toString();
        try {
            if (data.trim().startsWith("{")) {
                mapper = ObjectMapperFactory.createJson();
            } else {
                mapper = ObjectMapperFactory.createYaml();
            }
            JsonNode rootNode = mapper.readTree(data);
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readWithInfo(rootNode);
            if (swaggerParseResult.getMessages().isEmpty()) {
                logger.trace("openAPIParser OK");
                return Single.just(swaggerParseResult);
            } else {
                if (swaggerParseResult.getMessages().size() == 1 && swaggerParseResult.getMessages().get(0).matches("unable to read location")) {
                    logger.error("openAPIParser error | {}", swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecNotExistsException(""));
                } else {
                    logger.error("openAPIParser error | {}", swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecInvalidException(StringUtils.join(swaggerParseResult.getMessages(), ", ")));
                }
            }
        } catch (Exception e) {
            SwaggerParseResult output = new SwaggerParseResult();
            logger.error("openAPIParser error | {} | {}", e.getLocalizedMessage(), e.getStackTrace());
            return Single.error(RouterFactoryException.createSpecInvalidException(e.getLocalizedMessage()));
        }


    }

}
