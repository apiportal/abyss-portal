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
import com.verapi.portal.service.idam.ContractService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
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

@AbyssApiController(apiSpec = "/openapi/Contract.yaml")
public class ContractApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractApiController.class);

    private static List<String> jsonbColumnsList = new ArrayList<>();

    static {
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_RESOURCES);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_PERMISSIONS);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_LICENSES);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_APP_OWNERS);
    }

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public ContractApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getContracts(RoutingContext routingContext) {
        try {
            getEntities(routingContext, ContractService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addContracts(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        try {
            addEntities(routingContext, ContractService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateContracts(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, ContractService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteContracts(RoutingContext routingContext) {
        try {
            deleteEntities(routingContext, ContractService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContract(RoutingContext routingContext) {
        try {
            getEntity(routingContext, ContractService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateContract(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, ContractService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteContract(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, ContractService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfApi(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_APIID)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfApp(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_APPID)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfApiOfUser(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_APIS_OF_USERID)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfAppOfUser(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_APPS_OF_USERID)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfLicense(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_LICENSEID)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfUser(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_LICENSES_OF_USER)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getContractsOfPolicy(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ContractService.class,
                    null,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.FILTER_BY_POLICY)
                            .setFilterQueryParams(new JsonArray().add("[\"" + routingContext.pathParam("uuid") + "\"]")));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addSubscriptions(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        //Change request to make it suitable for subject permission
        JsonObject appendRequestBody = new JsonObject()
                .put("organizationid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .put("crudsubjectid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME));

        requestBody.forEach((Object requestItem) -> {
            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach((Map.Entry<String, Object> entry) -> ((JsonObject) requestItem).put(entry.getKey(), entry.getValue()));
            }
        });

        try {
            LOGGER.trace("---adding entities in a cascaded way for subscription");
            ContractService contractService = new ContractService(routingContext.vertx());
            Single<List<JsonObject>> insertAllCascadedResult = contractService.initJDBCClient(routingContext
                    .session()
                    .get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                    .flatMap(jdbcClient -> contractService.insertAllCascaded(routingContext, requestBody));
            subscribeAndResponseBulkList(routingContext, insertAllCascadedResult, jsonbColumnsList, HttpResponseStatus.MULTI_STATUS.code());
        } catch (Exception e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getSubscriptionsOfApi(RoutingContext routingContext) {

        try {
            getEntities(routingContext,
                    ContractService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ContractService.SQL_SUBSCRIPTIONS_OF_API)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }
}
