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

public class SubjectGroupController extends PortalAbstractController {
    private static Logger logger = LoggerFactory.getLogger(SubjectGroupController.class);

    public SubjectGroupController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("SubjectGroupController.defaultGetHandler invoked...");
        renderTemplate(routingContext, Controllers.SUBJECTGROUP.templateFileName);
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("SubjectGroupController.handle invoked...");
    }

}
