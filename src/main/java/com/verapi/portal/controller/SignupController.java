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
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

@AbyssController(routePathGET = "signup", routePathPOST = "sign-up", htmlTemplateFile = "signup.html", isPublic = true)
public class SignupController extends PortalAbstractController {

    private static Logger logger = LoggerFactory.getLogger(SignupController.class);

    private Integer subjectId;
    private String subjectUUID;
    private String authToken;

    public SignupController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.trace("SignupController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.trace("SignupController.handle invoked...");

        String firstname = routingContext.request().getFormAttribute("firstname");
        String lastname = routingContext.request().getFormAttribute("lastname");
        String username = routingContext.request().getFormAttribute("username");
        String email = routingContext.request().getFormAttribute("email");
        String password = routingContext.request().getFormAttribute("password");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String password2 = routingContext.request().getFormAttribute("password2");
        String isAgreedToTerms = routingContext.request().getFormAttribute("isAgreedToTerms");

        //TODO: OWASP Validate & Truncate the Fields that are going to be stored

        logger.trace("Received firstname:" + firstname);
        logger.trace("Received lastname:" + lastname);
        logger.trace("Received user:" + username);
        logger.trace("Received email:" + email);
        logger.trace("Received pass:" + password);
        logger.trace("Received pass2:" + password2);
        logger.trace("Received isAgreedToTerms:" + isAgreedToTerms);

        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM subject WHERE subjectName = ?", new JsonArray().add(username))) //DO NOT CHECK: isDeleted = false
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                subjectId = resultSet.getRows(true).get(0).getInteger("id");
                                subjectUUID = resultSet.getRows(true).get(0).getString("uuid");
                                logger.trace("user found: " + resultSet.toJson().encodePrettily());
                                if (resultSet.getRows(true).get(0).getBoolean("isActivated")) {
                                    return Single.error(new Exception("Username already exists / Username already taken")); // TODO: How to trigger activation mail resend: Option 1 -> If not activated THEN resend activation mail ELSE display error message
                                } else {
                                    //TODO: Cancel previous activation - Is it really required.
                                    logger.trace("Username already exists but NOT activated, create and send new activation record..."); //Skip user creation
                                    return Single.just(resultSet);
                                }
                            } else {
                                logger.trace("user NOT found, creating user and activation records...");
                                String salt = authProvider.generateSalt();
                                String hash = authProvider.computeHash(password, salt);

                                // save user to the database
                                return resConn.rxUpdateWithParams("INSERT INTO subject(" +
                                                "organizationId," +
                                                //"now()," +          //created
                                                //"now()," +          //updated
                                                "crudSubjectId," +
                                                "isActivated," +
                                                "subjectTypeId," +
                                                "subjectName," +
                                                "firstname," +
                                                "lastname," +
                                                "displayName," +
                                                "email," +
                                                "effectiveStartDate," +
                                                //"effectiveEndDate," +
                                                "password," +
                                                "passwordSalt," +
                                                "isPasswordChangeRequired," +
                                                "passwordExpiresAt," +
                                                "subjectDirectoryId) " +
                                                "VALUES (CAST(? AS uuid), CAST(? AS uuid), false, CAST(? AS uuid), ?, ?, ?, ?, ?, now(), ?, ?, false, NOW() + ? * INTERVAL '1 DAY', CAST(? AS uuid)) RETURNING id, uuid",
                                        new JsonArray()
                                                .add(Constants.DEFAULT_ORGANIZATION_UUID)
                                                .add(Constants.SYSTEM_USER_UUID)
                                                .add(Constants.SUBJECT_TYPE_USER)
                                                .add(username)
                                                .add(firstname)
                                                .add(lastname)
                                                .add(firstname + " " + lastname)
                                                .add(email)
                                                .add(hash)
                                                .add(salt)
                                                .add(Config.getInstance().getConfigJsonObject().getInteger(Constants.PASSWORD_EXPIRATION_DAYS))
                                                .add(Constants.INTERNAL_SUBJECT_DIRECTORY_UUID));
                            }
                        })
                        .flatMap(updateResult -> {
                            if (updateResult instanceof UpdateResult) {
                                subjectId = ((UpdateResult) updateResult).getKeys().getInteger(0);
                                subjectUUID = ((UpdateResult) updateResult).getKeys().getString(1);
                                logger.trace("[" + ((UpdateResult) updateResult).getUpdated() + "] user created successfully: " + ((UpdateResult) updateResult).getKeys().encodePrettily() + " | Integer Key @pos=0 (subjectId):" + subjectId + " | String Key @pos=1 (subjectUUID):" + subjectUUID);
                            } else if (updateResult instanceof ResultSet) {
                                logger.trace("[" + ((ResultSet) updateResult).getNumRows() + "] inactive user found: " + ((ResultSet) updateResult).toJson().encodePrettily() + " | subjectID:" + subjectId + " | subjectUUID:" + subjectUUID);
                            }


                            //Generate and Persist Activation Token
                            Token tokenGenerator = new Token();
                            AuthenticationInfo authInfo;
                            try {
                                authInfo = tokenGenerator.generateToken(Config.getInstance().getConfigJsonObject().getInteger("token.activation.signup.ttl") * Constants.ONE_MINUTE_IN_SECONDS,
                                        email,
                                        routingContext.vertx().getDelegate());
                                logger.trace("activation token is created successfully: " + authInfo.getToken());
                                authToken = authInfo.getToken();
                            } catch (UnsupportedEncodingException e) {
                                logger.trace("tokenGenerator.generateToken :" + e.getLocalizedMessage());
                                return Single.error(new Exception("activation token could not be generated"));
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
                                            .add(Constants.ACTIVATION_TOKEN)
                                            .add(email)
                                            .add(authInfo.getNonce())
                                            .add(authInfo.getUserData())
                            );
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> resConn.rxCommit().toSingleDefault(true))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess(succ -> {
                            logger.trace("User record and activation token is created and persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, authToken);
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.ACTIVATION_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, MailUtil.renderActivationMailBody(routingContext,
                                    Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_BASE_URL) + Constants.ACTIVATION_PATH + "/?v=" + authToken,
                                    Constants.ACTIVATION_TEXT));

                            logger.trace("User activation mail is rendered successfully");
                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, result -> {
                                if (result.succeeded()) {
                                    logger.trace("Activation Mailing Event Bus Result:" + result.toString() + " | Result:" + result.result().body().encodePrettily());
                                } else {
                                    logger.error("Activation Mailing Event Bus Result:" + result.toString() + " | Cause:" + result.cause());
                                }

                            });
                            logger.trace("User activation mail is sent to Mail Verticle over Event Bus");

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.trace("Subscription to Signup successfull:" + result);
                    showTrxResult(routingContext, logger, 200, "Activation Code is sent to your email address", "Please check spam folder also...", "");
                    //TODO: Send email to user
                }, t -> {
                    logger.error("Signup Error", t);
                    showTrxResult(routingContext, logger, 403, "Signup Error Occured", t.getLocalizedMessage(), "");
                }
        );
        super.handle(routingContext);
    }

}
