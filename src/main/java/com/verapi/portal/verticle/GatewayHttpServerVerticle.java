
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

import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.common.OpenAPIUtil;
import com.verapi.portal.service.idam.ApiService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.contract.RouterFactoryException;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class GatewayHttpServerVerticle extends AbstractGatewayVerticle implements IGatewayVerticle {
    private static Logger logger = LoggerFactory.getLogger(GatewayHttpServerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("---start invoked");
        verticleConf = new VerticleConf(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_GATEWAY_SERVER_HOST),
                Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_GATEWAY_SERVER_PORT),
                false,
                false);
        initializeServer()
                .andThen(Completable.defer(this::registerEchoHttpService))
                .andThen(Completable.defer(this::testEchoHttpService))
                //.andThen(Completable.defer(this::loadAllProxyApis))
                .doOnError(throwable -> logger.error("[doOnError]error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace()))
                .subscribe(() -> {
                            logger.trace("started");
                            super.start(startFuture);
                            loadAllProxyApis().subscribe();
                        }
                        , throwable -> {
                            logger.error("[subscribe]start error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                            startFuture.fail(throwable);
                        });
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        logger.trace("---stop invoked");
        super.stop(stopFuture);
    }

    public void routingContextHandler(RoutingContext context) {
        logger.trace("---routingContextHandler invoked");
        //String apiUUIDParamValue = context.request().getParam("apiUUID");
        String apiUUIDParamValue = context.request().path().substring(("/" + Constants.ABYSS_GW + "/").length(), ("/" + Constants.ABYSS_GW + "/").length() + 36);
        //String apiPathParamValue = context.request().getParam("apiPath");
        String apiPathParamValue = context.request().path().substring(("/" + Constants.ABYSS_GW + "/").length() + 36, context.request().path().length());
        logger.trace("captured path parameter: {} | {}", apiUUIDParamValue, apiPathParamValue);
        logger.trace("captured mountpoint: {} | method: {}", context.mountPoint(), context.request().method().toString());
        lookupHttpService(apiUUIDParamValue)
                .flatMap(abyssServiceReference -> {
                    JsonObject apiSpec = new JsonObject(abyssServiceReference.serviceReference.record().getMetadata().getString("apiSpec"));
                    if (apiSpec.getJsonObject("paths").getJsonObject(apiPathParamValue).containsKey(context.request().method().toString().toLowerCase())) {
                        JsonObject apiPath = apiSpec.getJsonObject("paths").getJsonObject(apiPathParamValue).getJsonObject(context.request().method().toString().toLowerCase());
                        logger.trace("invocation response | {}", apiPath.encode());
                        context.response().setStatusCode(HttpResponseStatus.OK.code()).end(apiPath.encode());
                        return Single.just(abyssServiceReference);
                    } else {
                        logger.error("requested path {}-{} not found inside openapi spec", context.request().method().toString().toLowerCase(), apiPathParamValue);
                        return Single.error(RouterFactoryException.createPathNotFoundException(""));
                    }
                })
                .doAfterSuccess(abyssServiceReference -> releaseHttpService(abyssServiceReference.serviceReference))
                .subscribe(
                        abyssServiceReference -> {
                            logger.trace("invocation completed");
                        }
                        , throwable -> {
                            logger.error("invocation error | {} | {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                        }
                );


        //if (apiUUIDParamValue.equals("echo")) {
        /////////////////////////////////////////////////////////testEchoHttpService().subscribe();  <<<<<<<<<<<<<<<<<<<<<<<<<****
        //}


        //context.response().setStatusCode(HttpResponseStatus.OK.code()).end(HttpResponseStatus.OK.reasonPhrase(), "UTF-8");
        //super.routingContextHandler(context);
    }

    public Completable loadAllProxyApis() {
        logger.trace("---loadAllProxyApis invoked");
        ApiService apiService = new ApiService(vertx);
        return Completable.fromObservable(apiService.initJDBCClient()
                .delay(3, TimeUnit.SECONDS)
                .flatMap(jdbcClient -> apiService.findAllProxies())
                .toObservable()
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0)
                        return Observable.empty();
                    else {
                        return Observable.fromIterable(resultSet.getRows());
                    }
                })
                .flatMap(o -> {
                            //gatewayRouter.route(Constants.ABYSS_GATEWAY_ROOT + "/" + o.getString("uuid")).handler(this::routingContextHandler);
                            //TODO:depreciate below

                            createSubRouter(o.getString("uuid"))
                                    .flatMap(this::enableCorsSupport)
                                    .doOnError(throwable -> logger.error("loadAllProxyApis createSubRouter error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace()))
                                    .subscribe(subRouter -> logger.trace("gatewayRouter route list{} \n subRouter route list: {}", gatewayRouter.getRoutes(), subRouter.getRoutes()));

                            return Observable.just(o);
                        }
                )
                .flatMap(o -> {
                    JsonObject apiSpec = new JsonObject(o.getString("openapidocument"));
                    OpenAPIUtil.openAPIParser(apiSpec).subscribe();
                    return Observable.just(o);
                })
                .flatMap(o -> {
/*
                            String mountPoint = Constants.ABYSS_GATEWAY_ROOT + "/" + o.getString("uuid");
                            Router subRouter = Router.router(vertx);
                            gatewayRouter.mountSubRouter(mountPoint, subRouter);
                            subRouter.route().handler(this::routingContextHandler);
                            logger.trace("route added for path {}", o.getString("uuid"));
                            //logger.trace("route added for path {} and openapi spec is {}", o.getString("openapidocument"));
                            logger.trace("gatewayRouter route list: {}", gatewayRouter.getRoutes());
                            logger.trace("subRouter route list: {}", subRouter.getRoutes());
*/
                            return Observable.just(new Record()
                                    .setType("http-endpoint")
                                    //.setLocation(new JsonObject().put("endpoint", "the-service-address"))
                                    .setLocation((new HttpLocation()
                                            .setSsl(false)
                                            .setHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_GATEWAY_SERVER_HOST))
                                            .setPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_GATEWAY_SERVER_PORT))
                                            .setRoot("/")
                                            .toJson()))
                                    .setName(o.getString("uuid"))
                                    .setMetadata(new JsonObject()
                                            .put("organization", o.getString("organizationid"))
                                            .put("apiSpec", o.getString("openapidocument"))));
                        }
                )
                .flatMap(record -> AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).toObservable()))
                .doOnError(throwable -> logger.error("loadAllProxyApis error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace()))
                .andThen(super.loadAllProxyApis())
                .doFinally(() -> {
                    String mountPoint = "";
                    mountPoint = Constants.ABYSS_GATEWAY_ROOT + "/" + "echo";
                    Router subRouter = Router.router(vertx);
                    subRouter.route().handler(this::echoContextHandler);
                    gatewayRouter.mountSubRouter(mountPoint, subRouter);
                    logger.trace("gatewayRouter route list: {}", gatewayRouter.getRoutes());
                    logger.trace("subRouter route list: {}", subRouter.getRoutes());
                    logger.info("loadAllProxyApis() completed");
                });
    }

    public void echoContextHandler(RoutingContext context) {
        logger.trace("---echoContextHandler invoked");
        context.response().setStatusCode(HttpResponseStatus.OK.code()).end(HttpResponseStatus.OK.reasonPhrase(), "UTF-8");
    }
}
