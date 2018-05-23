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
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public abstract class AbstractService implements IService {

    private static Logger logger = LoggerFactory.getLogger(AbstractService.class);

    protected Vertx vertx;
    protected JDBCClient jdbcClient;
    private AbyssJDBCService abyssJDBCService;

    public AbstractService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        this.vertx = vertx;
        this.abyssJDBCService = abyssJDBCService;
    }

    public AbstractService(Vertx vertx) {
        this.vertx = vertx;
    }

    public Single<JDBCClient> initJDBCClient() {

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
                    logger.trace("AbstractService() initJDBCClient() getServiceDiscovery().rxGetRecord for " + Constants.API_DATA_SOURCE_SERVICE + ", record = " + record1.toJson().encodePrettily());
                    JDBCClient jdbcClient = JDBCClient.createShared(vertx, record1.getMetadata(), Constants.API_DATA_SOURCE_SERVICE);
                    logger.trace("AbstractService() initJDBCClient() JDBCClient.createShared for " + Constants.API_DATA_SOURCE_SERVICE + " jdbcClient = " + jdbcClient);
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
    public Single<UpdateResult> insert(final JsonArray insertParams, final String insertQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(insertConn -> conn.rxUpdateWithParams(insertQuery, insertParams))
                        .flatMap(resultSet -> {
                            if (resultSet.getUpdated() == 0)
                                return Single.error(new Exception("unable to insert new record"));
                            logger.trace("{}::insert >> {} row(s) updated", this.getClass().getName(), resultSet.getUpdated());
                            return Single.just(resultSet);
                        })

                        // commit if all succeeded
                        .flatMap(updateResult -> conn.rxCommit().toSingleDefault(updateResult))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> {
                                    logger.warn("rollback!!");
                                    logger.error(ex.getLocalizedMessage());
                                    logger.error(Arrays.toString(ex.getStackTrace()));
                                    return Single.error(ex);
                                })
                        )

                        .doAfterSuccess(succ -> {
                            logger.trace("inserted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<UpdateResult> update(final UUID uuid, final JsonArray updateParams, final String updateQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(updateConn -> conn.rxUpdateWithParams(updateQuery, updateParams))
                        .flatMap(resultSet -> {
                            if (resultSet.getUpdated() == 0)
                                return Single.error(new Exception("unable to update record"));
                            logger.trace("{}::update >> {} row(s) updated", this.getClass().getName(), resultSet.getUpdated());
                            return Single.just(resultSet);
                        })

                        // commit if all succeeded
                        .flatMap(updateResult -> conn.rxCommit().toSingleDefault(updateResult))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> {
                                    logger.warn("rollback!!");
                                    logger.error(ex.getLocalizedMessage());
                                    logger.error(Arrays.toString(ex.getStackTrace()));
                                    return Single.error(ex);
                                })
                        )

                        .doAfterSuccess(succ -> {
                            logger.trace("updated successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<UpdateResult> updateAll(ArrayList<UUID> uuid, JsonObject updateRecord) {
        return null;
    }

    @Override
    public Single<UpdateResult> delete(final UUID uuid, final String deleteQuery) {
        JsonArray whereConditionParam = new JsonArray().add(uuid);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> conn.rxUpdateWithParams(deleteQuery, whereConditionParam))
                        .flatMap(resultSet -> {
                            if (resultSet.getUpdated() == 0)
                                return Single.error(new Exception("unable to update record for deletion marking"));
                            logger.trace("{}::deleteAll >> {} row(s) updated for deletion marking", this.getClass().getName(), resultSet.getUpdated());
                            return Single.just(resultSet);
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> conn.rxCommit().toSingleDefault(updateResult))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> {
                                    logger.warn("rollback!!");
                                    logger.error(ex.getLocalizedMessage());
                                    logger.error(Arrays.toString(ex.getStackTrace()));
                                    return Single.error(ex);
                                })
                        )

                        .doAfterSuccess(succ -> {
                            logger.trace("logically deleted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<UpdateResult> deleteAll(final String deleteAllQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> conn.rxUpdate(deleteAllQuery))
                        .flatMap(resultSet -> {
                            if (resultSet.getUpdated() == 0)
                                return Single.error(new Exception("unable to update record(s) for deletion marking"));
                            logger.trace("{}::deleteAll >> {} row(s) updated for deletion marking", this.getClass().getName(), resultSet.getUpdated());
                            return Single.just(resultSet);
                        })
                        // commit if all succeeded
                        .flatMap(updateResult -> conn.rxCommit().toSingleDefault(updateResult))

                        // Rollback if any failed with exception propagation
                        .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(true)
                                .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                .flatMap(ignore -> {
                                    logger.warn("rollback!!");
                                    logger.error(ex.getLocalizedMessage());
                                    logger.error(Arrays.toString(ex.getStackTrace()));
                                    return Single.error(ex);
                                })
                        )

                        .doAfterSuccess(succ -> {
                            logger.trace("logically deleted successfully");
                        })

                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<ResultSet> findById(final long id, final String findByIdQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> conn.rxQueryWithParams(findByIdQuery, new JsonArray().add(id)))
                        .flatMap(resultSet -> {
                            logger.trace("{}::findById >> {} row selected", this.getClass().getName(), resultSet.getNumRows());
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<ResultSet> findById(final UUID uuid, final String findByIdQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> conn.rxQueryWithParams(findByIdQuery, new JsonArray().add(uuid)))
                        .flatMap(resultSet -> {
                            logger.trace("{}::findById >> {} row selected", this.getClass().getName(), resultSet.getNumRows());
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<ResultSet> findByName(final String name, final String findByNameQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> conn.rxQueryWithParams(findByNameQuery, new JsonArray().add(name)))
                        .flatMap(resultSet -> {
                            logger.trace("{}::findByName >> {} row selected", this.getClass().getName(), resultSet.getNumRows());
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Single<ResultSet> findAll(final String findAllQuery) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> conn.rxQuery(findAllQuery))
                        .flatMap(resultSet -> {
                            logger.trace("{}::findAll >> {} row selected", this.getClass().getName(), resultSet.getNumRows());
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }
}
