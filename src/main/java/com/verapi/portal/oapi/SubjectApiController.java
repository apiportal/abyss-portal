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

import com.verapi.portal.common.Constants;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.idam.SubjectService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Objects;

import static com.verapi.portal.common.Util.encodeFileToBase64Binary;

@AbyssApiController(apiSpec = "/openapi/Subject.yaml")
public class SubjectApiController extends AbstractApiController {
    private static final Logger logger = LoggerFactory.getLogger(SubjectApiController.class);

/*
    private final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
            .createFor("/openapi/Subject.yaml")
            .build();
*/

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
        try {
            getEntities(routingContext, SubjectService.class, null, apiFilterQuery);
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

        // 1- generate password salt and password
        // 2-check subject request contains picture, if not then load default avatar picture
        requestBody.forEach(requestItem -> {
            String salt = authProvider.generateSalt();
            String hash = authProvider.computeHash(((JsonObject) requestItem).getString("password"), salt);
            ((JsonObject) requestItem).put("password", hash);
            ((JsonObject) requestItem).put("passwordsalt", salt);
/* TODO: read from jar file!!!
            if ((!((JsonObject) requestItem).containsKey("picture")) ||
                    (((JsonObject) requestItem).getValue("picture") == null) ||
                    (((JsonObject) requestItem).getValue("picture") == ""))
                try {
                    //insert default avatar image TODO: later use request base
                    ClassLoader classLoader = getClass().getClassLoader();
                    File file = new File(Objects.requireNonNull(classLoader.getResource(Constants.RESOURCE_DEFAULT_SUBJECT_AVATAR)).getFile());
                    ((JsonObject) requestItem).put("picture", "data:image/jpeg;base64," + encodeFileToBase64Binary(file));
                } catch (IOException e) {
                    logger.error(e.getLocalizedMessage());
                    logger.error(Arrays.toString(e.getStackTrace()));
                }
*/
            if (appendRequestBody != null && !appendRequestBody.isEmpty()) {
                appendRequestBody.forEach(entry -> {
                    ((JsonObject) requestItem).put(entry.getKey(), entry.getValue());
                });
            }

        });

        //now it is time to add entities
        try {
            addEntities(routingContext, SubjectService.class, requestBody);
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
            updateEntities(routingContext, SubjectService.class, requestBody, null, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void deleteEntities(RoutingContext routingContext, ApiFilterQuery apiFilterQuery) {
        try {
            deleteEntities(routingContext, SubjectService.class, apiFilterQuery);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    void execServiceMethod(RoutingContext routingContext, String method) {
        try {
            execServiceMethod(routingContext, SubjectService.class, null, method);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getSubjects(RoutingContext routingContext) {
        getEntities(routingContext, (ApiFilterQuery) null);
    }

    @AbyssApiOperationHandler
    public void addSubjects(RoutingContext routingContext) {

/*
        // ### STAR - request swagger validation using Atlassian SwaggerRequestResponseValidator ###
        Method requestMethod = Method.valueOf(routingContext.request().method().toString());
        String requestPath = routingContext.request().path().substring(routingContext.request().path().lastIndexOf(mountPoint) + mountPoint.length() + 1);
        SimpleRequest.Builder requestBuilder = new SimpleRequest.Builder(requestMethod, requestPath)
                .withBody(routingContext.getBodyAsJson().encode());
        routingContext.queryParams().names().forEach(paramName -> requestBuilder.withQueryParam(paramName, routingContext.queryParam(paramName)));
        routingContext.request().headers().names().forEach(headerName -> requestBuilder.withHeader(headerName, routingContext.request().getHeader(headerName)));
        ValidationReport report = validator.validateRequest(requestBuilder.build());
        List<ValidationReport.Message> messages = report.getMessages();
        if (!messages.isEmpty()) {
            logger.error(messages.toString());
            throwApiException(routingContext, UnProcessableEntity422Exception.class, HttpResponseStatus.UNPROCESSABLE_ENTITY.reasonPhrase(), messages.toString());
            return;
        }
        // ### END - request swagger validation using Atlassian SwaggerRequestResponseValidator ###
*/

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
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        try {
            getEntity(routingContext, SubjectService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void updateSubject(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        try {
            updateEntity(routingContext, SubjectService.class, requestBody);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void deleteSubject(RoutingContext routingContext) {

        try {
            deleteEntity(routingContext, SubjectService.class);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void getApps(RoutingContext routingContext) {
        getEntities(routingContext, new ApiFilterQuery().setFilterQuery(SubjectService.FILTER_APPS));
    }

    @AbyssApiOperationHandler
    public void addApps(RoutingContext routingContext) {
        addEntities(routingContext, new JsonObject().put("subjecttypeid", Constants.SUBJECT_TYPE_APP));
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
        addEntities(routingContext, new JsonObject().put("subjecttypeid", Constants.SUBJECT_TYPE_USER));
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
    public void updatePasswordOfSubject(RoutingContext routingContext) {
        execServiceMethod(routingContext, "changePassword");
    }

}
