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

import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.abyss.sql.builder.metadata.AbyssDatabaseMetadataDiscovery;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.es.ElasticSearchService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.jdbc.JDBCClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.UUID;

public abstract class AbstractService<T> implements IService<T> {

    private static Logger logger = LoggerFactory.getLogger(AbstractService.class);

    protected Vertx vertx;
    protected JDBCClient jdbcClient;
    protected DatabaseMetaData databaseMetaData;
    private AbyssJDBCService abyssJDBCService;
    protected static ElasticSearchService elasticSearchService = new ElasticSearchService();
    protected String organizationUuid;// = Constants.DEFAULT_ORGANIZATION_UUID;


    public static final String SQL_AND = "and\n";

    protected static final String SQL_WHERE = "where\n";

    protected static final String SQL_CONDITION_ID_IS = "id = ?\n";

    protected static final String SQL_CONDITION_UUID_IS = "uuid = CAST(? AS uuid)\n";

    protected static final String SQL_CONDITION_ORGANIZATION_IS = "organizationid = CAST(? AS uuid)\n";

    protected static final String SQL_FROM = "from\n";


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
                .put("max_pool_size", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_POOL_SIZE))
                .put("max_idle_time", Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBCONN_MAX_IDLE_TIME));

        this.jdbcClient = JDBCClient.createShared(vertx, jdbcConfig, Constants.API_DATA_SOURCE_SERVICE);

        /*return jdbcClient.rxGetConnection()
                .flatMap(sqlConnection -> {
                    java.sql.Connection con = sqlConnection.getDelegate().unwrap();
                    databaseMetaData = con.getMetaData();
                    con.close();
                    logger.trace("AbstractService - Got database metadata successfully: {} {}",
                            databaseMetaData.getDatabaseProductName(), databaseMetaData.getDatabaseProductVersion());
                    return Single.just(jdbcClient);
                });*/

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

    public Single<JDBCClient> initJDBCClient(String organizationUuid) {
        this.organizationUuid = organizationUuid;
        return initJDBCClient();
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
                                    if (resultSet.getUpdated() == 0 && !sql.contains("ON CONFLICT DO NOTHING")) {
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
/*
        java.sql.ResultSet tableMetaDataResultSet = null;
        try {
            tableMetaDataResultSet = databaseMetaData.getColumns("", "abyss", "api", "");
            try {
                while (tableMetaDataResultSet.next()) {
                    logger.debug("metadata:: table name:{} column name:{}, data type:{}"
                            , tableMetaDataResultSet.getString("TABLE_NAME"), tableMetaDataResultSet.getString("COLUMN_NAME"), tableMetaDataResultSet.getString("DATA_TYPE"));
                }
            } finally {
                tableMetaDataResultSet.close();
            }
        } catch (SQLException e) {
            logger.error("an error occurred while getting columns metada of API table. {}", (Object) e.getStackTrace());
        }
*/
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //execute query
                        .flatMap(conn1 -> (params == null) ? conn.rxQuery(sql) : conn.rxQueryWithParams(sql, params))
//                        .flatMap(conn1 -> { //TODO: Improve Organization Filter
//                            String tableName = getTableNameFromSql(sql).toLowerCase();
//                            logger.trace("TableName>>> {}\nSQL>>> {}\n", tableName, sql);
//                            boolean isParamTable = AbyssDatabaseMetadataDiscovery.getInstance().getTableMetadata(tableName).isParamTable;
//                            boolean isAdmin = false; //TODO: Admin Only
//                            boolean doesContainOrderBy = false; //TODO: Order By Handling
//                            logger.trace("SQL>>>> params:{} isParamTable:{}\n", params==null, isParamTable);
//                            if (organizationUuid == null || isParamTable) {
//                                if (params == null) {
//                                    return conn.rxQuery(sql);
//                                } else {
//                                    return conn.rxQueryWithParams(sql, params);
//                                }
//                            } else {
//
//                                if (params == null) {
//                                    return conn.rxQueryWithParams(sql.contains(SQL_WHERE) ? sql + SQL_AND + tableName + "."+ SQL_CONDITION_ORGANIZATION_IS : sql + SQL_WHERE + SQL_CONDITION_ORGANIZATION_IS, new JsonArray().add(organizationUuid));
//                                } else {
//                                    return conn.rxQueryWithParams(sql + SQL_AND + tableName + "."+ SQL_CONDITION_ORGANIZATION_IS, params.add(organizationUuid));
//                                }
//                            }
//                        })
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

    protected Single<CompositeResult> deleteAll(String sql, JsonArray params) {
        return rxUpdateWithParams(sql, params);
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

    protected Single<ResultSet> filter(ApiFilterQuery apiFilterQuery) {
        return rxQueryWithParams(apiFilterQuery.getFilterQuery(), apiFilterQuery.getFilterQueryParams());
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


    public enum Aggregation {
        COUNT("count"),
        SUM("sum"),
        AVG("avg");

        private String aggregation;

        Aggregation(String aggregation) {
            this.aggregation = aggregation;
        }

        public String getAggregation() {
            return aggregation;
        }

        public String getAggregationSQL(String aggregationColumn) {
            String aggregationCol = "*";

            if (aggregationColumn != null && !aggregationColumn.isEmpty()) {
                aggregationCol = aggregationColumn;
            }

            switch (this) {
                case COUNT:
                    return ",count(" + aggregationCol + ") as count\n";
                case SUM:
                    return ",sum(" + aggregationCol + ") as sum\n";
                case AVG:
                    return ",avg(" + aggregationCol + ") as avg\n";
                default:
                    return ",count(" + aggregationCol + ") as count\n";
                //throw new Exception("Unknown Aggregation " + this);
            }
        }
    }

    private static String getTableNameFromSql(String sql) {

        if (sql==null || sql.isEmpty())
            return "";

        sql = sql.toLowerCase();

        int tableNameStartIndex = sql.indexOf(SQL_FROM)+SQL_FROM.length();
        if (tableNameStartIndex==-1)
            return "";

        String tableNameStr = sql.substring(tableNameStartIndex);

        int tableNameEndIndex = tableNameStr.indexOf("\n");
        if (tableNameEndIndex==-1)
            return "";

        tableNameStr = tableNameStr.substring(0, tableNameEndIndex).trim();

        return tableNameStr;
    }
}
