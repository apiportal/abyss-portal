/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 6 2018
 *
 */

package com.verapi.portal.oapi.util;

import com.verapi.portal.common.PlatformAPIList;
import com.verapi.portal.oapi.AbstractApiController;
import com.verapi.portal.oapi.AbyssApiController;
import com.verapi.portal.oapi.AbyssApiOperationHandler;
import com.verapi.portal.oapi.exception.InternalServerError500Exception;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

@AbyssApiController(apiSpec = "/openapi/Util.yaml")
public class UtilApiController extends AbstractApiController {
    private static final Logger logger = LoggerFactory.getLogger(UtilApiController.class);

    /**
     * API verticle creates new API Controller instance via this constructor
     *
     * @param vertx        Vertx content
     * @param router       Vertx router
     * @param authProvider JDBC Auth provider
     */
    public UtilApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getYamlFileList(RoutingContext routingContext) {
        try {
            logger.info("getYamlFileList invoked");
            subscribeAndResponse(routingContext, PlatformAPIList.getInstance().getPlatformAPIList(), HttpResponseStatus.OK.code());
        } catch (Exception e) {
            logger.error(e.getLocalizedMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

}
