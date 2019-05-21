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
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePassword extends AbstractPortalHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChangePassword.class);
    @SuppressWarnings("squid:S2068")
    private static final String CHANGE_PASSWORD_ERROR_OCCURED = "Change Password Error Occured";

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    //private Integer subjectId;

    public ChangePassword(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super();
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }


    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.info("ChangePassword.handle invoked..");

        String username = routingContext.user().principal().getString("username");
        String oldPassword = routingContext.request().getFormAttribute("oldPassword");
        String newPassword = routingContext.request().getFormAttribute("newPassword");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String confirmPassword = routingContext.request().getFormAttribute("confirmPassword");

        LOGGER.info("Context user: {}", username);

        //TODO: OWASP Validate

        if (oldPassword == null || oldPassword.isEmpty()) {
            LOGGER.info("oldPassword is null or empty");
            generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "Please enter Old Password field", "");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            LOGGER.info("newPassword is null or empty");
            generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "Please enter New Password field", "");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            LOGGER.info("newPassword is null or empty");
            generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
                    , "Please enter Confirm Password field", "");
        }
        if (newPassword != null && !(newPassword.equals(confirmPassword))) {
            LOGGER.info("newPassword and confirmPassword does not match");
            generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED
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
                        .toSingleDefault(false)
                        .flatMap(checkAuth -> authProvider.rxAuthenticate(creds))
                        .flatMap((User user) -> {
                            LOGGER.info("Authenticated User with Old Password: {}", user.principal().encodePrettily());

                            LOGGER.info("Updating user records...");
                            String salt = authProvider.generateSalt();
                            String hash = authProvider.computeHash(newPassword, salt);

                            return resConn.rxUpdateWithParams("UPDATE portalschema.subject SET \n" +
                                            "  updated = now(), \n" +
                                            "  crud_subject_id = ?, \n" +
                                            "  password = ?, \n" +
                                            "  password_salt = ? \n" +
                                            "WHERE\n" +
                                            "  subject_name = ?;",
                                    new JsonArray()
                                            .add(1)
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

                        .doAfterSuccess((Boolean succ) -> LOGGER.info("Change Password: User record is updated and persisted successfully"))

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe((Boolean result) -> {
                    LOGGER.info("Subscription to ChangePassword successfull: {}", result);
                    generateResponse(routingContext, LOGGER, HttpStatus.SC_OK, "Password is changed.", "Please use your new password.", "");
                    //TODO: Send email to user
                }, (Throwable t) -> {
                    LOGGER.error("ChangePassword Error", t);
                    generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, CHANGE_PASSWORD_ERROR_OCCURED, t.getLocalizedMessage(), "");

                }
        );

    }

    public void pageRender(RoutingContext routingContext) {
        LOGGER.info("ChangePassword.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_CHANGE_PASSWORD, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

}
