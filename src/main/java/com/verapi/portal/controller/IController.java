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

package com.verapi.portal.controller;

import io.vertx.core.Handler;
import io.vertx.reactivex.ext.web.RoutingContext;

public interface IController<T> extends Handler<RoutingContext> {

    void defaultGetHandler(RoutingContext routingContext);

    @Override
    void handle(RoutingContext event);
}
