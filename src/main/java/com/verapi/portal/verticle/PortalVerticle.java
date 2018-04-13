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

package com.verapi.portal.verticle;

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.common.Controllers;
import com.verapi.portal.controller.IndexController;
import com.verapi.portal.controller.LoginController;
import com.verapi.portal.controller.SignupController;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortalVerticle extends AbyssAbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(PortalVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        super.setHostParams(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_SERVER_HOST),
                Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_PORT),
                Router.router(vertx));
        //mountControllerRouters();
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        super.stop(stopFuture);
    }

    @Override
    protected void mountControllerRouters() {
        LoginController loginController = new LoginController(jdbcAuth);
        mountControllerRouter(HttpMethod.GET, Controllers.LOGIN.routePathGET, loginController::defaultPostHandler);
        mountControllerRouter(HttpMethod.POST, Controllers.LOGIN.routePathPOST, loginController);

        IndexController indexController = new IndexController(jdbcAuth);
        mountControllerRouter(HttpMethod.GET, Controllers.INDEX.routePathGET, indexController::defaultPostHandler);
        mountControllerRouter(HttpMethod.POST, Controllers.INDEX.routePathPOST, indexController);

        SignupController signupController = new SignupController(jdbcAuth, jdbcClient);
        mountControllerRouter(HttpMethod.GET, Controllers.SIGNUP.routePathGET, signupController::defaultPostHandler);
        mountControllerRouter(HttpMethod.POST, Controllers.SIGNUP.routePathPOST, signupController);

    }

}
