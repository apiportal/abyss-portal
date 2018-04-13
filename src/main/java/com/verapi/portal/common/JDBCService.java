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
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.JDBCDataSource;
import io.vertx.servicediscovery.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JDBCService extends AbstractVerticle {

    private static Logger logger = LoggerFactory.getLogger(JDBCService.class);

    private Record record;

    public JDBCService() {
    }

    public Completable publishDataSource() {
        record = JDBCDataSource.createRecord(
                Constants.PORTAL_DATA_SOURCE_SERVICE,
                new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
                new JsonObject().put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
        );
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).toCompletable();
    }

    public Completable unpublishDataSource() {
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).toCompletable();
    }

    public Single<JDBCClient> getJDBCServiceObject() {
        return JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), new JsonObject().put("name", Constants.PORTAL_DATA_SOURCE_SERVICE));
    }

    public void releaseJDBCServiceObject(JDBCClient jdbcClient) {
        ServiceDiscovery.releaseServiceObject(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), jdbcClient);
    }

}
