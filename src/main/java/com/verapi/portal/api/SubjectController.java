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

package com.verapi.portal.api;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.service.idam.SubjectService;
import com.verapi.portal.verticle.AbyssAbstractVerticle;
import com.verapi.portal.verticle.ApiHttpServerVerticle;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.http.HttpServer;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@Path("/abyss/api/subject")
public class SubjectController extends ApiAbstractController {
    private static Logger logger = LoggerFactory.getLogger(SubjectController.class);

    private JsonObject handlerResponse;

    @GET
    @Path("/subjects/{subjectID}")
    @Produces({MediaType.APPLICATION_JSON})
    public void getSubject(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context io.vertx.core.Vertx vertx,

            @PathParam("subjectID") String subjectID
    ) {
        logger.info("SubjectController.getSubject() invoked");

        if (subjectID == null) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }
        //Vertx reactiveVertx = ResteasyProviderFactory.popContextData(Vertx.class);
        Vertx reactiveVertx = Vertx.newInstance(vertx);
        logger.info("io.vertx.reactivex.core.Vertx got from ResteasyProviderFactory : " + reactiveVertx.toString());

        // Send a get message to the backend
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.addHeader("class", this.getClass().getCanonicalName());
        deliveryOptions.addHeader("method", Thread.currentThread().getStackTrace()[1].getMethodName());
        vertx.eventBus().
                <JsonObject>send(Config.getInstance().getConfigJsonObject().getString(Constants.EB_API_SERVER_ADDRESS),
                        new JsonObject().put("op", "get").put("id", subjectID), deliveryOptions,
                        msg -> {
                            logger.info("SubjectController.getSubject() : we have got the response from event bus");
                            // When we get the response we resume the Jax-RS async response
                            if (msg.succeeded()) {
                                JsonObject json = msg.result().body();
                                if (json != null) {
                                    logger.info("SubjectController.getSubject() : we have got the response from event bus with message: " + msg.result().body().encodePrettily());
                                    //asyncResponse.resume(json.encode());
                                    asyncResponse.resume(Response.status(Response.Status.OK).encoding("UTF-8").type(MediaType.APPLICATION_JSON).entity(json.encode()).build());
                                } else {
                                    logger.info("SubjectController.getSubject() : we have got *null* response from event bus");
                                    asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).build());
                                }
                            } else {
                                logger.error("SubjectController.getSubject() : we have got failed response from event bus " + msg.cause());
                                asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
                            }
                        });

    }

    @Override
    public Single<JsonObject> handle(Vertx vertx, Message message, AbyssJDBCService abyssJDBCService) throws Exception {
        logger.info("SubjectController.handle() invoked");
        String messageUUID = message.headers().get("uuid");
        String methodName = message.headers().get("method");
        logger.info(new JsonObject().put("uuid", messageUUID).put("method", methodName).encodePrettily());

        SubjectService subjectService = new SubjectService(vertx, abyssJDBCService);

        switch (methodName) {
            case "getSubject":
/*
                return subjectService.initJDBCClient()
                        .flatMap(jdbcClient -> subjectService.findAll())
                        .flatMap(result -> {
                            JsonObject jsonObject = new JsonObject().put("statusCode", "200").put("result", Json.encode(result));
                            logger.info(jsonObject.encodePrettily());
                            return Single.just(jsonObject);
                        });
*/
                return subjectService.initJDBCClient()
                        .flatMap(jdbcClient -> subjectService.findAll())
                        .flatMap(result -> {
                            JsonObject jsonObject = new JsonObject()
                                    .put("statusCode", "200")
                                    .put("userList", result.toJson().getValue("rows"))
                                    .put("totalPages", 1) //TODO:pagination
                                    .put("totalItems", result.getNumRows())
                                    .put("pageSize", 30)
                                    .put("currentPage", 1)
                                    .put("last", true)
                                    .put("first", true)
                                    .put("sort", "ASC SUBJECT NAME");
                            logger.info(jsonObject.encodePrettily());
                            return Single.just(jsonObject);
                        });

/*

                        .subscribe(r -> {
                            logger.info("SubjectController.handle() response from subjectService.findAll() " + r);
                            logger.info("SubjectController.handle() response from subjectService.findAll() " + Json.encodePrettily(r));
                            handlerResponse = new JsonObject().put("result", Json.encodePrettily(r));
                        }, t -> {
                            logger.error("SubjectController.handle() *error* response from subjectService.findAll() " + t.getLocalizedMessage() + Arrays.toString(t.getStackTrace()));
                            handlerResponse = new JsonObject().put("exception", t.getLocalizedMessage());
                        });
*/
            default:
                return Single.just(new JsonObject().put("statusCode", "400"));
        }

    }

    @GET
    @Path("/getAll")
    @Produces({MediaType.APPLICATION_JSON})
    public void getAll(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context io.vertx.core.Vertx vertx
    ) {
        logger.info("SubjectController.getAll() invoked");

        try {
            Vertx reactiveVertx = Vertx.newInstance(vertx);
            logger.info("io.vertx.reactivex.core.Vertx : " + reactiveVertx.toString());

            SubjectService subjectService = new SubjectService(reactiveVertx);

            Single<JsonObject> apiResponse = subjectService.initJDBCClient()
                    .flatMap(jdbcClient -> subjectService.findAll())
                    .flatMap(result -> {
                        JsonObject jsonObject = new JsonObject()
                                .put("statusCode", "200")
                                .put("userList", result.toJson().getValue("rows"))
                                .put("totalPages", 1) //TODO:pagination
                                .put("totalItems", result.getNumRows())
                                .put("pageSize", 30)
                                .put("currentPage", 1)
                                .put("last", true)
                                .put("first", true)
                                .put("sort", "ASC SUBJECT NAME");
                        logger.info(jsonObject.encodePrettily());
                        return Single.just(jsonObject);
                    });

            apiResponse.subscribe(resp -> {
                        asyncResponse.resume(Response.status(Response.Status.OK)
                                .encoding("UTF-8")
                                .type(MediaType.APPLICATION_JSON)
                                .header("Access-Control-Allow-Methods", "GET, POST, DELETE, PUT")
                                .header("Access-Control-Allow-Origin", "http://localhost:38081")
                                .header("origin", "http://localhost:38081")
                                .entity(resp.encode())
                                .build());

                        logger.info("SubjectController.getAll() replied successfully " + resp.encodePrettily());
                    },
                    throwable -> {
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(throwable).build());
                        logger.error("SubjectController.getAll() replied error : ", throwable.getLocalizedMessage() + Arrays.toString(throwable.getStackTrace()));
                    });

        } catch (Exception e) {
            logger.error("SubjectController.getAll() new SubjectService exception occured " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
            asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
        }

    }

}
