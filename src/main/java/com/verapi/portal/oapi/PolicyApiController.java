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
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.PolicyService;
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
import java.util.Map;

@AbyssApiController(apiSpec = "/openapi/Policy.yaml")
public class PolicyApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyApiController.class);

    private static List<String> jsonbColumnsList = new ArrayList<>();

    static {
        jsonbColumnsList.add(Constants.JSONB_COLUMN_POLICY_POLICYINSTANCE);
    }

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public PolicyApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    void getEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            getEntities(routingContext, PolicyService.class, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void addEntities(RoutingContext routingContext, JsonObject appendRequestBody) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        requestBody.forEach((Object requestItem) -> {
            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach((Map.Entry<String, Object> entry) -> ((JsonObject) requestItem).put(entry.getKey(), entry.getValue()));
            }
        });

        try {
            addEntities(routingContext, PolicyService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void updateEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, PolicyService.class, requestBody, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, PolicyService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getPolicies(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery());
    }

    @AbyssApiOperationHandler
    public void addPolicies(RoutingContext routingContext) {
        addEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void updatePolicies(RoutingContext routingContext) {
        updateEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deletePolicies(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery());
    }

    @AbyssApiOperationHandler
    public void getPolicy(RoutingContext routingContext) {
        try {
            getEntity(routingContext, PolicyService.class, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updatePolicy(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, PolicyService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deletePolicy(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, PolicyService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getPoliciesOfSubject(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(PolicyService.FILTER_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }

    @AbyssApiOperationHandler
    public void addPoliciesOfSubject(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam(STR_UUID)));
    }

    @AbyssApiOperationHandler
    public void updatePoliciesOfSubject(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(PolicyService.FILTER_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }

    @AbyssApiOperationHandler
    public void deletePoliciesOfSubject(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(PolicyService.SQL_DELETE_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }

}
