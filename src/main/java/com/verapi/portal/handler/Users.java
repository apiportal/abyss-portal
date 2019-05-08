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
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.thymeleaf.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Users extends PortalHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(Users.class);

    private final JDBCClient jdbcClient;

    //private String result;

    public Users(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("Users.handle invoked..");

        //TODO: pagination eklenmeli

        jdbcClient.rxGetConnection().flatMap(resConn ->
                resConn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(resQ -> resConn.rxQueryWithParams("SELECT " +
                                "uuid," +
                                //"organization_id," +
                                "created," +
                                "updated," +
                                "deleted," +
                                "is_deleted," +
                                //"crud_subject_id," +
                                "is_activated," +
                                //"subject_type_id," +
                                "subject_name," +
                                "first_name," +
                                "last_name," +
                                "display_name," +
                                "email," +
                                //"secondary_email," +
                                "effective_start_date," +
                                "effective_end_date " +
                                "FROM portalschema.SUBJECT ORDER BY SUBJECT_NAME", new JsonArray()))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("Number of users found:[" + resultSet.getNumRows() + "]");
                                //result = resultSet.toJson().encode();
                                return Single.just(resultSet);
                            } else {
                                logger.info("No users found...");
                                return Single.error(new Exception("No users found"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)
        ).subscribe(result -> {
                    logger.info("Subscription to Users successfull:" + result);
                    JsonObject usersResult = new JsonObject();
                    usersResult.put("userList", result.toJson().getValue("rows"));
                    usersResult.put("totalPages", 1).put("totalItems", result.getNumRows()).put("pageSize", 30).put("currentPage", 1).put("last", true).put("first", true).put("sort", "ASC SUBJECT NAME");
                    routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json; charset=utf-8").end(usersResult.toString(), "UTF-8");
                }, t -> {
                    logger.error("Users Error", t);
                    generateResponse(routingContext, logger, 401, "Users Handling Error Occured", t.getLocalizedMessage(), "", "");

                }
        );
    }


    public void pageRender(RoutingContext routingContext) {
        logger.info("Users.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create(routingContext.vertx());

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(new JsonObject(), Constants.TEMPLATE_DIR_ROOT + Constants.HTML_USERS, res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }
}
