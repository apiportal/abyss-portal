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

import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.ApiLicenseService;
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

@AbyssApiController(apiSpec = "/openapi/ApiLicense.yaml")
public class ApiLicenseApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiLicenseApiController.class);

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public ApiLicenseApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getApiLicenses(RoutingContext routingContext) {
        try {
            getEntities(routingContext, ApiLicenseService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addApiLicenses(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        try {
            addEntities(routingContext, ApiLicenseService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateApiLicenses(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, ApiLicenseService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteApiLicenses(RoutingContext routingContext) {
        try {
            deleteEntities(routingContext, ApiLicenseService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiLicense(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        try {
            getEntity(routingContext, ApiLicenseService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateApiLicense(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, ApiLicenseService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteApiLicense(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, ApiLicenseService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiLicensesOfApi(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters"); //TODO: Lazım mı?

        try {
            getEntities(routingContext,
                    ApiLicenseService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiLicenseService.SQL_LIST_API_LICENSES)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApisOfLicense(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters"); //TODO: Lazım mı?

        try {
            getEntities(routingContext,
                    ApiLicenseService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiLicenseService.SQL_LIST_LICENSE_APIS)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiLicensesOfUser(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters"); //TODO: Lazım mı?

        try {
            getEntities(routingContext,
                    ApiLicenseService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiLicenseService.SQL_LIST_API_LICENSES_OF_USER)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

}
