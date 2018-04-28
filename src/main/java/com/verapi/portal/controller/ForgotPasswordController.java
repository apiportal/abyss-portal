/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.portal.controller;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.common.MailUtil;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

@AbyssController(routePathGET = "forgot-password", routePathPOST = "forgot-password", htmlTemplateFile = "forgot-password.html", isPublic = true)
public class ForgotPasswordController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(ForgotPasswordController.class);

    private Integer subjectId;
    private String email;
    private String displayName;
    private String authToken;

    public ForgotPasswordController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("ForgotPasswordController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("ForgotPasswordController.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        logger.info("Received username:" + username);


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
                                logger.info("username NOT found...");
                                return Single.error(new Exception("Username not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                if (row.getInteger("is_activated") == 0) {
                                    logger.info("account connected to username is NOT activated");
                                    return Single.error(new Exception("Please activate your account by clicking the link inside activation mail."));
                                } else {
                                    subjectId = row.getInteger("id");
                                    email = row.getString("email");
                                    displayName = row.getString("display_name");
                                    logger.info("Activated account found:[" + subjectId + "]. Email:[" + email + "]Reset password token is going to be created...");

                                    //Generate and Persist Reset Password Token
                                    Token tokenGenerator = new Token();
                                    AuthenticationInfo authInfo;
                                    try {
                                        authInfo = tokenGenerator.generateToken(Config.getInstance().getConfigJsonObject().getInteger("quarter.hour.in.seconds"), username, routingContext.vertx().getDelegate());
                                        logger.info("Reset Password: token is created successfully: " + authInfo.getToken());
                                        authToken = authInfo.getToken();
                                    } catch (UnsupportedEncodingException e) {
                                        logger.error("Reset Password: tokenGenerator.generateToken :" + e.getLocalizedMessage());
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
                                                    .add(Constants.RESET_PASSWORD_TOKEN)
                                                    .add(email)
                                                    .add(authInfo.getNonce())
                                                    .add(authInfo.getUserData())
                                    );
                                }
                            } else {
                                logger.info("email is connected to multiple accounts [" + numOfRows + "]");
                                return Single.error(new Exception("This email is connected to multiple accounts. Please correct the other accounts by getting help from administration of your organization and try again."));
                            }
                        })
                        .flatMap(updateResult -> {
                            logger.info("ForgotPasswordController - Deactivating Subject with id:[" + subjectId + "] -> " + updateResult.getKeys().encodePrettily());
                            if (updateResult.getUpdated() == 1) {

                                return resConn.rxUpdateWithParams("UPDATE portalschema.subject SET " +
                                                "updated = now()," +
                                                "crud_subject_id = ?," +
                                                "is_activated = 0" +
                                                " WHERE " +
                                                "id = ?;",
                                        new JsonArray()
                                                .add(1)
                                                .add(subjectId));
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }

                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> {
                            if (updateResult.getUpdated() == 1) {
                                logger.info("Activate Account - Subject Activation Update Result information:" + updateResult.getKeys().encodePrettily());
                                return resConn.rxCommit().toSingleDefault(true);
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }
                        })
                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess(succ -> {
                            logger.info("Reset password token is created and persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, authToken);
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.RESET_PASSWORD_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderForgotPasswordMailBody(routingContext,
                                    Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_BASE_URL) + Constants.RESET_PASSWORD_PATH + "/?v=" + authToken,
                                    Constants.RESET_PASSWORD_TEXT));

                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, result -> {
                                logger.info(result.toString());
                            });

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to Forgot Password successfull:" + result);
                    showTrxResult(routingContext, logger, 200, "Reset Password Code is sent to your email address!", "Please check spam folder also...", "Please click the link inside the mail");
                }, t -> {
                    logger.error("Forgot Password Error", t);
                    showTrxResult(routingContext, logger, 401, "Error in Forgot Password Occured!", t.getLocalizedMessage(), "");
                }
        );


    }
}
