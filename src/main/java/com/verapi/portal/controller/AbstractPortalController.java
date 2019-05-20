/*
 * Copyright 2019 Verapi Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.verapi.portal.controller;

import com.verapi.abyss.common.Constants;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractPortalController<T> implements IController<T>, Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractPortalController.class);

    protected final JDBCAuth authProvider;
    protected JDBCClient jdbcClient;

    public AbstractPortalController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        this.authProvider = authProvider;
        this.jdbcClient = jdbcClient;
    }

    public abstract void defaultGetHandler(RoutingContext routingContext);

    protected void renderTemplate(RoutingContext routingContext, String templateFileName) {
        renderTemplate(routingContext, new JsonObject(), templateFileName, 200);
    }

    protected void renderTemplate(RoutingContext routingContext, JsonObject context, String templateFileName) {
        renderTemplate(routingContext, context, templateFileName, 200);
    }

    protected void renderTemplate(RoutingContext routingContext, JsonObject context, String templateFileName, int statusCode) {
        _renderTemplate(routingContext, context, templateFileName, statusCode);
    }

    private void _renderTemplate(RoutingContext routingContext, JsonObject context, String templateFileName, int statusCode) {
        final ThymeleafTemplateEngine templateEngine = ThymeleafTemplateEngine.create(routingContext.vertx());
        //templateEngine.render(routingContext, Constants.TEMPLATE_DIR_ROOT, templateFileName, res -> {
        templateEngine.render(context, Constants.TEMPLATE_DIR_ROOT + templateFileName, res -> {
            if (res.succeeded()) {
                responseHTML(routingContext, res.result(), statusCode);
                LOGGER.trace("renderTemplate using " + templateFileName + " finished successfully");
            } else {
                routingContext.fail(res.cause());
                LOGGER.trace("renderTemplate using " + templateFileName + " failed with " + res.cause().getLocalizedMessage());
            }
        });
    }

    protected void renderJson(RoutingContext routingContext, Object object) {
        responseJSON(routingContext, object);
    }

    private void responseHTML(RoutingContext routingContext, Buffer chunk, int statusCode) {
        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/html; charset=utf-8")
                .setStatusCode(statusCode)
                .end(chunk);
    }

    private void responseHTML(RoutingContext routingContext, Buffer chunk) {
        responseHTML(routingContext, chunk, 200);
    }


    private void responseJSON(RoutingContext routingContext, Object chunk) {
        routingContext.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                .end(Json.encode(chunk));
    }

    @Override
    public void handle(RoutingContext event) {

    }

    public void redirect(RoutingContext routingContext, String redirectTo, int redirectCode) {
        routingContext.response().putHeader("location", redirectTo).setStatusCode(redirectCode).end();
        LOGGER.trace("redirecting into " + redirectTo + " with http status code " + redirectCode);
    }

    public void redirect(RoutingContext routingContext, String redirectTo) {
        redirect(routingContext, redirectTo, 302);
    }

    protected void showTrxResult(RoutingContext routingContext, Logger LOGGER, int statusCode, String errorMessage, String errorAtUrl, String contextFailureMessage) {
        LOGGER.trace("showTrxResult invoked...");

        //Use user's session for storage
        routingContext.session().put(Constants.HTTP_STATUSCODE, statusCode);
        routingContext.session().put(Constants.HTTP_URL, errorAtUrl);
        routingContext.session().put(Constants.HTTP_ERRORMESSAGE, errorMessage);
        routingContext.session().put(Constants.CONTEXT_FAILURE_MESSAGE, contextFailureMessage);

        if (statusCode == 200) {
            redirect(routingContext, Constants.ABYSS_ROOT + "/success");
        } else {
            redirect(routingContext, Constants.ABYSS_ROOT + "/failure");
        }
    }

}
