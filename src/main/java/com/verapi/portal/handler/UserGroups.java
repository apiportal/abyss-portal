package com.verapi.portal.handler;

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import io.vertx.reactivex.ext.web.templ.ThymeleafTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserGroups extends PortalHandler implements Handler<RoutingContext> {

    private static Logger logger = LoggerFactory.getLogger(UserGroups.class);

    private final JDBCClient jdbcClient;

    //private String result;

    public UserGroups(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void handle(RoutingContext routingContext) {
        logger.info("UserGroups.handle invoked..");

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
                                "is_enabled," +
                                "group_name," +
                                "description," +
                                "effective_start_date," +
                                "effective_end_date " +
                                "FROM portalschema.SUBJECT_GROUP", new JsonArray()))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("Number of groups found:[" + resultSet.getNumRows() + "]");
                                //result = resultSet.toJson().encode();
                                return Single.just(resultSet);
                            } else {
                                logger.info("No groups found...");
                                return Single.error(new Exception("No groups found"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(resConn::close)
        ).subscribe(result -> {
                    logger.info("Subscription to UserGroups successfull:" + result);
                    routingContext.response().end(result.getRows().toString(), "UTF-8" );//.concat("\"totalItems\""+result.getNumRows())); //TODO: Türkçe karakter problemine bakılacak. users.js UTF-8 ...
                    //TODO: json envelope ve footer data eklenmesi
                }, t -> {
                    logger.error("UserGroups Error", t);
                    generateResponse(routingContext, logger, 401, "UserGroups Handling Error Occured", t.getLocalizedMessage(), "", "");

                }
        );
    }



    public void pageRender(RoutingContext routingContext) {
        logger.info("UserGroups.pageRender invoked...");

        // In order to use a Thymeleaf template we first need to create an engine
        final ThymeleafTemplateEngine engine = ThymeleafTemplateEngine.create();

        // we define a hardcoded title for our application
        //routingContext.put("signin", "Sign in Abyss");
        // and now delegate to the engine to render it.
        engine.render(routingContext, "webroot/", "user-groups.html", res -> {
            if (res.succeeded()) {
                routingContext.response().putHeader("Content-Type", "text/html");
                routingContext.response().end(res.result());
            } else {
                routingContext.fail(res.cause());
            }
        });
    }
}