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

//import com.atlassian.oai.validator.SwaggerRequestResponseValidator;

import com.verapi.auth.BasicTokenParseResult;
import com.verapi.auth.BasicTokenParser;
import com.verapi.portal.common.Constants;
import com.verapi.portal.oapi.exception.AbyssApiException;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import com.verapi.portal.oapi.exception.NoDataFoundException;
import com.verapi.portal.oapi.exception.NotFound404Exception;
import com.verapi.portal.oapi.exception.UnAuthorized401Exception;
import com.verapi.portal.oapi.exception.UnProcessableEntity422Exception;
import com.verapi.portal.oapi.schema.ApiSchemaError;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.IService;
import com.verapi.portal.service.es.ElasticSearchService;
import com.verapi.portal.service.idam.SubjectService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static com.verapi.portal.common.Util.nnvl;

public abstract class AbstractApiController implements IApiController {
    private static final Logger logger = LoggerFactory.getLogger(AbstractApiController.class);

    protected static ElasticSearchService elasticSearchService = new ElasticSearchService();
    protected Vertx vertx;
    private Router abyssRouter;
    protected JDBCAuth authProvider;
    private String apiSpec;

    protected AbstractApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        this.vertx = vertx;
        this.abyssRouter = router;
        this.authProvider = authProvider;
        this.apiSpec = this.getClass().getAnnotation(AbyssApiController.class).apiSpec();
/*
        final SwaggerRequestResponseValidator validator = SwaggerRequestResponseValidator
                .createFor(this.apiSpec)
                .build();
*/
        this.init();
    }

    private void init() {
        logger.trace("initializing");

        OpenAPI3RouterFactory.createRouterFactoryFromFile(vertx, apiSpec, ar -> {
                    // The router factory instantiation could fail
                    if (ar.succeeded()) {
                        logger.trace("OpenAPI3RouterFactory created");
                        OpenAPI3RouterFactory factory = ar.result();

                        Method[] methods = this.getClass().getMethods();
                        for (Method method : methods) {
                            if (method.getAnnotation(AbyssApiOperationHandler.class) != null) {
                                logger.trace("adding OpenAPI handler for the class {} and the method {}", getClass().getName(), method.getName());

                                // Now you can use the factory to mount map endpoints to Vert.x handlers
                                factory.addHandlerByOperationId(method.getName(), routingContext -> {
                                    try {
                                        getClass().getDeclaredMethod(method.getName(), RoutingContext.class).invoke(this, routingContext);
                                    } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                                        logger.error(e.getLocalizedMessage());
                                        logger.error(Arrays.toString(e.getStackTrace()));
                                    }
                                });

                                // Add a failure handler
                                factory.addFailureHandlerByOperationId(method.getName(), this::failureHandler);
                            }
                        }

                        // Add a security handlers
                        factory.addSecurityHandler("abyssCookieAuth", this::abyssCookieAuthSecurityHandler);
                        factory.addSecurityHandler("abyssHttpBasicAuth", this::abyssHttpBasicAuthSecurityHandler);
                        factory.addSecurityHandler("abyssApiKeyAuth", this::abyssApiKeyAuthSecurityHandler);
                        factory.addSecurityHandler("abyssJWTBearerAuth", this::abyssJWTBearerAuthSecurityHandler);
                        factory.addSecurityHandler("abyssAppAccessTokenAuth", this::abyssApiKeyAuthSecurityHandler); //TODO: implement app access token auth
                        factory.addSecurityHandler("abyssAppAccessTokenCookieAuth", this::abyssApiKeyAuthSecurityHandler); //TODO: implement app access token auth

/*
                        ChainAuthHandler chain = ChainAuthHandler.create();
                        factory.getRouter().route().handler(chain);
*/

                        // Before router creation you can enable/disable various router factory behaviours
                        RouterFactoryOptions factoryOptions = new RouterFactoryOptions()
                                .setMountValidationFailureHandler(true) // Disable mounting of dedicated validation failure handler
                                .setMountResponseContentTypeHandler(true) // Mount ResponseContentTypeHandler automatically
                                .setMountNotImplementedHandler(true);
                        // Now you have to generate the router
                        Router router = factory.setOptions(factoryOptions).getRouter();

                        //Mount router into main router
                        abyssRouter.mountSubRouter(mountPoint, router);

                        logger.trace("generated router : " + router.getRoutes().toString());
                        logger.trace("Abyss router : " + abyssRouter.getRoutes().toString());

                    } else {
                        logger.error("OpenAPI3RouterFactory creation failed, cause: " + ar.cause());
                        throw new RuntimeException("OpenAPI3RouterFactory creation failed, cause: " + ar.cause());
                    }
                }
        );
    }


    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz) {
        AbyssApiException abyssApiException;
        HttpResponseStatus httpResponseStatus;
        try {
            abyssApiException = (AbyssApiException) clazz.getConstructor(String.class).newInstance("new instance");
            httpResponseStatus = abyssApiException.getHttpResponseStatus();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            abyssApiException = new InternalServerError500Exception("new instance");
            httpResponseStatus = abyssApiException.getHttpResponseStatus();
            logger.error(e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
        }

        throwApiException(routingContext, clazz, httpResponseStatus.reasonPhrase(), null, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage) {
        throwApiException(routingContext, clazz, userMessage, null, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage) {
        throwApiException(routingContext, clazz, userMessage, detailedMessage, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage, String recommendation) {
        throwApiException(routingContext, clazz, userMessage, detailedMessage, recommendation, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage, String recommendation, String moreInfo) {
        logger.trace("throwApiException for " + userMessage);
        //replace Vertx Routing Context prohibited characters
        if (userMessage != null)
            userMessage = nnvl(userMessage, userMessage.replace("\r", "").replace("\n", ""));
        if (detailedMessage != null)
            detailedMessage = nnvl(detailedMessage, detailedMessage.replace("\r", "").replace("\n", ""));


        ApiSchemaError apiSchemaError = new ApiSchemaError();
        apiSchemaError.setCode(0)
                .setUsermessage(userMessage)
                .setInternalmessage(Arrays.toString(Thread.currentThread().getStackTrace()))
                .setDetails(detailedMessage)
                .setRecommendation(recommendation)
                .setMoreinfoURLasString(moreInfo)
                .setTimestamp(ZonedDateTime.now(ZoneId.systemDefault()).toString())
                .setPath(routingContext.normalisedPath());
        AbyssApiException abyssApiException;
        try {
            abyssApiException = (AbyssApiException) clazz.getConstructor(ApiSchemaError.class).newInstance(apiSchemaError);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            abyssApiException = new InternalServerError500Exception(apiSchemaError);
            logger.error(e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
        }
        routingContext.fail(abyssApiException);
    }

    public void failureHandler(RoutingContext routingContext) {
        logger.trace("failureHandler invoked; " + routingContext.failure().getLocalizedMessage());
        logger.trace("failureHandler invoked; " + Arrays.toString(routingContext.failure().getStackTrace()));

        // This is the failure handler
        Throwable failure = routingContext.failure();
        if (failure instanceof ValidationException)
            // Handle Validation Exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code())
                    .setStatusMessage(HttpResponseStatus.UNPROCESSABLE_ENTITY.reasonPhrase() + " " + ((ValidationException) failure).type().name() + " " + failure.getLocalizedMessage())
                    .end();
        else if (failure instanceof AbyssApiException)
            //Handle Abyss Api Exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(((AbyssApiException) failure).getApiError().getCode())
                    .setStatusMessage(((AbyssApiException) failure).getApiError().getUsermessage())
                    .end(((AbyssApiException) failure).getApiError().toJson().toString(), "UTF-8");
        else
            // Handle other exception
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .setStatusMessage("Exception thrown! " + failure.getLocalizedMessage())
                    .end(new ApiSchemaError().setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .setUsermessage(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .setInternalmessage(failure.getLocalizedMessage())
                            .setDetails(Arrays.toString(failure.getStackTrace()))
                            .setRecommendation(null)
                            .setMoreinfo(null)
                            .toJson()
                            .toString()
                    );

    }

    public void abyssCookieAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.trace(methodName + " invoked");

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);

        // Handle security here
        User user = routingContext.user();
        if (user == null) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
            return;
        }
        if (user.principal().isEmpty()) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
        }

        //if authorized then set this security handler's flag and route next
        routingContext.session().put(methodName, "OK");
        routingContext.next();
    }

    public void abyssHttpBasicAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.trace(methodName + " invoked");

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);
        //secondly check if the previous security handler's flag is set, if so  then route next
        if (routingContext.session().get("abyssCookieAuthSecurityHandler").toString().equals("OK")) {
            routingContext.next();
            return;
        }

        //http basic auth trial
        String authorizationBasicToken = routingContext.request().getHeader("Authorization");
        logger.trace(authorizationBasicToken);
        BasicTokenParseResult basicTokenParseResult = BasicTokenParser.authorizationBasicTokenParser(authorizationBasicToken);
        if (basicTokenParseResult.getIsFailed()) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
        } else {
            JsonObject creds = new JsonObject()
                    .put("username", basicTokenParseResult.getUsername())
                    .put("password", basicTokenParseResult.getPassword());

            authProvider.authenticate(creds, authResult -> {
                if (authResult.succeeded()) {
                    try {
                        SubjectService subjectService = new SubjectService(routingContext.vertx());

                        Single<JsonObject> apiResponse = subjectService.initJDBCClient()
                                .flatMap(jdbcClient -> subjectService.findByName(basicTokenParseResult.getUsername()))
                                .flatMap(result -> {
                                    //result.toJson().getValue("rows")
                                    logger.trace(result.toJson().encodePrettily());
                                    return Single.just(result.getRows().get(0));
                                });

                        apiResponse.subscribe(resp -> {
                                    logger.trace("abyssHttpBasicAuthSecurityHandler() subjectService.findBySubjectName replied successfully " + resp.encodePrettily());
                                    User user = authResult.result();
                                    String userUUID = resp.getString("uuid");
                                    user.principal().put("user.uuid", userUUID);
                                    routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context?
                                    routingContext.session().put("user.uuid", userUUID);
                                    routingContext.addCookie(Cookie.cookie("abyss.principal.uuid", userUUID));
//                                            .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));
                                    logger.trace("Logged in user: " + user.principal().encodePrettily());
                                    routingContext.put("username", user.principal().getString("username"));

                                    //if authorized then set this security handler's flag and route next
                                    routingContext.session().put(methodName, "OK");
                                    routingContext.next();
                                },
                                throwable -> {
                                    logger.error("LoginController.handle() subjectService.findBySubjectName replied error : ", Arrays.toString(throwable.getStackTrace()));
                                    throwApiException(routingContext, UnAuthorized401Exception.class);
                                });
                    } catch (Exception e) {
                        logger.error("LoginController.handle() subjectService error : ", Arrays.toString(e.getStackTrace()));
                        throwApiException(routingContext, UnAuthorized401Exception.class);
                    }
                } else {
                    logger.error("invalid credentials, auth failed");
                    throwApiException(routingContext, UnAuthorized401Exception.class);
                }
            });
        }
    }

    private void abyssApiKeyAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.trace(methodName + " invoked");

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);
        //secondly check if the previous security handler's flag is set, if so  then route next
        if ((routingContext.session().get("abyssCookieAuthSecurityHandler").toString().equals("OK")) || (routingContext.session().get("abyssHttpBasicAuthSecurityHandler").toString().equals("OK"))) {
            routingContext.next();
            return;
        }

        // Handle security here
        User user = routingContext.user();
        if (user == null) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
            return;
        }
        if (user.principal().isEmpty()) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
            return;
        }

        //if authorized then set this security handler's flag and route next
        routingContext.session().put(methodName, "OK");
        routingContext.next();
    }

    private void abyssJWTBearerAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.trace(methodName + " invoked");

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);
        //secondly check if the previous security handler's flag is set, if so  then route next
        if ((routingContext.session().get("abyssCookieAuthSecurityHandler").toString().equals("OK")) || (routingContext.session().get("abyssHttpBasicAuthSecurityHandler").toString().equals("OK")) || (routingContext.session().get("abyssApiKeyAuthSecurityHandler").toString().equals("OK"))) {
            routingContext.next();
            return;
        }

        // Handle security here
        User user = routingContext.user();
        if (user == null) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
            return;
        }
        if (user.principal().isEmpty()) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
        }

        //if authorized then set this security handler's flag and route next
        routingContext.session().put(methodName, "OK");
        routingContext.next();
    }

    private void processException(RoutingContext routingContext, Throwable throwable) {
        if (throwable instanceof NoDataFoundException)
            throwApiException(routingContext, NotFound404Exception.class, throwable.getLocalizedMessage());
        else if (throwable instanceof UnProcessableEntity422Exception) {
            logger.error("response has errors: {} | {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, UnProcessableEntity422Exception.class, throwable.getLocalizedMessage());
        } else {
            logger.error("response has errors: {} | {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
        }
    }

    private void subscribeAndResponseStatusOnly(RoutingContext routingContext, Single<CompositeResult> updateResultSingle, int httpResponseStatus) {
        updateResultSingle.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(httpResponseStatus)
                            .end();
                    logger.trace("replied successfully");
                },
                throwable -> {
                    processException(routingContext, throwable);
                });
    }

    private void subscribeAndResponseDeleteStatusOnly(RoutingContext routingContext, Single<ResultSet> updateResultSingle, int httpResponseStatus) {
        updateResultSingle.subscribe(resp -> {
                    resp.getRows().forEach(jsonObject -> {
                        elasticSearchService.indexDocument(routingContext, this.getClass().getSimpleName().replace("ApiController", "").toLowerCase() + "-api", jsonObject);
                    });
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(httpResponseStatus)
                            .end();
                    logger.trace("replied successfully");
                },
                throwable -> {
                    processException(routingContext, throwable);
                });
    }

    private void subscribeAndResponseStatusOnlyList(RoutingContext routingContext, Single<List<UpdateResult>> updateResultListSingle, int httpResponseStatus) {
        updateResultListSingle.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(httpResponseStatus)
                            .end();
                    logger.trace("replied successfully");
                },
                throwable -> {
                    processException(routingContext, throwable);
                });
    }

    private void subscribeAndResponse(RoutingContext routingContext, Single<ResultSet> resultSetSingle, int httpResponseStatus) {
        subscribeAndResponse(routingContext, resultSetSingle, null, httpResponseStatus);
    }

    private void subscribeAndResponse(RoutingContext routingContext, Single<ResultSet> resultSetSingle, List<String> jsonColumns, int httpResponseStatus) {
        resultSetSingle.subscribe(resp -> {
                    JsonArray arr = new JsonArray();
                    //if (jsonColumns.isEmpty()) {
                    if (jsonColumns == null) {
                        resp.getRows().forEach(arr::add);
                    } else {
                        resp.getResults().forEach(eachRow -> {
                            JsonObject row = new JsonObject();
                            for (int i = 0; i < resp.getColumnNames().size(); i++) {
                                if (jsonColumns.contains(resp.getColumnNames().get(i))) {
                                    if (eachRow.getString(i) == null) {
                                        row.put(resp.getColumnNames().get(i), new JsonObject());
                                    } else {
                                        row.put(resp.getColumnNames().get(i), new JsonObject(eachRow.getString(i)));
                                    }
                                } else {
                                    row.put(resp.getColumnNames().get(i), eachRow.getValue(i));
                                }
                            }
                            arr.add(row);
                        });
                    }
                    arr.forEach(arrayItem -> {
                        elasticSearchService.indexDocument(routingContext,
                                this.getClass().getSimpleName().replace("ApiController", "").toLowerCase() + "-api",
                                (JsonObject) arrayItem);
                    });
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(httpResponseStatus)
                            .end(arr.encode(), "UTF-8");
                    logger.trace("replied successfully " + arr.encodePrettily());
                },
                throwable -> {
                    processException(routingContext, throwable);
                });
    }

    protected void subscribeAndResponseBulk(RoutingContext routingContext, Single<JsonArray> jsonArraySingle, int httpResponseStatus) {
        jsonArraySingle.subscribe(resp -> {
                    routingContext.response()
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .setStatusCode(httpResponseStatus)
                            .end(resp.encode(), "UTF-8");
                    logger.trace("replied successfully " + resp.encodePrettily());
                },
                throwable -> {
                    processException(routingContext, throwable);
                });
    }

    private void subscribeAndResponseBulkList(RoutingContext routingContext, Single<List<JsonObject>> jsonListSingle, int httpResponseStatus) {
        subscribeAndResponseBulkList(routingContext, jsonListSingle, null, httpResponseStatus);
    }

    private void subscribeAndResponseBulkList(RoutingContext routingContext, Single<List<JsonObject>> jsonListSingle, List<String> jsonColumns, int httpResponseStatus) {
        logger.trace("---subscribeAndResponseBulkList invoked");
        JsonArray arr = new JsonArray();
        jsonListSingle
                .doOnError(throwable -> {
                    logger.error("exception occured " + throwable.getLocalizedMessage());
                    logger.error("exception occured " + Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                })
                .subscribe(resp -> {
                            //if (jsonColumns.isEmpty()) {
                            if (jsonColumns == null) {
                                resp.forEach(arr::add);
                            } else {
                                resp.forEach(eachJO -> {
                                    JsonObject newJsonObject = new JsonObject();
                                    eachJO.forEach(eachJOEntry -> {
                                        if (eachJOEntry.getKey().equals("response")) {
                                            JsonObject newResponseJO = new JsonObject();
                                            ((JsonObject) eachJOEntry.getValue()).forEach(responseJOEntry -> {
                                                if (jsonColumns.contains(responseJOEntry.getKey())) {
                                                    if (responseJOEntry.getValue() == null)
                                                        newResponseJO.put(responseJOEntry.getKey(), new JsonObject());
                                                    else
                                                        newResponseJO.put(responseJOEntry.getKey(), new JsonObject(responseJOEntry.getValue().toString()));
                                                } else {
                                                    newResponseJO.put(responseJOEntry.getKey(), responseJOEntry.getValue());
                                                }
                                            });
                                            newJsonObject.put(eachJOEntry.getKey(), newResponseJO);
                                        } else {
                                            newJsonObject.put(eachJOEntry.getKey(), eachJOEntry.getValue());
                                        }
                                    });
                                    arr.add(newJsonObject);
                                });
                            }
                            arr.forEach(arrayItem -> {
                                elasticSearchService.indexDocument(routingContext,
                                        this.getClass().getSimpleName().replace("ApiController", "").toLowerCase() + "-api",
                                        ((JsonObject) arrayItem).getJsonObject("response"));
                            });
                            routingContext.response()
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .setStatusCode(httpResponseStatus)
                                    .end(arr.encode(), "UTF-8");
                            logger.trace("replied successfully " + arr.encodePrettily());
                        },
                        throwable -> {
                            processException(routingContext, throwable);
                        });
    }

    protected void subscribeAndResponse(RoutingContext routingContext, JsonArray response, int httpResponseStatus) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(httpResponseStatus)
                .end(response.encode());
        logger.trace("replied successfully");
    }

    protected void subscribeAndResponseJsonObject(RoutingContext routingContext, Single<JsonObject> response, int httpResponseStatus) {
        subscribeAndResponseJsonObject(routingContext, response, httpResponseStatus, false);
    }

    protected void subscribeAndResponseJsonObject(RoutingContext routingContext, Single<JsonObject> response, int httpResponseStatus, boolean onlyStatus) {
        response.subscribe(jsonObject -> {
            elasticSearchService.indexDocument(routingContext,
                    this.getClass().getSimpleName().replace("ApiController", "").toLowerCase() + "-api",
                    jsonObject);
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .setStatusCode(httpResponseStatus)
                    .end((onlyStatus) ? null : jsonObject.encode());
            logger.trace("replied successfully");
        }, throwable -> {
            processException(routingContext, throwable);
        });
    }


    <T extends IService> void getEntities(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        getEntities(routingContext, clazz, null, new ApiFilterQuery());
    }

    <T extends IService> void getEntities(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        getEntities(routingContext, clazz, jsonColumns, new ApiFilterQuery());
    }

    <T extends IService> void getEntities(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns, ApiFilterQuery apiFilterQuery) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        String filterByNameParameter = null;
        String filterLikeNameParameter = null;
        if (!routingContext.queryParams().isEmpty()) {
            if (!routingContext.queryParam(Constants.RESTAPI_FILTERING_BY_NAME).isEmpty()) {
                filterByNameParameter = routingContext.queryParam(Constants.RESTAPI_FILTERING_BY_NAME).get(0);
            }
            if (!routingContext.queryParam(Constants.RESTAPI_FILTERING_LIKE_NAME).isEmpty()) {
                if (filterByNameParameter != null && !filterByNameParameter.isEmpty())
                    throwApiException(routingContext, UnProcessableEntity422Exception.class, "Both Filter By Name AND Filter Like Name CANNOT BE used at the same time, choose only one");
                filterLikeNameParameter = routingContext.queryParam(Constants.RESTAPI_FILTERING_LIKE_NAME).get(0);
            }
        }

        String finalFilterByNameParameter = filterByNameParameter;
        String finalFilterLikeNameParameter = filterLikeNameParameter;
        Single<ResultSet> findAllResult = service.initJDBCClient()
                .flatMap(jdbcClient -> {
                    if (apiFilterQuery == null || apiFilterQuery.getFilterQuery().isEmpty()) {
                        if ((finalFilterByNameParameter == null) && (finalFilterLikeNameParameter == null)) {
                            return service.findAll();
                        } else {
                            if (finalFilterLikeNameParameter == null)
                                return service.findByName(finalFilterByNameParameter);
                            else
                                return service.findLikeName(finalFilterLikeNameParameter);
                        }
                    } else {
                        if ((finalFilterByNameParameter == null) && (finalFilterLikeNameParameter == null)) {
                            return service.findAll(apiFilterQuery);
                        } else {
                            if (finalFilterByNameParameter != null) {
                                apiFilterQuery.addFilterQuery(service.getAPIFilter().getApiFilterByNameQuery()).addFilterQueryParams(new JsonArray().add(finalFilterByNameParameter));
                                return service.findAll(apiFilterQuery);
                            } else {
                                apiFilterQuery.addFilterQuery(service.getAPIFilter().getApiFilterLikeNameQuery()).addFilterQueryParams(new JsonArray().add(finalFilterLikeNameParameter + "%"));
                                return service.findAll(apiFilterQuery);
                            }
                        }
                    }
                })
