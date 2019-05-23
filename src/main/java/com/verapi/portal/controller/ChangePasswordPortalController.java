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
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssController(routePathGET = "change-password", routePathPOST = "change-password", htmlTemplateFile = "change-password.html")
public class ChangePasswordPortalController extends AbstractPortalController {
    @SuppressWarnings("squid:S2068")
    private static final String CHANGE_PASSWORD_ERROR_OCCURED = "Change Password Error Occured!";
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangePasswordPortalController.class);

    public ChangePasswordPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        LOGGER.trace("ChangePasswordPortalController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.trace("ChangePasswordPortalController.handle invoked...");
        String username = routingContext.user().principal().getString("username");
        String oldPassword = routingContext.request().getFormAttribute("oldPassword");
        String newPassword = routingContext.request().getFormAttribute("newPassword");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String confirmPassword = routingContext.request().getFormAttribute("confirmPassword");

        LOGGER.trace("Context user: {}", username);

        //TODO: OWASP Validate

        if (oldPassword == null || oldPassword.isEmpty()) {
            LOGGER.warn("oldPassword is null or empty");
            showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "Please enter Old Password field", "");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            LOGGER.warn("newPassword is null or empty");
            showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "Please enter New Password field", "");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            LOGGER.warn("confirmPassword is null or empty");
            showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "Please enter Confirm Password field", "");
        }

        if (newPassword != null && !(newPassword.equals(confirmPassword))) {
            LOGGER.warn("newPassword and confirmPassword does not match");
            showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "New Password and Confirm Password does not match", "Please check and enter again");
        }

        JsonObject creds = new JsonObject()
                .put("username", username)
                .put("password", oldPassword);


        jdbcClient.rxGetConnection().flatMap((SQLConnection resConn) ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(Boolean.FALSE)
                        .flatMap(checkAuth -> authProvider.rxAuthenticate(creds))
                        .flatMap((User user) -> {
                            LOGGER.trace("Authenticated User with Old Password: {}", user.principal().encodePrettily());

                            LOGGER.trace("Updating user records...");
                            String salt = authProvider.generateSalt();
                            String hash = authProvider.computeHash(newPassword, salt);

                            return resConn.rxUpdateWithParams("UPDATE subject SET \n" +
                                            "  updated = now(), \n" +
                                            "  crudsubjectid = CAST(? AS uuid), \n" +
                                            "  password = ?, \n" +
                                            "  passwordsalt = ? \n" +
                                            "WHERE\n" +
                                            "  subjectname = ?;",
                                    new JsonArray()
                                            .add(Constants.SYSTEM_USER_UUID)
                                            .add(hash)
                                            .add(salt)
                                            .add(username));
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> resConn.rxCommit().toSingleDefault(Boolean.TRUE))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(Boolean.TRUE)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess((Boolean succ) -> LOGGER.trace("Change Password: User record is updated and persisted successfully"))

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe((Boolean result) -> {
                    LOGGER.info("Subscription to ChangePassword successfull: {}", result);
                    showTrxResult(routingContext, LOGGER, HttpStatus.SC_OK, "Password has been successfully changed!"
                            , "You may login using your new password", "");
                    //TODO: Send email to user
                }, (Throwable t) -> {
                    LOGGER.error("ChangePassword Error", t);
                    showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                            , t.getLocalizedMessage(), "");
                }
        );

    }
}
