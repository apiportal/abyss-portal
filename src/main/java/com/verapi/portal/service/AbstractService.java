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
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.oapi.schema.ApiSchemaError;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
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
import java.util.List;
import java.util.UUID;

public abstract class AbstractService<T> implements IService<T> {

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

    private Single<CompositeResult> rxUpdateWithParams(String sql) {
        return rxUpdateWithParams(sql, null);
    }

    private Single<CompositeResult> rxUpdateWithParams(String sql, JsonArray params) {
        logger.trace("---rxUpdateWithParams invoked");
        return jdbcClient
                .rxGetConnection()
                .flatMap(conn -> conn
                                .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                                // Disable auto commit to handle transaction manually
                                .rxSetAutoCommit(false)
                                // Switch from Completable to default Single value
                                .toSingleDefault(false)
                                //.toObservable()
                                //execute query
                                .flatMap(insertConn -> (params == null) ? conn.rxUpdate(sql)
                                        //.onErrorReturnItem(new UpdateResult().setKeys(new JsonArray().add(0)).setUpdated(1))
                                        //.onErrorResumeNext(throwable ->  Single.just(new UpdateResult().setKeys(new JsonArray().add(1)).setUpdated(1)))
                                        :
                                        conn.rxUpdateWithParams(sql, params))
                                //       .onErrorReturnItem(new UpdateResult().setKeys(new JsonArray().add(0)).setUpdated(1)))
                                //.onErrorResumeNext(throwable ->  Single.just(new UpdateResult().setKeys(new JsonArray().add(1)).setUpdated(1)))
                                .flatMap(resultSet -> {
                                    if (resultSet.getUpdated() == 0) {
                                        logger.error("unable to process sql with parameters");
                                        logger.error("unable to process sql {} with parameters {}", sql, params);
                                        //return Observable.error(new Exception("unable to process sql with parameters"));
                                        return Single.error(new Exception("unable to process sql with parameters"));
                                    }
                                    logger.trace("{}::rxUpdateWithParams >> {} row(s) processed", this.getClass().getName(), resultSet.getUpdated());
                                    //return Observable.just(resultSet);
                                    return Single.just(resultSet);
                                })

                                .flatMap(updateResult -> conn.rxCommit().toSingleDefault(updateResult).map(commit -> new CompositeResult(updateResult)))


/*
                        // commit if all succeeded
                        .flatMap(updateResult -> {
                            logger.trace("{}::rxUpdateWithParams >> commit processed", this.getClass().getName());
                            return conn.rxCommit().toSingleDefault(new CompositeResult(updateResult));
                        })
*/


                                // Rollback if any failed with exception propagation
                                .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(new CompositeResult(ex)).map(compositeResult -> compositeResult)
                                        .onErrorResumeNext(ex2 -> Single.just(new CompositeResult(new CompositeException(ex, ex2)))) //Single.error(new CompositeException(ex, ex2)))
                                        .flatMap(ignore -> {
                                            logger.warn("rollback transaction completed");
                                            logger.error(ex.getLocalizedMessage());
                                            logger.error(Arrays.toString(ex.getStackTrace()));
                                            //return Single.just(new UpdateResult().setKeys(new JsonArray().add(0)).setUpdated(1));
                                            return Single.just(new CompositeResult(ex));
                                        }))


//                                .onErrorResumeNext(throwable -> {
//                                    logger.warn("rollback transaction completed");
//                                    logger.error(throwable.getLocalizedMessage());
//                                    logger.error(Arrays.toString(throwable.getStackTrace()));
//                                    conn.rxRollback().toSingleDefault(true);
//                                    return Single.just(new CompositeResult(new UpdateResult(), throwable));
//                                })
//
                                .doAfterSuccess(succ -> {
                                    logger.trace("sql processed successfully");
                                })

                                // close the connection regardless succeeded or failed
                                .doAfterTerminate(conn::close)
                );
    }

    private Single<ResultSet> rxQueryWithParams(String sql) {
        return rxQueryWithParams(sql, null);
    }

    private Single<ResultSet> rxQueryWithParams(String sql, JsonArray params) {
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> (params == null) ? conn.rxQuery(sql) : conn.rxQueryWithParams(sql, params))
                        .flatMap(resultSet -> {
                            logger.trace("{}::rxQueryWithParams >> {} row selected", this.getClass().getName(), resultSet.getNumRows());
                            logger.trace("{}::rxQueryWithParams >> sql {} params {}", this.getClass().getName(), sql, params);
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    protected Single<CompositeResult> insert(final JsonArray insertParams, final String insertQuery) {
        logger.trace("---insert invoked");
        return rxUpdateWithParams(insertQuery, insertParams);
    }

    protected Single<CompositeResult> update(final JsonArray updateParams, final String updateQuery) {
        return rxUpdateWithParams(updateQuery, updateParams);
    }

    protected Single<CompositeResult> delete(final UUID uuid, final String deleteQuery) {
        return rxUpdateWithParams(deleteQuery, new JsonArray().add(uuid.toString()));
    }

    protected Single<CompositeResult> deleteAll(final String deleteAllQuery) {
        return rxUpdateWithParams(deleteAllQuery);
    }

    protected Single<ResultSet> findById(final long id, final String findByIdQuery) {
        return rxQueryWithParams(findByIdQuery, new JsonArray().add(id));
    }

    protected Single<ResultSet> findById(final UUID uuid, final String findByIdQuery) {
        return rxQueryWithParams(findByIdQuery, new JsonArray().add(uuid.toString()));
    }

    protected Single<ResultSet> findByName(final String name, final String findByNameQuery) {
        return rxQueryWithParams(findByNameQuery, new JsonArray().add(name));
    }

    protected Single<ResultSet> findLikeName(final String name, final String findLikeNameQuery) {
        return rxQueryWithParams(findLikeNameQuery, new JsonArray().add(name + "%"));
    }

    protected Single<ResultSet> findAll(final String findAllQuery) {
        return rxQueryWithParams(findAllQuery);
    }

    protected void subscribeAndProcess(JsonArray result, Single<ResultSet> resultSetSingle, int httpResponseStatus) {
        resultSetSingle.subscribe(resp -> {
                    JsonArray arr = new JsonArray();
                    resp.getRows().forEach(arr::add);
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", resp.getRows().get(0).getString("uuid"))
                            .put("status", httpResponseStatus)
                            .put("response", arr.getJsonObject(0))
                            .put("error", new ApiSchemaError().toJson());
                    result.add(recordStatus);
                },
                throwable -> {
                    //SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(apiSpec, null, OpenApi3Utils.getParseOptions());
                    //swaggerParseResult.getOpenAPI().getPaths().get("/subjects").getGet().getResponses().get("207")
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", "0")
                            .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put("response", new JsonObject()) //TODO: fill with empty Subject response json
                            .put("error", new ApiSchemaError()
                                    .setUsermessage(throwable.getLocalizedMessage())
                                    .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                    .toJson());
                    result.add(recordStatus);
                });
    }

    protected void subscribeAndProcess(JsonArray result, Observable<ResultSet> resultSetObservable, int httpResponseStatus) {
        resultSetObservable.subscribe(resp -> {
                    JsonArray arr = new JsonArray();
                    resp.getRows().forEach(arr::add);
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", resp.getRows().get(0).getString("uuid"))
                            .put("status", httpResponseStatus)
                            .put("response", arr.getJsonObject(0))
                            .put("error", new ApiSchemaError().toJson());
                    result.add(recordStatus);
                },
                throwable -> {
                    //SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(apiSpec, null, OpenApi3Utils.getParseOptions());
                    //swaggerParseResult.getOpenAPI().getPaths().get("/subjects").getGet().getResponses().get("207")
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", "0")
                            .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put("response", new JsonObject()) //TODO: fill with empty Subject response json
                            .put("error", new ApiSchemaError()
                                    .setUsermessage(throwable.getLocalizedMessage())
                                    .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                    .toJson());
                    result.add(recordStatus);
                });
    }

}
