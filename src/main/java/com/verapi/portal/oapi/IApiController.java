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
import io.vertx.reactivex.ext.web.RoutingContext;

public interface IApiController {
    String mountPoint = Constants.ABYSS_ROOT + "/oapi";

/*
    @SuppressWarnings("unused")
    void getSubjects(RoutingContext routingContext);

    @SuppressWarnings("unused")
    void addSubjects(RoutingContext routingContext);

    @SuppressWarnings("unused")
    void updateSubjects(RoutingContext routingContext);

    @SuppressWarnings("unused")
    void deleteSubjects(RoutingContext routingContext);

    @SuppressWarnings("unused")
    void getSubject(RoutingContext routingContext);

    @SuppressWarnings("unused")
    void updateSubject(RoutingContext routingContext);

    @SuppressWarnings("unused")
    void deleteSubject(RoutingContext routingContext);
*/

}
