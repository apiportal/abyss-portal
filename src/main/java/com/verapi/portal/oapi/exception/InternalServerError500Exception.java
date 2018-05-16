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

public class InternalServerError500Exception extends AbyssApiException {

    public InternalServerError500Exception(String message) {
        super(message);
        this.httpResponseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    public InternalServerError500Exception(String message, Throwable cause) {
        super(message, cause);
        this.httpResponseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    public InternalServerError500Exception(Throwable cause) {
        super(cause);
        this.httpResponseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    public InternalServerError500Exception(String message, boolean noStackTrace) {
        super(message, noStackTrace);
        this.httpResponseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
    }

    public InternalServerError500Exception(ApiSchemaError apiSchemaError) {
        super(apiSchemaError.toString());
        this.apiSchemaError = apiSchemaError;
        this.httpResponseStatus = HttpResponseStatus.INTERNAL_SERVER_ERROR;
        apiSchemaError.setCode(this.httpResponseStatus.code());
    }

    public ApiSchemaError getApiError() {
        return this.apiSchemaError;
    }
}
