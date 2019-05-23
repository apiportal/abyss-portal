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

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.abyss.sql.builder.metadata.AbyssDatabaseMetadataDiscovery;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.es.ElasticSearchService;
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
import io.vertx.reactivex.ext.sql.SQLConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.DatabaseMetaData;
import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

public abstract class AbstractService<T> implements IService<T> {

    public static final String SQL_AND = "and\n";
    protected static final String EXCEPTION_LOG_FORMAT = "{}\n{}";
    protected static final String SQL_WHERE = "where\n";
    protected static final String SQL_CONDITION_ID_IS = "id = ?\n";
    protected static final String SQL_CONDITION_UUID_IS = "uuid = CAST(? AS uuid)\n";
    protected static final String SQL_CONDITION_ORGANIZATION_IS = "organizationid = CAST(? AS uuid)\n";
    protected static final String SQL_CONDITION_ORGANIZATION_INSIDE = "organizationid in (CAST(? AS uuid), '" + Constants.DEFAULT_ORGANIZATION_UUID + "'::uuid)\n";
    protected static final String SQL_FROM = "from\n";
    protected static final String SQL_UPDATE_VERB = "update ";
    protected static final String SQL_INSERT_INTO_VERB = "insert into ";
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);
    protected static ElasticSearchService elasticSearchService = new ElasticSearchService();
    protected Vertx vertx;
    protected JDBCClient jdbcClient;
    protected DatabaseMetaData databaseMetaData;
    protected String organizationUuid;// = Constants.DEFAULT_ORGANIZATION_UUID;
    protected String operationId;
    protected Boolean autoCommit = Boolean.TRUE;
    protected SQLConnection sqlConnection = null;
    private AbyssJDBCService abyssJDBCService;
    private String tableName;
    protected static final String STR_UUID = "uuid";
    protected static final String STR_STATUS = "status";
    protected static final String STR_RESPONSE = "response";
    protected static final String STR_ERROR = "error";



    public AbstractService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        this.vertx = vertx;
        this.abyssJDBCService = abyssJDBCService;
        this.tableName = this.getClass().getAnnotation(AbyssTableName.class).tableName();
        LOGGER.trace("Table name: {}", this.tableName);
    }

    public AbstractService(Vertx vertx) {
        this.vertx = vertx;
        this.tableName = this.getClass().getAnnotation(AbyssTableName.class).tableName();
        LOGGER.trace("Table name: {}", this.tableName);
    }

    private static String getTableNameFromSql(String sql) {

        if (sql == null || sql.isEmpty())
            return "";

        sql = sql.toLowerCase();

        int tableNameStartIndex = sql.indexOf(SQL_FROM);
        if (tableNameStartIndex == -1)
            return "";

        tableNameStartIndex = tableNameStartIndex + SQL_FROM.length();

        String tableNameStr = sql.substring(tableNameStartIndex);

        int tableNameEndIndex = tableNameStr.indexOf("\n");
        if (tableNameEndIndex == -1)
            return "";

        tableNameStr = tableNameStr.substring(0, tableNameEndIndex).trim();

        return tableNameStr;
    }

    private static String getTableNameFromSqlForUpdate(String sql) {

        if (sql == null || sql.isEmpty())
            return "";

        sql = sql.toLowerCase();


        int tableNameStartIndex;
        String tableNameStr;
        int tableNameEndIndex;

        int indexOfSqlUpdateVerb = sql.indexOf(SQL_UPDATE_VERB);

        if (indexOfSqlUpdateVerb == -1) {
            int indexOfSqlInsertIntoVerb = sql.indexOf(SQL_INSERT_INTO_VERB);
            if (indexOfSqlInsertIntoVerb == -1) {
                return "";
            } else { //Insert Into
                return "INSERT";
                /*tableNameStartIndex = indexOfSqlInsertIntoVerb+SQL_INSERT_INTO_VERB.length();
                tableNameStr = sql.substring(tableNameStartIndex);
                tableNameEndIndex = tableNameStr.indexOf(" ");*/
            }
        } else { //Update
            tableNameStartIndex = indexOfSqlUpdateVerb + SQL_UPDATE_VERB.length();
            tableNameStr = sql.substring(tableNameStartIndex);
            tableNameEndIndex = tableNameStr.indexOf("\n");
        }

        if (tableNameEndIndex == -1)
            return "";

        tableNameStr = tableNameStr.substring(0, tableNameEndIndex).trim();

        return tableNameStr;
    }

    protected static JsonObject evaluateCompositeResultAndReturnRecordStatus(CompositeResult result) { //TODO: Static?
        return evaluateCompositeResultAndReturnRecordStatus(result, null);
    }

