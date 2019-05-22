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

package com.verapi.portal.handler;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.reactivex.Single;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.sql.SQLConnection;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class UserGroups extends AbstractPortalHandler implements Handler<RoutingContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserGroups.class);
    private static final int PAGESIZE = 30;

    private final JDBCClient jdbcClient;

    //private String result;

    public UserGroups(JDBCClient jdbcClient) {
        super();
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        LOGGER.info("UserGroups.handle invoked..");

        //TODO: pagination eklenmeli

        jdbcClient.rxGetConnection().flatMap((SQLConnection resConn) ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(Boolean.FALSE)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT " +
                                "uuid," +
                                //"organization_id," +
                                "created," +
                                "updated," +
                                "deleted," +
                                "is_deleted," +
                                //"crud_subject_id," +
                                "is_enabled," +
                                "group_name," +
                                "description," +
                                "effective_start_date," +
                                "effective_end_date " +
                                "FROM portalschema.SUBJECT_GROUP ORDER BY group_name", new JsonArray()))
                        .flatMap((ResultSet resultSet) -> {
                            if (resultSet.getNumRows() > 0) {
                                LOGGER.info("Number of groups found:[{}]", resultSet.getNumRows());
                                return Single.just(resultSet);
                            } else {
                                LOGGER.info("No groups found...");
                                return Single.error(new Exception("No groups found"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)
        ).subscribe((ResultSet result) -> {
                    LOGGER.info("Subscription to UserGroups successfull:" + result);
                    JsonObject groupsResult = new JsonObject();
                    groupsResult.put("groupList", result.toJson().getValue("rows"));
                    groupsResult
                            .put("totalPages", 1)
                            .put("totalItems", result.getNumRows())
                            .put("pageSize", PAGESIZE)
                            .put("currentPage", 1)
                            .put("last", Boolean.TRUE)
                            .put("first", Boolean.TRUE)
                            .put("sort", "ASC GROUP NAME");
                    routingContext
                            .response()
                            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8")
                            .end(groupsResult.toString(), StandardCharsets.UTF_8.toString());
                }, (Throwable t) -> {
                    LOGGER.error("UserGroups Error", t);
                    generateResponse(routingContext, LOGGER, HttpStatus.SC_UNAUTHORIZED, "UserGroups Handling Error Occured", t.getLocalizedMessage(), "");

                }
        );
    }


    public void pageRender(RoutingContext routingContext) {
        LOGGER.info("UserGroups.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_USERGROUPS, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }

    public void dirPageRender(RoutingContext routingContext) {
        LOGGER.info("UserGroups.dirPageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_USERDIRECTORIES, (AsyncResult<Buffer> res) -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }
}
