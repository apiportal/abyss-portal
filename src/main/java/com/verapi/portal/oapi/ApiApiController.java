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
import com.verapi.portal.service.idam.ApiService;
import com.verapi.portal.service.idam.ApiTagService;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@AbyssApiController(apiSpec = "/openapi/Api.yaml")
public class ApiApiController extends AbstractApiController {
    private static final Logger logger = LoggerFactory.getLogger(ApiApiController.class);

    private static List<String> jsonbColumnsList = new ArrayList<>();

    static {
        jsonbColumnsList.add(Constants.JSONB_COLUMN_API_OPENAPIDOCUMENT);
        jsonbColumnsList.add(Constants.JSONB_COLUMN_API_EXTENDEDDOCUMENT);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_RESOURCES);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_API_SERVERS);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_API_AVAILABLE_LICENSES);
    }

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public ApiApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    void getEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            getEntities(routingContext, ApiService.class, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void addEntities(RoutingContext routingContext, JsonObject appendRequestBody) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        requestBody.forEach(requestItem -> {
            //insert default avatar image TODO: later use request base
            if ((!((JsonObject) requestItem).containsKey("image")) ||
                    (((JsonObject) requestItem).getValue("image") == null) ||
                    (((JsonObject) requestItem).getValue("image") == ""))
                try {
                    InputStream in = getClass().getResourceAsStream(Constants.RESOURCE_DEFAULT_API_AVATAR);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    in.close();
                    ((JsonObject) requestItem).put("image", "data:image/jpeg;base64," + new String(Base64.getEncoder().encode(sb.toString().getBytes()), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    logger.error(e.getLocalizedMessage());
                    logger.error(Arrays.toString(e.getStackTrace()));
                }

            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach(entry -> ((JsonObject) requestItem).put(entry.getKey(), entry.getValue()));
            }
        });

        try {
            addEntities(routingContext, ApiService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void updateEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, ApiService.class, requestBody, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, ApiService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void getEntity(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            getEntity(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void updateEntity(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, ApiService.class, requestBody, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntity(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntity(routingContext, ApiService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApis(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery());
    }

    @AbyssApiOperationHandler
    public void addApis(RoutingContext routingContext) {
        addEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void updateApis(RoutingContext routingContext) {
        updateEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deleteApis(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery());
    }

    @AbyssApiOperationHandler
    public void getApi(RoutingContext routingContext) {
        getEntity(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void updateApi(RoutingContext routingContext) {
        updateEntity(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deleteApi(RoutingContext routingContext) {
        deleteEntity(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void getApisOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addApisOfSubject(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")));
    }

    @AbyssApiOperationHandler
    public void updateApisOfSubject(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void deleteApisOfSubject(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.SQL_DELETE_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void getBusinessApis(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.FILTER_BY_BUSINESS_API));
    }

    @AbyssApiOperationHandler
    public void addBusinessApis(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("isproxyapi", Boolean.FALSE));
    }

    @AbyssApiOperationHandler
    public void updateBusinessApis(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI));
    }

    @AbyssApiOperationHandler
    public void deleteBusinessApis(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI));
    }

    @AbyssApiOperationHandler
    public void getBusinessApi(RoutingContext routingContext) {
        getEntity(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.SQL_FIND_BY_UUID + ApiService.SQL_AND + ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                .addFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void updateBusinessApi(RoutingContext routingContext) {
        updateEntity(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deleteBusinessApi(RoutingContext routingContext) {
        deleteEntity(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void getApiProxies(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.FILTER_BY_PROXY_API));
    }

    @AbyssApiOperationHandler
    public void addApiProxies(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("isproxyapi", Boolean.TRUE));
    }

    @AbyssApiOperationHandler
    public void updateApiProxies(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI));
    }

    @AbyssApiOperationHandler
    public void deleteApiProxies(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI));
    }

    @AbyssApiOperationHandler
    public void getApiProxy(RoutingContext routingContext) {
        getEntity(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.SQL_FIND_BY_UUID + ApiService.SQL_AND + ApiService.SQL_CONDITION_IS_PROXYAPI)
                .addFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void updateApiProxy(RoutingContext routingContext) {
        updateEntity(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deleteApiProxy(RoutingContext routingContext) {
        deleteEntity(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void getBusinessApisOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addBusinessApisOfSubject(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")).put("isproxyapi", Boolean.FALSE));
    }

    @AbyssApiOperationHandler
    public void updateBusinessApisOfSubject(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                .addFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void deleteBusinessApisOfSubject(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.SQL_DELETE_BY_SUBJECT)
                .addFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void getApiProxiesOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void addApiProxiesOfSubject(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")).put("isproxyapi", Boolean.TRUE));
    }

    @AbyssApiOperationHandler
    public void updateApiProxiesOfSubject(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                .addFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void deleteApiProxiesOfSubject(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(ApiService.SQL_DELETE_BY_SUBJECT)
                .addFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void getTagsOfBusinessApisOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getAggregatedTagsOfBusinessApisOfSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiTagService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiTagService.SQL_BUSINESS_API_AGGREGATE_COUNT)
                            .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getBusinessApisOfSubjectByTag(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT_AND_TAG)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))
                                    .add(routingContext.pathParam("tag"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getBusinessApisOfSubjectByCategory(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT_AND_CATEGORY)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))
                                    .add(routingContext.pathParam("category"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getBusinessApisOfSubjectByGroup(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT_AND_GROUP)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_BUSINESSAPI)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))
                                    .add(routingContext.pathParam("group"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiProxiesOfSubjectByTag(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT_AND_TAG)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))
                                    .add(routingContext.pathParam("tag"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiProxiesOfSubjectByCategory(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT_AND_CATEGORY)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))
                                    .add(routingContext.pathParam("category"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiProxiesOfSubjectByGroup(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_SUBJECT_AND_GROUP)
                            .addFilterQuery(ApiService.SQL_CONDITION_IS_PROXYAPI)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))
                                    .add(routingContext.pathParam("group"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApisSharedWithSubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_APIS_SHARED_WITH_SUBJECT)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApisSharedBySubject(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_APIS_SHARED_BY_SUBJECT)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiProxiesUnderLicense(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_LICENSE)
                            .setFilterQueryParams(new JsonArray()
                                    .add(routingContext.pathParam("uuid"))));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApisByPolicy(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_BY_POLICY)
                            .setFilterQueryParams(new JsonArray().add("[\"" + routingContext.pathParam("uuid") + "\"]")));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiImage(RoutingContext routingContext) {

        if (routingContext.pathParam("uuid") == null || routingContext.pathParam("uuid").isEmpty()) {
            logger.error("getApiImage invoked - uuid null or empty");
            throwApiException(routingContext, BadRequest400Exception.class, "getApiImage uuid null or empty");
        }

        ApiService apiService = new ApiService(vertx);
        Single<ResultSet> resultSetSingle = apiService.initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME), routingContext.get(Constants.AUTH_ABYSS_PORTAL_ROUTING_CONTEXT_OPERATION_ID))
                .flatMap(jdbcClient -> apiService.findAll(
                        new ApiFilterQuery()
                                .setFilterQuery(ApiService.SQL_GET_IMAGE_BY_UUID)
                                .setFilterQueryParams(new JsonArray()
                                        .add(routingContext.pathParam("uuid"))))
                );
        subscribeForImage(routingContext, resultSetSingle, "getApiImage", "image");
    }

    @AbyssApiOperationHandler
    public void getApiProxiesExplore(RoutingContext routingContext) {
        try {
            getEntities(routingContext,
                    ApiService.class,
                    jsonbColumnsList,
                    new ApiFilterQuery()
                            .setFilterQuery(ApiService.FILTER_PROXIES_WITH_RESOURCES_FOR_EXPLORE));
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }
}
