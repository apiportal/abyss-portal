
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
import com.verapi.portal.service.idam.ApiService;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.servicediscovery.Record;
import io.vertx.servicediscovery.types.HttpLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                .andThen(Completable.defer(this::loadAllProxyApis))
                .doOnError(throwable -> logger.error("[doOnError]error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace()))
                .subscribe(() -> {
                            logger.trace("started:");
                            super.start(startFuture);
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
        //String apiPathParamValue = context.request().getParam("apipath");
        //logger.trace("captured path parameter: {}", apiPathParamValue);

        //if (apiPathParamValue.equals("echo")) {
        testEchoHttpService().subscribe();
        //}

        //context.response().setStatusCode(HttpResponseStatus.OK.code()).end(HttpResponseStatus.OK.reasonPhrase(), "UTF-8");
        super.routingContextHandler(context);
    }

    public Completable loadAllProxyApis() {
        logger.trace("---loadAllProxyApis invoked");
        ApiService apiService = new ApiService(vertx);
        return Completable.fromObservable(apiService.initJDBCClient()
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
                            createSubRouter(o.getString("uuid"))
                                    .flatMap(this::enableCorsSupport)
                                    .subscribe(subRouter -> logger.trace("gatewayRouter route list{} \n subRouter route list: {}", gatewayRouter.getRoutes(), subRouter.getRoutes()));
                            return Observable.just(o);
                        }
                )
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
                                            .setHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_ECHO_SERVER_HOST))
                                            .setPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_ECHO_SERVER_PORT))
                                            .setRoot("/")
                                            .toJson()))
                                    .setName(o.getString("uuid"))
                                    .setMetadata(new JsonObject().put("organization", o.getInteger("organizationid"))));
                        }
                )
                .flatMap(record -> AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).toObservable()))
                .doOnError(throwable -> logger.error("loadAllProxyApis error {} {}", throwable.getLocalizedMessage(), throwable.getStackTrace()))
                .andThen(super.loadAllProxyApis());
    }
}
