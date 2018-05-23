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

public class NotImplemented501Exception extends AbyssApiException {

    public NotImplemented501Exception(String message) {
        super(message);
        this.httpResponseStatus = HttpResponseStatus.NOT_IMPLEMENTED;
    }

    public NotImplemented501Exception(String message, Throwable cause) {
        super(message, cause);
        this.httpResponseStatus = HttpResponseStatus.NOT_IMPLEMENTED;
    }

    public NotImplemented501Exception(Throwable cause) {
        super(cause);
        this.httpResponseStatus = HttpResponseStatus.NOT_IMPLEMENTED;
    }

    public NotImplemented501Exception(String message, boolean noStackTrace) {
        super(message, noStackTrace);
        this.httpResponseStatus = HttpResponseStatus.NOT_IMPLEMENTED;
    }

    public NotImplemented501Exception(ApiSchemaError apiSchemaError) {
        super(apiSchemaError.toString());
        this.apiSchemaError = apiSchemaError;
        this.httpResponseStatus = HttpResponseStatus.NOT_IMPLEMENTED;
        apiSchemaError.setCode(this.httpResponseStatus.code());
    }

    public ApiSchemaError getApiError() {
        return this.apiSchemaError;
    }
}
