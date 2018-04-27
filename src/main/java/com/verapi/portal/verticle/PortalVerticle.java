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
import com.verapi.portal.controller.AbyssController;
import com.verapi.portal.controller.Controllers;
import com.verapi.portal.controller.IController;
import com.verapi.portal.controller.PortalAbstractController;
import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.matchprocessor.ClassAnnotationMatchProcessor;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Path;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

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
/*
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
            mountControllerRouter(jdbcAuth, Controllers.MYAPIS);
*/

        new FastClasspathScanner("com.verapi")
                //.verbose()
                .matchClassesWithAnnotation(AbyssController.class, new ClassAnnotationMatchProcessor() {
                    @Override
                    public void processMatch(Class<?> classWithAnnotation) {
                        logger.info("AbyssController annotated class found and mounted : " + classWithAnnotation);
                        IController<PortalAbstractController> requestHandlerInstance = null;
                        try {
                            requestHandlerInstance = (IController<PortalAbstractController>) classWithAnnotation
                                    .getConstructor(JDBCAuth.class, JDBCClient.class)
                                    .newInstance(jdbcAuth, jdbcClient);
                            if (!classWithAnnotation.getAnnotation(AbyssController.class).isPublic())
                                verticleRouter.route("/" + classWithAnnotation.getAnnotation(AbyssController.class).routePathGET())
                                        .handler(authHandler).failureHandler(PortalVerticle.super::failureHandler);
                            verticleRouter.route(HttpMethod.GET, "/" + classWithAnnotation.getAnnotation(AbyssController.class).routePathGET())
                                    .handler(requestHandlerInstance::defaultGetHandler).failureHandler(PortalVerticle.super::failureHandler);
                            verticleRouter.route(HttpMethod.POST, "/" + classWithAnnotation.getAnnotation(AbyssController.class).routePathPOST())
                                    .handler(requestHandlerInstance).failureHandler(PortalVerticle.super::failureHandler);

                        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                            logger.error("PortalVerticle.mountControllerRouters() exception : " + e.getLocalizedMessage() + Arrays.toString(e.getStackTrace()));
                        }
                    }
                })
                .scan();
    }

}
