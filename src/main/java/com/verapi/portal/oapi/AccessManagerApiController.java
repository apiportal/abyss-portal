/*
 * Copyright 2019 Verapi Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verapi.portal.oapi;

import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.portal.service.idam.AccessManagerService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

@AbyssApiController(apiSpec = "/openapi/AccessManager.yaml")
public class AccessManagerApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AccessManagerApiController.class);

    private static List<String> jsonbColumnsList = new ArrayList<>();

    static {
        jsonbColumnsList.add(Constants.JSONB_COLUMN_ACCESS_MANAGER_ATTRIBUTES);
    }


    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public AccessManagerApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getAccessManagers(RoutingContext routingContext) {
        try {
            getEntities(routingContext, AccessManagerService.class, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addAccessManagers(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        try {
            addEntities(routingContext, AccessManagerService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateAccessManagers(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, AccessManagerService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteAccessManagers(RoutingContext routingContext) {
        try {
            deleteEntities(routingContext, AccessManagerService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getAccessManager(RoutingContext routingContext) {
        try {
            getEntity(routingContext, AccessManagerService.class, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateAccessManager(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, AccessManagerService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteAccessManager(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, AccessManagerService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

}
