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
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FailureController extends PortalAbstractController {

    private static Logger logger = LoggerFactory.getLogger(FailureController.class);

    public FailureController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.info("FailureController.defaultGetHandler invoked...");
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Integer statusCode = routingContext.session().get(Constants.HTTP_STATUSCODE);
        statusCode = statusCode > 0 ? statusCode : 200;


        logger.info("FailureController.handle invoked - status code: " + statusCode);

        routingContext.put(Constants.HTTP_STATUSCODE, statusCode);
        routingContext.put(Constants.HTTP_URL, routingContext.session().get(Constants.HTTP_URL));
        routingContext.put(Constants.HTTP_ERRORMESSAGE, routingContext.session().get(Constants.HTTP_ERRORMESSAGE));
        routingContext.put(Constants.CONTEXT_FAILURE_MESSAGE, routingContext.session().get(Constants.CONTEXT_FAILURE_MESSAGE));

        renderTemplate(routingContext, Controllers.TRX_NOK.templateFileName, statusCode);

        super.handle(routingContext);
    }

}
