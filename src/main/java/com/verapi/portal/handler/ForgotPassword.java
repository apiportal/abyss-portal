package com.verapi.portal.handler;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.MailVerticle;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;


/**
 * Reset password using activation mail sent to selected email
 * TODO: same email may be connected to multiple usernames/accounts???
 */
public class ForgotPassword implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(ForgotPassword.class);

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    private Integer subjectId;

    private String authToken;

    public ForgotPassword(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }


    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("ForgotPassword.handle invoked..");

        String email = routingContext.request().getFormAttribute("email");
        logger.info("Received email:" + email);


        //TODO: OWASP Email Validate


        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM portalschema.SUBJECT WHERE EMAIL = ?", new JsonArray().add(email)))
                        .flatMap(resultSet -> {
                            int numOfRows = resultSet.getNumRows();
                            if (numOfRows == 0) {
                                logger.info("email NOT found...");
                                return Single.error(new Exception("Email not found in our records"));
                            } else if (numOfRows == 1) {
                                if (resultSet.getRows(true).get(0).getInteger("is_activated") == 0) {
                                    logger.info("account connected to email is NOT activated");
                                    return Single.error(new Exception("Please activate your account by clicking the link inside activation mail."));
                                } else {
                                    subjectId = resultSet.getRows(true).get(0).getInteger("id");

                                    logger.info("Activated account found:[" + subjectId + "]. Reset password token is going to be created...");


                                    //Generate and Persist Reset Password Token
                                    Token tokenGenerator = new Token();
                                    AuthenticationInfo authInfo;
                                    try {
                                        authInfo = tokenGenerator.encodeToken(Config.getInstance().getConfigJsonObject().getInteger("quarter.hour.in.seconds"), email, routingContext.vertx().getDelegate());
                                        logger.info("Reset Password: token is created successfully: " + authInfo.getToken());
                                        authToken = authInfo.getToken();
                                    } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                                        logger.error("Reset Password: tokenGenerator.encodeToken :" + e.getLocalizedMessage());
                                        return Single.error(new Exception("Reset Password: token could not be generated"));
                                    }
                                    return resConn.rxUpdateWithParams("INSERT INTO portalschema.subject_activation (" +
                                                    "organization_id," +
                                                    "crud_subject_id," +
                                                    "subject_id," +
                                                    "expire_date," +
                                                    "token) " +
                                                    "VALUES (?, ?, ?, ?, ?)",
                                            new JsonArray()
                                                    .add(0)
                                                    .add(1)
                                                    .add(subjectId)
                                                    .add(authInfo.getExpireDate())
                                                    .add(authInfo.getToken()));
                                }
                            } else {
                                logger.info("email is connected to multiple accounts [" + numOfRows + "]");
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
                            logger.info("Reset password token is created and persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(MailVerticle.TOKEN, authToken);
                            json.put(MailVerticle.TO, email);

                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(MailVerticle.ABYSS_MAIL_CLIENT, json, result -> {
                                logger.info(result.toString());
                            });

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to Forgot Password successfull:" + result);
                    generateResponse(routingContext, 200, "Reset Password Code is sent to your email address.", "Please check spam folder also...", "Please click the link inside the mail.", "");
                    //TODO: Send email to user
                }, t -> {
                    logger.error("Forgot Password Error", t);
                    generateResponse(routingContext, 401, "Error in Forgot Password Occured", t.getLocalizedMessage(), "", "");
                }
        );


    }


    public void pageRender(RoutingContext routingContext) {
        logger.info("ForgotPassword.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        // and now delegate to the engine to render it.
        engine.render(routingContext, "webroot/", "forgot-password.html", res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    private void generateResponse(RoutingContext context, int statusCode, String message1, String message2, String message3, String message4) {

        logger.info("generateResponse invoked...");

        //Use user's session for storage
        context.session().put(Constants.HTTP_STATUSCODE, statusCode);
        context.session().put(Constants.HTTP_URL, message2);
        context.session().put(Constants.HTTP_ERRORMESSAGE, message1);
        context.session().put(Constants.CONTEXT_FAILURE_MESSAGE, message3);

        context.response().putHeader("location", "/abyss/httperror").setStatusCode(302).end();
    }
}
