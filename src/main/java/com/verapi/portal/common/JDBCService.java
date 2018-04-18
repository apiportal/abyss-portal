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

package com.verapi.portal.common;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.JDBCDataSource;
import io.vertx.servicediscovery.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCService {

    private static Logger logger = LoggerFactory.getLogger(JDBCService.class);
    protected Vertx vertx;

    private Record record;

    public JDBCService(Vertx vertx) {
        this.vertx = vertx;
    }

    public Single<Record> publishDataSource() {
        logger.info("publishDataSource() running");
        record = JDBCDataSource.createRecord(
                Constants.PORTAL_DATA_SOURCE_SERVICE,
                new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
                new JsonObject().put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
        );

        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).flatMap(record1 -> {

            logger.info("publishDataSource() successful");
            record = record1;
            return Single.just(record1);
        });
    }

    public Completable unpublishDataSource() {
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).toCompletable();
    }

    public Single<JDBCClient> getJDBCServiceObject() {
        logger.info("getJDBCServiceObject() running");
        return JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), new JsonObject().put("name", Constants.PORTAL_DATA_SOURCE_SERVICE));
    }

    public void releaseJDBCServiceObject(JDBCClient jdbcClient) {
        logger.info("releaseJDBCServiceObject() running");
        ServiceDiscovery.releaseServiceObject(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), jdbcClient);
    }

}