//    public Single<JDBCClient> initJDBCClient(String organizationUuid, SQLConnection sqlConnection) {
//        this.organizationUuid = organizationUuid;
//        this.sqlConnection = sqlConnection;
//        return initJDBCClient();
//    }

    protected static JsonObject evaluateCompositeResultAndReturnRecordStatus(CompositeResult result, JsonObject parentRecordStatus) { //TODO: Static?
        JsonObject recordStatus = new JsonObject();
        if (result.getThrowable() != null) {
            LOGGER.trace("insertAll>> insert/find exception {}", result.getThrowable());
            LOGGER.error(result.getThrowable().getLocalizedMessage());
            LOGGER.error(Arrays.toString(result.getThrowable().getStackTrace()));
            recordStatus
                    .put(STR_UUID, "0")
                    .put(STR_STATUS, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                    .put(STR_RESPONSE, new JsonObject())
                    .put(STR_ERROR, new ApiSchemaError()
                            .setUsermessage(result.getThrowable().getLocalizedMessage())
                            .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .setInternalmessage(Arrays.toString(result.getThrowable().getStackTrace()))
                            .toJson());
        } else {
            LOGGER.trace("evaluateCompositeResultAndReturnRecordStatus>> insert getKeys {}", result.getUpdateResult().getKeys().encodePrettily());
            if (result.getResultSet() != null) {
                LOGGER.trace("evaluateCompositeResultAndReturnRecordStatus>> insert ResultSet {}", result.getResultSet().toJson().encodePrettily());
            } else {
                LOGGER.trace("evaluateCompositeResultAndReturnRecordStatus>> insert ResultSet is null");
            }

            if (result.getResultSet() != null && result.getResultSet().getNumRows() > 0) {
                JsonArray arr = new JsonArray();
                result.getResultSet().getRows().forEach(arr::add);
                recordStatus
                        .put(STR_UUID, result.getResultSet().getRows().get(0).getString(STR_UUID))
                        .put(STR_STATUS, HttpResponseStatus.CREATED.code())
                        .put(STR_RESPONSE, arr.getJsonObject(0))
                        .put(STR_ERROR, new ApiSchemaError().toJson());
            }
            if (parentRecordStatus != null) {
                recordStatus.put("parentRecordStatus", parentRecordStatus);
            }
        }
        return recordStatus;
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
                    LOGGER.trace("AbstractService - Got database metadata successfully: {} {}",
                            databaseMetaData.getDatabaseProductName(), databaseMetaData.getDatabaseProductVersion());
                    return Single.just(jdbcClient);
                });*/

        return Single.just(jdbcClient);

///***************

/*
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", Constants.API_DATA_SOURCE_SERVICE))
                .flatMap(record1 -> Single.just(JDBCClient.createShared(vertx, record1.getMetadata(), Constants.API_DATA_SOURCE_SERVICE)));
*/

