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

import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.ApiService;
import com.verapi.portal.service.idam.ApiTagService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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

    private static List<String> jsonbColumnsList = new ArrayList<String>() {{
        add(Constants.JSONB_COLUMN_API_OPENAPIDOCUMENT);
        add(Constants.JSONB_COLUMN_API_EXTENDEDDOCUMENT);
    }};

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
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, "utf-8"));
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
                appendRequestBody.forEach(entry -> {
                    ((JsonObject) requestItem).put(entry.getKey(), entry.getValue());
                });
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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
/*
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, ApiService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
*/
    }

    @AbyssApiOperationHandler
    public void deleteApi(RoutingContext routingContext) {
        deleteEntity(routingContext, (ApiFilterQuery) null);
/*
        try {
            deleteEntity(routingContext, ApiService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
*/
    }

    @AbyssApiOperationHandler
    public void getApisOfSubject(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        try {
            //getEntities(routingContext, ApiService.class, jsonbColumnsList, ApiService.FILTER_BY_SUBJECT.setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters"); //TODO: Lazım mı?

        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(ApiService.FILTER_BY_BUSINESS_API));
    }

    @AbyssApiOperationHandler
    public void addBusinessApis(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("isproxyapi", false));
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
        addEntities(routingContext, new JsonObject().put("isproxyapi", true));
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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")).put("isproxyapi", false));
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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")).put("isproxyapi", true));
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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters"); //TODO: Lazım mı?

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

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

}
