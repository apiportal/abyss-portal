/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 7 2018
 *
 */

package com.verapi.portal.oapi;

import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.portal.service.idam.AuthenticationService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@AbyssApiController(apiSpec = "/openapi/Authentication.yaml")
public class AuthenticationApiController extends AbstractApiController {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationApiController.class);

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public AuthenticationApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void login(RoutingContext routingContext) {
        try {
            logger.trace("login invoked");
            AuthenticationService authenticationService = new AuthenticationService(vertx);
            subscribeAndResponseJsonObject(routingContext, authenticationService.login(routingContext, authProvider), HttpResponseStatus.OK.code());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void logout(RoutingContext routingContext) {
        try {
            logger.trace("logout invoked");
            AuthenticationService authenticationService = new AuthenticationService(vertx);
            subscribeAndResponseJsonObject(routingContext, authenticationService.logout(routingContext), HttpResponseStatus.OK.code(), true);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void signup(RoutingContext routingContext) {
        try {
            logger.trace("signup invoked");
            AuthenticationService authenticationService = new AuthenticationService(vertx);
            subscribeAndResponseJsonObject(routingContext, authenticationService.signup(routingContext, authProvider), HttpResponseStatus.OK.code());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }


    //@AbyssApiOperationHandler //TODO: enable and define inside YAML
    public void validateToken(RoutingContext routingContext) {
        try {
            logger.trace("validateToken invoked");

            // Get the parsed parameters
            RequestParameters requestParameters = routingContext.get("parsedParameters");

            // We get an user JSON object validated by Vert.x Open API validator
            JsonObject requestBody = requestParameters.body().getJsonObject();

            AuthenticationService authenticationService = new AuthenticationService(vertx);
            subscribeAndResponseJsonObject(routingContext, authenticationService.rxValidateAccessToken(requestBody.getString("token")), HttpResponseStatus.OK.code(), true);
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

}
