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

package com.verapi.portal.service.idam;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.abyss.exception.Forbidden403Exception;
import com.verapi.abyss.exception.UnAuthorized401Exception;
import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.MailUtil;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.AbyssTableName;
import com.verapi.portal.service.ApiFilterQuery;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
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
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

@AbyssTableName(tableName = "")
public class AuthenticationService extends AbstractService<UpdateResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationService.class);

    private String userUUID;
    private String temporaryOrganizationName;
    private String userOrganizationUUID;

    public AuthenticationService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public AuthenticationService(Vertx vertx) {
        super(vertx);
    }

    @Override
    protected String getInsertSql() {
        return "";
    }

    @Override
    protected String getFindByIdSql() {
        return "";
    }

    @Override
    protected JsonArray prepareInsertParameters(JsonObject insertRecord) {
        return null;
    }

    public Single<JsonObject> login(RoutingContext routingContext, JDBCAuth jdbcAuth) {
        LOGGER.trace("login invoked");
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject creds = requestParameters.body().getJsonObject();
        String username = creds.getString("username");
        LOGGER.trace("Received user:" + username);

        class LoginMetadata {
            private User user;
            private SubjectService subjectService = new SubjectService(routingContext.vertx());
            private SubjectOrganizationService subjectOrganizationService = new SubjectOrganizationService(routingContext.vertx());
            private OrganizationService organizationService = new OrganizationService(routingContext.vertx());
            private SubjectMembershipService subjectMembershipService = new SubjectMembershipService(routingContext.vertx());

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
                    LOGGER.trace(result.toJson().encodePrettily());
                    userUUID = result.getRows().get(0).getString(STR_UUID);
                    userOrganizationUUID = result.getRows().get(0).getString("organizationid");
                    String displayName = result.getRows().get(0).getString("displayname");
                    temporaryOrganizationName = "Organization of " + displayName;
                    loginMetadata.user.principal().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID);
                    routingContext.setUser(loginMetadata.user); //TODO: Check context. Is this usefull? Should it be vertx context?
                    routingContext.session().regenerateId();
                    routingContext.session().destroy();
                    routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME, loginMetadata.user.principal().getString("username"));
                    routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME, userUUID); //XXX
                    routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_USER_DISPLAY_NAME_SESSION_VARIABLE_NAME, displayName);
                    routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME, userUUID).setPath("/")); //TODO: Remove for OWASP Compliance
                    //.setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                    LOGGER.debug("Logged in user: " + loginMetadata.user.principal().encodePrettily());
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
                    LOGGER.trace(userOrganizations.toJson().encodePrettily());
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
                                                        .put(STR_UUID, resultSet.getRows().get(0).getString(STR_UUID))
                                                        .put("name", resultSet.getRows().get(0).getString("name"))
                                                        .put("isactive", resultSet.getRows().get(0).getBoolean("isactive")) //TODO:? kontrol sql'de mi olmalı?
                                                        .put("isdeleted", resultSet.getRows().get(0).getBoolean("isdeleted")) //TODO:? kontrol sql'de mi olmalı?
                                                );
                                            }).toList();
                                });
                    }

                })
                .flatMap(organizationJsonObjects -> {
                    if (organizationJsonObjects.isEmpty()) {
                        //Create-organization
                        return loginMetadata.organizationService.initJDBCClient()
                                .flatMap(jdbcClient1 -> {
                                    return loginMetadata.organizationService.insertAll(new JsonArray().add(new JsonObject()
                                            .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                                            .put("crudsubjectid", userUUID)
                                            .put("name", temporaryOrganizationName)
                                            .put("description", temporaryOrganizationName)
                                            .put("url", "")
                                            .put("isactive", Boolean.TRUE)
                                            .put("picture", "")));

                                })
                                .flatMap(jsonObjects2 -> {
                                    LOGGER.trace("CreateOrganizationPortalController - organizationService.insertAll successfull: {}", jsonObjects2.get(0).encodePrettily());

                                    organizationUuid = jsonObjects2.get(0).getString(STR_UUID);

                                    return loginMetadata.subjectOrganizationService.initJDBCClient();
                                })
                                .flatMap(jdbcClient1 -> {
                                    return loginMetadata.subjectOrganizationService.insertAll(new JsonArray().add(new JsonObject()
                                            .put("organizationid", organizationUuid)
                                            .put("crudsubjectid", userUUID)
                                            .put("subjectid", userUUID)
                                            .put("organizationrefid", organizationUuid)
                                            .put("isowner", Boolean.TRUE)
                                            .put("isactive", Boolean.TRUE)));
                                })
                                .flatMap(jsonObjects2 -> {
                                    LOGGER.trace("CreateOrganizationPortalController - subjectOrganizationService.insertAll successfull: {}", jsonObjects2.get(0).encodePrettily());

                                    return loginMetadata.subjectMembershipService.initJDBCClient();
                                })
                                .flatMap(jdbcClient1 -> {
                                    return loginMetadata.subjectMembershipService.insertAll(new JsonArray().add(new JsonObject()
                                            .put("organizationid", organizationUuid)
                                            .put("crudsubjectid", userUUID)
                                            .put("subjectid", userUUID)
                                            .put("subjectgroupid", Constants.ROLE_ORGANIZATION_ADMIN)
                                            .put("subjectdirectoryid", Constants.INTERNAL_SUBJECT_DIRECTORY_UUID)
                                            .put("subjecttypeid", Constants.SUBJECT_TYPE_USER)
                                            .put("subjectgrouptypeid", Constants.SUBJECT_TYPE_ROLE)
                                            .put("isactive", Boolean.TRUE)));
                                })
                                .flatMap(jsonObjects2 -> {
                                    LOGGER.trace("CreateOrganizationPortalController - subjectMembershipService.insertAll successfull: {}", jsonObjects2.get(0).encodePrettily());

                                    return loginMetadata.subjectService.updateSubjectOrganization(new JsonArray().add(organizationUuid).add(userUUID).add(userUUID));
                                })
                                .flatMap(compositeResult -> {

                                    if (compositeResult.getThrowable() == null) {
                                        LOGGER.trace("Subject's Organization updated with newly created one successfully");
                                        try {
                                            //Url Encode for cookie compliance
                                            String userLoginOrganizationName = URLEncoder.encode(temporaryOrganizationName, StandardCharsets.UTF_8.toString());
                                            String userLoginOrganizationUUID = URLEncoder.encode(organizationUuid, StandardCharsets.UTF_8.toString());

                                            routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName);
                                            routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID);

                                            routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, userLoginOrganizationName));
                                            //.setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60)); //TODO: Remove Cookie at Session Timeout
                                            routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, userLoginOrganizationUUID));
                                            //.setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.SESSION_IDLE_TIMEOUT) * 60));

                                            return Single.just(new JsonObject().put("username", creds.getString("username")).put("sessionid", routingContext.session().id())
                                                    .put("principalid", userUUID).put("organizationid", organizationUuid).put("organizationname", temporaryOrganizationName));

                                        } catch (UnsupportedEncodingException e) {
                                            LOGGER.error("SelectOrganizationPortalController - POST handler : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
                                            throw new RuntimeException(e);
                                        }

                                    } else {
                                        LOGGER.trace("Subject's organization could not be updated with newly created one.");
                                        return Single.error(compositeResult.getThrowable());
                                    }
                                });
                    } else {
                        //Select-organization
                        JsonArray jsonArray = new JsonArray(organizationJsonObjects);
                        LOGGER.trace("LoginPortalController.handle() findByIdResult.subscribe result: {}", jsonArray);
                        routingContext.session().put("userOrganizationArray", jsonArray);

                        for (JsonObject organizationJsonObject : organizationJsonObjects) {
                            if (userOrganizationUUID.equals(organizationJsonObject.getString(STR_UUID))) {
                                if (isOrganizationUndeleletedAndActive(organizationJsonObject)) break;
                            }
                        }
                        if (organizationUuid == null || organizationUuid.isEmpty()) {
                            for (JsonObject organizationJsonObject : organizationJsonObjects) {
                                if (isOrganizationUndeleletedAndActive(organizationJsonObject)) break;
                            }
                        }

                        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, temporaryOrganizationName);
                        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, organizationUuid);

                        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, temporaryOrganizationName));
                        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, organizationUuid));

                        return Single.just(new JsonObject().put("username", creds.getString("username")).put("sessionid", routingContext.session().id())
                                .put("principalid", userUUID).put("organizationid", organizationUuid).put("organizationname", temporaryOrganizationName));
                    }

                });
    }

    private boolean isOrganizationUndeleletedAndActive(JsonObject organizationJsonObject) throws UnsupportedEncodingException {
        if ((!organizationJsonObject.getBoolean("isdeleted")) && (organizationJsonObject.getBoolean("isactive"))) {
            //Url Encode for cookie compliance
            organizationUuid = URLEncoder.encode(organizationJsonObject.getString(STR_UUID), StandardCharsets.UTF_8.toString());
            temporaryOrganizationName = URLEncoder.encode(organizationJsonObject.getString("name"), StandardCharsets.UTF_8.toString());
            return true;
        }
        return false;
    }

    public Single<JsonObject> logout(RoutingContext routingContext) {
        if (LOGGER.isErrorEnabled())
            LOGGER.error("logout invoked for sessionid in path: {}, in header: {}, session context: {}", routingContext.pathParam("sessionid"), routingContext.request().headers().get("Cookie"), routingContext.session().id());

        if (!Objects.equals(routingContext.pathParam("sessionid"), routingContext.session().id()))
            return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));

        String username = routingContext.user().principal().getString("username");
        String sessionid = routingContext.session().id();

        routingContext.user().clearCache();
        routingContext.clearUser();

        routingContext.removeCookie(Constants.AUTH_ABYSS_PORTAL_PRINCIPAL_UUID_COOKIE_NAME);
        routingContext.removeCookie(Constants.AUTH_ABYSS_PORTAL_SESSION_COOKIE_NAME);
        routingContext.removeCookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME);
        routingContext.removeCookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);

        routingContext.removeCookie(Constants.AUTH_ABYSS_GATEWAY_COOKIE_NAME);
        routingContext.session().destroy();

        routingContext.setUser(new User(new SignUpUser("logout." + username)));

        return Single.just(new JsonObject().put("username", username)
                .put("sessionid", sessionid));
    }

    public Single<JsonObject> signup(RoutingContext routingContext, JDBCAuth authProvider) {
        LOGGER.trace("signup invoked");
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject signupForm = requestParameters.body().getJsonObject();

        String firstname = signupForm.getString("firstname");
        String lastname = signupForm.getString("lastname");
        String username = signupForm.getString("username");
        String email = signupForm.getString("email");
        String password = signupForm.getString("password");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String password2 = signupForm.getString("password2");
        Boolean isAgreedToTerms = signupForm.getBoolean("isAgreedToTerms");

        //TODO: OWASP Validate & Truncate the Fields that are going to be stored

        LOGGER.trace("Received firstname:{}", firstname);
        LOGGER.trace("Received lastname:{}", lastname);
        LOGGER.trace("Received user:{}", username);
        LOGGER.trace("Received email:{}", email);
        LOGGER.trace("Received pass:{}", password);
        LOGGER.trace("Received pass2:{}", password2);
        LOGGER.trace("Received isAgreedToTerms:{}", isAgreedToTerms); //TODO: Add to subject model

        if (!isAgreedToTerms) {
            LOGGER.warn("Signing up User with info:[firstname:" + firstname + " lastname:" + lastname + " username:" + username + " email:" + email + "] has not aggreed to terms of use!. Thus rejected for sign up...");
            return Single.error(new Forbidden403Exception("Signing up User with info:[firstname:" + firstname + " lastname:" + lastname + " username:" + username + " email:" + email + "] has not aggreed to terms of use!. Thus rejected for sign up...", true));
        }

        if (password == null || password.isEmpty()) {
            LOGGER.warn("password is null or empty");
            return Single.error(new UnAuthorized401Exception("Please enter Password field"));
        }

        if (password2 == null || password2.isEmpty()) {
            LOGGER.warn("confirmPassword is null or empty");
            return Single.error(new UnAuthorized401Exception("Please enter Confirm Password field"));
        }

        if (!(password.equals(password2))) {
            LOGGER.warn("Password and confirmPassword does not match");
            return Single.error(new UnAuthorized401Exception("Password and Confirm Password does not match"));
        }

        class SignupMetadata {
            private String subjectUUID;
            private AuthenticationInfo authInfo;

            private SubjectService subjectService = new SubjectService(routingContext.vertx());
            private SubjectActivationService subjectActivationService = new SubjectActivationService(routingContext.vertx());

            private SignupMetadata() {
            }
        }

        SignupMetadata signupMetadata = new SignupMetadata();

        return signupMetadata.subjectService.initJDBCClient()
                .flatMap(jdbcClient1 -> signupMetadata.subjectService.findByName(username)
                )
                .flatMap(resultSet -> {
                    LOGGER.trace(resultSet.toJson().encodePrettily());

                    routingContext.setUser(new User(new SignUpUser(username)));

                    if (resultSet.getNumRows() > 0) {
                        LOGGER.trace("user found: {}", resultSet.toJson().encodePrettily());
                        signupMetadata.subjectUUID = resultSet.getRows(true).get(0).getString(STR_UUID);

                        if (resultSet.getRows(true).get(0).getBoolean("isactivated")) {
                            return Single.error(new Forbidden403Exception("Username already exists / Username already taken", true)); // TODO: How to trigger activation mail resend: Option 1 -> If not activated THEN resend activation mail ELSE display error message
                        } else {
                            //TODO: Cancel previous activation - Is it really required.
                            LOGGER.trace("Username already exists but NOT activated, create and send new activation record..."); //Skip user creation
                            return Single.just(resultSet);
                        }
                    } else {
                        LOGGER.trace("user NOT found, creating user and activation records...");

                        String salt = authProvider.generateSalt();
                        String hash = authProvider.computeHash(password, salt);

                        // save user to the database
                        JsonObject userRecord = new JsonObject();
                        userRecord
                                .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                                .put("crudsubjectid", Constants.SYSTEM_USER_UUID)
                                .put("isactivated", Boolean.FALSE)
                                .put("subjecttypeid", Constants.SUBJECT_TYPE_USER)
                                .put("subjectname", username)
                                .put("firstname", firstname)
                                .put("lastname", lastname)
                                .put("displayname", firstname + " " + lastname)
                                .put("email", email)
                                .put("effectivestartdate", Instant.now())
                                .put("effectiveenddate", Instant.now().plus(Config.getInstance().getConfigJsonObject().getInteger(Constants.PASSWORD_EXPIRATION_DAYS), DAYS))
                                .put("password", hash)
                                .put("passwordsalt", salt)
                                .put("picture", "")
                                .put("subjectdirectoryid", Constants.INTERNAL_SUBJECT_DIRECTORY_UUID)
                                .put("islocked", Boolean.FALSE)
                                .put("issandbox", Boolean.FALSE)
                                .put("url", "");

                        return signupMetadata.subjectService.insertAll(new JsonArray().add(userRecord))
                                .flatMap(jsonObjects -> {
                                    if (!jsonObjects.isEmpty() && jsonObjects.get(0).getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                                        return Single.just(new UpdateResult(jsonObjects.size(),
                                                new JsonArray().add(jsonObjects.get(0).getString(STR_UUID))));
                                    } else {
                                        if (!jsonObjects.isEmpty()) {
                                            LOGGER.trace("Signup insert subject error: {}", jsonObjects.get(0));
                                            return Single.error(new RuntimeException(jsonObjects.get(0).getJsonObject(STR_ERROR).encode()));
                                        } else {
                                            return Single.error(new RuntimeException("User could not be created during signup"));
                                        }
                                    }
                                });
                    }
                })
                .flatMap(updateResult -> {
                    if (updateResult instanceof UpdateResult) {
                        signupMetadata.subjectUUID = ((UpdateResult) updateResult).getKeys().getString(0);
                        LOGGER.trace("[{}] user created successfully: {}  | String Key @pos=1 (subjectUUID): {}", ((UpdateResult) updateResult).getUpdated(), ((UpdateResult) updateResult).getKeys().encodePrettily(), signupMetadata.subjectUUID);
                    } else if (updateResult instanceof ResultSet) {
                        LOGGER.trace("[" + ((ResultSet) updateResult).getNumRows() + "] inactive user found: " + ((ResultSet) updateResult).toJson().encodePrettily() + " | subjectUUID:" + signupMetadata.subjectUUID);
                    }

                    //Generate and Persist Activation Token
                    Token tokenGenerator = new Token();
                    try {
                        signupMetadata.authInfo = tokenGenerator.generateToken(Config.getInstance().getConfigJsonObject().getInteger("token.activation.signup.ttl") * Constants.ONE_MINUTE_IN_SECONDS,
                                email,
                                routingContext.vertx().getDelegate());
                        LOGGER.trace("activation token is created successfully: {}", signupMetadata.authInfo.getToken());
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.trace("tokenGenerator.generateToken: {}", e.getLocalizedMessage());
                        return Single.error(new RuntimeException("activation token could not be generated"));
                    }

                    return signupMetadata.subjectActivationService.initJDBCClient();
                })
                .flatMap(jdbcClient1 -> {
                    JsonObject userActivationRecord = new JsonObject();
                    userActivationRecord
                            .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                            .put("crudsubjectid", Constants.SYSTEM_USER_UUID)
                            .put("subjectid", signupMetadata.subjectUUID)
                            .put("expiredate", signupMetadata.authInfo.getExpireDate())
                            .put("token", signupMetadata.authInfo.getToken())
                            .put("tokentype", Constants.ACTIVATION_TOKEN)
                            .put("email", email)
                            .put("nonce", signupMetadata.authInfo.getNonce())
                            .put("userdata", signupMetadata.authInfo.getUserData());

                    return signupMetadata.subjectActivationService.insertAll(new JsonArray().add(userActivationRecord))
                            .flatMap(jsonObjects -> {
                                if (!jsonObjects.isEmpty() && jsonObjects.get(0).getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                                    return Single.just(new UpdateResult(jsonObjects.size(),
                                            new JsonArray().add(jsonObjects.get(0).getString(STR_UUID))));
                                } else {
                                    if (!jsonObjects.isEmpty()) {
                                        ApiSchemaError apiSchemaError = (ApiSchemaError) jsonObjects.get(0).getValue(STR_ERROR);
                                        return Single.error(new RuntimeException(apiSchemaError.getUsermessage()));
                                    } else {
                                        return Single.error(new RuntimeException("User Activation could not be created during signup"));
                                    }
                                }
                            });
                })
                .flatMap(updateResult -> {
                    JsonObject json = new JsonObject();
                    json.put(Constants.EB_MSG_TOKEN, signupMetadata.authInfo.getToken());
                    json.put(Constants.EB_MSG_TO_EMAIL, email);
                    json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.ACTIVATION_TOKEN);
                    json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderActivationMailBody(routingContext,
                            Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_BASE_URL) + Constants.ACTIVATION_PATH + "/?v=" + signupMetadata.authInfo.getToken(),
                            Constants.ACTIVATION_TEXT));

                    LOGGER.trace("User activation mail is rendered successfully");

                    //LOGGER.trace("User activation mail is sent to Mail Verticle over Event Bus");
                    return routingContext.vertx().eventBus().<JsonObject>rxSend(Constants.ABYSS_MAIL_CLIENT, json);
                })
                .flatMap(jsonObjectMessage -> {
                    LOGGER.trace("Activation Mailing Event Bus Result:" + jsonObjectMessage.isSend() + " | Result:" + jsonObjectMessage.body().encodePrettily());

                    return Single.just(new ApiSchemaError()
                            .setCode(HttpResponseStatus.CREATED.code())
                            .setUsermessage("Activation Code is sent to your email address")
                            .setInternalmessage("")
                            .setDetails("Please check spam folder also...")
                            .setRecommendation("Please check spam folder also...")
                            //.setMoreinfo(new URL(""))
                            .toJson());
                });
    }

    public Single<JsonObject> forgotPassword(RoutingContext routingContext) {
        LOGGER.trace("forgotPassword invoked");

        String username = routingContext.getBodyAsJson().getString("username");
        LOGGER.trace("forgotPassword - Received username:" + username);
        //TODO: OWASP Validate

        class ForgotPasswordMetadata {
            private String subjectUUID;
            private String email;
            private String displayName;
            private JsonObject subjectRow;
            private AuthenticationInfo authInfo;

            private SubjectService subjectService = new SubjectService(routingContext.vertx());
            private SubjectActivationService subjectActivationService = new SubjectActivationService(routingContext.vertx());

            private ForgotPasswordMetadata() {
            }
        }

        ForgotPasswordMetadata forgotPasswordMetadata = new ForgotPasswordMetadata();

        return forgotPasswordMetadata.subjectService.initJDBCClient()
                .flatMap(jdbcClient1 -> forgotPasswordMetadata.subjectService
                        .findAll(new ApiFilterQuery()
                                .setFilterQuery(SubjectService.SQL_FIND_BY_NAME_ONLY_NOTDELETED)
                                .setFilterQueryParams(new JsonArray().add(username))
                        )
                )
                .flatMap(resultSet -> {
                    LOGGER.trace(resultSet.toJson().encodePrettily());

                    int numOfRows = resultSet.getNumRows();
                    if (numOfRows == 0) {
                        LOGGER.error("forgotPassword - username NOT found...");
                        return Single.error(new Exception("Username not found in our records"));
                    } else if (numOfRows == 1) {
                        JsonObject row = resultSet.getRows(true).get(0);
                        if (!row.getBoolean("isactivated") && row.getInstant("created").equals(row.getInstant("updated"))) { //TODO ???
                            LOGGER.error("forgotPassword - account connected to username is NOT activated");
                            return Single.error(new Exception("Please activate your account by clicking the link inside activation mail."));
                        } else {
                            forgotPasswordMetadata.subjectUUID = row.getString(STR_UUID);
                            forgotPasswordMetadata.email = row.getString("email");
                            forgotPasswordMetadata.displayName = row.getString("displayname");
                            forgotPasswordMetadata.subjectRow = row;
                            LOGGER.trace("forgotPassword - Activated or old account found:[" + forgotPasswordMetadata.subjectUUID + "]. Email:[" + forgotPasswordMetadata.email + "] Display Name:[" + forgotPasswordMetadata.displayName + "] Reset password token is going to be created...");

                            //Generate and Persist Reset Password Token
                            Token tokenGenerator = new Token();
                            try {
                                forgotPasswordMetadata.authInfo = tokenGenerator.generateToken(Config.getInstance().getConfigJsonObject().getInteger("token.activation.renewal.password.ttl") * Constants.ONE_MINUTE_IN_SECONDS,
                                        username,
                                        routingContext.vertx().getDelegate());
                                LOGGER.trace("forgotPassword - Reset Password token is created successfully: " + forgotPasswordMetadata.authInfo.getToken());
                            } catch (UnsupportedEncodingException e) {
                                LOGGER.error("forgotPassword - Reset Password tokenGenerator.generateToken :" + e.getLocalizedMessage());
                                return Single.error(new Exception("Reset Password token could not be generated"));
                            }

                            return forgotPasswordMetadata.subjectActivationService.initJDBCClient();
                        }
                    } else {
                        LOGGER.error("forgotPassword - email is connected to multiple accounts [" + numOfRows + "]");
                        return Single.error(new Exception("This email is connected to multiple accounts. Please correct the other accounts by getting help from administration of your organization and try again."));
                    }
                })
                .flatMap(jdbcClient1 -> {
                    JsonObject resetPasswordRecord = new JsonObject();
                    resetPasswordRecord
                            .put("organizationid", Constants.DEFAULT_ORGANIZATION_UUID)
                            .put("crudsubjectid", Constants.SYSTEM_USER_UUID)
                            .put("subjectid", forgotPasswordMetadata.subjectUUID)
                            .put("expiredate", forgotPasswordMetadata.authInfo.getExpireDate())
                            .put("token", forgotPasswordMetadata.authInfo.getToken())
                            .put("tokentype", Constants.RESET_PASSWORD_TOKEN)
                            .put("email", forgotPasswordMetadata.email)
                            .put("nonce", forgotPasswordMetadata.authInfo.getNonce())
                            .put("userdata", forgotPasswordMetadata.authInfo.getUserData());

                    return forgotPasswordMetadata.subjectActivationService.insertAll(new JsonArray().add(resetPasswordRecord))
                            .flatMap(jsonObjects -> {
                                if (!jsonObjects.isEmpty() && jsonObjects.get(0).getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                                    return Single.just(new UpdateResult(jsonObjects.size(),
                                            new JsonArray().add(jsonObjects.get(0).getString(STR_UUID))));
                                } else {
                                    if (!jsonObjects.isEmpty()) {
                                        ApiSchemaError apiSchemaError = (ApiSchemaError) jsonObjects.get(0).getValue(STR_ERROR);
                                        LOGGER.trace("forgotPassword - " + apiSchemaError.getUsermessage());
                                        return Single.error(new RuntimeException(apiSchemaError.getUsermessage()));
                                    } else {
                                        LOGGER.trace("forgotPassword - Password Reset Token could not be created during forgot password");
                                        return Single.error(new RuntimeException("Password Reset Token could not be created during forgot password"));
                                    }
                                }
                            });
                })
                .flatMap(updateResult -> {
                    LOGGER.trace("forgotPassword - Deactivating Subject with id:[" + forgotPasswordMetadata.subjectUUID + "] -> " + updateResult.getKeys().encodePrettily());
                    if (updateResult.getUpdated() == 1) {
                        JsonObject updateJson = new JsonObject()
                                .put("organizationid", forgotPasswordMetadata.subjectRow.getString("organizationid"))
                                .put("crudsubjectid", Constants.SYSTEM_USER_UUID)
                                .put("isactivated", Boolean.FALSE)
                                .put("subjecttypeid", forgotPasswordMetadata.subjectRow.getString("subjecttypeid"))
                                .put("subjectname", forgotPasswordMetadata.subjectRow.getString("subjectname"))
                                .put("firstname", forgotPasswordMetadata.subjectRow.getString("firstname"))
                                .put("lastname", forgotPasswordMetadata.subjectRow.getString("lastname"))
                                .put("displayname", forgotPasswordMetadata.subjectRow.getString("displayname"))
                                .put("email", forgotPasswordMetadata.subjectRow.getString("email"))
                                .put("secondaryemail", forgotPasswordMetadata.subjectRow.getString("secondaryemail"))
                                .put("effectivestartdate", forgotPasswordMetadata.subjectRow.getInstant("effectivestartdate"))
                                .put("effectiveenddate", forgotPasswordMetadata.subjectRow.getInstant("effectiveenddate"))
                                .put("picture", forgotPasswordMetadata.subjectRow.getValue("picture"))
                                .put("subjectdirectoryid", forgotPasswordMetadata.subjectRow.getString("subjectdirectoryid"))
                                .put("islocked", forgotPasswordMetadata.subjectRow.getBoolean("islocked"))
                                .put("issandbox", forgotPasswordMetadata.subjectRow.getBoolean("issandbox"))
                                .put("url", forgotPasswordMetadata.subjectRow.getString("url"));

                        return forgotPasswordMetadata.subjectService.update(UUID.fromString(forgotPasswordMetadata.subjectUUID), updateJson)
                                .flatMap(compositeResult -> {
                                    if (compositeResult.getThrowable() == null) {
                                        LOGGER.trace("forgotPassword - subject {} deactivated", forgotPasswordMetadata.subjectUUID);
                                        return Single.just(compositeResult.getUpdateResult());
                                    } else {
                                        LOGGER.error("forgotPassword - subject update error {}", compositeResult.getThrowable());
                                        return Single.error(compositeResult.getThrowable());
                                    }
                                });
                    } else {
                        LOGGER.error("forgotPassword - Activation Insert Error Occurred");
                        return Single.error(new Exception("forgotPassword - Activation Insert Error Occurred"));
                    }
                })
                .flatMap(updateResult -> {
                    if (updateResult.getUpdated() == 1) {
                        LOGGER.trace("forgotPassword - Subject Activation Update Result information:" + updateResult.getKeys().encodePrettily());
                        return Single.just(updateResult);
                    } else {
                        LOGGER.error("forgotPassword - Activation Update Error Occurred");
                        return Single.error(new Exception("Activation Update Error Occurred"));
                    }
                })
                .flatMap(updateResult -> {
                    LOGGER.trace("Rendering Forgot Password mail");
                    JsonObject json = new JsonObject();
                    json.put(Constants.EB_MSG_TOKEN, forgotPasswordMetadata.authInfo.getToken());
                    json.put(Constants.EB_MSG_TO_EMAIL, forgotPasswordMetadata.email);
                    json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.RESET_PASSWORD_TOKEN);
                    json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderActivationMailBody(routingContext,
                            Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_BASE_URL) + Constants.RESET_PASSWORD_PATH + "/?v=" + forgotPasswordMetadata.authInfo.getToken(),
                            Constants.RESET_PASSWORD_TEXT));

                    LOGGER.trace("Forgot Password mail is rendered successfully");

                    //LOGGER.trace("User activation mail is sent to Mail Verticle over Event Bus");
                    return routingContext.vertx().eventBus().<JsonObject>rxSend(Constants.ABYSS_MAIL_CLIENT, json);
                })
                .flatMap(jsonObjectMessage -> {
                    LOGGER.trace("Forgot Password Mailing Event Bus Result:" + jsonObjectMessage.isSend() + " | Result:" + jsonObjectMessage.body().encodePrettily());

                    return Single.just(new ApiSchemaError()
                            .setCode(HttpResponseStatus.OK.code())
                            .setUsermessage("Reset Password Code is sent to your email address!")
                            .setInternalmessage("")
                            .setDetails("Please check spam folder also...")
                            .setRecommendation("Please click the link inside the mail")
                            //.setMoreinfo(new URL(""))
                            .toJson());
                })
                .onErrorResumeNext(Single.just(new ApiSchemaError()
                        .setCode(HttpResponseStatus.OK.code())
                        .setUsermessage("Reset Password Code is sent to your email address!")
                        .setInternalmessage("")
                        .setDetails("Please check spam folder also...")
                        .setRecommendation("Please click the link inside the mail")
                        //.setMoreinfo(new URL(""))
                        .toJson()));
    }

    public Single<JsonObject> checkResetPasswordToken(RoutingContext routingContext) {
        return Single.just(new JsonObject());
    }

    public Single<JsonObject> resetPassword(RoutingContext routingContext) {
        return Single.just(new JsonObject());
    }

    public Single<JsonObject> setCurrentOrganization(RoutingContext routingContext) {
        LOGGER.trace("setCurrentOrganization invoked");
        try {
            organizationUuid = URLEncoder.encode(routingContext.getBodyAsJson().getString("organizationid"), StandardCharsets.UTF_8.toString());
            temporaryOrganizationName = URLEncoder.encode(routingContext.getBodyAsJson().getString("organizationname"), StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException e) {

        }
        LOGGER.trace("setCurrentOrganization - Received organizationId:" + organizationUuid + " organizationName:" + temporaryOrganizationName);

        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, temporaryOrganizationName);
        routingContext.session().put(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, organizationUuid);

        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_NAME_COOKIE_NAME, temporaryOrganizationName));
        routingContext.addCookie(Cookie.cookie(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME, organizationUuid));

        String userName = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_NAME_SESSION_VARIABLE_NAME);
        String userId = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        return Single.just(new JsonObject()
                .put("username", userName)
                .put("sessionid", routingContext.session().id())
                .put("principalid", userId)
                .put("organizationid", organizationUuid)
                .put("organizationname", temporaryOrganizationName)
        );
    }

    public Single<JsonObject> inviteUser(RoutingContext routingContext, JDBCAuth jdbcAuth) {
        LOGGER.trace("inviteUser invoked");
        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject inviteUserForm = requestParameters.body().getJsonObject();

        String email = inviteUserForm.getString("email");
        String message = inviteUserForm.getString("message");
        Boolean hasConsentToShare = inviteUserForm.getBoolean("hasConsentToShare");

        //TODO: OWASP Validate & Truncate the Fields that are going to be stored

        LOGGER.trace("Received email:" + email);
        LOGGER.trace("Received message:" + message);
        LOGGER.trace("Received hasConsentToShare:" + hasConsentToShare);


        JsonObject json = new JsonObject();
        json.put(Constants.EB_MSG_TOKEN, ""); //inviteUserMetadata.authInfo.getToken()); //TODO: referral token
        json.put(Constants.EB_MSG_TO_EMAIL, email);
        json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.INVITE_USER_TOKEN);
        json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderInviteUserMailBody(routingContext,
                Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_SIGNUP_URL),
                Constants.INVITE_USER_TEXT));

        LOGGER.trace("User invitation mail is rendered successfully");

        return routingContext.vertx().eventBus().<JsonObject>rxSend(Constants.ABYSS_MAIL_CLIENT, json)
                .flatMap(jsonObjectMessage -> {
                    LOGGER.trace("Invitation Mailing Event Bus Result:" + jsonObjectMessage.isSend() + " | Result:" + jsonObjectMessage.body().encodePrettily());

                    return Single.just(new ApiSchemaError()
                            .setCode(HttpResponseStatus.CREATED.code())
                            .setUsermessage("Invitation Mail is sent to your friend's email address")
                            .setInternalmessage("")
                            .setDetails("Thank you")
                            .setRecommendation("")
                            //.setMoreinfo(new URL(""))
                            .toJson());
                });
    }

    private Single<JsonObject> rxValidateToken(String token) {
        LOGGER.trace("rxValidateToken invoked");

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
                        LOGGER.error("rxValidateToken error: {}", authResult.getResultText());
                        return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                    }
                })
                .flatMap(ratResultSet -> {
                    SubjectPermissionService subjectPermissionService = new SubjectPermissionService(vertx);
                    return subjectPermissionService.initJDBCClient()
                            .flatMap(jdbcClient -> subjectPermissionService.findById(UUID.fromString(ratResultSet.getRows().get(0).getString("subjectpermissionid"))))
                            .flatMap(spResultSet -> {
                                if (spResultSet.getNumRows() == 0) {
                                    LOGGER.error("rxValidateToken error: {} subjectpermissionid:{}", "no row for subject permission", ratResultSet.getRows().get(0).getString("subjectpermissionid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }

                                return Single.just(new JsonObject()
                                        .put("accesstokenisdeleted", ratResultSet.getRows().get(0).getBoolean("isdeleted"))
                                        .put("accesstokenisactive", ratResultSet.getRows().get(0).getBoolean("isactive"))
                                        .put("accesstokenexpiredate", ratResultSet.getRows().get(0).getInstant("expiredate"))
                                        .put("subjectpermissionid", spResultSet.getRows().get(0).getString(STR_UUID))
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
                                                LOGGER.error("rxValidateToken error: {} resourceid:{}", "no row for resource", jsonObject.getString("resourceid"));
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
                                                LOGGER.error("rxValidateToken error: {} resourceactionid:{}", "no row for resource action", jsonObject.getString("resourceactionid"));
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
                                                    LOGGER.error("rxValidateToken error: {} uuid:{}", "no row for proxy api", jsonObject.getString("resourcerefid"));
                                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                                } else {
                                                    return Single.just(jsonObject.put("apiuuid", apiResultSet.getRows().get(0).getString(STR_UUID))
                                                            .put("apiisproxyapi", apiResultSet.getRows().get(0).getBoolean("isproxyapi"))
                                                            .put("apiissandbox", apiResultSet.getRows().get(0).getBoolean("issandbox"))
                                                            .put("apiislive", apiResultSet.getRows().get(0).getBoolean("islive"))
                                                            .put("apiisdeleted", apiResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                            .put("businessapiid", apiResultSet.getRows().get(0).getString("businessapiid"))
                                                    );
                                                }
                                            });
                                } else {
                                    LOGGER.error("rxValidateToken error: {} {}", "undefined resource type", jsonObject.getString("resourcetypeid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }
                            })
                            .flatMap(jsonObject -> {
                                ApiService apiService = new ApiService(vertx);
                                return apiService.initJDBCClient()
                                        .flatMap(jdbcClient -> apiService.findById(UUID.fromString(jsonObject.getString("businessapiid"))))
                                        .flatMap(apiResultSet -> {
                                            if (apiResultSet.getNumRows() == 0) {
                                                LOGGER.error("rxValidateToken error: {} uuid:{}", "no row for business api", jsonObject.getString("businessapiid"));
                                                return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                            } else {
                                                return Single.just(jsonObject.put("businessapiuuid", apiResultSet.getRows().get(0).getString(STR_UUID))
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
                                                    LOGGER.error("rxValidateToken error: {} subjectpermissionid:{}", "no row for contract", jsonObject.getString("subjectpermissionid"));
                                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                                } else {
                                                    return Single.just(jsonObject
                                                            .put("contractisdeleted", contractResultSet.getRows().get(0).getBoolean("isdeleted"))
                                                            .put("contractsubjectid", contractResultSet.getRows().get(0).getString("subjectid"))
                                                            .put("contractenvironment", contractResultSet.getRows().get(0).getString("environment"))
                                                            .put("contractstateid", contractResultSet.getRows().get(0).getString("contractstateid"))
                                                            .put("contractstatus", contractResultSet.getRows().get(0).getString(STR_STATUS))
                                                    );
                                                }
                                            });
                                } else {
                                    LOGGER.error("rxValidateToken error: {} {}", "undefined resource type", jsonObject.getString("resourcetypeid"));
                                    return Single.error(new UnAuthorized401Exception(HttpResponseStatus.UNAUTHORIZED.reasonPhrase()));
                                }
                            });

                });
    }

    public Single<JsonObject> validateAccessToken(String token) {
        LOGGER.trace("validateAccessToken invoked");
        JsonObject validationStatus = new JsonObject().put(STR_STATUS, Boolean.FALSE).put(STR_ERROR, "").put("validationreport", new JsonObject());

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

                    return Single.just(validationStatus.put(STR_STATUS, Boolean.TRUE));
                });
        //.doAfterSuccess(validateAccessTokenStatus -> validationStatus.put(STR_STATUS, true).put(STR_ERROR, "").put("validationreport", validateAccessTokenStatus))
/*
                .doOnError(throwable -> {
                            Single.error(throwable);
                            //return Single.just(validationStatus.put(STR_STATUS, false).put(STR_ERROR, throwable.getLocalizedMessage()));
                        }
                );
*/
/*
                .subscribe(validateAccessTokenStatus -> {
                            validationStatus.put(STR_STATUS, true).put(STR_ERROR, "").put("validationreport", validateAccessTokenStatus);
                        }
                        , throwable -> {
                            validationStatus.put(STR_STATUS, false).put(STR_ERROR, throwable.getLocalizedMessage());
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

    class SignUpUser implements io.vertx.ext.auth.User {

        private String username;
        private JsonObject principal;

        SignUpUser(String username) {
            this.username = username;
        }

        @Override
        public io.vertx.ext.auth.User isAuthorized(String authority, Handler<AsyncResult<Boolean>> resultHandler) {
            return null;
        }

        @Override
        public io.vertx.ext.auth.User clearCache() {
            return null;
        }

        @Override
        public JsonObject principal() {
            if (principal == null) {
                principal = new JsonObject().put("username", username);
            }
            return principal;
        }

        @Override
        public void setAuthProvider(AuthProvider authProvider) {

        }
    }
}
