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

@AbyssController(routePathGET = "activate-account", routePathPOST = "", htmlTemplateFile = "", isPublic = true)
public class ActivateAccountPortalController extends AbstractPortalController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateAccountPortalController.class);

    private Integer tokenId;
    private String email;
    private String displayName;

    public ActivateAccountPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        LOGGER.trace("ActivateAccountPortalController.defaultGetHandler invoked..");
        handle(routingContext);
    }

    @Override
    public void handle(RoutingContext routingContext) {

        LOGGER.trace("ActivateAccountPortalController.handle invoked..");

        String token = routingContext.request().getParam("v");
        LOGGER.trace("Received token: {}", token);

        String path = routingContext.normalisedPath();
        LOGGER.trace("Received path: {}", path);

        //TODO: Get Stored Token Info
        jdbcClient.rxGetConnection().flatMap((SQLConnection resConn) ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(Boolean.FALSE)
                        //Check if user already exists
                        .flatMap(resQ -> resConn
                                .rxQueryWithParams("SELECT A.*, S.displayName FROM subject_activation A, subject S WHERE token = ? and A.subjectId = S.uuid"
                                        , new JsonArray().add(token)))
                        .flatMap((ResultSet resultSet) -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                LOGGER.error("token NOT found...");
                                return Single.error(new Exception("Token not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                LOGGER.trace("Token found: {}", row.encodePrettily());

                                if (row.getBoolean("isDeleted")) {
                                    LOGGER.error("Received Token is deleted");
                                    //TODO: Give "User already activated" message if Subject is activated
                                    return Single.error(new Exception("Token does not exist in our records. Please request a new token."));
                                }

                                if (!(row.getString("tokenType", "").equals(Constants.ACTIVATION_TOKEN))) {
                                    LOGGER.error("Received Token Type does not match: {}", row.getString("tokenType", "NULL"));
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
                                    LOGGER.trace("Received Token is valid.");

                                    email = row.getString("email");
                                    displayName = row.getString("displayName");

                                    return Single.just(row);
                                } else {
                                    //TODO: Update Token as deleted.
                                    LOGGER.error("Received Token is NOT valid: {}", authResult.getResultText());
                                    return Single.error(
                                            new Exception("Token is not valid. Please request a new activation token by singing up again with same username.")
                                    );
                                }

                            } else {
                                LOGGER.error("Multiple tokens found...");
                                return Single.error(new Exception("Valid token is not found in our records"));
                            }
                        })
                        .flatMap((JsonObject row) -> {
                                    LOGGER.trace("Activate Account - Updating Subject with uuid:[{}] -> {}", row.getString("subjectId"), row.encodePrettily());
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
                        .flatMap((UpdateResult updateResult) -> {
                            LOGGER.trace("Activate Account - Updating Subject... Number of rows updated: {}", updateResult.getUpdated());
                            LOGGER.trace("Activate Account - Updating Subject Activation...");
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
                            LOGGER.trace("Activate Account: User record is activated and Token is deleted. Both persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, "");
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.WELCOME_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderWelcomeMailBody(routingContext, displayName));

                            LOGGER.trace("Welcome mail is rendered successfully");
                            routingContext.vertx().getDelegate().eventBus()
                                    .<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, (AsyncResult<Message<JsonObject>> result) -> {
                                        if (result.succeeded()) {
                                            LOGGER.trace("Welcome Mailing Event Bus Result: {} | Result: {}", result, result.result().body().encodePrettily());
                                        } else {
                                            LOGGER.error("Welcome Mailing Event Bus Result: {} | Cause: {}", result, result.cause().getMessage());
                                        }
                                    });
                            LOGGER.trace("Welcome mail is sent to Mail Verticle over Event Bus");

                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe((Boolean result) -> {
                    LOGGER.trace("Subscription to ActivateAccount successful: {}", result);
                    showTrxResult(routingContext, LOGGER, HttpStatus.SC_OK, "Activation Successful!", "Welcome to API Portal", "");
                }, (Throwable t) -> {
                    LOGGER.error("ActivateAccount Error", t);
                    showTrxResult(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, "Activation Failed!", t.getLocalizedMessage(), "");
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
