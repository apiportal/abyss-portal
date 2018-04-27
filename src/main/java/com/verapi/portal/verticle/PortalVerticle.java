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
import com.verapi.portal.controller.Controllers;
import io.vertx.core.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

public class PortalVerticle extends AbstractPortalVerticle {

    private static Logger logger = LoggerFactory.getLogger(PortalVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.info("PortalVerticle.start invoked");
        super.setVerticleHost(Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_SERVER_HOST));
        super.setServerPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_SERVER_PORT));
        super.start(startFuture);
    }

    @Override
    public void stop(Future<Void> stopFuture) throws Exception {
        logger.info("PortalVerticle.stop invoked");
        super.stop(stopFuture);
    }

    @Override
    protected void mountControllerRouters() {
        logger.info("PortalVerticle.mountControllerRouters() invoked");
        try {
            mountControllerRouter(jdbcAuth, Controllers.LOGIN);
            mountControllerRouter(jdbcAuth, Controllers.INDEX);
            mountControllerRouter(jdbcAuth, Controllers.SIGNUP);
            mountControllerRouter(jdbcAuth, Controllers.TRX_OK);
            mountControllerRouter(jdbcAuth, Controllers.ACTIVATE_ACCOUNT);
            mountControllerRouter(jdbcAuth, Controllers.FORGOT_PASSWORD);
            mountControllerRouter(jdbcAuth, Controllers.RESET_PASSWORD);
            mountControllerRouter(jdbcAuth, Controllers.CHANGE_PASSWORD);
            mountControllerRouter(jdbcAuth, Controllers.USERS);
            mountControllerRouter(jdbcAuth, Controllers.SUBJECTGROUP);
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

}
