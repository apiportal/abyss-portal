package com.verapi.portal.handler;

import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;

public class Signup implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(Signup.class);

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    public Signup(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("Signup.handle invoked..");

        /*
        firstname:Ökkeş
        lastname:Tutan
        username:okkes.tutan
        email:okkes.tutan69@example.com
        password:pwd
        password2:pwd
        isAgreedToTerms:on
         */

        String firstname = routingContext.request().getFormAttribute("firstname");
        String lastname = routingContext.request().getFormAttribute("lastname");
        String username = routingContext.request().getFormAttribute("username");
        String email = routingContext.request().getFormAttribute("email");
        String password = routingContext.request().getFormAttribute("password");
        //TODO: should password consistency check be performed @FE or @BE or BOTH?
        String password2 = routingContext.request().getFormAttribute("password2");
        String isAgreedToTerms = routingContext.request().getFormAttribute("isAgreedToTerms");

        //TODO: OWASP Validate

        logger.info("Received firstname:" + firstname);
        logger.info("Received lastname:" + lastname);
        logger.info("Received user:" + username);
        logger.info("Received email:" + email);
        logger.info("Received pass:" + password);
        logger.info("Received pass2:" + password2);
        logger.info("Received isAgreedToTerms:" + isAgreedToTerms);

        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM portalschema.USER WHERE USERNAME = ?", new JsonArray().add(email)))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("user found: " + resultSet.toJson().encodePrettily());
                                return Single.error(new Exception("User already exists"));
                            } else {
                                logger.info("user NOT found, creating user and activation records...");
                                String salt = authProvider.generateSalt();
                                String hash = authProvider.computeHash(password, salt);

                                // save user to the database
                                return resConn.rxUpdateWithParams("INSERT INTO portalschema.user VALUES (?, ?, ?)", new JsonArray().add(email).add(hash).add(salt));
                            }
                        })
                        .flatMap(updateResult -> {
                            logger.info("user created successfully: " + updateResult.getKeys().encodePrettily());

                            //Generate and Persist Activation Token
                            Token tokenGenerator = new Token();
                            AuthenticationInfo authInfo;
                            try {
                                authInfo = tokenGenerator.encodeToken(Config.getInstance().getConfigJsonObject().getInteger("one.hour.in.seconds"), email, routingContext.vertx().getDelegate());
                                logger.info("activation token is created successfully: " + authInfo.getToken());
                            } catch (UnsupportedEncodingException | NoSuchAlgorithmException e) {
                                logger.error("tokenGenerator.encodeToken :" + e.getLocalizedMessage());
                                return Single.error(new Exception("activation token could not be generated"));
                            }
                            return resConn.rxUpdateWithParams("INSERT INTO portalschema.user_activation (username, expire_date, token) VALUES (?, ?, ?)", new JsonArray().add(email).add(authInfo.getExpireDate()).add(authInfo.getToken()));
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> resConn.rxCommit().toSingleDefault(true))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> resConn.rxRollback().toSingleDefault(true)
                                            .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                            .flatMap(ignore -> Single.error(ex))
                        )

                        .doAfterSuccess(succ -> {
                            logger.info("activation token is created and persisted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

                        ).subscribe(result -> {
                                logger.info("Subscription to Signup successfull:" + result);
                                generateResponse(routingContext, 200, "Activation Code is sent to your email address", "Please check spam folder also...", "", "" );
                                //TODO: Send email to user
                            }, t -> {
                                logger.error("Signup Error", t);
                                generateResponse(routingContext, 401, "Signup Error Occured", t.getLocalizedMessage(), "", "" );

                            }
                        );
    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("Signup.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(routingContext, "webroot/", "signup.html", res -> {
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
