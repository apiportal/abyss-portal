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

import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.SubjectPermissionService;
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
import java.util.Arrays;

@AbyssApiController(apiSpec = "/openapi/SubjectPermission.yaml")
public class SubjectPermissionApiController extends AbstractApiController {
    private static final Logger logger = LoggerFactory.getLogger(SubjectPermissionApiController.class);

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public SubjectPermissionApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    void getEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            getEntities(routingContext, SubjectPermissionService.class, null, apiFilterQuery);
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
            addEntities(routingContext, SubjectPermissionService.class, requestBody, null);
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
            updateEntities(routingContext, SubjectPermissionService.class, requestBody, null, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, SubjectPermissionService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }


    @AbyssApiOperationHandler
    public void getSubjectPermissions(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery());
/*
        try {
            getEntities(routingContext, SubjectPermissionService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
*/
    }

    @AbyssApiOperationHandler
    public void addSubjectPermissions(RoutingContext routingContext) {
        addEntities(routingContext, null);
/*
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON array validated by Vert.x Open API validator
        JsonArray requestBody = requestParameters.body().getJsonArray();

        try {
            addEntities(routingContext, SubjectPermissionService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
*/
    }

    @AbyssApiOperationHandler
    public void updateSubjectPermissions(RoutingContext routingContext) {
        updateEntities(routingContext, null);
/*
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        //now it is time to update entities
        try {
            updateEntities(routingContext, SubjectPermissionService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
*/
    }

    @AbyssApiOperationHandler
    public void deleteSubjectPermissions(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery());
/*
        try {
            deleteEntities(routingContext, SubjectPermissionService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
*/
    }

    @AbyssApiOperationHandler
    public void getSubjectPermission(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        try {
            getEntity(routingContext, SubjectPermissionService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateSubjectPermission(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, SubjectPermissionService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteSubjectPermission(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, SubjectPermissionService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApiSubscriptionsOfSubject(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(SubjectPermissionService.SQL_LIST_SUBJECT_API_SUBSCRIPTIONS)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void addApiSubscriptionsOfSubject    (RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjectid", routingContext.pathParam("uuid")));
    }

    @AbyssApiOperationHandler
    public void updateApiSubscriptionsOfSubject(RoutingContext routingContext) {
        updateEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(SubjectPermissionService.FILTER_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

    @AbyssApiOperationHandler
    public void deleteApiSubscriptionsOfSubject(RoutingContext routingContext) {
        deleteEntities(routingContext, new ApiFilterQuery()
                .setFilterQuery(SubjectPermissionService.SQL_DELETE_BY_SUBJECT)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam("uuid"))));
    }

}
