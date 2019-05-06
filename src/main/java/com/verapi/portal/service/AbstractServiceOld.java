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

package com.verapi.portal.service;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Deprecated
public abstract class AbstractServiceOld<T> implements IServiceOld<T>, AutoCloseable {

    private static Logger logger = LoggerFactory.getLogger(AbstractServiceOld.class);

    protected Vertx vertx;
    protected JDBCClient jdbcClient;
    private AbyssJDBCService abyssJDBCService;

    public AbstractServiceOld(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        logger.trace("AbstractServiceOld() invoked " + vertx + abyssJDBCService);
        this.vertx = vertx;
        this.abyssJDBCService = abyssJDBCService;
    }

    public AbstractServiceOld(Vertx vertx) {
        logger.trace("AbstractServiceOld() invoked");
        this.vertx = vertx;
    }

    public Single<JDBCClient> initJDBCClient() {
        logger.trace("AbstractServiceOld() initJDBCClient ok");

/*
        return JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), new JsonObject().put("name", Constants.API_DATA_SOURCE_SERVICE))
                .flatMap(jdbcClient1 -> {
                    this.jdbcClient = jdbcClient1;
                    return Single.just(jdbcClient1);
                });
*/

///***************** TODO: service discovery den alınan jdbc client shared çalışmıyor, bu nedenle aşağıdaki kod eklendi
        JsonObject jdbcConfig = new JsonObject().put("url", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_URL))
                .put("driver_class", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_JDBC_DRIVER_CLASS))
                .put("user", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_NAME))
                .put("password", Config.getInstance().getConfigJsonObject().getString(Constants.PORTAL_DBUSER_PASSWORD))
                .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE));

        this.jdbcClient = JDBCClient.createShared(vertx, jdbcConfig, Constants.API_DATA_SOURCE_SERVICE);

        return Single.just(jdbcClient);

///***************

/*
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", Constants.API_DATA_SOURCE_SERVICE))
                .flatMap(record1 -> Single.just(JDBCClient.createShared(vertx, record1.getMetadata(), Constants.API_DATA_SOURCE_SERVICE)));
*/

/* TODO: aşağıdaki kodda vertx sapıtıyor ve hata üretiyor, JDBCClient.createShared hata fırlatıyor
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", Constants.API_DATA_SOURCE_SERVICE))
                .flatMap(record1 -> {
                    logger.trace("AbstractServiceOld() initJDBCClient() getServiceDiscovery().rxGetRecord for " + Constants.API_DATA_SOURCE_SERVICE + ", record = " + record1.toJson().encodePrettily());
                    JDBCClient jdbcClient = JDBCClient.createShared(vertx, record1.getMetadata(), Constants.API_DATA_SOURCE_SERVICE);
                    logger.trace("AbstractServiceOld() initJDBCClient() JDBCClient.createShared for " + Constants.API_DATA_SOURCE_SERVICE + " jdbcClient = " + jdbcClient);
                    return Single.just(jdbcClient);
                    //return Single.just(JDBCClient.createShared(vertx, record1.getMetadata(), dataSourceName));
                });
*/

    }


    public Vertx getVertx() {
        return vertx;
    }

    public void setVertx(Vertx vertx) {
        this.vertx = vertx;
    }

    public JDBCClient getJdbcClient() {
        return jdbcClient;
    }

    public void setJdbcClient(JDBCClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void close() throws Exception {
        abyssJDBCService.releaseJDBCServiceObject(jdbcClient);
        logger.trace("AbstractServiceOld.close() invoked " + vertx);
    }
}
