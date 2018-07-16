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
import com.verapi.portal.oapi.exception.UnProcessableEntity422Exception;
import io.reactivex.Single;
import io.swagger.v3.parser.ObjectMapperFactory;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.api.contract.RouterFactoryException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OpenAPIUtil {
    private static Logger logger = LoggerFactory.getLogger(OpenAPIUtil.class);

    public static final String OPENAPI_SECTION_SERVERS = "servers";

    public static Single<SwaggerParseResult> openAPIParser(JsonObject apiSpec) {
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
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readWithInfo(rootNode);
            if (swaggerParseResult.getMessages().isEmpty()) {
                logger.trace("openAPIParser OK");
                return Single.just(swaggerParseResult);
            } else {
                if (swaggerParseResult.getMessages().size() == 1 && swaggerParseResult.getMessages().get(0).matches("unable to read location")) {
                    logger.error("openAPIParser error for: {}| {}", data.substring(1,40), swaggerParseResult.getMessages());
                    return Single.error(RouterFactoryException.createSpecNotExistsException(""));
                } else {
                    logger.error("openAPIParser error for: {}| {}", data.substring(1,40), swaggerParseResult.getMessages());
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
            SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readWithInfo(rootNode);
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

}
