package com.verapi.portal.handler;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangePassword extends PortalHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(ChangePassword.class);

    private final JDBCClient jdbcClient;

    private final JDBCAuth authProvider;

    //private Integer subjectId;

    public ChangePassword(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }


    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("ChangePassword.handle invoked..");

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
            generateResponse(routingContext, logger, 401, "Change Password Error Occured", "Please enter Old Password field", "", "");
        }
        if (newPassword == null || newPassword.isEmpty()) {
            logger.info("newPassword is null or empty");
            generateResponse(routingContext, logger, 401, "Change Password Error Occured", "Please enter New Password field", "", "");
        }
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            logger.info("newPassword is null or empty");
            generateResponse(routingContext, logger, 401, "Change Password Error Occured", "Please enter Confirm Password field", "", "");
        }
        if (!(newPassword.equals(confirmPassword))) {
            logger.info("newPassword and confirmPassword does not match");
            generateResponse(routingContext, logger, 401, "Change Password Error Occured", "New Password and Confirm Password does not match", "Please check and enter again", "");
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

                        .doAfterSuccess(succ -> {
                            logger.info("Change Password: User record is updated and persisted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)

        ).subscribe(result -> {
                    logger.info("Subscription to ChangePassword successfull:" + result);
                    generateResponse(routingContext, logger, 200, "Password is changed.", "Please use your new password.", "", "");
                    //TODO: Send email to user
                }, t -> {
                    logger.error("ChangePassword Error", t);
                    generateResponse(routingContext, logger, 401, "Change Password Error Occured", t.getLocalizedMessage(), "", "");

                }
        );

    }

    public void pageRender(RoutingContext routingContext) {
        logger.info("ChangePassword.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_CHANGE_PASSWORD, res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

}
