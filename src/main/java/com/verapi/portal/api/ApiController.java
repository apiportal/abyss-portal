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

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.verticle.ApiHttpServerVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;
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

@Path("/abyss/api/subjectOLDDDDDDDDDDDDDDDD")
public class ApiController {
    private static Logger logger = LoggerFactory.getLogger(ApiHttpServerVerticle.class);

    @GET
    @Path("/subjects/{subjectID}")
    @Produces({MediaType.APPLICATION_JSON})
    public void getSubject(
            // Suspend the request
            @Suspended final AsyncResponse asyncResponse,

            // Inject the Vertx instance
            @Context Vertx vertx,

            @PathParam("subjectID") String subjectID
    ) {
        logger.info("ApiController.getSubject() invoked");

        if (subjectID == null) {
            asyncResponse.resume(Response.status(Response.Status.BAD_REQUEST).build());
            return;
        }

        // Send a get message to the backend
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.addHeader("class", this.getClass().getCanonicalName());
        deliveryOptions.addHeader("method",Thread.currentThread().getStackTrace()[1].getMethodName());
        vertx.eventBus().<JsonObject>send(Config.getInstance().getConfigJsonObject().getString(Constants.EB_API_SERVER_ADDRESS), new JsonObject()
                .put("op", "get")
                .put("id", subjectID), deliveryOptions, msg -> {

            // When we get the response we resume the Jax-RS async response
            if (msg.succeeded()) {
                JsonObject json = msg.result().body();
                if (json != null) {
                    asyncResponse.resume(json.encode());
                } else {
                    asyncResponse.resume(Response.status(Response.Status.NOT_FOUND).build());
                }
            } else {
                asyncResponse.resume(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());
            }
        });

    }



}
