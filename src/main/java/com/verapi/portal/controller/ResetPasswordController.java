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

@AbyssController(routePathGET = "reset-password", routePathPOST = "reset-password", htmlTemplateFile = "reset-password.html", isPublic = true)
public class ResetPasswordController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(ResetPasswordController.class);

    private Integer tokenId;
    private String subjectId;
    private String email;
    private String displayName;

    public ResetPasswordController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.trace("ResetPasswordController.defaultGetHandler invoked...");

        String token = routingContext.request().getParam("v");
        logger.trace("Received token:" + token);

        String path = routingContext.normalisedPath();
        logger.trace("Received path:" + path);

        //Get Stored Token Info
        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT A.*, S.displayName FROM subject_activation A, subject S WHERE TOKEN = ? and A.subjectId = S.uuid", new JsonArray().add(token)))
                        .flatMap(resultSet -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                logger.error("token NOT found...");
                                return Single.error(new Exception("Token not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                logger.trace("Token found:" + row.encodePrettily());

                                if (row.getBoolean("isDeleted")) {
                                    logger.error("Received Token is deleted");
                                    return Single.error(new Exception("Token does not exist in our records. Please request a new token.")); //TODO: Give "User already activated" message if Subject is activated
                                }

                                if (!(row.getString("tokenType", "").equals(Constants.RESET_PASSWORD_TOKEN))) {
                                    logger.error("Received Token Type does not match: " + row.getString("tokenType", "NULL"));
                                    return Single.error(new Exception("Right token does not exist in our records. Please request a new token."));
                                }

                                tokenId = row.getInteger("id");

                                AuthenticationInfo authInfo = new AuthenticationInfo(
                                        row.getString("token"),
                                        row.getString("nonce"),
                                        row.getInstant("expireDate"),
                                        row.getString("userData"));

                                Token tokenValidator = new Token();

                                AuthenticationInfo authResult = tokenValidator.validateToken(token, authInfo);

                                if (authResult.isValid()) {
                                    logger.trace("Received Token is valid.");

                                    email = row.getString("email");
                                    displayName = row.getString("displayName");

                                    return Single.just(row);
                                } else {
                                    logger.error("Received Token is NOT valid: " + authResult.getResultText());
                                    return Single.error(new Exception("Token is not valid. Please request a new token."));
                                }

                            } else {
                                logger.error("Multiple tokens found...");
                                return Single.error(new Exception("Valid token is not found in our records"));
                            }
                        })
                        .doAfterSuccess(succ -> {
                            logger.info("ResetPasswordController Get: Reset Password Token is validated.");
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
            logger.info("Subscription to ResetPasswordController Get successful:" + result);
            //showTrxResult(routingContext, logger, 200, "Your password has been successfully reset!", "Welcome to API Portal again", "");
            routingContext.session().put(Constants.RESET_PASSWORD_TOKEN, token);
            renderTemplate(routingContext, Controllers.RESET_PASSWORD.templateFileName);
        }, t -> {
            logger.error("ResetPasswordController Get -  Error", t);
            showTrxResult(routingContext, logger, 401, "Reset Password Failed!", t.getLocalizedMessage(), "");
        });
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.trace("ResetPasswordController.handle invoked..");

        String token = routingContext.session().get(Constants.RESET_PASSWORD_TOKEN);
        String newPassword = routingContext.request().getFormAttribute("newPassword");
        String confirmPassword = routingContext.request().getFormAttribute("confirmPassword");

        if (newPassword == null || newPassword.isEmpty() || confirmPassword == null || confirmPassword.isEmpty()) {
            logger.error("ResetPasswordController - Received new / confirm Password is null or empty");
            showTrxResult(routingContext, logger, 403, "Please enter valid new & confirm password fields!", "", "");
        }

        if (!newPassword.equals(confirmPassword)) {
            logger.error("ResetPasswordController - Passwords does NOT match!");
            showTrxResult(routingContext, logger, 403, "Please enter SAME new & confirm password!", "", "");
        }

        logger.trace("Received token:" + token);

        String path = routingContext.normalisedPath();
        logger.trace("Received path:" + path);

        //Get Stored Token Info
        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT A.*, S.displayName FROM subject_activation A, subject S WHERE token = ? and A.subjectId = S.uuid", new JsonArray().add(token)))
                        .flatMap(resultSet -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                logger.error("token NOT found...");
                                return Single.error(new Exception("Token not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                logger.trace("Token found:" + row.encodePrettily());

                                if (row.getBoolean("isDeleted")) {
                                    logger.error("Received Token is deleted");
                                    return Single.error(new Exception("Token does not exist in our records. Please request a new token."));
                                }

                                if (!(row.getString("tokenType", "").equals(Constants.RESET_PASSWORD_TOKEN))) {
                                    logger.error("Received Token Type does not match: " + row.getString("tokenType", "NULL"));
                                    return Single.error(new Exception("Right token does not exist in our records. Please request a new token."));
                                }

                                tokenId = row.getInteger("id");
                                subjectId = row.getString("subjectId");

                                AuthenticationInfo authInfo = new AuthenticationInfo(
                                        row.getString("token"),
                                        row.getString("nonce"),
                                        row.getInstant("expireDate"),
                                        row.getString("userData"));

                                Token tokenValidator = new Token();

                                AuthenticationInfo authResult = tokenValidator.validateToken(token, authInfo);

                                if (authResult.isValid()) {
                                    logger.trace("Received Token is valid.");

                                    email = row.getString("email");
                                    displayName = row.getString("displayName");

                                    return Single.just(row);
                                } else {
                                    logger.error("Received Token is NOT valid: " + authResult.getResultText());
                                    return Single.error(new Exception("Token is not valid. Please request a new token."));
                                }

                            } else {
                                logger.error("Multiple tokens found...");
                                return Single.error(new Exception("Valid token is not found in our records"));
                            }
                        })
                        .flatMap(row -> {
                                    logger.trace("ResetPasswordController - Updating Subject with uuid:[" + subjectId + "] -> " + row.encodePrettily());

                                    String salt = authProvider.generateSalt();
                                    String hash = authProvider.computeHash(newPassword, salt);

                                    return resConn.rxUpdateWithParams("UPDATE subject SET " +
                                                    "updated = now()," +
                                                    "crudSubjectId = CAST(? AS uuid)," +
                                                    "isActivated = true," +
                                                    "password = ?," +
                                                    "passwordSalt = ?, " +
                                                    "isPasswordChangeRequired = false," +
                                                    "passwordExpiresAt = NOW() + " + String.valueOf(Config.getInstance().getConfigJsonObject().getInteger(Constants.PASSWORD_EXPIRATION_DAYS)) + " * INTERVAL '1 DAY' " +
                                                    " WHERE " +
                                                    "uuid = CAST(? AS uuid);",
                                            new JsonArray()
                                                    .add(Constants.SYSTEM_USER_UUID)
                                                    .add(hash)
                                                    .add(salt)
                                                    .add(subjectId));
                                }
                        )
                        .flatMap(updateResult -> {
                            logger.trace("ResetPasswordController - Updating Subject... Number of rows updated:" + updateResult.getUpdated());
                            //logger.info("Activate Account - Subject Update Result information:" + updateResult.getKeys().encodePrettily());
                            logger.trace("ResetPasswordController - Updating Subject Activation...");
                            if (updateResult.getUpdated() == 1) {
                                return resConn.rxUpdateWithParams("UPDATE subject_activation SET " +
                                                "deleted = now()," +
                                                "crudSubjectId = CAST(? AS uuid)," +
                                                "isDeleted = true" +
                                                " WHERE " +
                                                "id = ?;",
                                        new JsonArray()
                                                .add(Constants.SYSTEM_USER_UUID)
                                                .add(tokenId));
                            } else {
                                logger.error("ResetPasswordController - Activation Update Error Occurred - update result in not 1: " + updateResult.getUpdated());
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> {
                            if (updateResult.getUpdated() == 1) {
                                logger.trace("ResetPasswordController - Subject Activation Update Result information:" + updateResult.getKeys().encodePrettily());
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
                            logger.trace("ResetPasswordController: User password is reset and token is deleted. Both persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, "");
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.PASSWORD_RESET_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderPasswordResetMailBody(routingContext, displayName));

                            logger.trace("Password Reset mail is rendered successfully");
                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, result -> {
                                if (result.succeeded()) {
                                    logger.trace("Password Reset Mailing Event Bus Result:" + result.toString() + " | Result:" + result.result().body().encodePrettily());
                                } else {
                                    logger.error("Password Reset Mailing Event Bus Result:" + result.toString() + " | Cause:" + result.cause());
                                }

                            });
                            logger.trace("Password Reset mail is sent to Mail Verticle over Event Bus");

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.trace("Subscription to ResetPasswordController successful:" + result);
                    showTrxResult(routingContext, logger, 200, "Your password has been successfully reset!", "Welcome to API Portal again", "");
                }, t -> {
                    logger.error("ResetPasswordController Error", t);
                    showTrxResult(routingContext, logger, 401, "Reset Password Failed!", t.getLocalizedMessage(), "");
                }
        );
    }
}
