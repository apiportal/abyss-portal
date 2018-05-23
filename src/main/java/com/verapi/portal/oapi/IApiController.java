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

package com.verapi.portal.oapi;

import com.verapi.portal.common.Constants;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.IService;
import io.vertx.reactivex.ext.web.RoutingContext;

import java.lang.reflect.InvocationTargetException;

public interface IApiController {
    String mountPoint = Constants.ABYSS_ROOT + "/oapi";

    void init();

    <T extends IService> void getSubjects(RoutingContext routingContext, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException;

}
