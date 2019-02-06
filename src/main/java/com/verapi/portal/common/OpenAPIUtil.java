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

package com.verapi.portal.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.verapi.abyss.exception.UnProcessableEntity422Exception;
import io.reactivex.Single;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.ResolverCache;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.contract.RouterFactoryException;
import io.vertx.ext.web.api.contract.openapi3.impl.OpenAPI3RouterFactoryImpl;
import io.vertx.ext.web.api.contract.openapi3.impl.OpenApi3Utils;
import io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAPIUtil {
    private static Logger logger = LoggerFactory.getLogger(OpenAPIUtil.class);

    public static final String OPENAPI_SECTION_SERVERS = "servers";

    @Deprecated
    public static Single<SwaggerParseResult> _openAPIParser(JsonObject apiSpec) {
        logger.trace("---openAPIParser invoked");
        ObjectMapper mapper;
        String data = apiSpec.toString();
        try {
            if (data.trim().startsWith("{")) {
                mapper = ObjectMapperFactory.createJson();
            } else {
                mapper = ObjectMapperFactory.createYaml();
            }
            JsonNode rootNode = mapper.readTree(data);
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readWithInfo("streamed yaml", rootNode);
            if (swaggerParseResult.getMessages().isEmpty()) {
                logger.trace("openAPIParser OK");
                return Single.just(swaggerParseResult);
            } else {
                if (swaggerParseResult.getMessages().size() == 1 && swaggerParseResult.getMessages().get(0).matches("unable to read location")) {
                    logger.error("openAPIParser error for: {}| {}", data.substring(1, 40), swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecNotExistsException(""));
                } else {
                    logger.error("openAPIParser error for: {}| {}", data.substring(1, 40), swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecInvalidException(StringUtils.join(swaggerParseResult.getMessages(), ", ")));
                }
            }
        } catch (Exception e) {
            logger.error("openAPIParser error | {} | {}", e.getLocalizedMessage(), e.getStackTrace());
            return Single.error(RouterFactoryException.createSpecInvalidException(e.getLocalizedMessage()));
        }
    }

    public static Single<SwaggerParseResult> openAPIParser(JsonObject apiSpec) {
        logger.trace("---openAPIParser invoked");
        String data = apiSpec.toString();
        try {
            //SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(data);
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(data, null, OpenApi3Utils.getParseOptions());
            if (swaggerParseResult.getMessages().isEmpty()) {
                logger.trace("openAPIParser OK");
                return Single.just(swaggerParseResult);
            } else {
                if (swaggerParseResult.getMessages().size() == 1 && swaggerParseResult.getMessages().get(0).matches("unable to read location")) {
                    logger.error("openAPIParser error for: {}| {}", data.substring(1, 40), swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecNotExistsException(""));
                } else {
                    logger.error("openAPIParser error for: {}| {}", data.substring(1, 40), swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecInvalidException(StringUtils.join(swaggerParseResult.getMessages(), ", ")));
                }
            }
        } catch (Exception e) {
            logger.error("openAPIParser error | {} | {}", e.getLocalizedMessage(), e.getStackTrace());
            return Single.error(RouterFactoryException.createSpecInvalidException(e.getLocalizedMessage()));
        }
    }

    public static Single<JsonArray> openAPIParser(String apiSpec) {
        logger.trace("---openAPIParser invoked");
        ObjectMapper mapper;
        try {
            if (apiSpec.trim().startsWith("{")) {
                mapper = ObjectMapperFactory.createJson();
            } else {
                mapper = ObjectMapperFactory.createYaml();
            }
            JsonNode rootNode = mapper.readTree(apiSpec);
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readWithInfo("streamed yaml", rootNode);
            JsonArray jsonArray = new JsonArray();
            if (swaggerParseResult.getMessages().size() == 0) {
                //no result message means openapi spec is valid, it is OK, so return empty error message array
                return Single.just(jsonArray);
            } else {
                //there are parse validation errors, so return eror message array
                swaggerParseResult.getMessages().forEach(jsonArray::add);
                return Single.error(new UnProcessableEntity422Exception(jsonArray.encode()));
            }
        } catch (Exception e) {
            logger.error("openAPIParser error | {} | {}", e.getLocalizedMessage(), e.getStackTrace());
            return Single.error(RouterFactoryException.createSpecInvalidException(e.getLocalizedMessage()));
        }
    }

    public static void createOpenAPI3RouterFactory(io.vertx.reactivex.core.Vertx vertx, String yaml, Handler<AsyncResult<OpenAPI3RouterFactory>> handler) {
        SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readContents(yaml);
        createOpenAPI3RouterFactory(vertx, swaggerParseResult.getOpenAPI(), handler);
    }

    public static void createOpenAPI3RouterFactory(io.vertx.reactivex.core.Vertx vertx, OpenAPI openAPI, Handler<AsyncResult<OpenAPI3RouterFactory>> handler) {
        createOpenAPI3RouterFactoryImpl(vertx.getDelegate(), openAPI, ar -> {
            if (ar.succeeded()) {
                handler.handle(Future.succeededFuture(io.vertx.reactivex.ext.web.api.contract.openapi3.OpenAPI3RouterFactory.newInstance(ar.result())));
            } else {
                handler.handle(Future.failedFuture(ar.cause()));
            }
        });
    }

    private static void createOpenAPI3RouterFactoryImpl(Vertx vertx, OpenAPI openAPI, Handler<AsyncResult<io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory>>
            handler) {
        vertx.executeBlocking((Future<io.vertx.ext.web.api.contract.openapi3.OpenAPI3RouterFactory> future) -> {
            future.complete(new OpenAPI3RouterFactoryImpl(vertx, openAPI, new ResolverCache(openAPI, null, null)));
        }, handler);
    }

}
