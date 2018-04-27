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

import com.verapi.portal.common.Constants;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.User;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssController(routePathGET = "login", routePathPOST = "login-auth", htmlTemplateFile = "login.html", isPublic = true)
public class LoginController extends PortalAbstractController {

    private static Logger logger = LoggerFactory.getLogger(LoginController.class);

    public LoginController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("LoginController.defaultGetHandler invoked...");
        renderTemplate(routingContext, Controllers.LOGIN.templateFileName);
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
                User user = authResult.result();
                routingContext.setUser(user); //TODO: Check context. Is this usefull? Should it be vertx context?
                logger.info("Logged in user: " + user.principal().encodePrettily());
                routingContext.put("username", user.principal().getString("username"));
                redirect(routingContext, Constants.ABYSS_ROOT+"/index");
            } else {
                routingContext.fail(401);
            }
        });

        super.handle(routingContext);
    }
}
