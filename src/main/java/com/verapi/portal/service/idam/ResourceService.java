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

import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.ApiFilterQuery;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ResourceService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(ResourceService.class);

    public ResourceService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public ResourceService(Vertx vertx) {
        super(vertx);
    }

    @Override
    protected String getInsertSql() { return SQL_INSERT; }

    @Override
    protected String getFindByIdSql() { return SQL_FIND_BY_ID; }

    @Override
    protected JsonArray prepareInsertParameters(JsonObject insertRecord) {
        return new JsonArray()
                .add(insertRecord.getString("organizationid"))
                .add(insertRecord.getString("crudsubjectid"))
                .add(insertRecord.getString("resourcetypeid"))
                .add(insertRecord.getString("resourcename"))
                .add(insertRecord.getString("description"))
                .add(insertRecord.getString("resourcerefid"))
                .add(insertRecord.getBoolean("isactive"));
    }


    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        logger.trace("---insertAll invoked");
        return insertAllWithSql(insertRecords, SQL_INSERT);
    }

    public Single<List<JsonObject>> insertAllWithConflict(JsonArray insertRecords) {
        logger.trace("---insertAllWithConflict invoked");
        return insertAllWithSql(insertRecords, SQL_INSERT_WITH_CONFLICT);
    }

    public Single<List<JsonObject>> insertAllWithSql(JsonArray insertRecords, String sql) {
        logger.trace("---insertAllWithSql invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(jsonObj -> {
                    JsonArray insertParam = prepareInsertParameters(jsonObj);
                    return insert(insertParam, sql).toObservable();
                })
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        if (insertResult.getUpdateResult() != null && insertResult.getUpdateResult().getUpdated()>0) {
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
                    } else {
                        return Observable.just(insertResult);
                    }
                })
                .flatMap(result -> Observable.just(evaluateCompositeResultAndReturnRecordStatus(result))
                )
                .toList();
    }

    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        return update(uuid, updateRecord, SQL_UPDATE_BY_UUID);
    }

    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord, String sql) {
        JsonArray updateParams = prepareInsertParameters(updateRecord)
                .add(uuid.toString());
        return update(updateParams, sql);
    }

    public Single<CompositeResult> updateByRef(JsonObject updateRecord) {
        JsonArray updateParams = prepareInsertParameters(updateRecord)
                //Where Condition Args
                .add(updateRecord.getString("resourcerefid"))
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("resourcetypeid"));
        return update(updateParams, SQL_UPDATE_BY_REF_UUID);
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
                        logger.trace("updateAll>> update/find exception {}", result.getThrowable());
                        logger.error(result.getThrowable().getLocalizedMessage());
                        logger.error(Arrays.toString(result.getThrowable().getStackTrace()));
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
                        logger.trace("updateAll>> update getKeys {}", result.getUpdateResult().getKeys().encodePrettily());
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

    private static final String SQL_INSERT = "insert into resource (organizationid, crudsubjectid, resourcetypeid, resourcename, description, resourcerefid, isactive)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS uuid), ?)";

    private static final String SQL_DELETE = "update resource\n" +
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
            "  resourcetypeid,\n" +
            "  resourcename,\n" +
            "  description,\n" +
            "  resourcerefid,\n" +
            "  isactive\n" +
            "from\n" +
            "resource\n";

    private static final String SQL_UPDATE = "UPDATE resource\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , resourcetypeid      = CAST(? AS uuid)\n" +
            "  , resourcename      = ?\n" +
            "  , description      = ?\n" +
            "  , resourcerefid       = CAST(? AS uuid)\n" +
            "  , isactive      = ?\n";


    private static final String SQL_CONDITION_NAME_IS = "lower(resourcename) = lower(?)\n";

    private static final String SQL_CONDITION_NAME_LIKE = "lower(resourcename) like lower(?)\n";

    private static final String SQL_ORDERBY_NAME = "order by resourcename\n";

    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";

    private static final String SQL_CONDITION_RESOURCEREFID_IS = "resourcerefid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_CRUDSUBJECTID_IS = "crudsubjectid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_RESOURCETYPEID_IS = "resourcetypeid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_ORGANIZATIONID_IS = "organizationid = CAST(? AS uuid)\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;

    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BY_REF_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_RESOURCEREFID_IS +
                                                                        SQL_AND + SQL_CONDITION_ORGANIZATIONID_IS +
                                                                        SQL_AND + SQL_CONDITION_RESOURCETYPEID_IS +
                                                                        SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    public static final String FILTER_BY_RESOURCEREFERENCE = SQL_SELECT + SQL_WHERE + SQL_CONDITION_RESOURCEREFID_IS;

    public static final String FILTER_BY_CRUDSUBJECT = SQL_SELECT + SQL_WHERE + SQL_CONDITION_CRUDSUBJECTID_IS;

    public static final String FILTER_BY_RESOURCETYPE = SQL_SELECT + SQL_WHERE + SQL_CONDITION_RESOURCETYPEID_IS;

    public static final String FILTER_BY_ORGANIZATION = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ORGANIZATIONID_IS;

    //INSERT ... ON CONFLICT DO NOTHING/UPDATE
    public static final String SQL_INSERT_WITH_CONFLICT = SQL_INSERT + "\nON CONFLICT DO NOTHING\n";

    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

}