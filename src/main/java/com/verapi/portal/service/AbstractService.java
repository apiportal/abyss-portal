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

package com.verapi.portal.service;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.AbyssServiceDiscovery;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import io.vertx.reactivex.servicediscovery.ServiceDiscovery;
import io.vertx.reactivex.servicediscovery.types.JDBCDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractService<T> implements IService<T>, AutoCloseable {

    private static Logger logger = LoggerFactory.getLogger(AbstractService.class);

    protected Vertx vertx;
    protected JDBCClient jdbcClient;
    private AbyssJDBCService abyssJDBCService;

    public AbstractService(Vertx vertx, AbyssJDBCService abyssJDBCService) throws Exception {
        logger.info("AbstractService() invoked " + vertx + abyssJDBCService);
        this.vertx = vertx;
        this.abyssJDBCService = abyssJDBCService;
    }

    public AbstractService(Vertx vertx) throws Exception {
        logger.info("AbstractService() invoked " + vertx);
        this.vertx = vertx;
    }

    public Single<JDBCClient> initJDBCClient() {
        logger.info("AbstractService() initJDBCClient ok");
/*
        return abyssJDBCService.getJDBCServiceObject(Constants.API_DATA_SOURCE_SERVICE)
                .flatMap(jdbcClient1 -> {
                    this.jdbcClient = jdbcClient1;
                    return Single.just(jdbcClient1);
                });
*/

        return JDBCDataSource.rxGetJDBCClient(AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery(), new JsonObject().put("name", Constants.API_DATA_SOURCE_SERVICE))
                .flatMap(jdbcClient1 -> {
                    this.jdbcClient = jdbcClient1;
                    return Single.just(jdbcClient1);
                });
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
    }
}
