/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.oapi;

import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
//import io.vertx.reactivex.ext.web.api.RequestParameters;
import io.vertx.ext.web.api.RequestParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssApiController(apiSpec = "/openapi/Subject.yaml")
public class SubjectApiController extends AbstractApiController {
    private static Logger logger = LoggerFactory.getLogger(SubjectApiController.class);

    public SubjectApiController(Vertx vertx, Router router, JDBCAuth authProvider) {
        super(vertx, router, authProvider);
    }

    @AbyssApiOperationHandler
    public void getSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

        // Get the parsed parameters
        //RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        //JsonObject subjects = params.body().getJsonObject();

        JsonObject subjects = new JsonObject().put("path", "getSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString())
                    .put("method", methodName);

        }
/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

    @AbyssApiOperationHandler
    public void addSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

            // Get the parsed parameters
            RequestParameters params = routingContext.get("parsedParameters");

            // We get an user JSON object validated by Vert.x Open API validator
            JsonObject subjects = params.body().getJsonObject();

/*
        JsonObject subjects = new JsonObject().put("path", "addSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString());
        }
*/


/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

    @AbyssApiOperationHandler
    public void updateSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

        // Get the parsed parameters
        //RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        //JsonObject subjects = params.body().getJsonObject();

        JsonObject subjects = new JsonObject().put("path", "addSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString());
            //.put("method", getClass().getEnclosingMethod().getName());

        }
/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

    @AbyssApiOperationHandler
    public void deleteSubjects(RoutingContext routingContext) {
        String methodName = new Object() {
        }.getClass().getEnclosingMethod().getName();
        logger.info(methodName + " invoked");

        // Get the parsed parameters
        //RequestParameters params = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        //JsonObject subjects = params.body().getJsonObject();

        JsonObject subjects = new JsonObject().put("path", "addSubjects invoked");
        if (routingContext.user() != null) {
            subjects
                    .put("user", routingContext.user().toString())
                    .put("user.principal", routingContext.user().principal().toString());
            //.put("method", getClass().getEnclosingMethod().getName());

        }
/*
        if (1 == 1) {
            throwApiException(routingContext, BadRequest400Exception.class, "test exception", "very detailed message");
            return;
        }
*/

        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .setStatusCode(200)
                .end(subjects.toString(), "UTF-8");

    }

}