/*
                .flatMap(jdbcClient -> ((finalFilterByNameParameter == null) && (finalFilterLikeNameParameter == null)) ?
                        ((apiFilterQuery.getFilterQuery().isEmpty()) ? service.findAll() : service.findAll(apiFilterQuery)) :
                        (finalFilterLikeNameParameter == null) ?
                                service.findByName(finalFilterByNameParameter) :
                                service.findLikeName(finalFilterLikeNameParameter))
*/
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException("no_data_found"));
                    } else
                        return Single.just(resultSet);
                });
        subscribeAndResponse(routingContext, findAllResult, jsonColumns, HttpResponseStatus.OK.code());
    }

    <T extends IService> void getEntity(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        getEntity(routingContext, clazz, null, null);
    }

    <T extends IService> void getEntity(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        getEntity(routingContext, clazz, jsonColumns, null);
    }

    <T extends IService> void getEntity(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns, ApiFilterQuery apiFilterQuery) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> findAllResult = service.initJDBCClient()
                .flatMap(jdbcClient -> (apiFilterQuery == null) ? service.findById(UUID.fromString(routingContext.pathParam("uuid"))) : service.findAll(apiFilterQuery));
        subscribeAndResponse(routingContext, findAllResult, jsonColumns, HttpResponseStatus.OK.code());
    }

    <T extends IService> void addEntities(RoutingContext routingContext, Class<T> clazz, JsonArray requestBody) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        addEntities(routingContext, clazz, requestBody, null);
    }

    <T extends IService> void addEntities(RoutingContext routingContext, Class<T> clazz, JsonArray requestBody, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        logger.trace("---addEntities invoked");
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<List<JsonObject>> insertAllResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.insertAll(requestBody));
        subscribeAndResponseBulkList(routingContext, insertAllResult, jsonColumns, HttpResponseStatus.MULTI_STATUS.code());
    }

    <T extends IService> void updateEntities(RoutingContext routingContext, Class<T> clazz, JsonObject requestBody) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        updateEntities(routingContext, clazz, requestBody, null, null);
    }

    <T extends IService> void updateEntities(RoutingContext routingContext, Class<T> clazz, JsonObject requestBody, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        updateEntities(routingContext, clazz, requestBody, jsonColumns, null);
    }

    <T extends IService> void updateEntities(RoutingContext routingContext, Class<T> clazz, JsonObject requestBody, List<String> jsonColumns, ApiFilterQuery apiFilterQuery) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<List<JsonObject>> updateAllResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.updateAll(requestBody)); //TODO: an overloaded version of updateAll() using ApiFilterQuery param should be developed, now suppressing apiFilterQuery param
        subscribeAndResponseBulkList(routingContext, updateAllResult, jsonColumns, HttpResponseStatus.MULTI_STATUS.code());
    }

    <T extends IService> void updateEntity(RoutingContext routingContext, Class<T> clazz, JsonObject requestBody) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        updateEntity(routingContext, clazz, requestBody, null, null);
    }

    <T extends IService> void updateEntity(RoutingContext routingContext, Class<T> clazz, JsonObject requestBody, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        updateEntity(routingContext, clazz, requestBody, jsonColumns, null);
    }

    <T extends IService> void updateEntity(RoutingContext routingContext, Class<T> clazz, JsonObject requestBody, List<String> jsonColumns, ApiFilterQuery apiFilterQuery) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> updateAllResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.update(UUID.fromString(routingContext.pathParam("uuid")), requestBody)) //TODO:  an overloaded version of update() using ApiFilterQuery param should be developed, now suppressing apiFilterQuery param
                .flatMap(resultSet -> service.findById(UUID.fromString(routingContext.pathParam("uuid"))))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException("no_data_found"));
                    } else
                        return Single.just(resultSet);
                });
        subscribeAndResponse(routingContext, updateAllResult, jsonColumns, HttpResponseStatus.OK.code());
    }

    <T extends IService> void deleteEntities(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        deleteEntities(routingContext, clazz, new ApiFilterQuery());
    }

    <T extends IService> void deleteEntities(RoutingContext routingContext, Class<T> clazz, ApiFilterQuery apiFilterQuery) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<CompositeResult> deleteAllResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.deleteAll(apiFilterQuery));
        subscribeAndResponseStatusOnly(routingContext, deleteAllResult, HttpResponseStatus.NO_CONTENT.code());
    }

