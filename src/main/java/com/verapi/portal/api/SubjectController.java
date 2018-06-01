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
import com.verapi.portal.entity.idam.Subject;
import com.verapi.portal.service.idam.SubjectIndexService;
import com.verapi.portal.service.idam.SubjectService;
import com.verapi.portal.service.idam.SubjectServiceOld;
import io.reactivex.Single;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
        logger.trace("SubjectController.getSubject() invoked");

        if (subjectID == null) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }
        //Vertx reactiveVertx = ResteasyProviderFactory.popContextData(Vertx.class);
        Vertx reactiveVertx = Vertx.newInstance(vertx);
        logger.trace("io.vertx.reactivex.core.Vertx got from ResteasyProviderFactory : " + reactiveVertx.toString());

        // Send a get message to the backend
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.addHeader("class", this.getClass().getCanonicalName());
        deliveryOptions.addHeader("method", Thread.currentThread().getStackTrace()[1].getMethodName());
        vertx.eventBus().
                <JsonObject>send(Config.getInstance().getConfigJsonObject().getString(Constants.EB_API_SERVER_ADDRESS),
                        new JsonObject().put("op", "get").put("id", subjectID), deliveryOptions,
                        msg -> {
                            logger.trace("SubjectController.getSubject() : we have got the response from event bus");
                            // When we get the response we resume the Jax-RS async response
                            if (msg.succeeded()) {
                                JsonObject json = msg.result().body();
                                if (json != null) {
                                    logger.trace("SubjectController.getSubject() : we have got the response from event bus with message: " + msg.result().body().encodePrettily());
                                    //asyncResponse.resume(json.encode());
                                    asyncResponse.resume(Response.status(Response.Status.OK).encoding("UTF-8").type(MediaType.APPLICATION_JSON).entity(json.encode()).build());
                                } else {
                                    logger.trace("SubjectController.getSubject() : we have got *null* response from event bus");
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
        logger.trace("SubjectController.handle() invoked");
        String messageUUID = message.headers().get("uuid");
        String methodName = message.headers().get("method");
        logger.trace(new JsonObject().put("uuid", messageUUID).put("method", methodName).encodePrettily());

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
                            logger.debug(jsonObject.encodePrettily());
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
            @Context io.vertx.core.Vertx vertx,

            @QueryParam("q") String subjectName

    ) {
        logger.trace("SubjectController.getAll() invoked");

        try {
            //logger.info("SubjectController.getAll() injected vertx : " + vertx.toString());
            Vertx reactiveVertx = Vertx.newInstance(vertx);
            //logger.info("SubjectController.getAll() io.vertx.reactivex.core.Vertx : " + reactiveVertx.toString());

            SubjectServiceOld subjectService = new SubjectServiceOld(reactiveVertx);

            Single<JsonObject> apiResponse = subjectService.initJDBCClient()
                    .flatMap(jdbcClient -> (subjectName == null) ? subjectService.findAll() : subjectService.filterBySubjectName(subjectName))
                    .flatMap(result -> {
                        JsonArray jsonArray = new JsonArray();
                        if (subjectName == null) {
                            for (JsonObject row : result.getRows(true)) {
                                jsonArray.add(new JsonObject(row.getString("rowjson")));
                            }
                        }
                        JsonObject jsonObject = new JsonObject()
                                .put("statusCode", "200")
                                //.put("userList", result.toJson().getValue("rows"))
                                //.put("userList", result.getRows())
                                //.put("userList", new JsonObject(result.getResults().get(0).getString(0)))
                                .put("respDataList", (subjectName == null) ? jsonArray : result.toJson().getValue("rows"))
                                .put("totalPages", 1) //TODO:pagination
                                .put("totalItems", result.getNumRows())
                                .put("pageSize", 30)
                                .put("currentPage", 1)
                                .put("last", true)
                                .put("first", true)
                                .put("sort", "ASC SUBJECT NAME");
                        logger.debug(jsonObject.encodePrettily());
                        return Single.just(jsonObject);
                    });

            apiResponse.subscribe(resp -> {
                        asyncResponse.resume(Response.status(Response.Status.OK)
                                .encoding("UTF-8")
                                .type(MediaType.APPLICATION_JSON)
                                .entity(resp.encode())
                                .build());

                        logger.trace("SubjectController.getAll() replied successfully " + resp.encodePrettily());
                    },
                    throwable -> {
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(throwable).build());
                        //logger.error("SubjectController.getAll() replied error : ", throwable.getLocalizedMessage() + Arrays.toString(throwable.getStackTrace()));
                        logger.error("SubjectController.getAll() replied error : ", Arrays.toString(throwable.getStackTrace()));
                    });

        } catch (Exception e) {
            logger.error("SubjectController.getAll() new SubjectServiceOld exception occured " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
            asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
        }

    }

    @GET
    @Path("/getIndex")
    @Produces({MediaType.APPLICATION_JSON})
    public void getIndex(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context io.vertx.core.Vertx vertx,

            //@Context io.vertx.ext.web.RoutingContext routingContext,

            //@QueryParam("q") String subjectName
            @QueryParam("q") String subjectUUID

    ) {
        logger.trace("SubjectController.getIndex() invoked");

        try {
            //logger.info("SubjectController.getAll() injected vertx : " + vertx.toString());
            Vertx reactiveVertx = Vertx.newInstance(vertx);
            //logger.info("SubjectController.getAll() io.vertx.reactivex.core.Vertx : " + reactiveVertx.toString());

            SubjectIndexService subjectIndexService = new SubjectIndexService(reactiveVertx);

            Single<JsonObject> apiResponse = subjectIndexService.initJDBCClient()
                    .flatMap(jdbcClient -> (subjectUUID == null) ? subjectIndexService.findAll() : subjectIndexService.findBySubjectUuid(subjectUUID))
                    .flatMap(result -> {

                        //TODO: Check # 0f row == 1
                        JsonObject jsonObject = new JsonObject(result.getRows(true).get(0).getString("result"));

                        jsonObject.getJsonObject("user").remove("id");

                        jsonObject.put("statusCode", "200");
                        //.mergeIn(result.getRows(true).get(0));
                        logger.trace(jsonObject.encodePrettily());
                        return Single.just(jsonObject);
                    });

            apiResponse.subscribe(resp -> {
                        asyncResponse.resume(Response.status(Response.Status.OK)
                                .encoding("UTF-8")
                                .type(MediaType.APPLICATION_JSON)
                                .entity(resp.encode())
                                .build());

                        logger.trace("SubjectController.getIndex() replied successfully " + resp.encodePrettily());
                    },
                    throwable -> {
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(throwable).build());
                        //logger.error("SubjectController.getAll() replied error : ", throwable.getLocalizedMessage() + Arrays.toString(throwable.getStackTrace()));
                        logger.error("SubjectController.getIndex() replied error : ", Arrays.toString(throwable.getStackTrace()));
                    });

        } catch (Exception e) {
            logger.error("SubjectController.getIndex() new SubjectServiceOld exception occured " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
            asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
        }

    }

    @POST
    @Path("/addSubject")
    @Produces({MediaType.APPLICATION_JSON})
    @Consumes({MediaType.APPLICATION_JSON})
    public void addSubject(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context io.vertx.core.Vertx vertx,

            @RequestBody(description = "New subject to add", content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = Subject.class))) String subject) {
        logger.trace("SubjectController.addSubject() invoked");
        asyncResponse.resume(Response.status(Response.Status.OK)
                .encoding("UTF-8")
                .type(MediaType.APPLICATION_JSON)
                .entity(new JsonObject(subject).encode())
                .build());

    }
}

