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
import com.verapi.portal.service.ApiService;
import com.verapi.portal.verticle.ApiHttpServerVerticle;
import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
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
import java.util.Iterator;

@Path("/abyss/api/my-apis")
public class ApiController extends ApiAbstractController{
    private static Logger logger = LoggerFactory.getLogger(ApiHttpServerVerticle.class);

        @GET
    @Path("/getAll")
    @Produces({MediaType.APPLICATION_JSON})
    public void getAll(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context io.vertx.core.Vertx vertx,

            @QueryParam("q") String apiOwnerSubjectName

    ) {
        logger.info("ApiController.getAll() invoked");

        try {
            //logger.info("SubjectController.getAll() injected vertx : " + vertx.toString());
            io.vertx.reactivex.core.Vertx reactiveVertx = io.vertx.reactivex.core.Vertx.newInstance(vertx);
            //logger.info("SubjectController.getAll() io.vertx.reactivex.core.Vertx : " + reactiveVertx.toString());

            ApiService apiService = new ApiService(reactiveVertx);

            Single<JsonObject> apiResponse = apiService.initJDBCClient()
                    .flatMap(jdbcClient -> (apiOwnerSubjectName == null) ? apiService.findAll() : apiService.filterBySubjectName(apiOwnerSubjectName))
                    .flatMap(result -> {

                        JsonArray apiProxyList = new JsonArray("[" +
                                "{\n" +
                                "\"uuid\": \"65bd3e20-ff9c-4570-87c2-61e5447f3d5d\",\n" +
                                "\"name\": \"My Proxy 1\",\n" +
                                "\"context\": \"my_proxy_1\"\n" +
                                "},\n" +
                                "{\n" +
                                "\"uuid\": \"9b2858f0-6963-4d7c-91d7-e1e64ac24a22\",\n" +
                                "\"name\": \"My Proxy 2\",\n" +
                                "\"context\": \"my_proxy_2\"\n" +
                                "},\n" +
                                "{\n" +
                                "\"uuid\": \"9b4bde91-53e4-411a-9449-5b33afc41c19\",\n" +
                                "\"name\": \"My Proxy 3\",\n" +
                                "\"context\": \"my_proxy_3\"\n" +
                                "}\n" +
                                "]\n");

                        JsonArray apiList = new JsonArray();
                        for (JsonObject row : result.getRows(true)) {
                            JsonObject jsonObj = new JsonObject(row.getString("openapi"));
                            JsonObject jsonObjPlatform = new JsonObject(row.getString("rowjson"));
                            jsonObjPlatform.remove("openapi_document");
                            jsonObjPlatform.put("proxies_summary", apiProxyList);
                            jsonObj.put("x-abyss-platform", jsonObjPlatform);
                            apiList.add(jsonObj);
                        }

                        JsonObject jsonObject = new JsonObject()
                                .put("statusCode", "200")
                                .put("openApiList", apiList)
                                .put("totalPages", 1) //TODO:pagination
                                .put("totalItems", result.getNumRows())
                                .put("pageSize", 30)
                                .put("currentPage", 1)
                                .put("last", true)
                                .put("first", true)
                                .put("sort", "ASC SUBJECT NAME");
                        logger.trace(jsonObject.encodePrettily());
                        return Single.just(jsonObject);
                    });

            apiResponse.subscribe(resp -> {
                        asyncResponse.resume(Response.status(Response.Status.OK)
                                .encoding("UTF-8")
                                .type(MediaType.APPLICATION_JSON)
                                .entity(resp.encode())
                                .build());

                        logger.trace("ApiController.getAll() replied successfully " + resp.encodePrettily());
                    },
                    throwable -> {
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(throwable).build());
                        //logger.error("SubjectController.getAll() replied error : ", throwable.getLocalizedMessage() + Arrays.toString(throwable.getStackTrace()));
                        logger.error("ApiController.getAll() replied error : ", Arrays.toString(throwable.getStackTrace()));
                    });

        } catch (Exception e) {
            logger.error("ApiController.getAll() new ApiService exception occured " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
            asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
        }

    }


    @Override
    public Single<JsonObject> handle(io.vertx.reactivex.core.Vertx vertx, Message message, AbyssJDBCService abyssJDBCService) throws Exception {
        return null;
    }
}
