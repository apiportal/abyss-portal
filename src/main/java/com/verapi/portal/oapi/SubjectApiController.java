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
import com.verapi.portal.oapi.exception.NotImplemented501Exception;
import com.verapi.portal.service.idam.SubjectService;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.UUID;

import static com.verapi.portal.common.Util.encodeFileToBase64Binary;

@AbyssApiController(apiSpec = "/openapi/Subject.yaml")
public class SubjectApiController extends AbstractApiController {
    private static Logger logger = LoggerFactory.getLogger(SubjectApiController.class);

/*
    private final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
            .createFor("/openapi/Subject.yaml")
            .build();
*/

    public SubjectApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getSubjects(RoutingContext routingContext) {
        SubjectService subjectService = new SubjectService(vertx);
        Single<ResultSet> findAllResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.findAll())
                .flatMap(result -> {
                    logger.trace(result.getNumRows() + " rows selected");
                    return Single.just(result);
                });

        findAllResult.subscribe(resp -> {
                    JsonArray arr = new JsonArray();
                    resp.getRows().forEach(arr::add);

                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(arr.encode(), "UTF-8");

                    logger.trace("replied successfully " + arr.encodePrettily());
                },
                throwable -> {
                    logger.error("exception occured " + throwable.getLocalizedMessage());
                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });

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

        // Get the parsed parameters
        RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject subjects = params.body().getJsonObject();

        String salt = authProvider.generateSalt();
        String hash = authProvider.computeHash(subjects.getString("password"), salt);
        subjects.put("password", hash);
        subjects.put("passwordsalt", salt);
        if ((!subjects.containsKey("picture")) || (subjects.getValue("picture") == null))
            try {
                //insert default avatar image TODO: later use request base64 img data
                ClassLoader classLoader = getClass().getClassLoader();
                File file = new File(Objects.requireNonNull(classLoader.getResource("webroot/dist/img/avatar.jpg")).getFile());
                subjects.put("picture", encodeFileToBase64Binary(file));
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
                logger.error(Arrays.toString(e.getStackTrace()));
            }

        SubjectService subjectService = new SubjectService(vertx);

        Single<ResultSet> insertResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.insert(subjects))
                .flatMap(result -> {
                    logger.trace("inserted row key: " + result.getKeys().toString());
                    return Single.just(result);
                })
                .flatMap(insResult -> {
                    Single<ResultSet> findResult = subjectService.findById(insResult.getKeys().getInteger(0));
                    logger.trace("selected row: " + findResult.toString());
                    return findResult;
                });
        insertResult.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(201)
                            .end(resp.getRows().get(0).encode(), "UTF-8");
                    logger.trace("replied successfully " + resp.toJson().encodePrettily());
                },
                throwable -> {
                    logger.error("exception occured " + throwable.getLocalizedMessage());
                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });

    }

    @AbyssApiOperationHandler
    public void updateSubjects(RoutingContext routingContext) {
        throwApiException(routingContext, NotImplemented501Exception.class);
    }

    @AbyssApiOperationHandler
    public void deleteSubjects(RoutingContext routingContext) {
        throwApiException(routingContext, NotImplemented501Exception.class);
    }

    @AbyssApiOperationHandler
    public void getSubject(RoutingContext routingContext) {
        // Get the parsed parameters
        RequestParameters params = routingContext.get("parsedParameters");

        SubjectService subjectService = new SubjectService(vertx);
        Single<ResultSet> findResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.findById(UUID.fromString(params.pathParameter("uuid").getString())))
                .flatMap(result -> {
                    logger.trace(result.getNumRows() + " rows selected");
                    return Single.just(result);
                });

        findResult.subscribe(resp -> {

                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(resp.toJson().getJsonObject("rows").encode(), "UTF-8");

                    logger.trace("replied successfully " + resp.toJson().getJsonObject("rows").encodePrettily());
                },
                throwable -> {
                    logger.error("exception occured " + throwable.getLocalizedMessage());
                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });

    }

/*
    @AbyssApiOperationHandler
    public void addSubject(RoutingContext routingContext) {
        throwApiException(routingContext, MethodNotAllowed405Exception.class);
    }
*/

    @AbyssApiOperationHandler
    public void updateSubject(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject subjects = params.body().getJsonObject();

        SubjectService subjectService = new SubjectService(vertx);

        Single<ResultSet> updateResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.update(UUID.fromString(params.pathParameter("uuid").getString()), subjects))
                .flatMap(result -> {
                    logger.trace("updated row key: " + result.getKeys().toString());
                    return Single.just(result);
                })
                .flatMap(updResult -> {
                    Single<ResultSet> findResult = subjectService.findById(updResult.getKeys().getInteger(0));
                    logger.trace("updated row: " + findResult.toString());
                    return findResult;
                });
        updateResult.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(200)
                            .end(resp.getRows().get(0).encode(), "UTF-8");
                    logger.trace("replied successfully " + resp.toJson().encodePrettily());
                },
                throwable -> {
                    logger.error("exception occured " + throwable.getLocalizedMessage());
                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });
    }

    @AbyssApiOperationHandler
    public void deleteSubject(RoutingContext routingContext) {

        // Get the parsed parameters
        RequestParameters params = routingContext.get("parsedParameters");

        SubjectService subjectService = new SubjectService(vertx);

        Single<UpdateResult> updateResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.delete(UUID.fromString(params.pathParameter("uuid").getString())))
                .flatMap(result -> {
                    logger.trace("deleted row key: " + result.getKeys().toString());
                    return Single.just(result);
                });
        updateResult.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(204)
                            .end();
                    logger.trace("replied successfully " + resp.toJson().encodePrettily());
                },
                throwable -> {
                    logger.error("exception occured " + throwable.getLocalizedMessage());
                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });

    }

}
