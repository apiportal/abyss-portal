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

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssController(routePathGET = "change-password", routePathPOST = "change-password", htmlTemplateFile = "change-password.html")
public class ChangePasswordController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(ChangePasswordController.class);

    public ChangePasswordController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("ChangePasswordController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("ChangePasswordController.handle invoked...");
        String username = routingContext.user().principal().getString("username");
        String oldPassword = routingContext.request().getFormAttribute("oldPassword");
        String newPassword = routingContext.request().getFormAttribute("newPassword");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String confirmPassword = routingContext.request().getFormAttribute("confirmPassword");

        logger.info("Context user:" + username);
        logger.info("Received old Password:" + oldPassword);
        logger.info("Received new Password:" + newPassword);
        logger.info("Received confirm Password:" + confirmPassword);

        //TODO: OWASP Validate

        if (oldPassword == null || oldPassword.isEmpty()) {
            logger.info("oldPassword is null or empty");
            showTrxResult(routingContext, logger, 401, "Change Password Error Occured!", "Please enter Old Password field", "");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            logger.info("newPassword is null or empty");
            showTrxResult(routingContext, logger, 401, "Change Password Error Occured!", "Please enter New Password field", "");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            logger.info("newPassword is null or empty");
            showTrxResult(routingContext, logger, 401, "Change Password Error Occured!", "Please enter Confirm Password field", "");
        }
        if (!(newPassword.equals(confirmPassword))) {
            logger.info("newPassword and confirmPassword does not match");
            showTrxResult(routingContext, logger, 401, "Change Password Error Occured!", "New Password and Confirm Password does not match", "Please check and enter again");
        }

        JsonObject creds = new JsonObject()
                .put("username", username)
                .put("password", oldPassword);


        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        .flatMap(checkAuth -> authProvider.rxAuthenticate(creds))
                        .flatMap(user -> {
                            logger.info("Authenticated User with Old Password: " + user.principal().encodePrettily());

                            logger.info("Updating user records...");
                            String salt = authProvider.generateSalt();
                            String hash = authProvider.computeHash(newPassword, salt);

                            return resConn.rxUpdateWithParams("UPDATE subject SET \n" +
                                            "  updated = now(), \n" +
                                            "  crudSubjectId = ?, \n" +
                                            "  password = ?, \n" +
                                            "  passwordSalt = ? \n" +
                                            "WHERE\n" +
                                            "  subjectName = ?;",
                                    new JsonArray()
                                            .add(Constants.SYSTEM_USER_ID)
                                            .add(hash)
                                            .add(salt)
                                            .add(username));
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> resConn.rxCommit().toSingleDefault(true))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess(succ -> {
                            logger.info("Change Password: User record is updated and persisted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to ChangePassword successfull:" + result);
                    showTrxResult(routingContext, logger, 200, "Password has been successfully changed!", "You may login using your new password", "");
                    //TODO: Send email to user
                }, t -> {
                    logger.error("ChangePassword Error", t);
                    showTrxResult(routingContext, logger, 401, "Change Password Error Occured!", t.getLocalizedMessage(), "");
                }
        );

    }
}
