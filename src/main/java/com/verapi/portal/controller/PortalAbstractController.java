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
import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PortalAbstractController implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(PortalAbstractController.class);

    protected final JDBCAuth authProvider;
    protected JDBCClient jdbcClient;

    public PortalAbstractController(JDBCAuth authProvider) {
        this.authProvider = authProvider;
    }

    public PortalAbstractController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }

    public abstract void defaultPostHandler(RoutingContext routingContext);

    protected void renderTemplate(RoutingContext routingContext, String templateFileName) {
        final ThymeleafTemplateEngine templateEngine = ThymeleafTemplateEngine.create();
        templateEngine.render(routingContext, Constants.TEMPLATE_PREFIX, templateFileName, res -> {
            if (res.succeeded()) {
                responseHTML(routingContext, res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    protected void renderJson(RoutingContext routingContext, Object object) {
        responseJSON(routingContext, object);
        }

    private void responseHTML(RoutingContext routingContext, Buffer chunk) {
        routingContext.response()
                .putHeader("content-type", "text/html; charset=utf-8")
                .end(chunk);
    }

    private void responseJSON(RoutingContext routingContext, Object  chunk) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encode(chunk));
    }

    @Override
    public void handle(RoutingContext event) {

    }
}
