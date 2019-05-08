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

package com.verapi.portal.common;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
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

public class AbyssJDBCService {

    private static Logger logger = LoggerFactory.getLogger(AbyssJDBCService.class);
    protected Vertx vertx;

    private Record record;

    public AbyssJDBCService(Vertx vertx) {
        this.vertx = vertx;
    }

    public Single<Record> publishDataSource(String dataSourceName) {
        logger.trace("AbyssJDBCService.publishDataSource() running for " + dataSourceName);
        record = JDBCDataSource.createRecord(
                dataSourceName,
                new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL)),
                new JsonObject()
                        .put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL))
                        .put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                        .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                        .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                        .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
                        .put("max_idle_time", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_IDLE_TIME))
        );

        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxPublish(record).flatMap(record1 -> {

            logger.trace("AbyssJDBCService.publishDataSource() successful " + record1.toJson().encodePrettily());
            record = record1;
            return Single.just(record1);
        });
    }

    public Completable unpublishDataSource() {
        logger.trace("AbyssJDBCService.unpublishDataSource() running");
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxUnpublish(record.getRegistration());
    }

    public Single<JDBCClient> getJDBCServiceObject(String dataSourceName) {
        logger.trace("AbyssJDBCService.getJDBCServiceObject() running for " + dataSourceName);
///***************** TODO: service discovery den alınan jdbc client shared çalışmıyor, bu nedenle aşağıdaki kod eklendi
//        JsonObject jdbcConfig = new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL))
//                .put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
//                .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
//                .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
//                .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE));
//
//        JDBCClient jdbcClient = JDBCClient.createShared(vertx, jdbcConfig, dataSourceName);
//
//        return Single.just(jdbcClient);

///***************
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", dataSourceName))
                .flatMapSingle(record1 -> {
                    logger.trace("AbyssJDBCService.getJDBCServiceObject() getServiceDiscovery().rxGetRecord for " + dataSourceName + ", record = " + record1.toJson().encodePrettily());
                    JDBCClient jdbcClient = JDBCClient.createShared(vertx, record1.getMetadata(), dataSourceName);
                    logger.trace("AbyssJDBCService.getJDBCServiceObject() JDBCClient.createShared for " + dataSourceName + " jdbcClient = " + jdbcClient);
                    return Single.just(jdbcClient);
                    //return Single.just(JDBCClient.createShared(vertx, record1.getMetadata(), dataSourceName));
                });


//        return JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), new JsonObject().put("name", dataSourceName));
    }

    public void releaseJDBCServiceObject(JDBCClient jdbcClient) {
        logger.trace("AbyssJDBCService.releaseJDBCServiceObject() running");
        ServiceDiscovery.releaseServiceObject(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), jdbcClient);
    }

}
