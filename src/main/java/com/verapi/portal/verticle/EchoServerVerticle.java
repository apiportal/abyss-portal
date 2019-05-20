/*
 * Copyright 2019 Verapi Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private static final Logger LOGGER = LoggerFactory.getLogger(EchoServerVerticle.class);

    @Override
    public void start(Future<Void> startFuture) throws Exception {
        LOGGER.trace("---start invoked");
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
                    LOGGER.error("EchoServerVerticle error");
                    LOGGER.error(t.getLocalizedMessage());
                    LOGGER.error(Arrays.toString(t.getStackTrace()));
                });
        server.listen(
                Config.getInstance().getConfigJsonObject().getInteger(Constants.HTTP_ECHO_SERVER_PORT),
                Config.getInstance().getConfigJsonObject().getString(Constants.HTTP_ECHO_SERVER_HOST)
                , result -> {
                    if (result.succeeded()) {
                        LOGGER.trace("echo server started");
                        try {
                            super.start(startFuture);
                        } catch (Exception e) {
                            LOGGER.error("echo server is unable to start");
                            LOGGER.error(result.cause().getLocalizedMessage());
                            LOGGER.error(Arrays.toString(result.cause().getStackTrace()));
                            startFuture.fail(e);
                        }
                    } else {
                        LOGGER.error("echo server is unable to start");
                        LOGGER.error(result.cause().getLocalizedMessage());
                        LOGGER.error(Arrays.toString(result.cause().getStackTrace()));
                        startFuture.fail(result.cause());
                    }
                });

    }

}
