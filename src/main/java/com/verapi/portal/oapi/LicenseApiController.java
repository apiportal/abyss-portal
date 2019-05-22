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
import com.verapi.portal.service.idam.LicenseService;
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

@AbyssApiController(apiSpec = "/openapi/License.yaml")
public class LicenseApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(LicenseApiController.class);

    private static List<String> jsonbColumnsList = new ArrayList<String>() {{
        add(Constants.JSONB_COLUMN_LICENSE_LICENSEDOCUMENT);
        add(Constants.NESTED_COLUMN_USER_RESOURCES);
    }};

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public LicenseApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    void getEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            getEntities(routingContext, LicenseService.class, jsonbColumnsList, apiFilterQuery);
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

        requestBody.forEach(requestItem -> {
            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach(entry -> {
                    ((JsonObject) requestItem).put(entry.getKey(), entry.getValue());
                });
            }
        });

        try {
            if (isCascaded) {
                LOGGER.trace("---adding entities in a cascaded way");
                LicenseService licenseService = new LicenseService(routingContext.vertx());
                //licenseService.setAutoCommit(false);
                Single<List<JsonObject>> insertAllCascadedResult = licenseService.initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                        .flatMap(jdbcClient -> licenseService.insertAllCascaded(routingContext, requestBody));
                subscribeAndResponseBulkList(routingContext, insertAllCascadedResult, jsonbColumnsList, HttpResponseStatus.MULTI_STATUS.code());
            } else {
                addEntities(routingContext, LicenseService.class, requestBody, jsonbColumnsList);
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
            updateEntities(routingContext, LicenseService.class, requestBody, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, LicenseService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getLicenses(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery());
    }

    @AbyssApiOperationHandler
    public void addLicenses(RoutingContext routingContext) {
        addEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void updateLicenses(RoutingContext routingContext) {
        updateEntities(routingContext, null);
    }

    @AbyssApiOperationHandler
    public void deleteLicenses(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery());
    }

    @AbyssApiOperationHandler
    public void getLicense(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        try {
            getEntity(routingContext, LicenseService.class, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateLicense(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, LicenseService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteLicense(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, LicenseService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            LOGGER.error(EXCEPTION_LOG_FORMAT, e.getMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getLicensesOfSubject(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.FILTER_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void addLicensesOfSubject(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")));
    }

    @AbyssApiOperationHandler
    public void updateLicensesOfSubject(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.FILTER_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void deleteLicensesOfSubject(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.SQL_DELETE_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void getLicensesOfApi(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.FILTER_BY_API)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void getLicensesOfApiInUse(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.FILTER_BY_API)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void getLicensesOfSubjectCascaded(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.FILTER_BY_SUBJECT_WITH_RESOURCES_AND_PERMISSIONS)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void addLicensesOfSubjectCascaded(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject()
                        .put("organizationid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                        .put("crudsubjectid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME))
                        .put("subjectid", (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME))
                , true
        );
    }

    @AbyssApiOperationHandler
    public void getLicensesOfPolicy(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(LicenseService.FILTER_BY_POLICY)
                .setFilterQueryParams(new JsonArray().add("[\"" + routingContext.pathParam("uuid") + "\"]")));
    }

}
