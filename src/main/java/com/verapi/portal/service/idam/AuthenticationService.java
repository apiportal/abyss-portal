/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 7 2018
 *
 */

package com.verapi.portal.service.idam;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.abyss.exception.UnAuthorized401Exception;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.ApiFilterQuery;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AuthenticationService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private String userUUID;
    private String temporaryOrganizationName;

    public AuthenticationService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public AuthenticationService(Vertx vertx) {
        super(vertx);
    }

    public Single<JsonObject> login(RoutingContext routingContext, JDBCAuth jdbcAuth) {
        logger.trace("login invoked");
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject creds = requestParameters.body().getJsonObject();
        String username = creds.getString("username");
        logger.trace("Received user:" + username);

        class LoginMetadata {
            private User user;
            private SubjectService subjectService = new SubjectService(routingContext.vertx());
            private SubjectOrganizationService subjectOrganizationService = new SubjectOrganizationService(routingContext.vertx());
            private OrganizationService organizationService = new OrganizationService(routingContext.vertx());

            LoginMetadata() {
            }
        }

        LoginMetadata loginMetadata = new LoginMetadata();

        return jdbcAuth.rxAuthenticate(creds)
                .doOnError(throwable -> {
                    throw new UnAuthorized401Exception(throwable);
                })
                .flatMap(loginUser -> {

                    loginMetadata.user = loginUser;

                    //TODO: Check if password has expired and force change
                    routingContext.setUser(loginUser); //TODO: Check context. Is this usefull? Should it be vertx context?

                    return loginMetadata.subjectService.initJDBCClient();
                })
                .flatMap(jdbcClient1 -> loginMetadata.subjectService.findByName(username)
                )
                .flatMap(result -> {
                    //result.toJson().getValue("rows")
                    logger.trace(result.toJson().encodePrettily());
                    userUUID = result.getRows().get(0).getString("uuid");
                    temporaryOrganizationName = "Organization of " + result.getRows().get(0).getString("displayname");
                    loginMetadata.user.principal().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID);
                    routingContext.setUser(loginMetadata.user); //TODO: Check context. Is this usefull? Should it be vertx context?
                    routingContext.session().regenerateId();
                    routingContext.session().destroy();
                    routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME, loginMetadata.user.principal().getString("username"));
                    routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID); //XXX
                    routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME, userUUID)); //TODO: Remove for OWASP Compliance
                    //.setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                    logger.debug("Logged in user: " + loginMetadata.user.principal().encodePrettily());
                    routingContext.put("username", loginMetadata.user.principal().getString("username"));

                    // --------- getOrganizationListOfSubject(userUUID);--------------
                    return loginMetadata.subjectOrganizationService.initJDBCClient();
                })
                .flatMap(jdbcClient1 ->
                        loginMetadata.subjectOrganizationService.findAll(new ApiFilterQuery()
                                .setFilterQuery(SubjectOrganizationService.FILTER_BY_SUBJECT)
                                .setFilterQueryParams(new JsonArray().add(userUUID)))
                )
                .flatMap(userOrganizations -> {
                    logger.trace(userOrganizations.toJson().encodePrettily());
                    if (userOrganizations.getNumRows() == 0) {
                        return Single.just(new ArrayList<JsonObject>());
                    } else {
                        return loginMetadata.organizationService.initJDBCClient()
                                .flatMap(jdbcClient -> {

                                    Observable<JsonObject> observable = Observable.fromIterable(userOrganizations.getRows());

                                    return observable
                                            .flatMap(entries -> loginMetadata.organizationService.findById(UUID.fromString(entries.getString("organizationrefid"))).toObservable())
                                            .flatMap(resultSet -> {

                                                //userOrganizationArray.add(
                                                return Observable.just(new JsonObject()
                                                        .put("uuid", resultSet.getRows().get(0).getString("uuid"))
                                                        .put("name", resultSet.getRows().get(0).getString("name"))
                                                );
                                            }).toList();
                                });
                    }

                })
                .flatMap(jsonObjects -> {
                    if (jsonObjects.isEmpty()) {
                        //Create-organization
                        return loginMetadata.organizationService.initJDBCClient()
                                .flatMap(jdbcClient1 -> {
                                    return loginMetadata.organizationService.insertAll(new JsonArray().add(new JsonObject()
                                            .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                                            .put("crudsubjectid", userUUID)
                                            .put("name", temporaryOrganizationName)
                                            .put("description", temporaryOrganizationName)
                                            .put("url", "")
                                            .put("isactive", true)
                                            .put("picture", "")));

                                })
                                .flatMap(jsonObjects2 -> {
                                    logger.trace("CreateOrganizationController - organizationService.insertAll successfull: {}", jsonObjects2.get(0).encodePrettily());

                                    organizationUuid = jsonObjects2.get(0).getString("uuid");

                                    return loginMetadata.subjectOrganizationService.initJDBCClient();
                                })
                                .flatMap(jdbcClient1 -> {
                                    return loginMetadata.subjectOrganizationService.insertAll(new JsonArray().add(new JsonObject()
                                            .put("organizationid", organizationUuid)
                                            .put("crudsubjectid", userUUID)
                                            .put("subjectid", userUUID)
                                            .put("organizationrefid", organizationUuid)
                                            .put("isowner", true)
                                            .put("isactive", true)));
                                })
                                .flatMap(jsonObjects2 -> {
                                    logger.trace("CreateOrganizationController - subjectOrganizationService.insertAll successfull: {}", jsonObjects2.get(0).encodePrettily());

                                    try {
                                        //Url Encode for cookie compliance
                                        String userLoginOrganizationName = URLEncoder.encode(temporaryOrganizationName, "UTF-8");
                                        String userLoginOrganizationUUID = URLEncoder.encode(organizationUuid, "UTF-8");

                                        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName);
                                        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID);

                                        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName));
                                        //.setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60)); //TODO: Remove Cookie at Session Timeout
                                        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID));
                                        //.setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                                        return Single.just(new JsonObject().put("username", creds.getString("username")).put("sessionid", routingContext.session().id()));

                                    } catch (UnsupportedEncodingException e) {
                                        logger.error("SelectOrganizationController - POST handler : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
                                        throw new RuntimeException(e);
                                    }

                                });
                    } else {
                        //Select-organization
                        JsonArray jsonArray = new JsonArray(jsonObjects);
                        logger.trace("LoginController.handle() findByIdResult.subscribe result: {}", jsonArray);
                        routingContext.session().put("userOrganizationArray", jsonArray);

                        return Single.just(new JsonObject().put("username", creds.getString("username")).put("sessionid", routingContext.session().id()));
                    }

                });
    }

    public Single<JsonObject> logout(RoutingContext routingContext) {
        logger.trace("logout invoked");
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        if (!Objects.equals(requestBody.getString("sessionid"), routingContext.session().id()))
            return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));

        String username = routingContext.user().principal().getString("username");
        String sessionid = routingContext.session().id();

        routingContext.user().clearCache();
        routingContext.clearUser();
        routingContext.removeCookie(Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);

        return Single.just(new JsonObject().put("username", username)
                .put("sessionid", sessionid));
    }

    private Single<JsonObject> rxValidateToken(String token) {
        logger.trace("rxValidateToken invoked");

        ResourceAccessTokenService resourceAccessTokenService = new ResourceAccessTokenService(vertx);

        return resourceAccessTokenService.initJDBCClient()
                .flatMap(jdbcClient -> resourceAccessTokenService.findAll(new ApiFilterQuery()
                        .setFilterQuery(ResourceAccessTokenService.SQL_FIND_BY_SUBJECT_TOKEN)
                        .setFilterQueryParams(new JsonArray().add(token)))
                )
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() == 0)
                        return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                    AuthenticationInfo authInfo = new AuthenticationInfo(
                            token,
                            resultSet.getRows().get(0).getString("nonce"),
                            resultSet.getRows().get(0).getInstant("expiredate"),
                            resultSet.getRows().get(0).getString("userdata"));
                    Token tokenValidator = new Token();

