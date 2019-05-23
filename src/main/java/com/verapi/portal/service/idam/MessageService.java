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

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@AbyssTableName(tableName = "message")
public class MessageService extends AbstractService<UpdateResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageService.class);
    private static final String SQL_INSERT = "insert into message (organizationid, crudsubjectid, messagetypeid, parentmessageid, \n" +
            "ownersubjectid, conversationid, folder, sender, receiver, \n" +
            "subject, bodycontenttype, body, priority, isread, sentat, readat)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), \n" +
            "CAST(? AS uuid), ?, ?, ?::JSON, ?::JSON,\n" +
            "?, ?, ?, ?, false, now(), null)";
    private static final String SQL_INSERT_NEW_CONVERSATION = "insert into message (organizationid, crudsubjectid, messagetypeid, parentmessageid, \n" +
            "ownersubjectid, folder, sender, receiver, \n" +
            "subject, bodycontenttype, body, priority, isread, sentat, readat)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), \n" +
            "CAST(? AS uuid), ?, ?::JSON, ?::JSON,\n" +
            "?, ?, ?, ?, false, now(), null)";
    private static final String SQL_DELETE = "update message\n" +
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
            "  messagetypeid,\n" +
            "  parentmessageid,\n" +
            "  ownersubjectid, \n" +
            "  conversationid, \n" +
            "  folder, \n" +
            "  sender::JSON,\n" +
            "  receiver::JSON,\n" +
            "  subject,\n" +
            "  bodycontenttype,\n" +
            "  body,\n" +
            "  priority,\n" +
            "  isstarred,\n" +
            "  isread,\n" +
            "  sentat,\n" +
            "  readat,\n" +
            "  istrashed\n" +
            "from\n" +
            "message\n";
    private static final String SQL_UPDATE = "UPDATE message\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated           = now()\n" +
            "  , crudsubjectid     = CAST(? AS uuid)\n" +
            "  , messagetypeid     = CAST(? AS uuid)\n" +
            "  , parentmessageid   = CAST(? AS uuid)\n" +
            "  , ownersubjectid    = CAST(? AS uuid)\n" +
            "  , conversationid    = ?\n" +
            "  , folder            = ?\n" +
            "  , sender            = ?::JSON\n" +
            "  , receiver          = ?::JSON\n" +
            "  , subject           = ?\n" +
            "  , bodycontenttype   = ?\n" +
            "  , body              = ?\n" +
            "  , priority          = ?\n" +
            "  , isstarred         = ?\n" +
            "  , isread            = ?\n" +
            "  , sentat            = ?\n" +
            "  , readat            = ?\n" +
            "  , istrashed         = ?";
    private static final String SQL_CONDITION_NAME_IS = "lower(subject) = lower(?)\n";
    private static final String SQL_CONDITION_NAME_LIKE = "lower(subject) like lower(?)\n";
    private static final String SQL_ORDERBY_NAME = "order by subject\n";
    private static final String SQL_ORDERBY_CONVERSATION_AND_SENTAT = "order by conversationid desc, sentat desc\n";
    private static final String SQL_CONDITION_SUBJECT_UUID_IS = "ownersubjectid = CAST(? AS uuid)\n";
    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";
    public static final String SQL_FIND_BY_SUBJECT = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_UUID_IS +
            SQL_AND + SQL_CONDITION_ONLY_NOTDELETED +
            SQL_ORDERBY_CONVERSATION_AND_SENTAT;
    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;
    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;
    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;
    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;
    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);
    private Integer senderRecordId;
    private Integer conversationId = null;
    private JsonObject senderJson;

    public MessageService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public MessageService(Vertx vertx) {
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
        JsonArray insertParam = new JsonArray()
                .add(insertRecord.getString("organizationid"))
                .add(insertRecord.getString("crudsubjectid"))
                .add(insertRecord.getString("messagetypeid"))
                .add(insertRecord.getString("parentmessageid")) //TODO: Change parent for receiver???
                .add(insertRecord.getString("ownersubjectid")); //TODO: Change owner for receiver
        if (conversationId > 0) { //if zero-->nullify then get new id from sequence
            insertParam.add(insertRecord.getInteger("conversationid")); //TODO: Add sender's conversation id for receiver
        }
        insertParam
                .add(insertRecord.getString("folder")) //TODO: Change folder for receiver
                .add(insertRecord.getJsonObject("sender").encode())
                .add(insertRecord.getJsonObject("receiver").encode())
                .add(insertRecord.getString("subject"))
                .add(insertRecord.getString("bodycontenttype"))
                .add(insertRecord.getString("body"))
                .add(insertRecord.getString("priority"));
        //.add(insertRecord.getBoolean("isstarred"))
        //.add(insertRecord.getBoolean("isread"))
        //.add(insertRecord.getInstant("sentat"))
        //.add(insertRecord.getInstant("readat"))
        //.add(insertRecord.getBoolean("istrashed"));

        return insertParam;
    }

    protected JsonArray prepareUpdateParameters(JsonObject updateRecord) {
        return new JsonArray()
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("crudsubjectid"))
                .add(updateRecord.getString("messagetypeid"))
                .add(updateRecord.getString("parentmessageid"))
                .add(updateRecord.getString("ownersubjectid"))
                .add(updateRecord.getInteger("conversationid"))
                .add(updateRecord.getString("folder"))
                .add(updateRecord.getJsonObject("sender").encode())
                .add(updateRecord.getJsonObject("receiver").encode())
                .add(updateRecord.getString("subject"))
                .add(updateRecord.getString("bodycontenttype"))
                .add(updateRecord.getString("body"))
                .add(updateRecord.getString("priority"))
                .add(updateRecord.getBoolean("isstarred"))
                .add(updateRecord.getBoolean("isread"))
                .add(updateRecord.getInstant("sentat"))
                .add(updateRecord.getInstant("readat"))
                .add(updateRecord.getBoolean("istrashed"));
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        LOGGER.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(jsonObj -> {
                    conversationId = jsonObj.getInteger("conversationid");
                    senderJson = jsonObj;
                    JsonArray insertParam = prepareInsertParameters(jsonObj);
                    return insert(insertParam, conversationId > 0 ? SQL_INSERT : SQL_INSERT_NEW_CONVERSATION).toObservable();
                })
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        if (conversationId == 0) {
                            conversationId = insertResult.getUpdateResult().getKeys().getInteger(11);
                        }
                        senderRecordId = insertResult.getUpdateResult().getKeys().getInteger(0);

                        JsonArray insertReceiverParam = new JsonArray()
                                .add(senderJson.getJsonObject("receiver").getString("organizationid")) //Receiver Org
                                .add(senderJson.getString("crudsubjectid"))
                                .add(senderJson.getString("messagetypeid"))
                                .add(senderJson.getString("parentmessageid")) //TODO: Change parent for receiver???
                                .add(senderJson.getJsonObject("receiver").getString("subjectid")) //Change owner for receiver
                                .add(conversationId) //Add sender's conversation id for receiver
                                .add("Inbox") //Change folder for receiver
                                .add(senderJson.getJsonObject("sender").encode())
                                .add(senderJson.getJsonObject("receiver").encode())
                                .add(senderJson.getString("subject"))
                                .add(senderJson.getString("bodycontenttype"))
                                .add(senderJson.getString("body"))
                                .add(senderJson.getString("priority"));
                        //.add(senderJson.getBoolean("isstarred"))
                        //.add(jsonObj.getBoolean("isread"))
                        //.add(jsonObj.getInstant("sentat"))
                        //.add(jsonObj.getInstant("readat"))
                        //.add(jsonObj.getBoolean("istrashed"));
                        return insert(insertReceiverParam, SQL_INSERT).toObservable();
                    } else {
                        return Observable.just(insertResult);
                    }
                })
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        return findById(senderRecordId, SQL_FIND_BY_ID)
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
                                .put(STR_STATUS, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                .put(STR_RESPONSE, new JsonObject())
                                .put(STR_ERROR, new ApiSchemaError()
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
                                .put(STR_STATUS, HttpResponseStatus.CREATED.code())
                                .put(STR_RESPONSE, arr.getJsonObject(0))
                                .put(STR_ERROR, new ApiSchemaError().toJson());
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
                                .put(STR_STATUS, HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                .put(STR_RESPONSE, new JsonObject())
                                .put(STR_ERROR, new ApiSchemaError()
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
                                .put(STR_STATUS, HttpResponseStatus.CREATED.code())
                                .put(STR_RESPONSE, arr.getJsonObject(0))
                                .put(STR_ERROR, new ApiSchemaError().toJson());
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

}
