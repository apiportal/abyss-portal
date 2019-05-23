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

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.key.generate.impl.Token;
import com.verapi.key.model.AuthenticationInfo;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AbyssTableName(tableName = "resource_access_token")
public class ResourceAccessTokenService extends AbstractService<UpdateResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceAccessTokenService.class);
    private static final String SQL_INSERT = "insert into resource_access_token (organizationid, crudsubjectid, subjectpermissionid, resourcetypeid, resourcerefid, \n" +
            "token, expiredate, nonce, userdata, isactive)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), \n" +
            "?, ?, ?, ?, ?)";
    private static final String SQL_DELETE = "update resource_access_token\n" +
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
            "  subjectpermissionid,\n" +
            "  resourcetypeid,\n" +
            "  resourcerefid,\n" +
            "  token,\n" +
            "  expiredate,\n" +
            "  isactive\n" +
            "from\n" +
            "resource_access_token\n";
    private static final String SQL_SELECT_ALL = "select\n" +
            "  uuid,\n" +
            "  organizationid,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  isdeleted,\n" +
            "  crudsubjectid,\n" +
            "  subjectpermissionid,\n" +
            "  resourcetypeid,\n" +
            "  resourcerefid,\n" +
            "  token,\n" +
            "  expiredate,\n" +
            "  nonce,\n" +
            "  userdata,\n" +
            "  isactive\n" +
            "from\n" +
            "resource_access_token\n";
    private static final String SQL_UPDATE = "UPDATE resource_access_token\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , subjectpermissionid      = CAST(? AS uuid)\n" +
            "  , resourcetypeid      = CAST(? AS uuid)\n" +
            "  , resourcerefid      = CAST(? AS uuid)\n" +
            "  , isactive      = ?\n";
    private static final String SQL_CONDITION_NAME_IS = "lower(token) = lower(?)\n";
    private static final String SQL_CONDITION_NAME_LIKE = "lower(token) like lower(?)\n";
    private static final String SQL_CONDITION_SUBJECT_PERMISSION_UUID_IS = "subjectpermissionid = CAST(? AS uuid)\n";
    public static final String SQL_FIND_BY_SUBJECT_PERMISSION_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_PERMISSION_UUID_IS;
    private static final String SQL_ORDERBY_NAME = "order by token\n";
    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";
    private static final String SQL_CONDITION_TOKEN_IS = "token = ?\n";
    public static final String SQL_FIND_BY_SUBJECT_TOKEN = SQL_SELECT_ALL + SQL_WHERE + SQL_CONDITION_TOKEN_IS;
    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;
    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;
    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;
    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;
    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    public ResourceAccessTokenService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public ResourceAccessTokenService(Vertx vertx) {
        super(vertx);
    }

    protected void prepareTokenParameters(JsonObject insertRecord) throws UnsupportedEncodingException {

        //Generate and Persist Activation Token
        Token tokenGenerator = new Token();
        AuthenticationInfo authInfo;

        long tokenTTL = 0;
        if (insertRecord.getString("resourcetypeid").equals(Constants.RESOURCE_TYPE_API))
            tokenTTL = Config.getInstance().getConfigJsonObject().getInteger("token.access.api.ttl") * Constants.ONE_DAY_IN_SECONDS;
        else if (insertRecord.getString("resourcetypeid").equals(Constants.RESOURCE_TYPE_APP))
            tokenTTL = Config.getInstance().getConfigJsonObject().getInteger("token.access.app.ttl") * Constants.ONE_DAY_IN_SECONDS;
        authInfo = tokenGenerator.generateToken(tokenTTL,
                insertRecord.getString("subjectpermissionid") + insertRecord.getString("resourcerefid"),
                vertx.getDelegate());
        LOGGER.trace("access token is created successfully: " + authInfo.getToken());

        insertRecord.put("token", authInfo.getToken());
        insertRecord.put("expiredate", authInfo.getExpireDate());
        insertRecord.put("nonce", authInfo.getNonce());
        insertRecord.put("userdata", authInfo.getUserData());
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
                .add(insertRecord.getString("subjectpermissionid"))
                .add(insertRecord.getString("resourcetypeid"))
                .add(insertRecord.getString("resourcerefid"))
                .add(insertRecord.getString("token"))
                .add(insertRecord.getInstant("expiredate"))
                .add(insertRecord.getString("nonce"))
                .add(insertRecord.getString("userdata"))
                .add(insertRecord.getBoolean("isactive"));
    }

    protected JsonArray prepareUpdateParameters(JsonObject updateRecord) {
        return new JsonArray()
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("crudsubjectid"))
                .add(updateRecord.getString("subjectpermissionid"))
                .add(updateRecord.getString("resourcetypeid"))
                .add(updateRecord.getString("resourcerefid"))
                .add(updateRecord.getBoolean("isactive"));
    }

    /**
     * @param insertRecord
     * @return recordStatus
     */
    @Override
    public Single<JsonObject> insert(JsonObject insertRecord, JsonObject parentRecordStatus) {
        LOGGER.trace("---insert invoked");

        try {
            prepareTokenParameters(insertRecord);
        } catch (UnsupportedEncodingException e) {
            LOGGER.trace("tokenGenerator.generateToken :" + e.getLocalizedMessage());
            return Single.error(new Exception("activation token could not be generated", e));
        }

        return super.insert(insertRecord, parentRecordStatus);
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        LOGGER.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(jsonObj -> {

                    try {
                        prepareTokenParameters(jsonObj);
                    } catch (UnsupportedEncodingException e) {
                        LOGGER.trace("tokenGenerator.generateToken :" + e.getLocalizedMessage());
                        return Observable.error(new Exception("activation token could not be generated"));
                    }

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
                                .put(STR_UUID, "0")
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
                                .put(STR_UUID, result.getResultSet().getRows().get(0).getString(STR_UUID))
                                .put("status", HttpResponseStatus.CREATED.code())
                                .put("response", arr.getJsonObject(0))
                                .put("error", new ApiSchemaError().toJson());
                    }
                    return Observable.just(recordStatus);
                })
                .toList();
    }

    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        JsonArray updateParams = prepareUpdateParameters(updateRecord)
                .add(uuid.toString());
        return update(updateParams, SQL_UPDATE_BY_UUID);
    }

    public Single<List<JsonObject>> updateAll(JsonObject updateRecords) {
        JsonArray jsonArray = new JsonArray();
        updateRecords.forEach(updateRow -> {
            jsonArray.add(new JsonObject(updateRow.getValue().toString())
                    .put(STR_UUID, updateRow.getKey()));
        });
        Observable<Object> updateParamsObservable = Observable.fromIterable(jsonArray);
        return updateParamsObservable
                .flatMap(o -> {
                    JsonObject jsonObj = (JsonObject) o;
                    JsonArray updateParam = prepareUpdateParameters(jsonObj)
                            .add(jsonObj.getString(STR_UUID));
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
                                .put(STR_UUID, "0")
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
                                .put(STR_UUID, result.getResultSet().getRows().get(0).getString(STR_UUID))
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

    public Single<ResultSet> findBySubjectPermissionId(UUID uuid) {
        return findById(uuid, SQL_FIND_BY_SUBJECT_PERMISSION_UUID);
    }

    public ApiFilterQuery.APIFilter getAPIFilter() {
        return apiFilter;
    }

}
