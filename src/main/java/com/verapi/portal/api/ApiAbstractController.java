/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.portal.api;

import com.verapi.portal.common.AbyssJDBCService;
import io.reactivex.Single;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ApiAbstractController<T> implements IApiController<T> {
    private static Logger logger = LoggerFactory.getLogger(ApiAbstractController.class);

    @Override
    public Single<Message<T>> sendToEB(String address, Object message, DeliveryOptions options) {
        return null;
    }

    @Override
    public abstract Single<JsonObject> handle(Vertx vertx, Message message, AbyssJDBCService abyssJDBCService) throws Exception;

}
