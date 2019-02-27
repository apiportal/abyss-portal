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

package com.verapi.portal.api;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;

public class HttpStatusUtil {
    /**
     * Send back a response with status 200 Ok.
     *
     * @param context routing context
     */
    protected void ok(RoutingContext context) {
        context.response().end();
    }

    /**
     * Send back a response with status 200 OK.
     *
     * @param context routing context
     * @param content body content in JSON format
     */
    protected void ok(RoutingContext context, String content) {
        context.response().setStatusCode(200)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(content);
    }

    /**
     * Send back a response with status 201 Created.
     *
     * @param context routing context
     */
    protected void created(RoutingContext context) {
        context.response().setStatusCode(201).end();
    }

    /**
     * Send back a response with status 201 Created.
     *
     * @param context routing context
     * @param content body content in JSON format
     */
    protected void created(RoutingContext context, String content) {
        context.response().setStatusCode(201)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(content);
    }

    /**
     * Send back a response with status 204 No Content.
     *
     * @param context routing context
     */
    protected void noContent(RoutingContext context) {
        context.response().setStatusCode(204).end();
    }

    /**
     * Send back a response with status 400 Bad Request.
     *
     * @param context routing context
     * @param ex      exception
     */
    protected void badRequest(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(400)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    /**
     * Send back a response with status 400 Bad Request.
     *
     * @param context routing context
     */
    protected void badRequest(RoutingContext context) {
        context.response().setStatusCode(400)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("error", "bad_request").encodePrettily());
    }

    /**
     * Send back a response with status 404 Not Found.
     *
     * @param context routing context
     */
    protected void notFound(RoutingContext context) {
        context.response().setStatusCode(404)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("message", "not_found").encodePrettily());
    }

    /**
     * Send back a response with status 500 Internal Error.
     *
     * @param context routing context
     * @param ex      exception
     */
    protected void internalError(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(500)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    /**
     * Send back a response with status 500 Internal Error.
     *
     * @param context routing context
     * @param cause   error message
     */
    protected void internalError(RoutingContext context, String cause) {
        context.response().setStatusCode(500)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("error", cause).encodePrettily());
    }

    /**
     * Send back a response with status 503 Service Unavailable.
     *
     * @param context routing context
     */
    protected void serviceUnavailable(RoutingContext context) {
        context.fail(503);
    }

    /**
     * Send back a response with status 503 Service Unavailable.
     *
     * @param context routing context
     * @param ex      exception
     */
    protected void serviceUnavailable(RoutingContext context, Throwable ex) {
        context.response().setStatusCode(503)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("error", ex.getMessage()).encodePrettily());
    }

    /**
     * Send back a response with status 503 Service Unavailable.
     *
     * @param context routing context
     * @param cause   error message
     */
    protected void serviceUnavailable(RoutingContext context, String cause) {
        context.response().setStatusCode(503)
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .end(new JsonObject().put("error", cause).encodePrettily());
    }

}
