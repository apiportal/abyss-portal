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

package com.verapi.portal.oapi.exception;

import com.verapi.portal.oapi.AbstractApiController;
import com.verapi.portal.oapi.schema.ApiSchemaError;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbyssApiException extends AbyssException {
    private static Logger logger = LoggerFactory.getLogger(AbyssApiException.class);

    ApiSchemaError apiSchemaError;
    HttpResponseStatus httpResponseStatus; //http://www.restapitutorial.com/httpstatuscodes.html

    public AbyssApiException(String message) {
        super(message);
    }

    public AbyssApiException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbyssApiException(Throwable cause) {
        super(cause);
    }

    public AbyssApiException(String message, boolean noStackTrace) {
        super(message, noStackTrace);
    }

    public AbyssApiException(ApiSchemaError apiSchemaError) {
        super(apiSchemaError.toString());
        logger.error(this.getClass().getName(), apiSchemaError);
        this.apiSchemaError = apiSchemaError;
    }

    public ApiSchemaError getApiError() {
        return this.apiSchemaError;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }
}

