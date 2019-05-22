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
import com.verapi.portal.service.idam.ApiApiTagService;
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

@AbyssApiController(apiSpec = "/openapi/ApiApiTag.yaml")
public class ApiApiTagApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiApiTagApiController.class);

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public ApiApiTagApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getApiApiTags(RoutingContext routingContext) {
        try {
            getEntities(routingContext, ApiApiTagService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addApiApiTags(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        try {
            addEntities(routingContext, ApiApiTagService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateApiApiTags(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, ApiApiTagService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteApiApiTags(RoutingContext routingContext) {
        try {
            deleteEntities(routingContext, ApiApiTagService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiApiTag(RoutingContext routingContext) {
        try {
            getEntity(routingContext, ApiApiTagService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateApiApiTag(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, ApiApiTagService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteApiApiTag(RoutingContext routingContext) {
        try {
            deleteEntity(routingContext, ApiApiTagService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiTagsByApi(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiApiTagService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiApiTagService.SQL_LIST_API_TAGS)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiTagsOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiApiTagService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiApiTagService.SQL_LIST_SUBJECT_API_TAGS)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getBusinessApiTagsOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiApiTagService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiApiTagService.SQL_LIST_SUBJECT_BUSINESS_API_TAGS)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getProxyApiTagsOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiApiTagService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiApiTagService.SQL_LIST_SUBJECT_PROXY_API_TAGS)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiApiTagByApiAndTag(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiApiTagService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiApiTagService.SQL_API_API_TAGS_BY_API_AND_TAG)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid")).add(routingContext.pathParam("taguuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e);
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

}
