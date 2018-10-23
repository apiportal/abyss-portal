/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 7 2018
 *
 */

package com.verapi.portal.service.idam;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.oapi.schema.ApiSchemaError;
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

public class MessageService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(MessageService.class);

    private Integer senderRecordId;
    private Integer conversationId = null;
    private JsonObject senderJson;

    public MessageService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public MessageService(Vertx vertx) {
        super(vertx);
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        logger.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(jsonObj -> {
                    conversationId = jsonObj.getInteger("conversationid");
                    senderJson = jsonObj;
                    JsonArray insertParam = new JsonArray()
                            .add(jsonObj.getString("organizationid"))
                            .add(jsonObj.getString("crudsubjectid"))
                            .add(jsonObj.getString("messagetypeid"))
                            .add(jsonObj.getString("parentmessageid")) //TODO: Change parent for receiver???
                            .add(jsonObj.getString("ownersubjectid")); //TODO: Change owner for receiver
                    if (conversationId>0) { //if zero-->nullify then get new id from sequence
                        insertParam.add(jsonObj.getInteger("conversationid")); //TODO: Add sender's conversation id for receiver
                    }
                    insertParam
                            .add(jsonObj.getString("folder")) //TODO: Change folder for receiver
                            .add(jsonObj.getJsonObject("sender").encode())
                            .add(jsonObj.getJsonObject("receiver").encode())
                            .add(jsonObj.getString("subject"))
                            .add(jsonObj.getString("bodycontenttype"))
                            .add(jsonObj.getString("body"))
                            .add(jsonObj.getString("priority"));
                            //.add(jsonObj.getBoolean("isstarred"))
                            //.add(jsonObj.getBoolean("isread"))
                            //.add(jsonObj.getInstant("sentat"))
                            //.add(jsonObj.getInstant("readat"))
                            //.add(jsonObj.getBoolean("istrashed"));
                    return insert(insertParam, conversationId>0 ? SQL_INSERT : SQL_INSERT_NEW_CONVERSATION).toObservable();
                })
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        if (conversationId==0) {
                            conversationId = insertResult.getUpdateResult().getKeys().getInteger(11);
                        }
                        senderRecordId = insertResult.getUpdateResult().getKeys().getInteger(0);

                        JsonArray insertReceiverParam = new JsonArray()
                                .add(senderJson.getJsonObject("receiver").getString("receiverorganizationid")) //Receiver Org
                                .add(senderJson.getString("crudsubjectid"))
                                .add(senderJson.getString("messagetypeid"))
                                .add(senderJson.getString("parentmessageid")) //TODO: Change parent for receiver???
                                .add(senderJson.getJsonObject("receiver").getString("receiversubjectid")) //Change owner for receiver
                                .add(conversationId) //Add sender's conversation id for receiver
                                .add("INBOX") //Change folder for receiver
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
                        logger.trace("insertAll>> insert/find exception {}", result.getThrowable());
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
                        logger.trace("insertAll>> insert getKeys {}", result.getUpdateResult().getKeys().encodePrettily());
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
        JsonArray updateParams = new JsonArray()
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
                .add(updateRecord.getBoolean("istrashed"))
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
                    JsonArray updateParam = new JsonArray()
                            .add(jsonObj.getString("organizationid"))
                            .add(jsonObj.getString("crudsubjectid"))
                            .add(jsonObj.getString("messagetypeid"))
                            .add(jsonObj.getString("parentmessageid"))
                            .add(jsonObj.getString("ownersubjectid"))
                            .add(jsonObj.getInteger("conversationid"))
                            .add(jsonObj.getString("folder"))
                            .add(jsonObj.getJsonObject("sender").encode())
                            .add(jsonObj.getJsonObject("receiver").encode())
                            .add(jsonObj.getString("subject"))
                            .add(jsonObj.getString("bodycontenttype"))
                            .add(jsonObj.getString("body"))
                            .add(jsonObj.getString("priority"))
                            .add(jsonObj.getBoolean("isstarred"))
                            .add(jsonObj.getBoolean("isread"))
                            .add(jsonObj.getInstant("sentat"))
                            .add(jsonObj.getInstant("readat"))
                            .add(jsonObj.getBoolean("istrashed"))
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
            "from message\n";

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

    private static final String SQL_AND = "and\n";

    private static final String SQL_WHERE = "where\n";

    private static final String SQL_CONDITION_ID_IS = "id = ?\n";

    private static final String SQL_CONDITION_UUID_IS = "uuid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_NAME_IS = "lower(subject) = lower(?)\n";

    private static final String SQL_CONDITION_NAME_LIKE = "lower(subject) like lower(?)\n";

    private static final String SQL_ORDERBY_NAME = "order by subject\n";

    private static final String SQL_ORDERBY_CONVERSATION_AND_SENTAT = "order by conversationid desc, sentat desc\n";

    private static final String SQL_CONDITION_SUBJECT_UUID_IS = "ownersubjectid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;

    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;

    public static final String SQL_FIND_BY_SUBJECT = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_UUID_IS +
                                                    SQL_AND + SQL_CONDITION_ONLY_NOTDELETED +
                                                    SQL_ORDERBY_CONVERSATION_AND_SENTAT;

    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

}