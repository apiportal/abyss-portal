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
import com.verapi.portal.service.idam.SubjectGroupService;
import com.verapi.portal.service.idam.SubjectPermissionService;
import io.reactivex.Single;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@Path("/abyss/api/subjectpermission")
public class SubjectPermissionController extends ApiAbstractController {
    private static Logger logger = LoggerFactory.getLogger(SubjectPermissionController.class);

    @GET
    @Path("/getAll")
    @Produces({MediaType.APPLICATION_JSON})
    public void getAll(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context io.vertx.core.Vertx vertx,

            @QueryParam("q") String permissionName

    ) {
        logger.info("SubjectPermissionController.getAll() invoked");

        try {
            Vertx reactiveVertx = Vertx.newInstance(vertx);
            logger.info("io.vertx.reactivex.core.Vertx : " + reactiveVertx.toString());

            SubjectPermissionService subjectPermissionService = new SubjectPermissionService(reactiveVertx);

            Single<JsonObject> apiResponse = subjectPermissionService.initJDBCClient()
                    .flatMap(jdbcClient -> (permissionName == null) ? subjectPermissionService.findAll() : subjectPermissionService.filterByPermissionName(permissionName))
                    .flatMap(result -> {
                        JsonObject jsonObject = new JsonObject()
                                .put("statusCode", "200")
                                .put("groupList", result.toJson().getValue("rows"))
                                .put("totalPages", 1) //TODO:pagination
                                .put("totalItems", result.getNumRows())
                                .put("pageSize", 30)
                                .put("currentPage", 1)
                                .put("last", true)
                                .put("first", true)
                                .put("sort", "ASC PERMISSION NAME");
                        logger.info(jsonObject.encodePrettily());
                        return Single.just(jsonObject);
                    });

            apiResponse.subscribe(resp -> {
                        asyncResponse.resume(Response.status(Response.Status.OK)
                                .encoding("UTF-8")
                                .type(MediaType.APPLICATION_JSON)
                                .entity(resp.encode())
                                .build());

                        logger.info("SubjectPermissionController.getAll() replied successfully " + resp.encodePrettily());
                    },
                    throwable -> {
                        asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(throwable).build());
                        logger.error("SubjectPermissionController.getAll() replied error : ", throwable.getLocalizedMessage() + Arrays.toString(throwable.getStackTrace()));
                    });

        } catch (Exception e) {
            logger.error("SubjectPermissionController.getAll() new SubjectService exception occured " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
            asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e).build());
        }

    }

    @Override
    public Single<JsonObject> handle(Vertx vertx, Message message, AbyssJDBCService abyssJDBCService) throws Exception {
        return null;
    }
}
