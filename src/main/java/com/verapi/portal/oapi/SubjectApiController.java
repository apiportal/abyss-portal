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

import com.atlassian.oai.validator.SwaggerRequestResponseValidator;
import com.atlassian.oai.validator.model.Request;
import com.atlassian.oai.validator.model.Request.Method;
import com.atlassian.oai.validator.model.SimpleRequest;
import com.atlassian.oai.validator.report.ValidationReport;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.oapi.exception.UnProcessableEntity422Exception;
import com.verapi.portal.service.idam.SubjectService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@AbyssApiController(apiSpec = "/openapi/Subject.yaml")
public class SubjectApiController extends AbstractApiController {
    private static Logger logger = LoggerFactory.getLogger(SubjectApiController.class);

    private final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
            .createFor("/openapi/Subject.yaml")
            .build();

    public SubjectApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

        JsonObject subjects = new JsonObject().put("path", "getSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString())
                    .put("method", methodName);

        }

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

    @AbyssApiOperationHandler
    public void addSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

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

        // Get the parsed parameters
        RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject subjects = params.body().getJsonObject();

        SubjectService subjectService = new SubjectService(vertx);

        Single<JsonObject> serviceResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.insert(subjects))
                .flatMap(insertResult -> {
                    logger.trace("inserted row key: " + insertResult.getKeys().toString());
                    return subjectService.findBySubjectId(insertResult.getKeys().getInteger(0));
                })
                .flatMap(findResult -> {
                    logger.trace("inserted row: " + findResult.getRows());
                    return Single.just(findResult.toJson());
                });

        serviceResult.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(201)
                            .end(resp.encode(), "UTF-8");
                    logger.trace(methodName + " replied successfully " + resp.encodePrettily());
                },
                throwable -> {
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                    logger.error(methodName + " exception occured " + throwable.getLocalizedMessage());
                    logger.error(methodName + " exception occured " + Arrays.toString(throwable.getStackTrace()));
                });

    }

    @AbyssApiOperationHandler
    public void updateSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

        // Get the parsed parameters
        //RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        //JsonObject subjects = params.body().getJsonObject();

        JsonObject subjects = new JsonObject().put("path", "addSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString());
            //.put("method", getClass().getEnclosingMethod().getName());

        }
/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

    @AbyssApiOperationHandler
    public void deleteSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

        // Get the parsed parameters
        //RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        //JsonObject subjects = params.body().getJsonObject();

        JsonObject subjects = new JsonObject().put("path", "addSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString());
            //.put("method", getClass().getEnclosingMethod().getName());

        }
/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

}
