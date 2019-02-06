package com.verapi.portal.handler;

import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

public class Signup extends PortalHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(Signup.class);

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    private Integer subjectId;

    private String authToken;

    private String htmlString;

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

        //TODO: OWASP Validate & Truncate the Fields that are going to be stored

        logger.info("Received firstname:" + firstname);
        logger.info("Received lastname:" + lastname);
        logger.info("Received user:" + username);
        logger.info("Received email:" + email);
        logger.info("Received pass:" + password);
        logger.info("Received pass2:" + password2);
        logger.info("Received isAgreedToTerms:" + isAgreedToTerms);

        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT * FROM portalschema.SUBJECT WHERE SUBJECT_NAME = ?", new JsonArray().add(username)))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                subjectId = resultSet.getRows(true).get(0).getInteger("id");
                                logger.info("user found: " + resultSet.toJson().encodePrettily());
                                if (resultSet.getRows(true).get(0).getBoolean("is_activated")) {
                                    return Single.error(new Exception("Username already exists / Username already taken")); // TODO: How to trigger activation mail resend: Option 1 -> If not activated THEN resend activation mail ELSE display error message
                                } else {
                                    //TODO: Cancel previous activation - Is it really required.
                                    logger.info("Username already exists but NOT activated, create and send new activation record..."); //Skip user creation
                                    return Single.just(resultSet);
                                }
                            } else {
                                logger.info("user NOT found, creating user and activation records...");
                                String salt = authProvider.generateSalt();
                                String hash = authProvider.computeHash(password, salt);

                                // save user to the database
                                return resConn.rxUpdateWithParams("INSERT INTO portalschema.subject(" +
                                                "organization_id," +
                                                //"now()," +          //created
                                                //"now()," +          //updated
                                                "crud_subject_id," +
                                                "is_activated," +
                                                "subject_type_id," +
                                                "subject_name," +
                                                "first_name," +
                                                "last_name," +
                                                "display_name," +
                                                "email," +
                                                "effective_start_date," +
                                                //"effective_end_date," +
                                                "password," +
                                                "password_salt) " +
                                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, now(), ?, ?) RETURNING id",
                                        new JsonArray()
                                                .add(0)
                                                .add(1)
                                                .add(0)
                                                .add(1)
                                                .add(username)
                                                .add(firstname)
                                                .add(lastname)
                                                .add(firstname + " " + lastname)
                                                .add(email)
                                                .add(hash)
                                                .add(salt));
                            }
                        })
                        .flatMap(updateResult -> {
                            if (updateResult instanceof UpdateResult) {
                                subjectId = ((UpdateResult) updateResult).getKeys().getInteger(0);
                                logger.info("[" + ((UpdateResult) updateResult).getUpdated() + "] user created successfully: " + ((UpdateResult) updateResult).getKeys().encodePrettily() + " | Integer Key @pos=0:" + subjectId);
                            } else if (updateResult instanceof ResultSet) {
                                logger.info("[" + ((ResultSet) updateResult).getNumRows() + "] inactive user found: " + ((ResultSet) updateResult).toJson().encodePrettily() + " | Integer Key @pos=0:" + ((ResultSet) updateResult).getRows(true).get(0).getInteger("id") + " subjectID:" + subjectId);
                            }


                            //Generate and Persist Activation Token
                            Token tokenGenerator = new Token();
                            AuthenticationInfo authInfo;
                            try {
                                authInfo = tokenGenerator.generateToken(Config.getInstance().getConfigJsonObject().getInteger("token.activation.signup.ttl") * Constants.ONE_MINUTE_IN_SECONDS,
                                        email,
                                        routingContext.vertx().getDelegate());
                                logger.info("activation token is created successfully: " + authInfo.getToken());
                                authToken = authInfo.getToken();
                            } catch (UnsupportedEncodingException e) {
                                logger.error("tokenGenerator.generateToken :" + e.getLocalizedMessage());
                                return Single.error(new Exception("activation token could not be generated"));
                            }
                            return resConn.rxUpdateWithParams("INSERT INTO portalschema.subject_activation (" +
                                            "organization_id," +
                                            "crud_subject_id," +
                                            "subject_id," +
                                            "expire_date," +
                                            "token," +
                                            "token_type, " +
                                            "email," +
                                            "nonce," +
                                            "user_data) " +
                                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                                    new JsonArray()
                                            .add(0)
                                            .add(1)
                                            .add(subjectId)
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
                            logger.info("User record and activation token is created and persisted successfully");

                            JsonObject json = new JsonObject();
                            json.put(Constants.EB_MSG_TOKEN, authToken);
                            json.put(Constants.EB_MSG_TO_EMAIL, email);
                            json.put(Constants.EB_MSG_TOKEN_TYPE, Constants.ACTIVATION_TOKEN);
                            json.put(Constants.EB_MSG_HTML_STRING, renderMailPage(routingContext,
                                    Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_BASE_URL) + Constants.ACTIVATION_PATH + "/?v=" + authToken,
                                    Constants.ACTIVATION_TEXT));

                            routingContext.vertx().getDelegate().eventBus().<JsonObject>send(Constants.ABYSS_MAIL_CLIENT, json, result -> {
                                logger.info(result.toString());
                            });

                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to Signup successfull:" + result);
                    generateResponse(routingContext, logger, 200, "Activation Code is sent to your email address", "Please check spam folder also...", "", "");
                    //TODO: Send email to user
                }, t -> {
                    logger.error("Signup Error", t);
                    generateResponse(routingContext, logger, 401, "Signup Error Occured", t.getLocalizedMessage(), "", "");
                }
        );
    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("Signup.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_SIGNUP, res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    public String renderMailPage(RoutingContext routingContext, String activationUrl, String activationText) {
        logger.info("renderMailPage invoked...");


        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        routingContext.put("url.activation", activationUrl);
        routingContext.put("text.activation", activationText);
        routingContext.put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));

        JsonObject templateContext = new JsonObject()
                .put("url.activation", activationUrl)
                .put("text.activation", activationText)
                .put("mail.image.url", Config.getInstance().getConfigJsonObject().getString(Constants.MAIL_IMAGE_URL));


        // and now delegate to the engine to render it.
        engine.render(templateContext, Constants.TEMPLATE_DIR_EMAIL + Constants.HTML_ACTIVATE, res -> {
            if (res.succeeded()) {
                //routingContext.response().putHeader("Content-Type", "text/html");
                //routingContext.response().end(res.result());

                this.htmlString = res.result().toString("UTF-8");
            } else {
                routingContext.fail(res.cause());
            }
        });

        return htmlString;

    }

}
