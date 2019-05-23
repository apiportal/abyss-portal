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
import com.verapi.abyss.exception.NoDataFoundException;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.SubjectService;
import io.netty.handler.codec.http.HttpResponseStatus;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@AbyssApiController(apiSpec = "/openapi/Subject.yaml")
public class SubjectApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectApiController.class);
    private static final String PICTURE = "picture";
    private static final String SUBJECTTYPEID = "subjecttypeid";

    private static List<String> jsonbColumnsList = new ArrayList<>();

    static {
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_GROUPS);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_PERMISSIONS);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_RESOURCES);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_CONTRACTS);
        jsonbColumnsList.add(Constants.NESTED_COLUMN_USER_ORGANIZATIONS);
    }

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public SubjectApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    void getEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        getEntities(routingContext, null, apiFilterQuery);
    }

    void getEntities(RoutingContext routingContext, List<String> jsonColumns, ApiFilterQuery apiFilterQuery) {
        try {
            getEntities(routingContext, SubjectService.class, jsonColumns, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException | UnsupportedEncodingException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void addEntities(RoutingContext routingContext, JsonObject appendRequestBody) {
        addEntities(routingContext, appendRequestBody, false);
    }


    void addEntities(RoutingContext routingContext, JsonObject appendRequestBody, boolean isCascaded) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        // 1- generate password salt and password
        // 2-check subject request contains picture, if not then load default avatar picture
        requestBody.forEach((Object requestItem) -> {
            String salt = authProvider.generateSalt();
            String hash = authProvider.computeHash(((JsonObject) requestItem).getString("password"), salt);
            ((JsonObject) requestItem).put("password", hash);
            ((JsonObject) requestItem).put("passwordsalt", salt);


            //insert default avatar image
            if ((!((JsonObject) requestItem).containsKey(PICTURE)) ||
                    (((JsonObject) requestItem).getValue(PICTURE) == null) ||
                    (((JsonObject) requestItem).getValue(PICTURE) == "")) {
                try {
                    LOGGER.trace("addEntities - adding default avatar");
                    InputStream in = getClass().getResourceAsStream(Constants.RESOURCE_DEFAULT_SUBJECT_AVATAR);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line).append("\n");
                    }
                    in.close();
                    ((JsonObject) requestItem).put(PICTURE
                            , "data:image/jpeg;base64," + new String(Base64.getEncoder().encode(sb.toString().getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8));
                } catch (IOException e) {
                    LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
                }
            }

            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach((Map.Entry<String, Object> entry) -> ((JsonObject) requestItem).put(entry.getKey(), entry.getValue()));
            }

        });

        //now it is time to add entities
        try {
            if (isCascaded) {
                LOGGER.trace("---adding entities in a cascaded way");
                SubjectService subjectService = new SubjectService(routingContext.vertx());
                Single<List<JsonObject>> insertAllCascadedResult = subjectService
                        .initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                        .flatMap(jdbcClient -> subjectService.insertAllCascaded(routingContext, requestBody));
                subscribeAndResponseBulkList(routingContext, insertAllCascadedResult, null, HttpResponseStatus.MULTI_STATUS.code());
            } else {
                addEntities(routingContext, SubjectService.class, requestBody);
            }
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
            updateEntities(routingContext, SubjectService.class, requestBody, null, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, SubjectService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    private void execServiceMethod(RoutingContext routingContext, String method) {
        try {
            execServiceMethod(routingContext, SubjectService.class, null, method);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getSubjects(RoutingContext routingContext) {
        getEntities(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void addSubjects(RoutingContext routingContext) {
        addEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void updateSubjects(RoutingContext routingContext) {
        updateEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deleteSubjects(RoutingContext routingContext) {
        deleteEntities(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void getSubject(RoutingContext routingContext) {
        try {
            getEntity(routingContext, SubjectService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    private void getSubject(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            getEntity(routingContext,
                    SubjectService.class,
                    jsonbColumnsList,
                    apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    private void updateEntityCascaded(RoutingContext routingContext, JsonObject appendRequestBody) {
        LOGGER.trace("---updating entities in a cascaded way");

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
            appendRequestBody.forEach((Map.Entry<String, Object> entry) -> requestBody.put(entry.getKey(), entry.getValue()));
        }

        SubjectService subjectService = new SubjectService(routingContext.vertx());
        Single<ResultSet> updateCascadedResult = subjectService.initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient -> subjectService.update(UUID.fromString(routingContext.pathParam(STR_UUID)), requestBody))
                .flatMap(resultSet -> subjectService.findAll(new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_APP_WITH_CONTRACTS_AND_ACCESS_TOKENS)
                        .setFilterQueryParams(new JsonArray().add((String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME)))
                        .addFilterQuery(SubjectService.FILTER_APP_UUID)
                        .addFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID)))
                ))
                .flatMap((ResultSet resultSet) -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException("no_data_found"));
                    } else {
                        return Single.just(resultSet);
                    }
                });
        subscribeAndResponse(routingContext, updateCascadedResult, jsonbColumnsList, HttpResponseStatus.OK.code());
    }

    @AbyssApiOperationHandler
    public void updateSubject(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, SubjectService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteSubject(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, SubjectService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApps(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_APPS));
    }

    @AbyssApiOperationHandler
    public void addApps(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put(SUBJECTTYPEID, Constants.SUBJECT_TYPE_APP));
    }

    @AbyssApiOperationHandler
    public void updateApps(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_APP));
    }

    @AbyssApiOperationHandler
    public void deleteApps(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_APP));
    }

    @AbyssApiOperationHandler
    public void getUsers(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_USERS));
    }

    @AbyssApiOperationHandler
    public void addUsers(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put(SUBJECTTYPEID, Constants.SUBJECT_TYPE_USER));
    }

    @AbyssApiOperationHandler
    public void updateUsers(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_USER));
    }

    @AbyssApiOperationHandler
    public void deleteUsers(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_USER));
    }

    @AbyssApiOperationHandler
    public void getGroups(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_GROUPS));
    }

    @AbyssApiOperationHandler
    public void addGroups(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put(SUBJECTTYPEID, Constants.SUBJECT_TYPE_GROUP));
    }

    @AbyssApiOperationHandler
    public void updateGroups(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_GROUP));
    }

    @AbyssApiOperationHandler
    public void deleteGroups(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_GROUP));
    }


    @AbyssApiOperationHandler
    public void getRoles(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_ROLES));
    }

    @AbyssApiOperationHandler
    public void addRoles(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put(SUBJECTTYPEID, Constants.SUBJECT_TYPE_ROLE));
    }

    @AbyssApiOperationHandler
    public void updateRoles(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_ROLE));
    }

    @AbyssApiOperationHandler
    public void deleteRoles(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.SQL_CONDITION_IS_ROLE));
    }


    @AbyssApiOperationHandler
    public void updatePasswordOfSubject(RoutingContext routingContext) {
        execServiceMethod(routingContext, "changePassword");
    }

    @AbyssApiOperationHandler
    public void getUsersWithGroups(RoutingContext routingContext) {
        getEntities(routingContext, jsonbColumnsList, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_USERS_WITH_GROUPS));
    }

    @AbyssApiOperationHandler
    public void getUserWithGroupsAndPermissions(RoutingContext routingContext) {
        getSubject(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_USER_WITH_GROUPS_AND_PERMISSIONS)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }

    @AbyssApiOperationHandler
    public void getUsersUnderDirectory(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_USERS_UNDER_DIRECTORY)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }

    @AbyssApiOperationHandler
    public void getGroupsUnderDirectory(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_GROUPS_UNDER_DIRECTORY)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }


    @AbyssApiOperationHandler
    public void getAppsOfUser(RoutingContext routingContext) {
        getEntities(routingContext, jsonbColumnsList, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_APP_WITH_CONTRACTS_AND_ACCESS_TOKENS)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID))));
    }

    @AbyssApiOperationHandler
    public void addAppsCascaded(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject()
                        .put(SUBJECTTYPEID, Constants.SUBJECT_TYPE_APP)
                        .put("crudsubjectid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME))
                        .put("organizationid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME)),
                true
        );
    }

    @AbyssApiOperationHandler
    public void updateAppCascaded(RoutingContext routingContext) {
        updateEntityCascaded(routingContext, new JsonObject()
                .put(SUBJECTTYPEID, Constants.SUBJECT_TYPE_APP)
                .put("organizationid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .put("crudsubjectid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME))
        );
    }


    @AbyssApiOperationHandler
    public void getCurrentUser(RoutingContext routingContext) {
        getEntities(routingContext, jsonbColumnsList, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_USER_WITH_ORGANIZATIONS)
                .setFilterQueryParams(new JsonArray().add((String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME))));
    }

    @AbyssApiOperationHandler
    public void getSubjectImage(RoutingContext routingContext) {

        if (routingContext.pathParam(STR_UUID) == null || routingContext.pathParam(STR_UUID).isEmpty()) {
            LOGGER.error("getSubjectImage invoked - uuid null or empty");
            throwApiException(routingContext, BadRequest400Exception.class, "getSubjectImage uuid null or empty");
        }

        SubjectService subjectService = new SubjectService(vertx);
        Single<ResultSet> resultSetSingle = subjectService.initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME)
                , routingContext.get(Constants.AUTH_ABYSS_PORTAL_ROUTING_CONTEXT_OPERATION_ID))
                .flatMap(jdbcClient -> subjectService.findAll(
                        new ApiFilterQuery()
                                .setFilterQuery(SubjectService.SQL_GET_IMAGE_BY_UUID)
                                .setFilterQueryParams(new JsonArray()
                                        .add(routingContext.pathParam(STR_UUID))))
                );
        subscribeForImage(routingContext, resultSetSingle, "getSubjectImage", PICTURE);
    }

}
