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

@AbyssController(routePathGET = "activate-account", routePathPOST = "", htmlTemplateFile = "", isPublic = true)
public class ActivateAccountController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(ActivateAccountController.class);

    private Integer tokenId;
    private String email;
    private String displayName;

    public ActivateAccountController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("ActivateAccountController.defaultGetHandler invoked..");
        handle(routingContext);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("ActivateAccountController.handle invoked..");

        String token = routingContext.request().getParam("v");
        logger.info("Received token:" + token);

        String path = routingContext.normalisedPath();
        logger.info("Received path:" + path);

        //TODO: Get Stored Token Info
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
                                logger.info("token NOT found...");
                                return Single.error(new Exception("Token not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                logger.info("Token found:" + row.encodePrettily());

                                if (row.getBoolean("isDeleted")) {
                                    logger.error("Received Token is deleted");
                                    return Single.error(new Exception("Token does not exist in our records. Please request a new token.")); //TODO: Give "User already activated" message if Subject is activated
                                }

                                if (!(row.getString("tokenType", "").equals(Constants.ACTIVATION_TOKEN))) {
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
                                    logger.info("Received Token is valid.");

                                    email = row.getString("email");
                                    displayName = row.getString("displayName");

                                    return Single.just(row);
                                } else {
                                    logger.error("Received Token is NOT valid: " + authResult.getResultText()); //TODO: Update Token as deleted.
                                    return Single.error(new Exception("Token is not valid. Please request a new activation token by singing up again with same username."));
                                }

                            } else {
                                logger.info("Multiple tokens found...");
                                return Single.error(new Exception("Valid token is not found in our records"));
                            }
                        })
                        .flatMap(row -> {
                                    logger.info("Activate Account - Updating Subject with uuid:[" + row.getString("subjectId") + "] -> " + row.encodePrettily());
                                    return resConn.rxUpdateWithParams("UPDATE subject SET " +
                                                    "updated = now()," +
                                                    "crudSubjectId = CAST(? AS uuid)," +
                                                    "isActivated = true" +
                                                    " WHERE " +
                                                    "uuid = CAST(? AS uuid);",
                                            new JsonArray()
                                                    .add(Constants.SYSTEM_USER_UUID)
                                                    .add(row.getString("subjectId")));
                                }
                        )
                        .flatMap(updateResult -> {
                            logger.info("Activate Account - Updating Subject... Number of rows updated:" + updateResult.getUpdated());
                            //logger.info("Activate Account - Subject Update Result information:" + updateResult.getKeys().encodePrettily());
                            logger.info("Activate Account - Updating Subject Activation...");
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
                            logger.info("Activate Account: User record is activated and Token is deleted. Both persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, "");
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.WELCOME_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderWelcomeMailBody(routingContext, displayName));

                            logger.info("Welcome mail is rendered successfully");
                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, result -> {
                                if (result.succeeded()) {
                                    logger.info("Welcome Mailing Event Bus Result:" + result.toString() + " | Result:" + result.result().body().encodePrettily());
                                } else {
                                    logger.info("Welcome Mailing Event Bus Result:" + result.toString() + " | Cause:" + result.cause());
                                }


                            });
                            logger.info("Welcome mail is sent to Mail Verticle over Event Bus");

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to ActivateAccount successful:" + result);
                    showTrxResult(routingContext, logger, 200, "Activation Successful!", "Welcome to API Portal", "");
                }, t -> {
                    logger.error("ActivateAccount Error", t);
                    showTrxResult(routingContext, logger, 401, "Activation Failed!", t.getLocalizedMessage(), "");
                }
        );


        //TODO: Different action for different tokens and paths
/*        if (path.contains(Constants.ACTIVATION_PATH)) {

            //TODO: Validate Token

            //TODO: Update User Activated

        } else if (path.contains(Constants.RESET_PASSWORD_PATH)) {

            //TODO: Validate Token

            //TODO: Route to Reset Password Page as authenticated

        } else {

        }
*/
        //TODO: mark token as deleted

    }
}
