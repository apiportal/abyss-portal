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

import com.google.json.JsonSanitizer;
import com.verapi.abyss.common.Constants;
import com.verapi.abyss.common.OpenAPIUtil;
import com.verapi.abyss.exception.AbyssApiException;
import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.abyss.exception.BadRequest400Exception;
import com.verapi.abyss.exception.Forbidden403Exception;
import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.abyss.exception.NoDataFoundException;
import com.verapi.abyss.exception.NotFound404Exception;
import com.verapi.abyss.exception.UnAuthorized401Exception;
import com.verapi.abyss.exception.UnProcessableEntity422Exception;
import com.verapi.auth.BasicTokenParseResult;
import com.verapi.auth.BasicTokenParser;
import com.verapi.portal.service.ApiFilterQuery;
import com.verapi.portal.service.IService;
import com.verapi.portal.service.es.ElasticSearchService;
import com.verapi.portal.service.idam.ResourceService;
import com.verapi.portal.service.idam.SubjectPermissionService;
import com.verapi.portal.service.idam.SubjectService;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.swagger.parser.util.ClasspathHelper;
import io.swagger.v3.oas.models.Operation;
import io.vertx.core.AsyncResult;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.api.contract.RouterFactoryOptions;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.verapi.portal.common.Util.nnvl;

