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

import com.verapi.portal.oapi.schema.ApiSchemaError;
import io.netty.handler.codec.http.HttpResponseStatus;

public class TooManyRequests429Exception extends AbyssApiException {

    public TooManyRequests429Exception(String message) {
        super(message);
        this.httpResponseStatus = HttpResponseStatus.TOO_MANY_REQUESTS;
    }

    public TooManyRequests429Exception(String message, Throwable cause) {
        super(message, cause);
        this.httpResponseStatus = HttpResponseStatus.TOO_MANY_REQUESTS;
    }

    public TooManyRequests429Exception(Throwable cause) {
        super(cause);
        this.httpResponseStatus = HttpResponseStatus.TOO_MANY_REQUESTS;
    }

    public TooManyRequests429Exception(String message, boolean noStackTrace) {
        super(message, noStackTrace);
        this.httpResponseStatus = HttpResponseStatus.TOO_MANY_REQUESTS;
    }

    public TooManyRequests429Exception(ApiSchemaError apiSchemaError) {
        super(apiSchemaError.toString());
        this.apiSchemaError = apiSchemaError;
        this.httpResponseStatus = HttpResponseStatus.TOO_MANY_REQUESTS;
        apiSchemaError.setCode(this.httpResponseStatus.code());
    }

    public ApiSchemaError getApiError() {
        return this.apiSchemaError;
    }
}