/*
    <T extends IService> void deleteEntity(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<CompositeResult> deleteResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.delete(UUID.fromString(routingContext.pathParam("uuid"))));
        subscribeAndResponseStatusOnly(routingContext, deleteResult, HttpResponseStatus.NO_CONTENT.code());
    }
*/

    <T extends IService> void deleteEntity(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> deleteResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.delete(UUID.fromString(routingContext.pathParam("uuid"))))
                .flatMap(resultSet -> service.findById(UUID.fromString(routingContext.pathParam("uuid"))))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException("no_data_found"));
                    } else
                        return Single.just(resultSet);
                });
        subscribeAndResponseDeleteStatusOnly(routingContext, deleteResult, HttpResponseStatus.NO_CONTENT.code());
    }

    <T extends IService> void execServiceMethod(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns, String method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> funcResult = service.initJDBCClient()
                .flatMap(jdbcClient -> (Single<ResultSet>) service.getClass().getDeclaredMethod(method, RoutingContext.class, JDBCAuth.class).invoke(service, routingContext, authProvider))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException("no_data_found"));
                    } else
                        return Single.just(resultSet);
                });
        subscribeAndResponse(routingContext, funcResult, jsonColumns, HttpResponseStatus.OK.code());
    }

/*
    <T extends IService> void execServiceMethod(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns, Function<RoutingContext, Single<ResultSet>> func, String method) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> funcResult = service.initJDBCClient()
                .flatMap(jdbcClient -> func.apply(routingContext));
        service.getClass().getDeclaredMethod(method, RoutingContext.class).invoke(routingContext);
        subscribeAndResponse(routingContext, funcResult, jsonColumns, HttpResponseStatus.OK.code());
    }
*/

//    public static final String SQL_CONDITION_NAME_IS = null;
//
//    public static final String SQL_CONDITION_NAME_LIKE = null;

}
