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

package com.verapi.portal.oapi.util;

import com.verapi.abyss.common.OpenAPIUtil;
import com.verapi.abyss.exception.InternalServerError500Exception;
import com.verapi.portal.common.PlatformAPIList;
import com.verapi.portal.oapi.AbstractApiController;
import com.verapi.portal.oapi.AbyssApiController;
import com.verapi.portal.oapi.AbyssApiOperationHandler;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@AbyssApiController(apiSpec = "/openapi/Util.yaml")
public class UtilApiController extends AbstractApiController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UtilApiController.class);

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
            LOGGER.trace("getYamlFileList invoked");
            subscribeAndResponse(routingContext, PlatformAPIList.getInstance().getPlatformAPIList(), HttpResponseStatus.OK.code());
        } catch (Exception e) {
            LOGGER.error("getYamlFileList error : {}\n{}", e.getLocalizedMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void validateOpenAPIv3Spec(RoutingContext routingContext) {
        try {
            LOGGER.trace("validateOpenAPIv3Spec invoked");
            // Get the parsed parameters
            RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

            // We get an user JSON array validated by Vert.x Open API validator
            JsonObject requestBody = requestParameters.body().getJsonObject();

            subscribeAndResponseBulk(routingContext, OpenAPIUtil.openAPIParser(requestBody.getJsonObject("spec").encode()), HttpResponseStatus.OK.code());
        } catch (Exception e) {
            LOGGER.error("validateOpenAPIv3Spec error : {}\n{}", e.getLocalizedMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }

    @AbyssApiOperationHandler
    public void convertSwaggerToOpenAPIv3Spec(RoutingContext routingContext) {
        try {
            LOGGER.trace("convertSwaggerToOpenAPIv3Spec invoked");
            // Get the parsed parameters
            RequestParameters requestParameters = routingContext.get(PARSED_PARAMETERS);

            // We get an user JSON array validated by Vert.x Open API validator
            JsonObject requestBody = requestParameters.body().getJsonObject();

            subscribeAndResponseString(routingContext
                    , OpenAPIUtil.convertSwaggerToOpenAPI(requestBody.getJsonObject("spec"))
                    , HttpResponseStatus.OK.code(), false);
        } catch (Exception e) {
            LOGGER.error("convertSwaggerToOpenAPIv3Spec error : {}\n{}", e.getLocalizedMessage(), e.getStackTrace());
            throwApiException(routingContext, InternalServerError500Exception.class, e.getLocalizedMessage());
        }
    }


}
