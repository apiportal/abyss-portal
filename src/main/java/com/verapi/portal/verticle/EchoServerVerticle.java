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

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.vertx.core.Future;
import io.vertx.core.http.HttpHeaders;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.core.http.HttpServer;
import io.vertx.reactivex.core.http.HttpServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class EchoServerVerticle extends AbstractVerticle {
    private static Logger logger = LoggerFactory.getLogger(EchoServerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        logger.trace("---start invoked");
        HttpServer server = vertx.createHttpServer();
        server.requestStream().toFlowable().subscribe(req -> {
                    HttpServerResponse resp = req.response();
                    String contentType = req.getHeader(HttpHeaders.CONTENT_TYPE);
                    if (contentType != null) {
                        resp.putHeader("abyss-echo-server", "")
                                .putHeader(HttpHeaders.CONTENT_TYPE, contentType);
                    }
                    resp.setChunked(true);
                    req.toFlowable().subscribe(
                            resp::write,
                            err -> {
                            },
                            resp::end
                    );
                },
                t -> {
                    logger.error("EchoServerVerticle error");
                    logger.error(t.getLocalizedMessage());
                    logger.error(Arrays.toString(t.getStackTrace()));
                });
        server.listen(
                Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_ECHO_SERVER_PORT),
                Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_ECHO_SERVER_HOST)
                , result -> {
                    if (result.succeeded()) {
                        logger.trace("echo server started");
                        try {
                            super.start(startFuture);
                        } catch (Exception e) {
                            logger.error("echo server is unable to start");
                            logger.error(result.cause().getLocalizedMessage());
                            logger.error(Arrays.toString(result.cause().getStackTrace()));
                            startFuture.fail(e);
                        }
                    } else {
                        logger.error("echo server is unable to start");
                        logger.error(result.cause().getLocalizedMessage());
                        logger.error(Arrays.toString(result.cause().getStackTrace()));
                        startFuture.fail(result.cause());
                    }
                });

    }

}