//TODO: aşağıdaki kodda vertx sapıtıyor ve hata üretiyor, JDBCClient.createShared hata fırlatıyor
/*
        return AbyssServiceDiscovery.getInstance(vertx).getServiceDiscovery().rxGetRecord(new JsonObject().put("name", Constants.API_DATA_SOURCE_SERVICE))
                .flatMap(record1 -> {
                    LOGGER.trace("AbstractService() initJDBCClient() getServiceDiscovery().rxGetRecord for " + Constants.API_DATA_SOURCE_SERVICE + ", record = " + record1.toJson().encodePrettily());
                    JDBCClient jdbcClient = JDBCClient.createShared(vertx, record1.getMetadata(), Constants.API_DATA_SOURCE_SERVICE);
                    LOGGER.trace("AbstractService() initJDBCClient() JDBCClient.createShared for " + Constants.API_DATA_SOURCE_SERVICE + " jdbcClient = " + jdbcClient);
                    return Single.just(jdbcClient);
                    //return Single.just(JDBCClient.createShared(vertx, record1.getMetadata(), dataSourceName));
                });
*/

    }

    public Single<JDBCClient> initJDBCClient(String organizationUuid) {
        this.organizationUuid = organizationUuid;
        return initJDBCClient();
    }

    public Single<JDBCClient> initJDBCClient(String organizationUuid, String operationId) {
        this.organizationUuid = organizationUuid;
        this.operationId = operationId;
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

    public Boolean getAutoCommit() {
        return autoCommit;
    }

    public void setAutoCommit(Boolean autoCommit) {
        this.autoCommit = autoCommit;
    }

    private Single<CompositeResult> rxUpdateWithParams(String sql) {
        return rxUpdateWithParams(sql, null);
    }

    private Single<CompositeResult> rxUpdateWithParams(String sql, JsonArray params) {
        LOGGER.trace("---rxUpdateWithParams invoked");
        return (sqlConnection == null ? jdbcClient
                .rxGetConnection() : Single.just(sqlConnection))
                .flatMap((SQLConnection conn) -> conn
                                .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                                // Disable auto commit to handle transaction manually
                                .rxSetAutoCommit(false)
                                // Switch from Completable to default Single value
                                .toSingleDefault(Boolean.FALSE)
                                //.toObservable()
                                //execute query
//                                .flatMap(insertConn -> (params == null) ? conn.rxUpdate(sql)
//                                        //.onErrorReturnItem(new UpdateResult().setKeys(new JsonArray().add(0)).setUpdated(1))
//                                        //.onErrorResumeNext(throwable ->  Single.just(new UpdateResult().setKeys(new JsonArray().add(1)).setUpdated(1)))
//                                        :
//                                        conn.rxUpdateWithParams(sql, params))
//                                //       .onErrorReturnItem(new UpdateResult().setKeys(new JsonArray().add(0)).setUpdated(1)))
//                                //.onErrorResumeNext(throwable ->  Single.just(new UpdateResult().setKeys(new JsonArray().add(1)).setUpdated(1)))

                                .flatMap((Boolean conn1) -> { //TODO: Improve Organization Filter
                                    //sqlConnection = conn;

                                    boolean isParamTable = true;
                                    String tableName = "";
                                    //TODO: Organization Filter Disabled for testing
                                    Boolean isOrganizationFilteringEnabled = Config
                                            .getInstance()
                                            .getConfigJsonObject()
                                            .getBoolean(Constants.ACCESS_CONTROL_ORGANIZATION_FILTERING_ENABLED);
                                    LOGGER.trace("isOrganizationFilteringEnabled: {}", isOrganizationFilteringEnabled);
                                    if (isOrganizationFilteringEnabled) {
                                        tableName = getTableNameFromSqlForUpdate(sql).toLowerCase(Locale.ENGLISH);
                                        LOGGER.trace("TableName>>> {}\nSQL>>> {}\n", tableName, sql);
                                        if (!tableName.isEmpty() && !tableName.equals("insert")) {
                                            isParamTable = AbyssDatabaseMetadataDiscovery.getInstance().getTableMetadata(tableName).isParamTable;
                                        }
                                        //TODO: Check organizationid == org uuid in session

                                        //TODO: Admin Only
                                        boolean isAdmin = false;

                                        //TODO: Order By Handling
                                        boolean doesContainOrderBy = false;
                                        LOGGER.trace("SQL>>>> params:{} isParamTable:{}\n", params == null, isParamTable);
                                    }


                                    if (isParamTable || organizationUuid == null) {
                                        if (params == null) {
                                            LOGGER.trace("{}::rxQuery >> sql {}", this.getClass().getName(), sql);
                                            return conn.rxUpdate(sql);
                                        } else {
                                            LOGGER.trace("{}::rxQueryWithParams >> sql {} params {}", this.getClass().getName(), sql, params);
                                            return conn.rxUpdateWithParams(sql, params);
                                        }
                                    } else {
                                        LOGGER.trace("Current organizationUuid: {}", organizationUuid);
                                        String sqlWithOrganizationFilter;
                                        JsonArray paramWithOrganizationFilter;
                                        if (params == null) {
                                            sqlWithOrganizationFilter = sql.contains(SQL_WHERE) ? sql + SQL_AND + tableName + "."
                                                    + SQL_CONDITION_ORGANIZATION_IS : sql + SQL_WHERE + SQL_CONDITION_ORGANIZATION_IS;
                                            paramWithOrganizationFilter = new JsonArray().add(organizationUuid);
                                        } else {
                                            sqlWithOrganizationFilter = sql + SQL_AND + tableName + "." + SQL_CONDITION_ORGANIZATION_IS;
                                            paramWithOrganizationFilter = params.add(organizationUuid);
                                        }
                                        LOGGER.trace("{}::rxQueryWithParams Organization Filtered >> sql {} params {}"
                                                , this.getClass().getName(), sqlWithOrganizationFilter, paramWithOrganizationFilter);
                                        return conn.rxUpdateWithParams(sqlWithOrganizationFilter, paramWithOrganizationFilter);
                                    }
                                })

                                .flatMap((UpdateResult resultSet) -> {
                                    if (resultSet.getUpdated() == 0 && !sql.contains("ON CONFLICT DO NOTHING")) {
                                        LOGGER.error("unable to process sql with parameters");
                                        LOGGER.error("unable to process sql {} with parameters {}", sql, params);
                                        //return Observable.error(new Exception("unable to process sql with parameters"));
                                        return Single.error(new Exception("unable to process sql with parameters"));
                                    }
                                    LOGGER.trace("{}::rxUpdateWithParams >> {} row(s) processed", this.getClass().getName(), resultSet.getUpdated());
                                    //return Observable.just(resultSet);
                                    return Single.just(resultSet);
                                })

                                .flatMap((UpdateResult updateResult) -> {
                                    if (autoCommit) {
                                        return conn.rxCommit().toSingleDefault(updateResult).map(commit -> new CompositeResult(updateResult));
                                    } else {
                                        return Single.just(new CompositeResult(updateResult));
                                    }
                                })


/*
                        // commit if all succeeded
                        .flatMap(updateResult -> {
                            LOGGER.trace("{}::rxUpdateWithParams >> commit processed", this.getClass().getName());
                            return conn.rxCommit().toSingleDefault(new CompositeResult(updateResult));
                        })
*/


                                // Rollback if any failed with exception propagation
                                .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(new CompositeResult(ex)).map(compositeResult -> compositeResult)
                                        .onErrorResumeNext(ex2 -> Single.just(new CompositeResult(new CompositeException(ex, ex2)))) //Single.error(new CompositeException(ex, ex2)))
                                        .flatMap(ignore -> {
                                            LOGGER.warn("rollback transaction completed");
                                            LOGGER.error(ex.getLocalizedMessage());
                                            LOGGER.error(Arrays.toString(ex.getStackTrace()));
                                            //return Single.just(new UpdateResult().setKeys(new JsonArray().add(0)).setUpdated(1));
                                            return Single.just(new CompositeResult(ex));
                                        }))


//                                .onErrorResumeNext(throwable -> {
//                                    LOGGER.warn("rollback transaction completed");
//                                    LOGGER.error(throwable.getLocalizedMessage());
//                                    LOGGER.error(Arrays.toString(throwable.getStackTrace()));
//                                    conn.rxRollback().toSingleDefault(true);
//                                    return Single.just(new CompositeResult(new UpdateResult(), throwable));
//                                })
//
                                .doAfterSuccess(succ -> {
                                    LOGGER.trace("sql processed successfully");
                                })

                                // close the connection regardless succeeded or failed
                                .doAfterTerminate(conn::close) //TODO: ne zaman tetikleniyor?
                );
    }

    private Single<ResultSet> rxQueryWithParams(String sql) {
        return rxQueryWithParams(sql, null);
    }

    private Single<ResultSet> rxQueryWithParams(String sql, JsonArray params) {
        LOGGER.trace("---rxQueryWithParams invoked");
/*
        java.sql.ResultSet tableMetaDataResultSet = null;
        try {
            tableMetaDataResultSet = databaseMetaData.getColumns("", "abyss", "api", "");
            try {
                while (tableMetaDataResultSet.next()) {
                    LOGGER.debug("metadata:: table name:{} column name:{}, data type:{}"
                            , tableMetaDataResultSet.getString("TABLE_NAME"), tableMetaDataResultSet.getString("COLUMN_NAME"), tableMetaDataResultSet.getString("DATA_TYPE"));
                }
            } finally {
                tableMetaDataResultSet.close();
            }
        } catch (SQLException e) {
            LOGGER.error("an error occurred while getting columns metada of API table. {}", (Object) e.getStackTrace());
        }
*/
        return (sqlConnection == null ? jdbcClient
                .rxGetConnection() : Single.just(sqlConnection))
                .flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(Boolean.FALSE)
                        //execute query
                        //.flatMap(conn1 -> (params == null) ? conn.rxQuery(sql) : conn.rxQueryWithParams(sql, params))
                        .flatMap(conn1 -> { //TODO: Improve Organization Filter
                            //sqlConnection = conn;

                            boolean isParamTable = true;
                            String tableName = "";
                            Boolean isOrganizationFilteringEnabled = Config.getInstance().getConfigJsonObject().getBoolean(Constants.ACCESS_CONTROL_ORGANIZATION_FILTERING_ENABLED);  //TODO: Organization Filter Disabled for testing
                            LOGGER.trace("isOrganizationFilteringEnabled: {}", isOrganizationFilteringEnabled);
                            if (isOrganizationFilteringEnabled) {

                                tableName = getTableNameFromSql(sql).toLowerCase();
                                LOGGER.trace("TableName>>> {}\nSQL>>> {}\n", tableName, sql);

                                if (operationId != null && !operationId.equals("getCurrentUser")) {
                                    if (!tableName.isEmpty()) {
                                        isParamTable = AbyssDatabaseMetadataDiscovery.getInstance().getTableMetadata(tableName).isParamTable;
                                    }
                                }

                                boolean isAdmin = false; //TODO: Admin Only
                                boolean doesContainOrderBy = false; //TODO: Order By Handling
                                LOGGER.trace("SQL>>>> params:{} isParamTable:{}\n", params == null, isParamTable);
                            }


                            if (isParamTable || organizationUuid == null) {
                                if (params == null) {
                                    LOGGER.trace("{}::rxQuery >> sql {}", this.getClass().getName(), sql);
                                    return conn.rxQuery(sql);
                                } else {
                                    LOGGER.trace("{}::rxQueryWithParams >> sql {} params {}", this.getClass().getName(), sql, params);
                                    return conn.rxQueryWithParams(sql, params);
                                }
                            } else {
                                LOGGER.trace("Current organizationUuid: {}", organizationUuid);
                                String sqlWithOrganizationFilter;
                                JsonArray paramWithOrganizationFilter;
                                if (params == null) {
                                    sqlWithOrganizationFilter = sql.contains(SQL_WHERE) ? sql + SQL_AND + tableName + "."
                                            + SQL_CONDITION_ORGANIZATION_INSIDE : sql + SQL_WHERE + SQL_CONDITION_ORGANIZATION_INSIDE;
                                    paramWithOrganizationFilter = new JsonArray().add(organizationUuid);
                                } else {
                                    sqlWithOrganizationFilter = sql + SQL_AND + tableName + "." + SQL_CONDITION_ORGANIZATION_INSIDE;
                                    paramWithOrganizationFilter = params.add(organizationUuid);
                                }
                                LOGGER.trace("{}::rxQueryWithParams Organization Filtered >> sql {} params {}"
                                        , this.getClass().getName(), sqlWithOrganizationFilter, paramWithOrganizationFilter);
                                return conn.rxQueryWithParams(sqlWithOrganizationFilter, paramWithOrganizationFilter);
                            }
                        })
                        .flatMap((ResultSet resultSet) -> {
                            LOGGER.trace("{}::rxQueryWithParams >> {} row selected", this.getClass().getName(), resultSet.getNumRows());

                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    protected Single<CompositeResult> insert(final JsonArray insertParams, final String insertQuery) {
        LOGGER.trace("---insert invoked");
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
        resultSetSingle.subscribe((ResultSet resp) -> {
                    JsonArray arr = new JsonArray();
                    resp.getRows().forEach(arr::add);
                    JsonObject recordStatus = new JsonObject()
                            .put(STR_UUID, resp.getRows().get(0).getString(STR_UUID))
                            .put(STR_STATUS, httpResponseStatus)
                            .put(STR_RESPONSE, arr.getJsonObject(0))
                            .put(STR_ERROR, new ApiSchemaError().toJson());
                    result.add(recordStatus);
                },
                (Throwable throwable) -> {
                    //SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(apiSpec, null, OpenApi3Utils.getParseOptions());
                    //swaggerParseResult.getOpenAPI().getPaths().get("/subjects").getGet().getResponses().get("207")
                    JsonObject recordStatus = new JsonObject()
                            .put(STR_UUID, "0")
                            .put(STR_STATUS, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(STR_RESPONSE, new JsonObject()) //TODO: fill with empty Subject response json
                            .put(STR_ERROR, new ApiSchemaError()
                                    .setUsermessage(throwable.getLocalizedMessage())
                                    .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                    .toJson());
                    result.add(recordStatus);
                });
    }

    protected void subscribeAndProcess(JsonArray result, Observable<ResultSet> resultSetObservable, int httpResponseStatus) {
        resultSetObservable.subscribe((ResultSet resp) -> {
                    JsonArray arr = new JsonArray();
                    resp.getRows().forEach(arr::add);
                    JsonObject recordStatus = new JsonObject()
                            .put(STR_UUID, resp.getRows().get(0).getString(STR_UUID))
                            .put(STR_STATUS, httpResponseStatus)
                            .put(STR_RESPONSE, arr.getJsonObject(0))
                            .put(STR_ERROR, new ApiSchemaError().toJson());
                    result.add(recordStatus);
                },
                (Throwable throwable) -> {
                    //SwaggerParseResult swaggerParseResult = new OpenAPIV3Parser().readLocation(apiSpec, null, OpenApi3Utils.getParseOptions());
                    //swaggerParseResult.getOpenAPI().getPaths().get("/subjects").getGet().getResponses().get("207")
                    JsonObject recordStatus = new JsonObject()
                            .put(STR_UUID, "0")
                            .put(STR_STATUS, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put(STR_RESPONSE, new JsonObject()) //TODO: fill with empty Subject response json
                            .put(STR_ERROR, new ApiSchemaError()
                                    .setUsermessage(throwable.getLocalizedMessage())
                                    .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                    .toJson());
                    result.add(recordStatus);
                });
    }

    abstract protected JsonArray prepareInsertParameters(JsonObject insertRecord);

    abstract protected String getInsertSql();

    abstract protected String getFindByIdSql();

    /**
     * insert
     *
     * @param insertRecord
     * @return recordStatus
     */
    public Single<JsonObject> insert(JsonObject insertRecord, JsonObject parentRecordStatus) {
        LOGGER.trace("---insert invoked");

        JsonArray insertParam = prepareInsertParameters(insertRecord);
        return insert(insertParam, getInsertSql())
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        return findById(insertResult.getUpdateResult().getKeys().getInteger(0), getFindByIdSql())
                                .flatMap(resultSet -> Single.just(insertResult.setResultSet(resultSet)));
                    } else {
                        return Single.just(insertResult);
                    }
                })
                .flatMap(result -> Single.just(evaluateCompositeResultAndReturnRecordStatus(result, parentRecordStatus)));
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

}
