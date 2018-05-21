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

import static com.verapi.portal.common.Util.encodeFileToBase64Binary;

import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.service.idam.SubjectService;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
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

        String salt = authProvider.generateSalt();
        String hash = authProvider.computeHash(subjects.getString("password"), salt);
        subjects.put("password", hash);
        subjects.put("passwordsalt", salt);
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
                .flatMap(jdbcClient -> subjectService.insertJson(subjects))
                .flatMap(result -> {
                    logger.trace("inserted row key: " + result.getKeys().toString());
                    return Single.just(result);
                })
                .flatMap(insResult -> {
                    Single<ResultSet> updResult = subjectService.findBySubjectId(insResult.getKeys().getInteger(0));
                    logger.trace("selected row: " + insResult.toString());
                    return updResult;
                });
        insertResult.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(201)
                            .end(resp.getRows().get(0).encode(), "UTF-8");
                    logger.trace(methodName + " replied successfully " + resp.toJson().encodePrettily());
                },
                throwable -> {
                    logger.error(methodName + " exception occured " + throwable.getLocalizedMessage());
                    logger.error(methodName + " exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });



/*
        Single<UpdateResult> insertResult = subjectService.initJDBCClient()
                .flatMap(jdbcClient -> subjectService.insertJson(subjects))
                .flatMap(result -> {
                    logger.trace("inserted row key: " + result.getKeys().toString());
                    return Single.just(result);
                });
*/
/*
                .flatMap(findResult -> {
                    subjectService.findBySubjectId(findResult.getKeys().getInteger(0));
                    logger.trace("selected row: " + findResult.toString());
                    return Single.just(findResult.toJson());
                });
*//*


        insertResult.subscribe(resp -> {
                    Single<ResultSet> selectResult = subjectService.findBySubjectId(resp.getKeys().getInteger(0))
                            .flatMap(resultSet -> {
                                logger.trace("selected row: " + resultSet.toString());
                                return Single.just(resultSet);
                            });

                    selectResult.subscribe(res -> {
                                routingContext.response()
                                        .putHeader("content-type", "application/json; charset=utf-8")
                                        .setStatusCode(201)
                                        .end(res.getRows(true).get(0).encode(), "UTF-8");
                                logger.trace(methodName + " replied successfully " + res.toJson().encodePrettily());
                            },
                            th -> {
                                logger.error(methodName + " exception occured " + th.getLocalizedMessage());
                                logger.error(methodName + " exception occured " + Arrays.toString(th.getStackTrace()));
                                throwApiException(routingContext, InternalServerError500Exception.class, th.getLocalizedMessage());
                            });

                },
                throwable -> {
                    logger.error(methodName + " exception occured " + throwable.getLocalizedMessage());
                    logger.error(methodName + " exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                });
*/

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
