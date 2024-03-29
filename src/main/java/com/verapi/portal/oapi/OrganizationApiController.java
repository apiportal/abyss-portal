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
import com.verapi.abyss.exception.BadRequest400Exception;
import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.OrganizationService;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

@AbyssApiController(apiSpec = "/openapi/Organization.yaml")
public class OrganizationApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrganizationApiController.class);

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public OrganizationApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getOrganizations(RoutingContext routingContext) {
        try {
            getEntities(routingContext, OrganizationService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addOrganizations(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        try {
            addEntities(routingContext, OrganizationService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateOrganizations(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, OrganizationService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteOrganizations(RoutingContext routingContext) {
        try {
            deleteEntities(routingContext, OrganizationService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getOrganization(RoutingContext routingContext) {
        try {
            getEntity(routingContext, OrganizationService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateOrganization(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, OrganizationService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteOrganization(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, OrganizationService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getOrganizationImage(RoutingContext routingContext) {

        if (routingContext.pathParam(STR_UUID) == null || routingContext.pathParam(STR_UUID).isEmpty()) {
            LOGGER.error("getOrganizationImage invoked - uuid null or empty");
            throwApiException(routingContext, BadRequest400Exception.class, "getOrganizationImage uuid null or empty");
        }

        OrganizationService organizationService = new OrganizationService(vertx);
        Single<ResultSet> resultSetSingle = organizationService.initJDBCClient(
                routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME)
                , routingContext.get(Constants.AUTH_ABYSS_PORTAL_ROUTING_CONTEXT_OPERATION_ID))
                .flatMap(jdbcClient -> organizationService.findAll(
                        new ApiFilterQuery()
                                .setFilterQuery(OrganizationService.SQL_GET_IMAGE_BY_UUID)
                                .setFilterQueryParams(new JsonArray()
                                        .add(routingContext.pathParam(STR_UUID))))
                );
        subscribeForImage(routingContext, resultSetSingle, "getOrganizationImage", "picture");
    }

}
