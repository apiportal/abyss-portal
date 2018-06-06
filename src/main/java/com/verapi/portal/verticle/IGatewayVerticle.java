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

package com.verapi.portal.verticle;

import io.reactivex.Completable;
import io.vertx.reactivex.ext.web.RoutingContext;

public interface IGatewayVerticle {

    void routingContextHandler(RoutingContext context);
    Completable loadAllProxyApis();

}
