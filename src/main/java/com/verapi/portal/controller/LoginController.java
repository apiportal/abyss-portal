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
import com.verapi.portal.service.idam.SubjectService;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.Cookie;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;

@AbyssController(routePathGET = "login", routePathPOST = "login-auth", htmlTemplateFile = "login.html", isPublic = true)
public class LoginController extends PortalAbstractController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    public LoginController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("LoginController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("LoginController.handle invoked..");

        String username = routingContext.request().getFormAttribute("username");
        String password = routingContext.request().getFormAttribute("password");

        logger.info("Received user:" + username);
        logger.info("Received pass:" + password);

        JsonObject creds = new JsonObject()
                .put("username", username)
                .put("password", password);

        authProvider.authenticate(creds, authResult -> {
            if (authResult.succeeded()) {
                try {
                    SubjectService subjectService = new SubjectService(routingContext.vertx());

                    Single<JsonObject> apiResponse = subjectService.initJDBCClient()
                            .flatMap(jdbcClient -> subjectService.findBySubjectName(username))
                            .flatMap(result -> {
                                //result.toJson().getValue("rows")
                                logger.trace(result.toJson().encodePrettily());
                                return Single.just(result.getRows().get(0));
                            });

                    apiResponse.subscribe(resp -> {
                                logger.trace("LoginController.handle() subjectService.findBySubjectName replied successfully " + resp.encodePrettily());
                                User user = authResult.result();
                                String userUUID = resp.getString("uuid");
                                user.principal().put("user.uuid", userUUID);
                                routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context?
                                routingContext.session().put("user.uuid", userUUID);
                                routingContext.addCookie(Cookie.cookie("abyss.principal.uuid", userUUID)
                                        .setMaxAge(Config.getInstance().getConfigJsonObject().getInteger(Constants.BROWSER_SESSION_TIMEOUT) * 60));
                                logger.info("Logged in user: " + user.principal().encodePrettily());
                                routingContext.put("username", user.principal().getString("username"));
                                redirect(routingContext, Constants.ABYSS_ROOT + "/index");
                            },
                            throwable -> {
                                logger.error("LoginController.handle() subjectService.findBySubjectName replied error : ", throwable.getLocalizedMessage());
                                logger.error("LoginController.handle() subjectService.findBySubjectName replied error : ", Arrays.toString(throwable.getStackTrace()));
                            });
                } catch (Exception e) {
                    logger.error("LoginController.handle() subjectService error : ", Arrays.toString(e.getStackTrace()));
                    routingContext.fail(500);
                }
            } else {
                routingContext.fail(401);
            }
        });

        super.handle(routingContext);
    }
}
