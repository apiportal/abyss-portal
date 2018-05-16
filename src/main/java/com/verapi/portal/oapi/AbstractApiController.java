/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.oapi;

import com.verapi.portal.oapi.exception.AbyssApiException;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.oapi.exception.UnAuthorized401Exception;
import com.verapi.portal.oapi.schema.ApiSchemaError;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;

public abstract class AbstractApiController implements IApiController {
    private static Logger logger = LoggerFactory.getLogger(AbstractApiController.class);

    Vertx vertx;
    Router abyssRouter;
    String apiSpec;

    AbstractApiController(Vertx vertx, Router router) {
        this.vertx = vertx;
        this.abyssRouter = router;
        //this.apiSpec = apiSpec;
        this.apiSpec = this.getClass().getAnnotation(AbyssApiController.class).apiSpec();
        this.init();
    }

    public abstract void init();


    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz) {
        AbyssApiException abyssApiException;
        HttpResponseStatus httpResponseStatus;
        try {
            abyssApiException = (AbyssApiException) clazz.getConstructor(String.class).newInstance("new instance");
            httpResponseStatus = abyssApiException.getHttpResponseStatus();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            abyssApiException = new InternalServerError500Exception("new instance");
            httpResponseStatus = abyssApiException.getHttpResponseStatus();
            logger.error(e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
        }

        throwApiException(routingContext, clazz, httpResponseStatus.reasonPhrase(), null, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage) {
        throwApiException(routingContext, clazz, userMessage, null, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage) {
        throwApiException(routingContext, clazz, userMessage, detailedMessage, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage, String recommendation) {
        throwApiException(routingContext, clazz, userMessage, detailedMessage, recommendation, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage, String recommendation, String moreInfo) {
        logger.trace("throwApiException for " + userMessage);
        ApiSchemaError apiSchemaError = new ApiSchemaError();
        apiSchemaError.setCode(0)
                .setUsermessage(userMessage)
                .setInternalmessage(Arrays.toString(Thread.currentThread().getStackTrace()))
                .setDetails(detailedMessage)
                .setRecommendation(recommendation)
                .setMoreinfoURLasString(moreInfo)
                .setTimestamp(ZonedDateTime.now(ZoneId.systemDefault()).toString())
                .setPath(routingContext.normalisedPath());
        AbyssApiException abyssApiException;
        try {
            abyssApiException = (AbyssApiException) clazz.getConstructor(ApiSchemaError.class).newInstance(apiSchemaError);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            abyssApiException = new InternalServerError500Exception(apiSchemaError);
            logger.error(e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
        }
        routingContext.fail(abyssApiException);
    }

    public void failureHandler(RoutingContext routingContext) {
        logger.info("failureHandler invoked; " + routingContext.failure().getLocalizedMessage());
        logger.trace("failureHandler invoked; " + Arrays.toString(routingContext.failure().getStackTrace()));

        // This is the failure handler
        Throwable failure = routingContext.failure();
        if (failure instanceof ValidationException)
            // Handle Validation Exception
            routingContext.response()
                    .setStatusCode(422)
                    .setStatusMessage("ValidationException thrown! " + ((ValidationException) failure).type().name())
                    .end();
        else if (failure instanceof AbyssApiException)
            //Handle Abyss Api Exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(((AbyssApiException) failure).getApiError().getCode())
                    .setStatusMessage(((AbyssApiException) failure).getApiError().getUsermessage())
                    .end(((AbyssApiException) failure).getApiError().toJson().toString(), "UTF-8");
        else
            // Handle other exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(500)
                    .setStatusMessage("Exception thrown! " + failure.getLocalizedMessage())
                    .end(new ApiSchemaError().setCode(500)
                            .setUsermessage("An unknown error occured")
                            .setInternalmessage(failure.getLocalizedMessage())
                            .setDetails(Arrays.toString(failure.getStackTrace()))
                            .setRecommendation(null)
                            .setMoreinfo(null)
                            .toJson()
                            .toString()
                    );

    }

    public void cookiesecurityHandler(RoutingContext routingContext) {
        logger.info("cookiesecurityHandler invoked");
        // Handle security here
        User user = routingContext.user();
        if (user == null) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
            return;
        }
        if (user != null && user.principal().isEmpty()) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
            return;
        }
        routingContext.next();
    }

}
