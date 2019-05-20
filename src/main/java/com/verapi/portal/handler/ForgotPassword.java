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

package com.verapi.portal.handler;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;


/**
 * Reset password using activation mail sent to selected email
 * TODO: same email may be connected to multiple usernames/accounts???
 */
public class ForgotPassword extends PortalHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ForgotPassword.class);

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    private Integer subjectId;
    private String email;

    private String authToken;

    public ForgotPassword(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }


    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.info("ForgotPasswordPortalController.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        LOGGER.info("Received username:" + username);


        //TODO: OWASP Email Validate


        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM portalschema.SUBJECT WHERE SUBJECT_NAME = ?", new JsonArray().add(username)))
                        .flatMap(resultSet -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                LOGGER.info("username NOT found...");
                                return Single.error(new Exception("Username not found in our records"));
                            } else if (numOfRows == 1) {
                                if (!resultSet.getRows(true).get(0).getBoolean("is_activated")) {
                                    LOGGER.info("account connected to username is NOT activated");
                                    return Single.error(new Exception("Please activate your account by clicking the link inside activation mail."));
                                } else {
                                    subjectId = resultSet.getRows(true).get(0).getInteger("id");
                                    email = resultSet.getRows(true).get(0).getString("email");
                                    LOGGER.info("Activated account found:[" + subjectId + "]. Email:[" + email + "]Reset password token is going to be created...");


                                    //Generate and Persist Reset Password Token
                                    Token tokenGenerator = new Token();
                                    AuthenticationInfo authInfo;
                                    try {
                                        authInfo = tokenGenerator.generateToken(Config.getInstance().getConfigJsonObject().getInteger("token.activation.renewal.password.ttl") * Constants.ONE_MINUTE_IN_SECONDS,
                                                username,
                                                routingContext.vertx().getDelegate());
                                        LOGGER.info("Reset Password: token is created successfully: " + authInfo.getToken());
                                        authToken = authInfo.getToken();
                                    } catch (UnsupportedEncodingException e) {
                                        LOGGER.error("Reset Password: tokenGenerator.generateToken :" + e.getLocalizedMessage());
                                        return Single.error(new Exception("Reset Password: token could not be generated"));
                                    }
                                    return resConn.rxUpdateWithParams("INSERT INTO portalschema.subject_activation (" +
                                                    "organization_id," +
                                                    "crud_subject_id," +
                                                    "subject_id," +
                                                    "expire_date," +
                                                    "token," +
                                                    "token_type, " +
                                                    "email," +
                                                    "nonce," +
                                                    "user_data) " +
                                                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                            new JsonArray()
                                                    .add(0)
                                                    .add(1)
                                                    .add(subjectId)
                                                    .add(authInfo.getExpireDate())
                                                    .add(authInfo.getToken())
                                                    .add(Constants.ACTIVATION_TOKEN)
                                                    .add(email)
                                                    .add(authInfo.getNonce())
                                                    .add(authInfo.getUserData())
                                    );
                                }
                            } else {
                                LOGGER.info("email is connected to multiple accounts [" + numOfRows + "]");
                                return Single.error(new Exception("This email is connected to multiple accounts. Please correct the other accounts by getting help from administration of your organization and try again."));
                            }
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> resConn.rxCommit().toSingleDefault(true))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess(succ -> {
                            LOGGER.info("Reset password token is created and persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, authToken);
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.RESET_PASSWORD_TOKEN);

                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, result -> {
                                LOGGER.info(result.toString());
                            });

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    LOGGER.info("Subscription to Forgot Password successfull:" + result);
                    generateResponse(routingContext, LOGGER, 200, "Reset Password Code is sent to your email address.", "Please check spam folder also...", "Please click the link inside the mail.", "");
                    //TODO: Send email to user
                }, t -> {
                    LOGGER.error("Forgot Password Error", t);
                    generateResponse(routingContext, LOGGER, 401, "Error in Forgot Password Occured", t.getLocalizedMessage(), "", "");
                }
        );


    }


    public void pageRender(RoutingContext routingContext) {
        LOGGER.info("ForgotPasswordPortalController.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // we define a hardcoded title for our application
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_FORGOT_PASSWORD, res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

}
