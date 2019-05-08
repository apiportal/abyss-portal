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
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssController(routePathGET = "failure", routePathPOST = "failure", htmlTemplateFile = "failure.html", isPublic = true)
public class FailureController extends PortalAbstractController {

    private static Logger logger = LoggerFactory.getLogger(FailureController.class);

    public FailureController(JDBCAuth authProvider, JDBCClient jdbcClient) {
        super(authProvider, jdbcClient);
    }

    @Override
    public void defaultGetHandler(RoutingContext routingContext) {
        logger.trace("FailureController.defaultGetHandler invoked...");
    }

    @Override
    public void handle(RoutingContext routingContext) {
        Integer statusCode = routingContext.session().get(Constants.HTTP_STATUSCODE);
        statusCode = (statusCode != null) ? (statusCode > 0) ? statusCode : 200 : 200;


        logger.trace("FailureController.handle invoked - status code: " + statusCode);

        routingContext.put(Constants.HTTP_STATUSCODE, statusCode);
        routingContext.put(Constants.HTTP_URL, routingContext.session().get(Constants.HTTP_URL));
        routingContext.put(Constants.HTTP_ERRORMESSAGE, routingContext.session().get(Constants.HTTP_ERRORMESSAGE));
        routingContext.put(Constants.CONTEXT_FAILURE_MESSAGE, routingContext.session().get(Constants.CONTEXT_FAILURE_MESSAGE));

        JsonObject templateContext = new JsonObject()
                .put(Constants.HTTP_STATUSCODE, statusCode)
                .put(Constants.HTTP_URL, (String) routingContext.session().get(Constants.HTTP_URL))
                .put(Constants.HTTP_ERRORMESSAGE, (String) routingContext.session().get(Constants.HTTP_ERRORMESSAGE))
                .put(Constants.CONTEXT_FAILURE_MESSAGE, (String) routingContext.session().get(Constants.CONTEXT_FAILURE_MESSAGE));


        renderTemplate(routingContext, templateContext, Controllers.TRX_NOK.templateFileName, statusCode);

        super.handle(routingContext); //TODO: bu satır çağrılmalı mı?
    }

}
