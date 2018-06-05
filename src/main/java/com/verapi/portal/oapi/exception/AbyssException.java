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

import io.vertx.core.VertxException;

public abstract class AbyssException extends VertxException  {
    public AbyssException(String message) {
        super(message);
    }

    public AbyssException(String message, Throwable cause) {
        super(message, cause);
    }

    public AbyssException(Throwable cause) {
        super(cause);
    }

    public AbyssException(String message, boolean noStackTrace) {
        //super(message, noStackTrace);
        super(message);
    }
}