//                    return Single.just(resultSet); //TODO:remove after debug
                    AuthenticationInfo authResult = tokenValidator.validateToken(token, authInfo);
                    if (authResult.isValid()) {
                        return Single.just(resultSet);
                    } else {
                        logger.error("rxValidateToken error: {}", authResult.getResultText());
                        return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                    }
                })
                .flatMap(ratResultSet -> {
                    SubjectPermissionService subjectPermissionService = new SubjectPermissionService(vertx);
                    return subjectPermissionService.initJDBCClient()
                            .flatMap(jdbcClient -> subjectPermissionService.findById(UUID.fromString(ratResultSet.getRows().get(0).getString("subjectpermissionid"))))
                            .flatMap(spResultSet -> {
                                if (spResultSet.getNumRows() == 0) {
                                    logger.error("rxValidateToken error: {} subjectpermissionid:{}", "no row for subject permission", ratResultSet.getRows().get(0).getString("subjectpermissionid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }

                                return Single.just(new JsonObject()
                                        .put("accesstokenisdeleted", ratResultSet.getRows().get(0).getBoolean("isdeleted"))
                                        .put("accesstokenisactive", ratResultSet.getRows().get(0).getBoolean("isactive"))
                                        .put("accesstokenexpiredate", ratResultSet.getRows().get(0).getInstant("expiredate"))
                                        .put("subjectpermissionid", spResultSet.getRows().get(0).getString("uuid"))
                                        .put("subjectid", spResultSet.getRows().get(0).getString("subjectid"))
                                        .put("resourceid", spResultSet.getRows().get(0).getString("resourceid"))
                                        .put("resourceactionid", spResultSet.getRows().get(0).getString("resourceactionid"))
                                        .put("subjectpermissioneffectiveenddate", spResultSet.getRows().get(0).getInstant("effectiveenddate"))
                                        .put("subjectpermissionisdeleted", spResultSet.getRows().get(0).getBoolean("isdeleted"))
                                        .put("subjectpermissionisactive", spResultSet.getRows().get(0).getBoolean("isactive"))
                                );
                            })
                            .flatMap(jsonObject -> {
                                ResourceService resourceService = new ResourceService(vertx);
                                return resourceService.initJDBCClient()
                                        .flatMap(jdbcClient1 -> resourceService.findById(UUID.fromString(jsonObject.getString("resourceid"))))
                                        .flatMap(resourceResultSet -> {
                                            if (resourceResultSet.getNumRows() == 0) {
                                                logger.error("rxValidateToken error: {} resourceid:{}", "no row for resource", jsonObject.getString("resourceid"));
                                                return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                            }

                                            return Single.just(jsonObject.put("resourcename", resourceResultSet.getRows().get(0).getString("resourcename"))
                                                    .put("resourcetypeid", resourceResultSet.getRows().get(0).getString("resourcetypeid"))
                                                    .put("resourcerefid", resourceResultSet.getRows().get(0).getString("resourcerefid"))
                                                    .put("resourceisdeleted", resourceResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                    .put("resourceisactive", resourceResultSet.getRows().get(0).getBoolean("isactive"))
                                            );
                                        });
                            })
                            .flatMap(jsonObject -> {
                                ResourceActionService resourceActionService = new ResourceActionService(vertx);
                                return resourceActionService.initJDBCClient()
                                        .flatMap(jdbcClient1 -> resourceActionService.findById(UUID.fromString(jsonObject.getString("resourceactionid"))))
                                        .flatMap(resourceActionResultSet -> {
                                            if (resourceActionResultSet.getNumRows() == 0) {
                                                logger.error("rxValidateToken error: {} resourceactionid:{}", "no row for resource action", jsonObject.getString("resourceactionid"));
                                                return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                            }

                                            return Single.just(jsonObject.put("resourceactionname", resourceActionResultSet.getRows().get(0).getString("actionname"))
                                                    .put("resourceactionisdeleted", resourceActionResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                    .put("resourceactionisactive", resourceActionResultSet.getRows().get(0).getBoolean("isactive"))
                                            );
                                        });
                            })
                            .flatMap(jsonObject -> {
                                if (jsonObject.getString("resourcetypeid").equals(Constants.RESOURCE_TYPE_API)) {
                                    ApiService apiService = new ApiService(vertx);
                                    return apiService.initJDBCClient()
                                            .flatMap(jdbcClient -> apiService.findById(UUID.fromString(jsonObject.getString("resourcerefid"))))
                                            .flatMap(apiResultSet -> {
                                                if (apiResultSet.getNumRows() == 0) {
                                                    logger.error("rxValidateToken error: {} uuid:{}", "no row for proxy api", jsonObject.getString("resourcerefid"));
                                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                                } else {
                                                    return Single.just(jsonObject.put("apiuuid", apiResultSet.getRows().get(0).getString("uuid"))
                                                            .put("apiisproxyapi", apiResultSet.getRows().get(0).getBoolean("isproxyapi"))
                                                            .put("apiissandbox", apiResultSet.getRows().get(0).getBoolean("issandbox"))
                                                            .put("apiislive", apiResultSet.getRows().get(0).getBoolean("islive"))
                                                            .put("apiisdeleted", apiResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                            .put("businessapiid", apiResultSet.getRows().get(0).getString("businessapiid"))
                                                    );
                                                }
                                            });
                                } else {
                                    logger.error("rxValidateToken error: {} {}", "undefined resource type", jsonObject.getString("resourcetypeid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }
                            })
                            .flatMap(jsonObject -> {
                                ApiService apiService = new ApiService(vertx);
                                return apiService.initJDBCClient()
                                        .flatMap(jdbcClient -> apiService.findById(UUID.fromString(jsonObject.getString("businessapiid"))))
                                        .flatMap(apiResultSet -> {
                                            if (apiResultSet.getNumRows() == 0) {
                                                logger.error("rxValidateToken error: {} uuid:{}", "no row for business api", jsonObject.getString("businessapiid"));
                                                return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                            } else {
                                                return Single.just(jsonObject.put("businessapiuuid", apiResultSet.getRows().get(0).getString("uuid"))
                                                        .put("businessapiisproxyapi", apiResultSet.getRows().get(0).getBoolean("isproxyapi"))
                                                        .put("businessapiissandbox", apiResultSet.getRows().get(0).getBoolean("issandbox"))
                                                        .put("businessapiislive", apiResultSet.getRows().get(0).getBoolean("islive"))
                                                        .put("businessapiisdeleted", apiResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                        .put("businessapiopenapidocument", apiResultSet.getRows().get(0).getString("openapidocument"))
                                                );
                                            }
                                        });
                            })
                            .flatMap(jsonObject -> {
                                if (jsonObject.getString("resourcetypeid").equals(Constants.RESOURCE_TYPE_API)) {
                                    ContractService contractService = new ContractService(vertx);
                                    return contractService.initJDBCClient()
                                            .flatMap(jdbcClient -> contractService.findAll(new ApiFilterQuery()
                                                    .setFilterQuery(ContractService.FILTER_BY_SUBJECTPERMISSIONID)
                                                    .setFilterQueryParams(new JsonArray()
                                                            .add(jsonObject.getString("subjectpermissionid")))))
                                            .flatMap(contractResultSet -> {
                                                if (contractResultSet.getNumRows() == 0) {
                                                    logger.error("rxValidateToken error: {} subjectpermissionid:{}", "no row for contract", jsonObject.getString("subjectpermissionid"));
                                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                                } else {
                                                    return Single.just(jsonObject
                                                            .put("contractisdeleted", contractResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                            .put("contractsubjectid", contractResultSet.getRows().get(0).getString("subjectid"))
                                                            .put("contractenvironment", contractResultSet.getRows().get(0).getString("environment"))
                                                            .put("contractstateid", contractResultSet.getRows().get(0).getString("contractstateid"))
                                                            .put("contractstatus", contractResultSet.getRows().get(0).getString("status"))
                                                    );
                                                }
                                            });
                                } else {
                                    logger.error("rxValidateToken error: {} {}", "undefined resource type", jsonObject.getString("resourcetypeid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }
                            });

                });
    }

    public Single<JsonObject> validateAccessToken(String token) {
        logger.trace("validateAccessToken invoked");
        JsonObject validationStatus = new JsonObject().put("status", false).put("error", "").put("validationreport", new JsonObject());

        return rxValidateToken(token)
                .flatMap(jsonObject -> {
                    validationStatus.put("validationreport", jsonObject);
                    //check if any record is deleted
                    if (jsonObject.getBoolean("accesstokenisdeleted"))
                        return Single.error(new Exception("access token is deleted"));
                    if (jsonObject.getBoolean("subjectpermissionisdeleted"))
                        return Single.error(new Exception("subject permission is deleted"));
                    if (jsonObject.getBoolean("resourceisdeleted"))
                        return Single.error(new Exception("resource is deleted"));
                    if (jsonObject.getBoolean("resourceactionisdeleted"))
                        return Single.error(new Exception("resource action is deleted"));
                    if (jsonObject.getBoolean("apiisdeleted"))
                        return Single.error(new Exception("proxy api is deleted"));
                    if (jsonObject.getBoolean("businessapiisdeleted"))
                        return Single.error(new Exception("business api is deleted"));
                    if (jsonObject.getBoolean("contractisdeleted"))
                        return Single.error(new Exception("contract is deleted"));

                    //check if any record is not active
                    if (!jsonObject.getBoolean("accesstokenisactive"))
                        return Single.error(new Exception("access token is not active"));
                    if (!jsonObject.getBoolean("subjectpermissionisactive"))
                        return Single.error(new Exception("subject permission is not active"));
                    if (!jsonObject.getBoolean("resourceisactive"))
                        return Single.error(new Exception("resource is not active"));
                    if (!jsonObject.getBoolean("resourceactionisactive"))
                        return Single.error(new Exception("resource action is not active"));

                    //check if any record is expired
                    if (jsonObject.getInstant("accesstokenexpiredate").isBefore(Instant.now()))
                        return Single.error(new Exception("access token is expired"));
                    if (jsonObject.getInstant("subjectpermissioneffectiveenddate").isBefore(Instant.now()))
                        return Single.error(new Exception("subject permission is expired"));

                    //check if contract is valid
                    if (!Objects.equals(jsonObject.getString("contractstateid"), Constants.CONTRACT_STATE_IS_ACTIVATED))
                        return Single.error(new Exception("contract state is not 'activated'"));
                    if (!Objects.equals(jsonObject.getString("contractstatus"), Constants.CONTRACT_STATUS_IS_INFORCE))
                        return Single.error(new Exception("contract status is not 'inforce'"));

                    return Single.just(validationStatus.put("status", true));
                });
        //.doAfterSuccess(validateAccessTokenStatus -> validationStatus.put("status", true).put("error", "").put("validationreport", validateAccessTokenStatus))
/*
                .doOnError(throwable -> {
                            Single.error(throwable);
                            //return Single.just(validationStatus.put("status", false).put("error", throwable.getLocalizedMessage()));
                        }
                );
*/
/*
                .subscribe(validateAccessTokenStatus -> {
                            validationStatus.put("status", true).put("error", "").put("validationreport", validateAccessTokenStatus);
                        }
                        , throwable -> {
                            validationStatus.put("status", false).put("error", throwable.getLocalizedMessage());
                        });
*/
        //return validationStatus;
    }

    public Single<JsonObject> rxValidateAccessToken(String token) {
        return validateAccessToken(token);
    }

    @Override
    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        return null;
    }

    @Override
    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        return null;
    }

    @Override
    public Single<List<JsonObject>> updateAll(JsonObject updateRecords) {
        return null;
    }

    @Override
    public Single<CompositeResult> delete(UUID uuid) {
        return null;
    }

    @Override
    public Single<CompositeResult> deleteAll() {
        return null;
    }

    @Override
    public Single<CompositeResult> deleteAll(ApiFilterQuery apiFilterQuery) {
        return null;
    }

    @Override
    public Single<ResultSet> findById(long id) {
        return null;
    }

    @Override
    public Single<ResultSet> findById(UUID uuid) {
        return null;
    }

    @Override
    public Single<ResultSet> findByName(String name) {
        return null;
    }

    @Override
    public Single<ResultSet> findLikeName(String name) {
        return null;
    }

    @Override
    public Single<ResultSet> findAll() {
        return null;
    }

    @Override
    public Single<ResultSet> findAll(ApiFilterQuery apiFilterQuery) {
        return null;
    }

    @Override
    public ApiFilterQuery.APIFilter getAPIFilter() {
        return null;
    }
}