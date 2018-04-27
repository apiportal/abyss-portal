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

import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssController(routePathGET = "my-apis", routePathPOST = "my-apis", htmlTemplateFile = "my-apis.html")
public class MyApisController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(MyApisController.class);

    public MyApisController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("MyApisController.defaultGetHandler invoked...");
        renderTemplate(routingContext, Controllers.MYAPIS.templateFileName);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("MyApisController.handle invoked...");
    }

}
