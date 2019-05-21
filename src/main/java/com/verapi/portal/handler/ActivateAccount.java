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
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivateAccount extends AbstractPortalHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ActivateAccount.class);

    private final JDBCClient jdbcClient;

    private Integer tokenId;

    private String email;

    private String displayName;

    private String htmlString;

    public ActivateAccount(JDBCClient jdbcClient) {
        super();
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.info("ActivateAccount.handle invoked..");

        String token = routingContext.request().getParam("v");
        LOGGER.info("Received token: {}", token);

        String path = routingContext.normalisedPath();
        LOGGER.info("Received path: {}", path);

        //TODO: Get Stored Token Info
        jdbcClient.rxGetConnection().flatMap((SQLConnection resConn) ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap((Boolean resQ) -> resConn
                                .rxQueryWithParams("SELECT A.*, S.display_name FROM portalschema.SUBJECT_ACTIVATION A, portalschema.SUBJECT S " +
                                                "WHERE TOKEN = ? and A.subject_id = S.id"
                                        , new JsonArray().add(token)))
                        .flatMap((ResultSet resultSet) -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                LOGGER.info("token NOT found...");
                                return Single.error(new Exception("Token not found in our records"));
                            } else if (numOfRows == 1) {
                                JsonObject row = resultSet.getRows(true).get(0);
                                LOGGER.info("Token found: {}", row.encodePrettily());

                                if (row.getBoolean("is_deleted")) {
                                    LOGGER.error("Received Token is deleted");
                                    return Single.error(new Exception("Token does not exist in our records. Please request a new token.")); //TODO: Give "User already activated" message if Subject is activated
                                }

                                if (!(row.getString("token_type", "").equals(Constants.ACTIVATION_TOKEN))) {
                                    LOGGER.error("Received Token Type does not match: {}", row.getString("token_type", "NULL"));
                                    return Single.error(new Exception("Right token does not exist in our records. Please request a new token."));
                                }

                                tokenId = row.getInteger("id");

                                AuthenticationInfo authInfo = new AuthenticationInfo(
                                        row.getString("token"),
                                        row.getString("nonce"),
                                        row.getInstant("expire_date"),
                                        row.getString("user_data"));

                                Token tokenValidator = new Token();

                                AuthenticationInfo authResult = tokenValidator.validateToken(token, authInfo);

                                if (authResult.isValid()) {
                                    LOGGER.info("Received Token is valid.");

                                    email = row.getString("email");
                                    displayName = row.getString("display_name");

                                    return Single.just(row);
                                } else {
                                    LOGGER.error("Received Token is NOT valid: {}", authResult.getResultText());
                                    return Single.error(new Exception("Token is not valid. Please request a new token."));
                                }

                            } else {
                                LOGGER.info("Multiple tokens found...");
                                return Single.error(new Exception("Valid token is not found in our records"));
                            }
                        })
                        .flatMap((JsonObject row) -> {
                                    LOGGER.info("Activate Account - Updating Subject with id:[{}] -> {}", row.getInteger("subject_id"), row.encodePrettily());
                                    return resConn.rxUpdateWithParams("UPDATE portalschema.subject SET " +
                                                    "updated = now()," +
                                                    "crud_subject_id = ?," +
                                                    "is_activated = true" +
                                                    " WHERE " +
                                                    "id = ?;",
                                            new JsonArray()
                                                    .add(1)
                                                    .add(row.getInteger("subject_id")));
                                }
                        )
                        .flatMap((UpdateResult updateResult) -> {
                            LOGGER.info("Activate Account - Updating Subject... Number of rows updated: {}", updateResult.getUpdated());
                            LOGGER.info("Activate Account - Updating Subject Activation...");
                            if (updateResult.getUpdated() == 1) {
                                return resConn.rxUpdateWithParams("UPDATE portalschema.subject_activation SET " +
                                                "deleted = now()," +
                                                "crud_subject_id = ?," +
                                                "is_deleted = true" +
                                                " WHERE " +
                                                "id = ?;",
                                        new JsonArray()
                                                .add(1)
                                                .add(tokenId));
                            } else {
                                return Single.error(new Exception("Activation Update Error Occurred"));
                            }
                        })
                        // commit if all succeeded
                        .flatMap((UpdateResult updateResult) -> {
                            if (updateResult.getUpdated() == 1) {
                                LOGGER.info("Activate Account - Subject Activation Update Result information: {}", updateResult.getKeys().encodePrettily());
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

                        .doAfterSuccess((Boolean succ) -> {
                            LOGGER.info("Activate Account: User record is activated and Token is deleted. Both persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, "");
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.WELCOME_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, renderMailPage(routingContext, displayName));

                            routingContext.vertx().getDelegate().eventBus()
                                    .<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, (AsyncResult<Message<JsonObject>> result) -> LOGGER.info(result.toString()));
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe((Boolean result) -> {
                    LOGGER.info("Subscription to ActivateAccount successful:" + result);
                    generateResponse(routingContext, LOGGER, HttpStatus.SC_OK, "Activation Successful!", "Welcome to API Portal.", "");
                    //TODO: Send email to user
                }, (Throwable t) -> {
                    LOGGER.error("ActivateAccount Error", t);
                    generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, "Activation Failed!", t.getLocalizedMessage(), "");

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

    private String renderMailPage(RoutingContext routingContext, String fullName) {
        LOGGER.info("renderWelcomeMailPage invoked...");


        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put("full.name", fullName);
        routingContext.put("url.login", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL));
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        //after vert.x 3.6, ThymeleafTemplateEngine render requires a JsonObject instead of RoutingContext
        //so we prepare a JsonObject
        JsonObject templateContext = new JsonObject()
                .put("full.name", fullName)
                .put("url.login", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_LOGIN_URL))
                .put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));
        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_WELCOME, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                //routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                //routingContext.response().end(res.result());

                this.htmlString = res.result().toString("UTF-8");
            } else {
                routingContext.fail(res.cause());
            }
        });

        return htmlString;
    }

}