public abstract class AbstractApiController implements IApiController {
    protected static final String PARSED_PARAMETERS = "parsedParameters";
    protected static final String EXCEPTION_LOG_FORMAT = "{}\n{}";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractApiController.class);
    private static final String OPEN_API_OPERATION = "openApiOperation";
    private static final String ABYSS_COOKIE_AUTH_SECURITY_HANDLER = "abyssCookieAuthSecurityHandler";
    private static final String ABYSS_HTTP_BASIC_AUTH_SECURITY_HANDLER = "abyssHttpBasicAuthSecurityHandler";
    private static final String ABYSS_API_KEY_AUTH_SECURITY_HANDLER = "abyssApiKeyAuthSecurityHandler";
    private static final String RESPONSE_HAS_ERRORS = "response has errors: {} | {}";
    private static final String ORIGIN = "Origin";
    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String METHOD_INVOKED = "{} invoked";
    private static final String NO_DATA_FOUND = "no_data_found";
    private static final String API_CONTROLLER = "ApiController";
    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
    private static ElasticSearchService elasticSearchService = new ElasticSearchService();
    protected Vertx vertx;
    protected JDBCAuth authProvider;
    private String mountPoint = Constants.ABYSS_ROOT + "/oapi";
    private Router abyssRouter;
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

    private static void returnAsImage(RoutingContext routingContext, ResultSet resultSet, String imageColumnName) {
        String sourceData = resultSet.getRows().get(0).getString(imageColumnName);

        // tokenize the data
        String[] parts = sourceData.split(",");
        String imageString = parts[1];

        String contentType = parts[0].substring(parts[0].indexOf(':') + 1, parts[0].indexOf(';'));
        String imageFormat = "." + contentType.substring(contentType.indexOf('/') + 1);
        LOGGER.trace("imageColumnName: {}. contentType: {}. imageFormat: {}", imageColumnName, contentType, imageFormat);

        Base64.Decoder base64Decoder = Base64.getDecoder();
        byte[] imageByte = base64Decoder.decode(imageString);

        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, contentType)
                .putHeader(HttpHeaders.CACHE_CONTROL, HttpHeaderValues.MAX_AGE + "=864000")
                .putHeader(HttpHeaders.CONTENT_DISPOSITION,
                        //HttpHeaderValues.ATTACHMENT + "; " +
                        //"inline; " +
                        HttpHeaderValues.FILENAME + "=\"" + routingContext.pathParam("uuid") + imageFormat + "\"")
                .setChunked(true)
                .setStatusCode(HttpResponseStatus.OK.code())
                .write(Buffer.buffer(imageByte))
                .end();
    }

    private void failureHandler(RoutingContext routingContext) {
        Throwable failure;
        if (routingContext.failure() == null) {
            failure = new InternalServerError500Exception("Routing Context Failure is null!");
        } else {
            failure = routingContext.failure();
        }

        LOGGER.trace("failureHandler invoked; error: {} \n stack trace: {} "
                , failure.getLocalizedMessage()
                , failure.getStackTrace());

        if (routingContext.response().ended()) {
            //routingContext.next();
            return;
        }
        // This is the failure handler
        if (failure instanceof ValidationException) {
            // Handle Validation Exception
            routingContext.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                    .setStatusCode(HttpResponseStatus.UNPROCESSABLE_ENTITY.code())
                    .setStatusMessage(HttpResponseStatus.UNPROCESSABLE_ENTITY.reasonPhrase() + " "
                            + ((ValidationException) failure).type().name() + " "
                            + failure.getLocalizedMessage())
                    .end();
        } else if (failure instanceof AbyssApiException) {
            //Handle Abyss Api Exception
            routingContext.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                    .setStatusCode(((AbyssApiException) failure).getApiError().getCode())
                    .setStatusMessage(((AbyssApiException) failure).getApiError().getUsermessage())
                    .end(JsonSanitizer.sanitize(((AbyssApiException) failure).getApiError().toJson().toString()), StandardCharsets.UTF_8.toString());
        } else {
            // Handle other exception
            routingContext.response()
                    .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                    .setStatusCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .setStatusMessage("Exception thrown! " + failure.getLocalizedMessage())
                    .end(JsonSanitizer.sanitize(new ApiSchemaError().setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .setUsermessage(HttpResponseStatus.INTERNAL_SERVER_ERROR.reasonPhrase())
                            .setInternalmessage(failure.getLocalizedMessage())
                            .setDetails(Arrays.toString(failure.getStackTrace()))
                            .setRecommendation(null)
                            .setMoreinfo(null)
                            .toJson()
                            .toString()), StandardCharsets.UTF_8.toString()
                    );
        }

    }

    private void init() {
        LOGGER.trace("initializing AbstractApiController for api spec: {}", apiSpec);
        OpenAPIUtil.createOpenAPI3RouterFactory(vertx, ClasspathHelper.loadFileFromClasspath(apiSpec), (AsyncResult<OpenAPI3RouterFactory> ar) -> {
                    // The router factory instantiation could fail
                    if (ar.succeeded()) {
                        LOGGER.trace("OpenAPI3RouterFactory created");
                        OpenAPI3RouterFactory factory = ar.result();

                        Method[] methods = this.getClass().getDeclaredMethods();
                        String classCanonicalName = this.getClass().getCanonicalName();

                        for (Method method : methods) {
                            if (method.getAnnotation(AbyssApiOperationHandler.class) != null) {
                                LOGGER.trace("adding OpenAPI handler for the class {} and the method {}", getClass().getName(), method.getName());

                                final String methodName = method.getName();

                                // Add a failure handler
                                factory.addFailureHandlerByOperationId(methodName, this::failureHandler);

                                //Authorization Handler
                                factory.addHandlerByOperationId(methodName, this::abyssPathAuthorizationHandler);

                                // Now you can use the factory to mount map endpoints to Vert.x handlers
                                factory.addHandlerByOperationId(methodName, (RoutingContext routingContext) -> {
                                    try {
                                        getClass().getDeclaredMethod(methodName, RoutingContext.class).invoke(this, routingContext);
                                    } catch (NoSuchMethodException | IllegalAccessException e) {
                                        LOGGER.error("{}.{} invocation error: {} \n stack trace: {}"
                                                , getClass().getName(), method.getName(), e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
                                        throwApiException(routingContext, InternalServerError500Exception.class, e);
                                    } catch (InvocationTargetException eITE) {
                                        LOGGER.error("{}.{} invocation error: {} \n stack trace:\n{}\ntarget error msg: {}\n target stack trace:\n{}\n"
                                                , getClass().getName(), method.getName(),
                                                eITE.getLocalizedMessage(), Arrays.toString(eITE.getStackTrace()),
                                                eITE.getTargetException().getLocalizedMessage(), Arrays.toString(eITE.getTargetException().getStackTrace())
                                        );
                                        throwApiException(routingContext, InternalServerError500Exception.class, eITE);
                                    }
                                });

                                //TODO: Try to Insert to DB all operationIDs of all API Proxies to Resource -> INSERT ... ON CONFLICT DO NOTHING/UPDATE
                                // |   method.getName() IS EQUAL TO openAPI Operation ID
                                ResourceService resourceService = new ResourceService(vertx);

                                resourceService.initJDBCClient()
                                        .flatMap((JDBCClient jdbcClient1) ->
                                                resourceService.insertAllWithConflict(new JsonArray().add(new JsonObject()
                                                        .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                                                        .put("crudsubjectid", Constants.SYSTEM_USER_UUID)
                                                        .put("resourcetypeid", Constants.RESOURCE_TYPE_OPENAPI_OPERATION)
                                                        .put("resourcename", methodName)
                                                        .put("description", apiSpec + "-" + classCanonicalName + "-" + methodName)
                                                        .put("resourcerefid", Constants.RESOURCE_TYPE_OPENAPI_OPERATION)
                                                        .put("isactive", Boolean.TRUE)
                                                ))
                                        )
                                        .subscribe((List<JsonObject> jsonObjects) -> {
                                            if (jsonObjects.isEmpty() || jsonObjects.get(0).size() == 0) {
                                                LOGGER.trace("Resource Record exists for operation: {}", methodName);
                                            } else {
                                                LOGGER.trace("Resource Record {}\n for operation: {} inserted"
                                                        , jsonObjects.get(0).encodePrettily(), methodName);
                                            }
                                        }, throwable -> LOGGER.error("Resource Recording [{}] error {} | {}: "
                                                , methodName, throwable.getLocalizedMessage(), throwable.getStackTrace()));
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
                                .setMountNotImplementedHandler(true)
                                .setOperationModelKey(OPEN_API_OPERATION);
                        // Now you have to generate the router
                        Router router = factory.setOptions(factoryOptions).getRouter();

//                        router.route().handler(this::logHandler);

                        //Mount router into main router
                        abyssRouter.mountSubRouter(mountPoint, router);

                        LOGGER.trace("generated router : {}", router.getRoutes());
                        LOGGER.trace("Abyss router : {}", abyssRouter.getRoutes());

                    } else {
                        LOGGER.error("OpenAPI3RouterFactory creation failed, cause: {}", ar.cause().getLocalizedMessage());
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
            LOGGER.error("throwApiException {}\n{}", e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
        }

        throwApiException(routingContext, clazz, httpResponseStatus.reasonPhrase(), null, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage) {
        throwApiException(routingContext, clazz, userMessage, null, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, Exception e) {
        LOGGER.trace("throwApiException(RoutingContext routingContext, Class<T> clazz, Exception e) is starting");
        throwApiException(routingContext, clazz, e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()), null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage) {
        throwApiException(routingContext, clazz, userMessage, detailedMessage, null, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage, String recommendation) {
        throwApiException(routingContext, clazz, userMessage, detailedMessage, recommendation, null);
    }

    public <T> void throwApiException(RoutingContext routingContext, Class<T> clazz, String userMessage, String detailedMessage, String recommendation, String moreInfo) {
        LOGGER.trace("throwApiException for {}", userMessage);
        //replace Vertx Routing Context prohibited characters
        if (userMessage != null) {
            userMessage = nnvl(userMessage, userMessage.replace("\r", "").replace("\n", ""));
        }
        if (detailedMessage != null) {
            detailedMessage = nnvl(detailedMessage, detailedMessage.replace("\r", "").replace("\n", ""));
        }


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
            LOGGER.error("Error occurred during AbyssApiException instance constructing error:{} \n stack trace:{}"
                    , e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
        }
        routingContext.fail(abyssApiException);
    }

    private void abyssCookieAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        LOGGER.trace(METHOD_INVOKED, methodName);

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

    private void abyssHttpBasicAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        LOGGER.trace(METHOD_INVOKED, methodName);

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);
        //secondly check if the previous security handler's flag is set, if so  then route next
        if (routingContext.session().get(ABYSS_COOKIE_AUTH_SECURITY_HANDLER) != null
                && "OK".equals(routingContext.session().get(ABYSS_COOKIE_AUTH_SECURITY_HANDLER).toString())) {
            routingContext.next();
            return;
        }

        //http basic auth trial
        String authorizationBasicToken = routingContext.request().getHeader("Authorization");
        LOGGER.trace(authorizationBasicToken);
        BasicTokenParseResult basicTokenParseResult = BasicTokenParser.authorizationBasicTokenParser(authorizationBasicToken);
        if (basicTokenParseResult.getIsFailed()) {
            throwApiException(routingContext, UnAuthorized401Exception.class);
        } else {
            JsonObject creds = new JsonObject()
                    .put("username", basicTokenParseResult.getUsername())
                    .put("password", basicTokenParseResult.getPassword());

            authProvider.authenticate(creds, (AsyncResult<User> authResult) -> {
                if (authResult.succeeded()) {
                    try {
                        SubjectService subjectService = new SubjectService(routingContext.vertx());

                        Single<JsonObject> apiResponse = subjectService.initJDBCClient()
                                .flatMap(jdbcClient -> subjectService.findByName(basicTokenParseResult.getUsername()))
                                .flatMap((ResultSet result) -> {
                                    //result.toJson().getValue("rows")
                                    LOGGER.trace(result.toJson().encodePrettily());
                                    return Single.just(result.getRows().get(0));
                                });

                        apiResponse.subscribe((JsonObject resp) -> {
                                    LOGGER.trace("abyssHttpBasicAuthSecurityHandler() subjectService.findBySubjectName replied successfully {}", resp.encodePrettily());
                                    User user = authResult.result();
                                    String userUUID = resp.getString("uuid");
                                    user.principal().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID);
                                    routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context?
                                    routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID);
                                    routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME, userUUID)); //TODO: Remove for OWASP
//                                            .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));
                                    LOGGER.trace("Logged in user: {}", user.principal().encodePrettily());
                                    routingContext.put("username", user.principal().getString("username"));

                                    //if authorized then set this security handler's flag and route next
                                    routingContext.session().put(methodName, "OK");
                                    routingContext.next();
                                },
                                (Throwable throwable) -> {
                                    LOGGER.error("LoginPortalController.handle() subjectService.findBySubjectName replied error : {}\n{}"
                                            , throwable.getLocalizedMessage(), Arrays.toString(throwable.getStackTrace()));
                                    throwApiException(routingContext, UnAuthorized401Exception.class);
                                });
                    } catch (Exception e) {
                        LOGGER.error("LoginPortalController.handle() subjectService error : {}\n{}", e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
                        throwApiException(routingContext, UnAuthorized401Exception.class);
                    }
                } else {
                    LOGGER.error("invalid credentials, auth failed");
                    throwApiException(routingContext, UnAuthorized401Exception.class);
                }
            });
        }
    }

    private void abyssApiKeyAuthSecurityHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        LOGGER.trace(METHOD_INVOKED, methodName);

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);
        //secondly check if the previous security handler's flag is set, if so  then route next
        if (routingContext.session().get(ABYSS_COOKIE_AUTH_SECURITY_HANDLER) != null
                && "OK".equals(routingContext.session().get(ABYSS_COOKIE_AUTH_SECURITY_HANDLER).toString())) {
            routingContext.next();
            return;
        }
        if (routingContext.session().get(ABYSS_HTTP_BASIC_AUTH_SECURITY_HANDLER) != null
                && "OK".equals(routingContext.session().get(ABYSS_HTTP_BASIC_AUTH_SECURITY_HANDLER).toString())) {
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
        LOGGER.trace(METHOD_INVOKED, methodName);

        //firstly clear this security handler's flag
        routingContext.session().remove(methodName);
        //secondly check if the previous security handler's flag is set, if so  then route next
        if (routingContext.session().get(ABYSS_COOKIE_AUTH_SECURITY_HANDLER) != null
                && "OK".equals(routingContext.session().get(ABYSS_COOKIE_AUTH_SECURITY_HANDLER).toString())) {
            routingContext.next();
            return;
        }
        if (routingContext.session().get(ABYSS_HTTP_BASIC_AUTH_SECURITY_HANDLER) != null
                && "OK".equals(routingContext.session().get(ABYSS_HTTP_BASIC_AUTH_SECURITY_HANDLER).toString())) {
            routingContext.next();
            return;
        }

        if (routingContext.session().get(ABYSS_API_KEY_AUTH_SECURITY_HANDLER) != null
                && "OK".equals(routingContext.session().get(ABYSS_API_KEY_AUTH_SECURITY_HANDLER).toString())) {
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

    private void abyssPathAuthorizationHandler(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        LOGGER.trace(METHOD_INVOKED, methodName);

        String organizationUuidTemp = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);
        if (organizationUuidTemp == null || organizationUuidTemp.isEmpty()) {
            organizationUuidTemp = Constants.DEFAULT_ORGANIZATION_UUID;
        }
        String organizationUuid = organizationUuidTemp;

        String userUuidTemp = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);
        if (userUuidTemp == null || userUuidTemp.isEmpty()) {
            userUuidTemp = Constants.PLATFORM_GUEST_USER_UUID;
        }
        String userUuid = userUuidTemp;

        String operationId = ((Operation) routingContext.data().get(OPEN_API_OPERATION)).getOperationId();
        routingContext.put(Constants.AUTH_ABYSS_PORTAL_ROUTING_CONTEXT_OPERATION_ID, operationId);

        //Get and check resource ID is a valid uuid
        String resourceIdTemp = routingContext.pathParam("uuid");
        if (resourceIdTemp != null) {
            String[] components = resourceIdTemp.split("-");
            if (components.length != 5) {
                //remove the following useless assignment
                resourceIdTemp = null;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("abyssPathAuthorizationHandler invoked,\n" +
                            "[{}]\n[{}]\n[{}]\n[{}]\n\n" +
                            "[{}]\n[{}]\n[{}]\n[{}]\n[{}]\n\n" +
                            "[{}]\n[{}]\n[{}]\n[{}]\n\n" +
                            "[{}]\n[{}]\n[{}]\n",
                    routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME),  //user.uuid
                    routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME),
                    routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME),    //organization.uuid
                    routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME),

                    //Path
                    routingContext.normalisedPath(),
                    routingContext.currentRoute().getPath(),
                    routingContext.request().path(),
                    routingContext.request().absoluteURI(),
                    routingContext.request().method().name(),   //Action

                    routingContext.request().host(),
                    routingContext.request().remoteAddress().host(),
                    routingContext.request().remoteAddress().port(),
                    routingContext.request().remoteAddress().path(),

                    routingContext.data().keySet(),
                    routingContext.session().data().keySet(),
                    ((Operation) routingContext.data().get(OPEN_API_OPERATION)).getOperationId());  //Operation
        }


        //TODO: TEST

        try {
            if ("login".equals(operationId)) {
                LOGGER.trace("abyssPathAuthorizationHandler() operationId: {}", operationId);

                //if authorized then set this security handler's flag and route next
                routingContext.next();
            } else {
                SubjectPermissionService subjectPermissionService = new SubjectPermissionService(routingContext.vertx());

                ApiFilterQuery apiFilterQuery = new ApiFilterQuery()
                        .setFilterQuery(SubjectPermissionService.SQL_CHECK_ROLE_BASED_PERMISSION_OF_SUBJECT_IN_ORGANIZATION)
                        .setFilterQueryParams(new JsonArray().add(organizationUuid).add(userUuid).add(operationId));

                Single<JsonObject> permissionResponse = subjectPermissionService.initJDBCClient()
                        .flatMap(jdbcClient -> subjectPermissionService.findAll(apiFilterQuery))
                        .flatMap((ResultSet result) -> {
                            if (result.getNumRows() > 0) {
                                LOGGER.trace("# of permissions: [{}]\n[{}]\n", result.getNumRows(), result.toJson().encodePrettily());
                                return Single.just(result.getRows().get(0));
                            } else {
                                return Single.error(new Forbidden403Exception("abyssPathAuthorizationHandler failed - " +
                                        "no permission for org:[" + organizationUuid + "] user:[" + userUuid + "] operation:[" + operationId + "]"));
                            }
                        })
//                        .flatMap(entries -> {
//                            if (resourceId != null && !resourceId.isEmpty()) {
//
//                                ApiFilterQuery apiFilterQueryForResourceAccess = new ApiFilterQuery()
//                                        .setFilterQuery(SubjectPermissionService.SQL_CHECK_RESOURCE_ACCESS_CONTROL)
//                                        .setFilterQueryParams(new JsonArray().add(organizationUuid).add(organizationUuid).add(userUuid).add(resourceId).add(operationId).add(operationId));
//
//                                return subjectPermissionService.findAll(apiFilterQueryForResourceAccess);
//
//                            } else {
//                                return Single.just(entries);
//                            }
//                        })
//                        .flatMap(result -> {
//                            if (result instanceof ResultSet) {
//                                ResultSet resultSet = (ResultSet)result;
//                                if (resultSet.getNumRows() > 0) {
//                                    LOGGER.trace("# of access permissions: [{}]\n[{}]\n", resultSet.getNumRows(), resultSet.toJson().encodePrettily());
//                                    return Single.just(resultSet.getRows().get(0));
//                                } else {
//                                    return Single.error(new Forbidden403Exception("abyssPathAuthorizationHandler failed - no permission for the resource:[" + resourceId + "] org:[" + organizationUuid + "] user:[" + userUuid + "] operation:[" + operationId + "]"));
//                                }
//                            } else {
//                                return Single.just((JsonObject)result);
//                            }
//                        })
                        ;

                permissionResponse.subscribe((JsonObject resp) -> {
                            LOGGER.trace("abyssPathAuthorizationHandler() subjectPermissionService.findAll replied successfully {}", resp.encodePrettily());

                            //if authorized then set this security handler's flag and route next
                            routingContext.next();
                        },
                        (Throwable throwable) -> {
                            LOGGER.error("abyssPathAuthorizationHandler() subjectPermissionService.findAll replied error : {}\n{}"
                                    , throwable.getLocalizedMessage(), Arrays.toString(throwable.getStackTrace()));
                            throwApiException(routingContext, Forbidden403Exception.class);
                        });
            }
        } catch (Exception e) {
            LOGGER.error("abyssPathAuthorizationHandler() subjectPermissionService.findAll error : {}\n{}", e.getLocalizedMessage(), Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, Forbidden403Exception.class);
        }

        //TODO: ???? routingContext.fail(new Forbidden403Exception("abyssPathAuthorizationHandler failed"));
    }

    private void processException(RoutingContext routingContext, Throwable throwable) {
        if (throwable instanceof CompositeException) {
            for (Throwable t : ((CompositeException) throwable).getExceptions()) {
                if (t instanceof UnAuthorized401Exception) {
                    LOGGER.error(RESPONSE_HAS_ERRORS, t.getCause().getLocalizedMessage(), throwable.getStackTrace());
                    throwApiException(routingContext, UnAuthorized401Exception.class, t.getCause().getLocalizedMessage());
                }
                if (t instanceof BadRequest400Exception) {
                    LOGGER.error(RESPONSE_HAS_ERRORS, t.getCause().getLocalizedMessage(), throwable.getStackTrace());
                    throwApiException(routingContext, BadRequest400Exception.class, t.getCause().getLocalizedMessage());
                }
                if (t instanceof Forbidden403Exception) {
                    LOGGER.error(RESPONSE_HAS_ERRORS, t.getCause().getLocalizedMessage(), throwable.getStackTrace());
                    throwApiException(routingContext, Forbidden403Exception.class, t.getCause().getLocalizedMessage());
                }
            }
        }

        if (throwable instanceof NoDataFoundException) {
            throwApiException(routingContext, NotFound404Exception.class, throwable.getLocalizedMessage());
        } else if (throwable instanceof UnAuthorized401Exception) {
            LOGGER.error(RESPONSE_HAS_ERRORS, throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, UnAuthorized401Exception.class, throwable.getLocalizedMessage());
        } else if (throwable instanceof BadRequest400Exception) {
            LOGGER.error(RESPONSE_HAS_ERRORS, throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, BadRequest400Exception.class, throwable.getLocalizedMessage());
        } else if (throwable instanceof Forbidden403Exception) {
            LOGGER.error(RESPONSE_HAS_ERRORS, throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, Forbidden403Exception.class, throwable.getLocalizedMessage());
        } else if (throwable instanceof UnProcessableEntity422Exception) {
            LOGGER.error(RESPONSE_HAS_ERRORS, throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, UnProcessableEntity422Exception.class, throwable.getLocalizedMessage());
        } else {
            LOGGER.error(RESPONSE_HAS_ERRORS, throwable.getLocalizedMessage(), throwable.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
        }
    }

    private void subscribeAndResponseStatusOnly(RoutingContext routingContext, Single<CompositeResult> updateResultSingle, int httpResponseStatus) {
        updateResultSingle.subscribe((CompositeResult resp) ->
                        routingContext.response()
                                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                                .setStatusCode(httpResponseStatus)
                                .end()
                ,
                (Throwable throwable) -> processException(routingContext, throwable));
    }

    private void subscribeAndResponseDeleteStatusOnly(RoutingContext routingContext, Single<ResultSet> updateResultSingle, int httpResponseStatus) {
        updateResultSingle.subscribe((ResultSet resp) -> {
                    resp.getRows().forEach((JsonObject jsonObject) -> elasticSearchService
                            .indexDocument(routingContext, this.getClass().getSimpleName().replace(API_CONTROLLER, "")
                                    .toLowerCase(Locale.ENGLISH) + "-api", jsonObject));
                    routingContext.response()
                            .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                            .setStatusCode(httpResponseStatus)
                            .end();
                },
                (Throwable throwable) -> processException(routingContext, throwable));
    }

    private void subscribeAndResponseStatusOnlyList(RoutingContext routingContext, Single<List<UpdateResult>> updateResultListSingle, int httpResponseStatus) {
        updateResultListSingle.subscribe((List<UpdateResult> resp) ->
                        routingContext.response()
                                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                                .setStatusCode(httpResponseStatus)
                                .end()
                ,
                (Throwable throwable) -> processException(routingContext, throwable));
    }

    private void subscribeAndResponse(RoutingContext routingContext, Single<ResultSet> resultSetSingle, int httpResponseStatus) {
        subscribeAndResponse(routingContext, resultSetSingle, null, httpResponseStatus);
    }

    void subscribeAndResponse(RoutingContext routingContext, Single<ResultSet> resultSetSingle, List<String> jsonColumns, int httpResponseStatus) {
        resultSetSingle.subscribe((ResultSet resp) -> {
                    JsonArray arr = new JsonArray();
                    if (jsonColumns == null) {
                        resp.getRows().forEach(arr::add);
                    } else {
                        resp.getResults().forEach((JsonArray eachRow) -> {
                            JsonObject row = new JsonObject();
                            for (int i = 0; i < resp.getColumnNames().size(); i++) {
                                if (jsonColumns.contains(resp.getColumnNames().get(i))) {
                                    if (eachRow.getString(i) == null) {
                                        row.put(resp.getColumnNames().get(i), new JsonObject());
                                    } else {
                                        if (eachRow.getString(i).startsWith("[")) {
                                            row.put(resp.getColumnNames().get(i), new JsonArray(eachRow.getString(i)));
                                        } else {
                                            row.put(resp.getColumnNames().get(i), new JsonObject(eachRow.getString(i)));
                                        }
                                    }
                                } else {
                                    row.put(resp.getColumnNames().get(i), eachRow.getValue(i));
                                }
                            }
                            arr.add(row);
                        });
                    }
                    arr.forEach((Object arrayItem) -> elasticSearchService
                            .indexDocument(routingContext, this.getClass().getSimpleName().replace(API_CONTROLLER, "")
                                    .toLowerCase(Locale.ENGLISH) + "-api", (JsonObject) arrayItem));
                    routingContext.response()
                            .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                            .setStatusCode(httpResponseStatus)
                            .end(JsonSanitizer.sanitize(arr.encode()), StandardCharsets.UTF_8.toString());
                },
                (Throwable throwable) -> processException(routingContext, throwable));
    }

    protected void subscribeAndResponseBulk(RoutingContext routingContext, Single<JsonArray> jsonArraySingle, int httpResponseStatus) {
        jsonArraySingle.subscribe((JsonArray resp) ->
                        routingContext.response()
                                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                                .setStatusCode(httpResponseStatus)
                                .end(JsonSanitizer.sanitize(resp.encode()), StandardCharsets.UTF_8.toString())
                ,
                (Throwable throwable) -> processException(routingContext, throwable));
    }

    private void subscribeAndResponseBulkList(RoutingContext routingContext, Single<List<JsonObject>> jsonListSingle, int httpResponseStatus) {
        subscribeAndResponseBulkList(routingContext, jsonListSingle, null, httpResponseStatus);
    }

    void subscribeAndResponseBulkList(RoutingContext routingContext, Single<List<JsonObject>> jsonListSingle, List<String> jsonColumns, int httpResponseStatus) {
        LOGGER.trace("---subscribeAndResponseBulkList invoked");
        JsonArray arr = new JsonArray();
        jsonListSingle
                .doOnError((Throwable throwable) -> {
                    LOGGER.error("exception occured {}", throwable.getLocalizedMessage());
                    LOGGER.error("exception occured {}", Arrays.toString(throwable.getStackTrace()));
                    throwApiException(routingContext, InternalServerError500Exception.class, throwable.getLocalizedMessage());
                })
                .subscribe((List<JsonObject> resp) -> {
                            if (jsonColumns == null) {
                                resp.forEach(arr::add);
                            } else {
                                resp.forEach((JsonObject eachJO) -> {
                                    JsonObject newJsonObject = new JsonObject();
                                    eachJO.forEach((Map.Entry<String, Object> eachJOEntry) -> {
                                        if (eachJOEntry.getKey().equals("response")) {
                                            JsonObject newResponseJO = new JsonObject();
                                            ((JsonObject) eachJOEntry.getValue()).forEach((Map.Entry<String, Object> responseJOEntry) -> {
                                                if (jsonColumns.contains(responseJOEntry.getKey())) {
                                                    if (responseJOEntry.getValue() == null) {
                                                        newResponseJO.put(responseJOEntry.getKey(), new JsonObject());
                                                    } else {
                                                        newResponseJO.put(responseJOEntry.getKey(), new JsonObject(responseJOEntry.getValue().toString()));
                                                    }
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
                            arr.forEach((Object arrayItem) -> elasticSearchService
                                    .indexDocument(routingContext, this.getClass().getSimpleName().replace(API_CONTROLLER, "")
                                            .toLowerCase(Locale.ENGLISH) + "-api", ((JsonObject) arrayItem).getJsonObject("response")));
                            routingContext.response()
                                    .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                                    .putHeader("Vary", ORIGIN)
                                    .putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                                    .setStatusCode(httpResponseStatus)
                                    .end(JsonSanitizer.sanitize(arr.encode()), StandardCharsets.UTF_8.toString());
                        },
                        (Throwable throwable) -> processException(routingContext, throwable));
    }

    protected void subscribeAndResponse(RoutingContext routingContext, JsonArray response, int httpResponseStatus) {
        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                .putHeader("Vary", ORIGIN)
                .putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                .setStatusCode(httpResponseStatus)
                .end(JsonSanitizer.sanitize(response.encode()), StandardCharsets.UTF_8.toString());
    }

    void subscribeAndResponseJsonObject(RoutingContext routingContext, Single<JsonObject> response, int httpResponseStatus) {
        subscribeAndResponseJsonObject(routingContext, response, httpResponseStatus, false);
    }

    void subscribeAndResponseJsonObject(RoutingContext routingContext, Single<JsonObject> response, int httpResponseStatus, boolean onlyStatus) {
        response.subscribe((JsonObject jsonObject) -> {
            elasticSearchService
                    .indexDocument(routingContext, this.getClass().getSimpleName().replace(API_CONTROLLER, "")
                            .toLowerCase(Locale.ENGLISH) + "-api", jsonObject);
            if (onlyStatus) {
                routingContext.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                        .putHeader("Vary", ORIGIN)
                        .putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                        .setStatusCode(httpResponseStatus)
                        .end(".");
            } else {
                routingContext.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                        .putHeader("Vary", ORIGIN)
                        .putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                        .setStatusCode(httpResponseStatus)
                        .end(JsonSanitizer.sanitize(jsonObject.encode()));
            }
        }, (Throwable throwable) -> processException(routingContext, throwable));
    }

    protected void subscribeAndResponseString(RoutingContext routingContext, Single<String> response, int httpResponseStatus, boolean onlyStatus) {
        response.subscribe((String stringObject) -> {
            if (onlyStatus) {
                routingContext.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/text; charset=utf-8")
                        .putHeader("Vary", ORIGIN)
                        .putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                        .setStatusCode(httpResponseStatus)
                        .end(".");
            } else {
                routingContext.response()
                        .putHeader(HttpHeaders.CONTENT_TYPE, "application/text; charset=utf-8")
                        .putHeader("Vary", ORIGIN)
                        .putHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true")
                        .setStatusCode(httpResponseStatus)
                        .end(stringObject);
            }
        }, (Throwable throwable) -> processException(routingContext, throwable));
    }

    <T extends IService> void getEntities(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, UnsupportedEncodingException {
        getEntities(routingContext, clazz, null, new ApiFilterQuery());
    }

    <T extends IService> void getEntities(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, UnsupportedEncodingException {
        getEntities(routingContext, clazz, jsonColumns, new ApiFilterQuery());
    }

    <T extends IService> void getEntities(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns, ApiFilterQuery apiFilterQuery) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException, UnsupportedEncodingException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        String filterByNameParameter = null;
        String filterLikeNameParameter = null;
        if (!routingContext.queryParams().isEmpty()) {
            if (!routingContext.queryParam(Constants.RESTAPI_FILTERING_BY_NAME).isEmpty()) {
                filterByNameParameter = URLDecoder.decode(routingContext.queryParam(Constants.RESTAPI_FILTERING_BY_NAME).get(0), StandardCharsets.UTF_8.toString());
            }
            if (!routingContext.queryParam(Constants.RESTAPI_FILTERING_LIKE_NAME).isEmpty()) {
                if (filterByNameParameter != null && !filterByNameParameter.isEmpty()) {
                    throwApiException(routingContext, UnProcessableEntity422Exception.class
                            , "Both Filter By Name AND Filter Like Name CANNOT BE used at the same time, choose only one");
                }
                filterLikeNameParameter = routingContext.queryParam(Constants.RESTAPI_FILTERING_LIKE_NAME).get(0);
            }
        }

        String finalFilterByNameParameter = filterByNameParameter;
        String finalFilterLikeNameParameter = filterLikeNameParameter;
        Single<ResultSet> findAllResult = service
                .initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME)
                        , routingContext.get(Constants.AUTH_ABYSS_PORTAL_ROUTING_CONTEXT_OPERATION_ID))
                .flatMap((JDBCClient jdbcClient) -> {
                    if (apiFilterQuery == null || apiFilterQuery.getFilterQuery().isEmpty()) {
                        if ((finalFilterByNameParameter == null) && (finalFilterLikeNameParameter == null)) {
                            return service.findAll();
                        } else {
                            if (finalFilterLikeNameParameter == null) {
                                return service.findByName(finalFilterByNameParameter);
                            } else {
                                return service.findLikeName(finalFilterLikeNameParameter);
                            }
                        }
                    } else {
                        if ((finalFilterByNameParameter == null) && (finalFilterLikeNameParameter == null)) {
                            return service.findAll(apiFilterQuery);
                        } else {
                            if (finalFilterByNameParameter != null) {
                                apiFilterQuery
                                        .addFilterQuery(service.getAPIFilter().getApiFilterByNameQuery())
                                        .addFilterQueryParams(new JsonArray().add(finalFilterByNameParameter));
                                return service.findAll(apiFilterQuery);
                            } else {
                                apiFilterQuery
                                        .addFilterQuery(service.getAPIFilter().getApiFilterLikeNameQuery())
                                        .addFilterQueryParams(new JsonArray().add(finalFilterLikeNameParameter + "%"));
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
                .flatMap((ResultSet resultSet) -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException(NO_DATA_FOUND));
                    } else {
                        return Single.just(resultSet);
                    }
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
        Single<ResultSet> findAllResult = service
                .initJDBCClient(routingContext
                        .session()
                        .get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME), routingContext.get(Constants.AUTH_ABYSS_PORTAL_ROUTING_CONTEXT_OPERATION_ID))
                .flatMap(jdbcClient -> (apiFilterQuery == null) ? service.findById(UUID.fromString(routingContext.pathParam("uuid"))) : service.findAll(apiFilterQuery));
        subscribeAndResponse(routingContext, findAllResult, jsonColumns, HttpResponseStatus.OK.code());
    }

    <T extends IService> void addEntities(RoutingContext routingContext, Class<T> clazz, JsonArray requestBody) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        addEntities(routingContext, clazz, requestBody, null);
    }

    <T extends IService> void addEntities(RoutingContext routingContext, Class<T> clazz, JsonArray requestBody, List<String> jsonColumns) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        LOGGER.trace("---addEntities invoked");
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<List<JsonObject>> insertAllResult = service
                .initJDBCClient(routingContext
                        .session()
                        .get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
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
        Single<List<JsonObject>> updateAllResult = service
                .initJDBCClient(routingContext
                        .session()
                        .get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
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
        Single<ResultSet> updateAllResult = service
                .initJDBCClient(routingContext
                        .session()
                        .get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient -> service
                        //TODO:  an overloaded version of update() using ApiFilterQuery param should be developed, now suppressing apiFilterQuery param
                        .update(UUID.fromString(routingContext.pathParam("uuid")), requestBody))
                .flatMap(resultSet -> service
                        //TODO: CompositeResult success & # of rows should be checked
                        .findById(UUID.fromString(routingContext.pathParam("uuid"))))
                .flatMap((ResultSet resultSet) -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException(NO_DATA_FOUND));
                    } else {
                        return Single.just(resultSet);
                    }
                });
        subscribeAndResponse(routingContext, updateAllResult, jsonColumns, HttpResponseStatus.OK.code());
    }

/*
    <T extends IService> void deleteEntity(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<CompositeResult> deleteResult = service.initJDBCClient()
                .flatMap(jdbcClient -> service.delete(UUID.fromString(routingContext.pathParam("uuid"))));
        subscribeAndResponseStatusOnly(routingContext, deleteResult, HttpResponseStatus.NO_CONTENT.code());
    }
*/

    <T extends IService> void deleteEntities(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        deleteEntities(routingContext, clazz, new ApiFilterQuery());
    }

    <T extends IService> void deleteEntities(RoutingContext routingContext, Class<T> clazz, ApiFilterQuery apiFilterQuery)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<CompositeResult> deleteAllResult = service.initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient -> service.deleteAll(apiFilterQuery));
        subscribeAndResponseStatusOnly(routingContext, deleteAllResult, HttpResponseStatus.NO_CONTENT.code());
    }

    <T extends IService> void deleteEntity(RoutingContext routingContext, Class<T> clazz)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> deleteResult = service.initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient -> service.delete(UUID.fromString(routingContext.pathParam("uuid"))))
                .flatMap(resultSet -> service.findById(UUID.fromString(routingContext.pathParam("uuid"))))
                .flatMap((ResultSet resultSet) -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException(NO_DATA_FOUND));
                    } else {
                        return Single.just(resultSet);
                    }
                });
        subscribeAndResponseDeleteStatusOnly(routingContext, deleteResult, HttpResponseStatus.NO_CONTENT.code());
    }

    <T extends IService> void execServiceMethod(RoutingContext routingContext, Class<T> clazz, List<String> jsonColumns, String method)
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        IService<T> service = clazz.getConstructor(Vertx.class).newInstance(vertx);
        Single<ResultSet> funcResult = service
                .initJDBCClient(routingContext
                        .session()
                        //TODO: access control
                        .get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient -> (Single<ResultSet>) service
                        .getClass()
                        .getDeclaredMethod(method, RoutingContext.class, JDBCAuth.class)
                        .invoke(service, routingContext, authProvider))
                .flatMap((ResultSet resultSet) -> {
                    if (resultSet.getNumRows() == 0) {
                        return Single.error(new NoDataFoundException(NO_DATA_FOUND));
                    } else {
                        return Single.just(resultSet);
                    }
                });
        subscribeAndResponse(routingContext, funcResult, jsonColumns, HttpResponseStatus.OK.code());
    }

    void subscribeForImage(RoutingContext routingContext, Single<ResultSet> resultSetSingle, String methodName, String imageColumnName) {
        resultSetSingle.subscribe((ResultSet resultSet) -> {
                    if (resultSet.getNumRows() > 0) {
                        String sourceData = resultSet.getRows().get(0).getString(imageColumnName);
                        if (sourceData == null || sourceData.isEmpty()) {
                            String errorMessage = methodName + " - picture not found for uuid: " + routingContext.pathParam("uuid");
                            LOGGER.error(errorMessage);
                            throwApiException(routingContext, NotFound404Exception.class, errorMessage);
                        } else {
                            returnAsImage(routingContext, resultSet, imageColumnName);
                        }
                    } else {
                        String errorMessage = methodName + " - record not found for uuid: " + routingContext.pathParam("uuid");
                        LOGGER.error(errorMessage);
                        throwApiException(routingContext, NotFound404Exception.class, errorMessage);
                    }
                },
                (Throwable throwable) -> processException(routingContext, throwable));
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
