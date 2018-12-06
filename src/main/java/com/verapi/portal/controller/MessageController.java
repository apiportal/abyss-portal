/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by İsmet Faik SAĞLAR <faik.saglar@verapi.com>, 7 2018
 *
 */

package com.verapi.portal.controller;

import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssController(routePathGET = "my-messages", routePathPOST = "", htmlTemplateFile = "my-messages.html")
public class MessageController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(MessageController.class);

    public MessageController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.trace("MessageController.defaultGetHandler invoked...");
        renderTemplate(routingContext, getClass().getAnnotation(AbyssController.class).htmlTemplateFile());
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.trace("MessageController.handle invoked...");
    }

}
