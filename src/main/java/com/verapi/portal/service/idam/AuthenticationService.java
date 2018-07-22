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
import com.verapi.portal.common.Constants;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.oapi.exception.UnAuthorized401Exception;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.ApiFilterQuery;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class AuthenticationService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

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

        return jdbcAuth.rxAuthenticate(creds)
                .flatMap(user -> {
                    routingContext.setUser(user);
                    return Single.just(new JsonObject().put("username", creds.getString("username"))
                            .put("sessionid", routingContext.session().id()));
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
        logger.trace("logout invoked");

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
                                                    logger.error("rxValidateToken error: {} resourcerefid:{}", "no row for api", jsonObject.getString("resourcerefid"));
                                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                                } else {
                                                    return Single.just(jsonObject.put("apiuuid", apiResultSet.getRows().get(0).getBoolean("uuid"))
                                                            .put("apiisproxyapi", apiResultSet.getRows().get(0).getBoolean("isproxyapi"))
                                                            .put("apiissandbox", apiResultSet.getRows().get(0).getBoolean("issandbox"))
                                                            .put("apiislive", apiResultSet.getRows().get(0).getBoolean("islive"))
                                                            .put("apiisdeleted", apiResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                    );
                                                }
                                            });
                                } else {
                                    logger.error("rxValidateToken error: {} {}", "undefined resource type", jsonObject.getString("resourcetypeid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }
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

    public JsonObject validateAccessToken(String token) {
        final JsonObject validationStatus = new JsonObject().put("status", false).put("error", "").put("validationreport", "");

        rxValidateToken(token)
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
                        return Single.error(new Exception("api is deleted"));
                    if (jsonObject.getBoolean("contractisdeleted"))
                        return Single.error(new Exception("contract is deleted"));

                    //check if any record is not active
                    if (jsonObject.getBoolean("accesstokenisactive"))
                        return Single.error(new Exception("access token is not active"));
                    if (jsonObject.getBoolean("subjectpermissionisactive"))
                        return Single.error(new Exception("subject permission is not active"));
                    if (jsonObject.getBoolean("resourceisactive"))
                        return Single.error(new Exception("resource is not active"));
                    if (jsonObject.getBoolean("resourceactionisactive"))
                        return Single.error(new Exception("resource action is not active"));

                    //check if any record is expired
                    if (jsonObject.getInstant("accesstokenexpiredate").isBefore(Instant.now()))
                        return Single.error(new Exception("access token is expired"));
                    if (jsonObject.getInstant("subjectpermissioneffectiveenddate").isBefore(Instant.now()))
                        return Single.error(new Exception("subject permission is expired"));

                    //check if contract is valid
                    if (Objects.equals(jsonObject.getString("contractstateid"), Constants.CONTRACT_STATE_IS_ACTIVATED))
                        return Single.error(new Exception("contract state is not 'activated'"));
                    if (Objects.equals(jsonObject.getString("contractstatus"), Constants.CONTRACT_STATUS_IS_INFORCED))
                        return Single.error(new Exception("contract status is not 'inforced'"));

                    return Single.just(jsonObject);
                })
                .subscribe(validateAccessTokenStatus -> {
                            validationStatus.put("status", true).put("error", "");
                        }
                        , throwable -> {
                            validationStatus.put("status", false).put("error", throwable.getLocalizedMessage());
                        });
        return validationStatus;
    }

    public Single<JsonObject> rxValidateAccessToken(String token) {
        return Single.just(validateAccessToken(token));
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