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

public class NotFound404Exception extends AbyssApiException {

    public NotFound404Exception(String message) {
        super(message);
        this.httpResponseStatus = HttpResponseStatus.NOT_FOUND;
    }

    public NotFound404Exception(String message, Throwable cause) {
        super(message, cause);
        this.httpResponseStatus = HttpResponseStatus.NOT_FOUND;
    }

    public NotFound404Exception(Throwable cause) {
        super(cause);
        this.httpResponseStatus = HttpResponseStatus.NOT_FOUND;
    }

    public NotFound404Exception(String message, boolean noStackTrace) {
        super(message, noStackTrace);
        this.httpResponseStatus = HttpResponseStatus.NOT_FOUND;
    }

    public NotFound404Exception(ApiSchemaError apiSchemaError) {
        super(apiSchemaError.toString());
        this.apiSchemaError = apiSchemaError;
        this.httpResponseStatus = HttpResponseStatus.NOT_FOUND;
        apiSchemaError.setCode(this.httpResponseStatus.code());
    }

    public ApiSchemaError getApiError() {
        return this.apiSchemaError;
    }
}
