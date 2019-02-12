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

import com.verapi.portal.api.ApiAbstractController;
import com.verapi.portal.api.IApiController;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.reactivex.Single;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.reactivex.core.http.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.UUID;

public class ApiBusServerVerticle extends AbyssAbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(ApiBusServerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        setAbyssJDBCService(new AbyssJDBCService(vertx));
        Disposable disposable
                =
                initializeJdbcClient(Constants.API_DATA_SOURCE_SERVICE)
                        .flatMap(Single::just)
                        .subscribe(jdbcClient -> {
                            super.start(startFuture);
                            logger.info("ApiBusServerVerticle initializeJdbcClient ok");
                        }, t -> {
                            logger.error("ApiBusServerVerticle initializeJdbcClient not ok", t);
                            startFuture.fail(t);
                        });
    }

    @Override
    public void start() throws Exception {

        vertx.getDelegate().eventBus().<JsonObject>consumer(Config.getInstance().getConfigJsonObject().getString(Constants.EB_API_SERVER_ADDRESS), msg -> {
            logger.info("ApiBusServerVerticle.start processing message : " + msg);
            try {
                processMessage(msg);
            } catch (Exception e) {
                logger.error("ApiBusServerVerticle.start exception occured while executing processMessage : " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
            }
        });

        super.start();

/*
        EventBus eb = vertx.eventBus();

        eb.consumer(Config.getInstance().getConfigJsonObject().getString(Constants.EB_API_SERVER_ADDRESS))
                .toFlowable()
                .subscribe(message -> {
                    logger.info("ApiBusServerVerticle.start processing message : " + message);
                    processMessage(message);
                    super.start();
                });
*/
    }

    private void processMessage(Message<JsonObject> message) throws Exception {
        logger.info("ApiBusServerVerticle.processMessage processing message");
        String className = message.headers().get("class");
        String methodName = message.headers().get("method");
        logger.info("ApiBusServerVerticle.processMessage processing message by using : " + className);
        Class<IApiController<ApiAbstractController>> clazz = (Class<IApiController<ApiAbstractController>>) Class.forName(className);
        IApiController<ApiAbstractController> apiRequestHandlerInstance = clazz.getConstructor().newInstance();

        final DeliveryOptions deliveryOptions = new DeliveryOptions().addHeader("uuid", UUID.randomUUID().toString()).addHeader("method", methodName);
        message.headers().add("uuid", deliveryOptions.getHeaders().get("uuid")).add("method", deliveryOptions.getHeaders().get("method"));
        //JsonObject apiResponse = apiRequestHandlerInstance.handle(vertx, message, getAbyssJDBCService());
        apiRequestHandlerInstance.handle(vertx, message, getAbyssJDBCService()).subscribe(apiResponse -> {
                    logger.info("ApiBusServerVerticle.processMessage() received message from " + className + " .handle() with the message : " + apiResponse);
                    message.reply(apiResponse, deliveryOptions);
                    logger.info("ApiBusServerVerticle.processMessage() replied message...");
                },
                throwable -> {
                    logger.error("ApiBusServerVerticle.processMessage message reply error : ", throwable.getLocalizedMessage(), throwable.getStackTrace());
                    message.reply(new JsonObject().put("statusCode", "400").put("exception", throwable.getStackTrace()), deliveryOptions);
                    logger.info("ApiBusServerVerticle.processMessage() replied exception message :( ...");
                });

/*
        apiRequestHandlerInstance.handle(message)
                .flatMap(apiResponse -> message.rxReply(apiResponse, deliveryOptions))
                .subscribe(repliedMessage -> logger.info("ApiBusServerVerticle.processMessage replied message : ", repliedMessage.toString()),
                        ex -> logger.error("ApiBusServerVerticle.processMessage message reply error : ", ex));
*/

        //,ex -> logger.error("ApiBusServerVerticle.processMessage " + className + " handle message error : ", ex));

    }

    @Override
    protected Single<HttpServer> createHttpServer() {
        return null;
    }
}
