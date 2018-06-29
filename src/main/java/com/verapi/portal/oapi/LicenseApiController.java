/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 6 2018
 *
 */

package com.verapi.portal.oapi;

import com.verapi.portal.common.Constants;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.LicenseService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@AbyssApiController(apiSpec = "/openapi/License.yaml")
public class LicenseApiController extends AbstractApiController {
    private static final Logger logger = LoggerFactory.getLogger(LicenseApiController.class);

    private static List<String> jsonbColumnsList = new ArrayList<String>() {{
        add(Constants.JSONB_COLUMN_LICENSE_LICENSEDOCUMENT);
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
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
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
            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach(entry -> {
                    ((JsonObject) requestItem).put(entry.getKey(), entry.getValue());
                });
            }
        });

        try {
            addEntities(routingContext, LicenseService.class, requestBody, jsonbColumnsList);
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
            updateEntities(routingContext, LicenseService.class, requestBody, jsonbColumnsList, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, LicenseService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
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
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        try {
            getEntity(routingContext, LicenseService.class, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateLicense(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, LicenseService.class, requestBody, jsonbColumnsList);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteLicense(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, LicenseService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
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

}
