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

package com.verapi.portal.controller;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.MailUtil;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.AsyncResult;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

@AbyssController(routePathGET = "forgot-password", routePathPOST = "forgot-password", htmlTemplateFile = "forgot-password.html", isPublic = true)
public class ForgotPasswordPortalController extends AbstractPortalController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPasswordPortalController.class);

    private Integer subjectId;
    private String subjectUUID;
    private String email;
    private String authToken;

    public ForgotPasswordPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        LOGGER.trace("ForgotPasswordPortalController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.trace("ForgotPasswordPortalController.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        LOGGER.trace("Received username: {}", username);


        //TODO: OWASP Email Validate


        jdbcClient.rxGetConnection().flatMap((SQLConnection resConn) ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(Boolean.FALSE)
                        //Check if user already exists
                        .flatMap((Boolean resQ) -> resConn.rxQueryWithParams("SELECT * FROM subject WHERE subjectName = ? and isDeleted = false"
                                , new JsonArray().add(username)))
                        .flatMap((ResultSet resultSet) -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                LOGGER.error("username NOT found...");
                                return Single.error(new Exception("Username not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                if (!row.getBoolean("isActivated")) {
                                    LOGGER.error("account connected to username is NOT activated");
                                    return Single.error(new Exception("Please activate your account by clicking the link inside activation mail."));
                                } else {
                                    subjectId = row.getInteger("id");
                                    subjectUUID = row.getString("uuid");
                                    email = row.getString("email");
                                    String displayName = row.getString("display_name");
                                    LOGGER.info("Activated account found:[{}({})]. Email:[{}] Display Name:[{}] Reset password token is going to be created..."
                                            , subjectId, subjectUUID, email, displayName);

                                    //Generate and Persist Reset Password Token
                                    Token tokenGenerator = new Token();
                                    AuthenticationInfo authInfo;
                                    try {
                                        authInfo = tokenGenerator
                                                .generateToken(Config
                                                                .getInstance()
                                                                .getConfigJsonObject()
                                                                .getInteger("token.activation.renewal.password.ttl") * (long) Constants.ONE_MINUTE_IN_SECONDS,
                                                        username,
                                                        routingContext.vertx().getDelegate());
                                        LOGGER.trace("Reset Password: token is created successfully: {}", authInfo.getToken());
                                        authToken = authInfo.getToken();
                                    } catch (UnsupportedEncodingException e) {
                                        LOGGER.error("Reset Password: tokenGenerator.generateToken: {}", e.getLocalizedMessage());
                                        return Single.error(new Exception("Reset Password: token could not be generated"));
                                    }
                                    return resConn.rxUpdateWithParams("INSERT INTO subject_activation (" +
                                                    "organizationId," +
                                                    "crudSubjectId," +
                                                    "subjectId," +
                                                    "expireDate," +
                                                    "token," +
                                                    "tokenType, " +
                                                    "email," +
                                                    "nonce," +
                                                    "userData) " +
                                                    "VALUES (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, ?, ?)",
                                            new JsonArray()
                                                    .add(Constants.DEFAULT_ORGANIZATION_UUID)
                                                    .add(Constants.SYSTEM_USER_UUID)
                                                    .add(subjectUUID)
                                                    .add(authInfo.getExpireDate())
                                                    .add(authInfo.getToken())
                                                    .add(Constants.RESET_PASSWORD_TOKEN)
                                                    .add(email)
                                                    .add(authInfo.getNonce())
                                                    .add(authInfo.getUserData())
                                    );
                                }
                            } else {
                                LOGGER.error("email is connected to multiple accounts [{}]", numOfRows);
                                return Single.error(new Exception("This email is connected to multiple accounts. " +
                                        "Please correct the other accounts by getting help from administration of your organization and try again."));
                            }
                        })
                        .flatMap((UpdateResult updateResult) -> {
                            LOGGER.trace("ForgotPasswordPortalController - Deactivating Subject with id:[{}({})] -> {}"
                                    , subjectId, subjectUUID, updateResult.getKeys().encodePrettily());
                            if (updateResult.getUpdated() == 1) {

                                return resConn.rxUpdateWithParams("UPDATE subject SET " +
                                                "updated = now()," +
                                                "crudSubjectId = CAST(? AS uuid)," +
                                                "isActivated = false" +
                                                " WHERE " +
                                                "id = ?;",
                                        new JsonArray()
                                                .add(Constants.SYSTEM_USER_UUID)
                                                .add(subjectId));
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }

                        })
                        // commit if all succeeded
                        .flatMap((UpdateResult updateResult) -> {
                            if (updateResult.getUpdated() == 1) {
                                LOGGER.trace("Activate Account - Subject Activation Update Result information: {}", updateResult.getKeys().encodePrettily());
                                return resConn.rxCommit().toSingleDefault(Boolean.TRUE);
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }
                        })
                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(Boolean.TRUE)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess((Boolean succ) -> {
                            LOGGER.trace("Reset password token is created and persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, authToken);
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.RESET_PASSWORD_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING,
                                    MailUtil.renderForgotPasswordMailBody(routingContext,
                                            Config
                                                    .getInstance()
                                                    .getConfigJsonObject()
                                                    .getString(Constants.MAIL_BASE_URL) + Constants.RESET_PASSWORD_PATH + "/?v=" + authToken,
                                            Constants.RESET_PASSWORD_TEXT));

                            LOGGER.trace("Forgot Password mail is rendered successfully");
                            routingContext.vertx().getDelegate().eventBus()
                                    .<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, (AsyncResult<Message<JsonObject>> result) -> {
                                        if (result.succeeded()) {
                                            LOGGER.trace("Forgot Password Mailing Event Bus Result: {} | Result: {}"
                                                    , result, result.result().body().encodePrettily());
                                        } else {
                                            LOGGER.error("Forgot Password Mailing Event Bus Result: {} | Cause: {}", result, result.cause().getMessage());
                                        }

                                    });
                            LOGGER.trace("Forgot Password mail is sent to Mail Verticle over Event Bus");

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe((Boolean result) -> {
                    LOGGER.info("Subscription to Forgot Password successfull: {}", result);
                    showTrxResult(routingContext, LOGGER, HttpStatus.SC_OK
                            , "Reset Password Code is sent to your email address!"
                            , "Please check spam folder also...", "Please click the link inside the mail");
                }, (Throwable t) -> {
                    LOGGER.error("Forgot Password Error", t);
                    //Due to OWASP regulations same output should be given even if error occured.
                    showTrxResult(routingContext, LOGGER, HttpStatus.SC_OK
                            , "Reset Password Code is sent to your email address!"
                            , "Please check spam folder also...", "Please click the link inside the mail");
                    //showTrxResult(routingContext, LOGGER, 401, "Error in Forgot Password Occured!", t.getLocalizedMessage(), "");
                }
        );


    }
}
