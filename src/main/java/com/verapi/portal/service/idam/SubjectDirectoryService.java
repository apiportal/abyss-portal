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

package com.verapi.portal.service.idam;

import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.abyss.exception.NotFound404Exception;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.AbyssTableName;
import com.verapi.portal.service.ApiFilterQuery;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AbyssTableName(tableName = "subject_directory")
public class SubjectDirectoryService extends AbstractService<UpdateResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SubjectDirectoryService.class);
    private static final String SQL_INSERT = "insert into subject_directory (organizationid, crudsubjectid, directoryname, description, isactive, istemplate,\n" +
            "                               directorytypeid, directorypriorityorder, directoryattributes, lastsyncronizedat, lastsyncronizationduration)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?,\n" +
            "  CAST(? AS uuid), ?, ?::JSON, ?, ?);";
    private static final String SQL_DELETE = "update subject_directory\n" +
            "set\n" +
            "  deleted     = now()\n" +
            "  , isdeleted = true\n";
    private static final String SQL_SELECT = "select\n" +
            "  uuid,\n" +
            "  organizationid,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  isdeleted,\n" +
            "  crudsubjectid,\n" +
            "  directoryname,\n" +
            "  description,\n" +
            "  isactive,\n" +
            "  istemplate,\n" +
            "  directorytypeid,\n" +
            "  directorypriorityorder,\n" +
            "  directoryattributes,\n" +
            "  lastsyncronizedat,\n" +
            "  lastsyncronizationduration\n" +
            "from\n" +
            "subject_directory\n";
    private static final String SQL_UPDATE = "UPDATE subject_directory\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , directoryname      = ?\n" +
            "  , description       = ?\n" +
            "  , isactive            = ?\n" +
            "  , istemplate            = ?\n" +
            "  , directorytypeid                 = CAST(? AS uuid)\n" +
            "  , directorypriorityorder    = ?\n" +
            "  , directoryattributes = ?::JSON\n" +
            "  , lastsyncronizedat = ?\n" +
            "  , lastsyncronizationduration = ?\n";
    private static final String SQL_CONDITION_NAME_IS = "lower(directoryname) = lower(?)\n";
    private static final String SQL_CONDITION_NAME_LIKE = "lower(directoryname) like lower(?)\n";
    private static final String SQL_ORDERBY_NAME = "order by directoryname\n";
    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";
    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;
    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;
    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;
    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;
    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    public SubjectDirectoryService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public SubjectDirectoryService(Vertx vertx) {
        super(vertx);
    }

    @Override
    protected String getInsertSql() {
        return SQL_INSERT;
    }

    @Override
    protected String getFindByIdSql() {
        return SQL_FIND_BY_ID;
    }

    @Override
    protected JsonArray prepareInsertParameters(JsonObject insertRecord) {
        return new JsonArray()
                .add(insertRecord.getString("organizationid"))
                .add(insertRecord.getString("crudsubjectid"))
                .add(insertRecord.getString("directoryname"))
                .add(insertRecord.getString("description"))
                .add(insertRecord.getBoolean("isactive"))
                .add(insertRecord.getBoolean("istemplate"))
                .add(insertRecord.getString("directorytypeid"))
                .add(((Number) insertRecord.getValue("directorypriorityorder")).longValue())
                .add(insertRecord.getJsonObject("directoryattributes").encode())
                .add(insertRecord.getInstant("lastsyncronizedat"))
                .add(((Number) insertRecord.getValue("lastsyncronizationduration")).longValue());
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        LOGGER.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(jsonObj -> {
                    JsonArray insertParam = prepareInsertParameters(jsonObj);
                    return insert(insertParam, SQL_INSERT).toObservable();
                })
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        return findById(insertResult.getUpdateResult().getKeys().getInteger(0), SQL_FIND_BY_ID)
                                .onErrorResumeNext(ex -> {
                                    insertResult.setThrowable(ex);
                                    return Single.just(insertResult.getResultSet()); //TODO: insertResult.throwable kayıp mı?
                                })
                                .flatMap(resultSet -> Single.just(insertResult.setResultSet(resultSet)))
                                .toObservable();
                    } else {
                        return Observable.just(insertResult);
                    }
                })
                .flatMap(result -> {
                    JsonObject recordStatus = new JsonObject();
                    if (result.getThrowable() != null) {
                        LOGGER.trace("insertAll>> insert/find exception {}", result.getThrowable());
                        LOGGER.error(result.getThrowable().getLocalizedMessage());
                        LOGGER.error(Arrays.toString(result.getThrowable().getStackTrace()));
                        recordStatus
                                .put("uuid", "0")
                                .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                .put("response", new JsonObject())
                                .put("error", new ApiSchemaError()
                                        .setUsermessage(result.getThrowable().getLocalizedMessage())
                                        .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                        .setInternalmessage(Arrays.toString(result.getThrowable().getStackTrace()))
                                        .toJson());
                    } else {
                        LOGGER.trace("insertAll>> insert getKeys {}", result.getUpdateResult().getKeys().encodePrettily());
                        JsonArray arr = new JsonArray();
                        result.getResultSet().getRows().forEach(arr::add);
                        recordStatus
                                .put("uuid", result.getResultSet().getRows().get(0).getString("uuid"))
                                .put("status", HttpResponseStatus.CREATED.code())
                                .put("response", arr.getJsonObject(0))
                                .put("error", new ApiSchemaError().toJson());
                    }
                    return Observable.just(recordStatus);
                })
                .toList();
    }

    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        JsonArray updateParams = prepareInsertParameters(updateRecord)
                .add(uuid.toString());
        return update(updateParams, SQL_UPDATE_BY_UUID);
    }

    public Single<List<JsonObject>> updateAll(JsonObject updateRecords) {
        JsonArray jsonArray = new JsonArray();
        updateRecords.forEach(updateRow -> {
            jsonArray.add(new JsonObject(updateRow.getValue().toString())
                    .put("uuid", updateRow.getKey()));
        });
        Observable<Object> updateParamsObservable = Observable.fromIterable(jsonArray);
        return updateParamsObservable
                .flatMap(o -> {
                    JsonObject jsonObj = (JsonObject) o;
                    JsonArray updateParam = prepareInsertParameters(jsonObj)
                            .add(jsonObj.getString("uuid"));
                    return update(updateParam, SQL_UPDATE_BY_UUID).toObservable();
                })
                .flatMap(updateResult -> {
                    if (updateResult.getThrowable() == null) {
                        return findById(updateResult.getUpdateResult().getKeys().getInteger(0), SQL_FIND_BY_ID)
                                .onErrorResumeNext(ex -> {
                                    updateResult.setThrowable(ex);
                                    return Single.just(updateResult.getResultSet()); //TODO: updateResult.throwable kayıp mı?
                                })
                                .flatMap(resultSet -> Single.just(updateResult.setResultSet(resultSet)))
                                .toObservable();
                    } else {
                        return Observable.just(updateResult);
                    }
                })
                .flatMap(result -> {
                    JsonObject recordStatus = new JsonObject();
                    if (result.getThrowable() != null) {
                        LOGGER.trace("updateAll>> update/find exception {}", result.getThrowable());
                        LOGGER.error(result.getThrowable().getLocalizedMessage());
                        LOGGER.error(Arrays.toString(result.getThrowable().getStackTrace()));
                        recordStatus
                                .put("uuid", "0")
                                .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                .put("response", new JsonObject())
                                .put("error", new ApiSchemaError()
                                        .setUsermessage(result.getThrowable().getLocalizedMessage())
                                        .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                        .setInternalmessage(Arrays.toString(result.getThrowable().getStackTrace()))
                                        .toJson());
                    } else {
                        LOGGER.trace("updateAll>> update getKeys {}", result.getUpdateResult().getKeys().encodePrettily());
                        JsonArray arr = new JsonArray();
                        result.getResultSet().getRows().forEach(arr::add);
                        recordStatus
                                .put("uuid", result.getResultSet().getRows().get(0).getString("uuid"))
                                .put("status", HttpResponseStatus.CREATED.code())
                                .put("response", arr.getJsonObject(0))
                                .put("error", new ApiSchemaError().toJson());
                    }
                    return Observable.just(recordStatus);
                })
                .toList();
    }

    public Single<CompositeResult> delete(UUID uuid) {
        return delete(uuid, SQL_DELETE_BY_UUID);
    }

    public Single<CompositeResult> deleteAll() {
        return deleteAll(SQL_DELETE_ALL);
    }

    public Single<CompositeResult> deleteAll(ApiFilterQuery apiFilterQuery) {
        ApiFilterQuery sqlDeleteAllQuery = new ApiFilterQuery().setFilterQuery(SQL_DELETE_ALL).addFilterQuery(apiFilterQuery.getFilterQuery());
        return deleteAll(sqlDeleteAllQuery.getFilterQuery());
    }

    public Single<ResultSet> findById(long id) {
        return findById(id, SQL_FIND_BY_ID);
    }

    public Single<ResultSet> findById(UUID uuid) {
        return findById(uuid, SQL_FIND_BY_UUID);
    }

    public Single<ResultSet> findByName(String name) {
        return findByName(name, SQL_FIND_BY_NAME);
    }

    public Single<ResultSet> findLikeName(String name) {
        return findLikeName(name, SQL_FIND_LIKE_NAME);
    }

    public Single<ResultSet> findAll() {
        return findAll(SQL_SELECT);
    }

    public Single<ResultSet> findAll(ApiFilterQuery apiFilterQuery) {
        return filter(apiFilterQuery);
    }

    public ApiFilterQuery.APIFilter getAPIFilter() {
        return apiFilter;
    }

    public Single<JsonObject> startSync(RoutingContext routingContext) {
        LOGGER.trace("startSync invoked");

        String directoryUuid = routingContext.pathParam("uuid");
        LOGGER.trace("Received directory uuid:" + directoryUuid);

        return initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient1 -> findById(UUID.fromString(directoryUuid)))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() > 0) {
                        JsonObject directoryJson = resultSet.getRows().get(0);
                        JsonObject directoryattributesJson = new JsonObject(directoryJson.getString("directoryattributes"));

                        directoryJson.put("directoryattributes", updateConnectionSettingsSyncValues(directoryattributesJson, "syncing"));
                        directoryJson.put("updated", Instant.now());
                        return update(UUID.fromString(directoryUuid), directoryJson);
                    } else {
                        LOGGER.trace("Subject Directory not Found");
                        return Single.error(new NotFound404Exception("Subject Directory not Found"));
                    }
                })
                .flatMap(compositeResult -> {
                    if (compositeResult.getThrowable() == null) {
                        LOGGER.trace("Subject Directory state updated as syncing");
                        return findById(UUID.fromString(directoryUuid));
                    } else {
                        LOGGER.error("Error in Subject Directory state update as syncing");
                        return Single.error(compositeResult.getThrowable());
                    }
                })
                .flatMap(resultSet -> Single.just(resultSet.getRows().get(0)));
    }

    private JsonObject updateConnectionSettingsSyncValues(JsonObject directoryattributesJson, String syncState) {

        if (syncState == null || syncState.isEmpty()) {
            LOGGER.error("updateConnectionSettingsSyncValues syncState received null or empty");
            return directoryattributesJson;
        }

        if ("idle".equals(syncState) || "syncFailed".equals(syncState)) {

            JsonObject connectionSettingsSync = directoryattributesJson.getJsonObject("connection.settings").getJsonObject("connection.settings.sync");
            connectionSettingsSync.put("connection.synchronisation.state", syncState);
            connectionSettingsSync.put("connection.synchronisation.duration.inquiry.previous.asMilliSecond", connectionSettingsSync.getInteger("connection.synchronisation.duration.inquiry.last.asMilliSecond"));
            connectionSettingsSync.put("connection.synchronisation.duration.write.previous.asMilliSecond", connectionSettingsSync.getInteger("connection.synchronisation.duration.write.last.asMilliSecond"));

            //long duration = Instant.now().compareTo(Instant.ofEpochMilli(connectionSettingsSync.getLong("connection.synchronisation.time.last.asUnixTime")));
            long duration = Instant.now().toEpochMilli() - connectionSettingsSync.getLong("connection.synchronisation.time.last.asUnixTime");
            connectionSettingsSync.put("connection.synchronisation.duration.inquiry.last.asMilliSecond", duration);
            connectionSettingsSync.put("connection.synchronisation.duration.write.last.asMilliSecond", duration);

            directoryattributesJson.getJsonObject("connection.settings").put("connection.settings.sync", connectionSettingsSync);

        } else if ("syncing".equals(syncState)) {

            JsonObject connectionSettingsSync = directoryattributesJson.getJsonObject("connection.settings").getJsonObject("connection.settings.sync");
            connectionSettingsSync.put("connection.synchronisation.state", syncState);
            connectionSettingsSync.put("connection.synchronisation.time.previous.asUnixTime", connectionSettingsSync.getInteger("connection.synchronisation.time.last.asUnixTime"));
            connectionSettingsSync.put("connection.synchronisation.time.last.asUnixTime", Instant.now().toEpochMilli());
            directoryattributesJson.getJsonObject("connection.settings").put("connection.settings.sync", connectionSettingsSync);

        } else {
            LOGGER.error("updateConnectionSettingsSyncValues unknown syncState: {} received", syncState);
        }

        return directoryattributesJson;
    }

    public Single<JsonObject> finishSync(RoutingContext routingContext) {
        LOGGER.trace("finishSync invoked");

        String directoryUuid = routingContext.pathParam("uuid");
        LOGGER.trace("Received directory uuid:" + directoryUuid);

        return initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient1 -> findById(UUID.fromString(directoryUuid)))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() > 0) {
                        JsonObject directoryJson = resultSet.getRows().get(0);
                        JsonObject directoryattributesJson = new JsonObject(directoryJson.getString("directoryattributes"));

                        directoryJson.put("directoryattributes", updateConnectionSettingsSyncValues(directoryattributesJson, "idle"));
                        directoryJson.put("updated", Instant.now());
                        directoryJson.put("lastsyncronizedat", Instant.now());
                        return update(UUID.fromString(directoryUuid), directoryJson);
                    } else {
                        LOGGER.trace("Subject Directory not Found");
                        return Single.error(new NotFound404Exception("Subject Directory not Found"));
                    }
                })
                .flatMap(compositeResult -> {
                    if (compositeResult.getThrowable() == null) {
                        LOGGER.trace("Subject Directory state updated as syncFailed");
                        return findById(UUID.fromString(directoryUuid));
                    } else {
                        LOGGER.error("Error in Subject Directory state update as syncFailed");
                        return Single.error(compositeResult.getThrowable());
                    }
                })
                .flatMap(resultSet -> Single.just(resultSet.getRows().get(0)));
    }

    public Single<JsonObject> failSync(RoutingContext routingContext) {
        LOGGER.trace("failSync invoked");

        String directoryUuid = routingContext.pathParam("uuid");
        LOGGER.trace("Received directory uuid:" + directoryUuid);

        return initJDBCClient(routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME))
                .flatMap(jdbcClient1 -> findById(UUID.fromString(directoryUuid)))
                .flatMap(resultSet -> {
                    if (resultSet.getNumRows() > 0) {
                        JsonObject directoryJson = resultSet.getRows().get(0);
                        JsonObject directoryattributesJson = new JsonObject(directoryJson.getString("directoryattributes"));

                        directoryJson.put("directoryattributes", updateConnectionSettingsSyncValues(directoryattributesJson, "syncFailed"));
                        directoryJson.put("updated", Instant.now());
                        directoryJson.put("lastsyncronizedat", Instant.now());
                        return update(UUID.fromString(directoryUuid), directoryJson);
                    } else {
                        LOGGER.trace("Subject Directory not Found");
                        return Single.error(new NotFound404Exception("Subject Directory not Found"));
                    }
                })
                .flatMap(compositeResult -> {
                    if (compositeResult.getThrowable() == null) {
                        LOGGER.trace("Subject Directory state updated as idle");
                        return findById(UUID.fromString(directoryUuid));
                    } else {
                        LOGGER.error("Error in Subject Directory state update as idle");
                        return Single.error(compositeResult.getThrowable());
                    }
                })
                .flatMap(resultSet -> Single.just(resultSet.getRows().get(0)));
    }

}
