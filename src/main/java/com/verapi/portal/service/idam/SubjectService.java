/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.service.idam;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Constants;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.oapi.schema.ApiSchemaError;
import com.verapi.portal.service.AbstractService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;

import static com.verapi.portal.common.Util.encodeFileToBase64Binary;

public class SubjectService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(SubjectService.class);

    public SubjectService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public SubjectService(Vertx vertx) {
        super(vertx);

        //************ TODO: test amaçlı, kontrol et...
/*
        ClassLoader classLoader = getClass().getClassLoader();
        File yamlFile = new File(Objects.requireNonNull(classLoader.getResource("/openapi/Subject.yaml")).getFile());
        try {
            requestJsonObject = Util.loadYamlFile(yamlFile);
        } catch (FileNotFoundException e) {
            logger.error(e.getLocalizedMessage(), (Object[]) e.getStackTrace());
        }
*/
        //*******
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        logger.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(o -> {
                    JsonObject jsonObj = (JsonObject) o;
                    JsonArray insertParam = new JsonArray()
                            .add(((Number) jsonObj.getValue("organizationid")).longValue())
                            .add(((Number) jsonObj.getValue("crudsubjectid")).longValue())
                            .add(((Number) jsonObj.getValue("subjecttypeid")).longValue())
                            .add(jsonObj.getString("subjectname"))
                            .add(jsonObj.getString("firstname"))
                            .add(jsonObj.getString("lastname"))
                            .add(jsonObj.getString("displayname"))
                            .add(jsonObj.getString("email"))
                            .add(jsonObj.getString("secondaryemail"))
                            .add(jsonObj.getInstant("effectivestartdate"))
                            .add(jsonObj.getInstant("effectiveenddate"))
                            .add(jsonObj.getValue("password"))
                            .add(jsonObj.getValue("passwordsalt"))
                            .add(jsonObj.getValue("picture"))
                            .add(((Number) jsonObj.getValue("subjectdirectoryid")).longValue());
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
                .add(((Number) updateRecord.getValue("organizationid")).longValue())
                .add(((Number) updateRecord.getValue("crudsubjectid")).longValue())
                .add(((Number) updateRecord.getValue("subjecttypeid")).longValue())
                .add(((String) updateRecord.getValue("subjectname")))
                .add(((String) updateRecord.getValue("firstname")))
                .add(((String) updateRecord.getValue("lastname")))
                .add(((String) updateRecord.getValue("displayname")))
                .add(((String) updateRecord.getValue("email")))
                .add(((String) updateRecord.getValue("secondaryemail")))
                .add((updateRecord.getInstant("effectivestartdate")))
                .add((updateRecord.getInstant("effectiveenddate")))
                .add(((String) updateRecord.getValue("picture")))
                .add(((Number) updateRecord.getValue("subjectdirectoryid")).longValue())
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
                    if ((!jsonObj.containsKey("picture")) || (jsonObj.getValue("picture") == null)) {
                        try {
                            //update default avatar image TODO: later use request base
                            ClassLoader classLoader = getClass().getClassLoader();
                            File file = new File(Objects.requireNonNull(classLoader.getResource(Constants.RESOURCE_DEFAULT_AVATAR)).getFile());
                            jsonObj.put("picture", encodeFileToBase64Binary(file));
                        } catch (IOException e) {
                            logger.error(e.getLocalizedMessage());
                            logger.error(Arrays.toString(e.getStackTrace()));
                        }
                    }
                    JsonArray updateParam = new JsonArray()
                            .add(((Number) jsonObj.getValue("organizationid")).longValue())
                            .add(((Number) jsonObj.getValue("crudsubjectid")).longValue())
                            .add(((Number) jsonObj.getValue("subjecttypeid")).longValue())
                            .add(jsonObj.getString("subjectname"))
                            .add(jsonObj.getString("firstname"))
                            .add(jsonObj.getString("lastname"))
                            .add(jsonObj.getString("displayname"))
                            .add(jsonObj.getString("email"))
                            .add(jsonObj.getString("secondaryemail"))
                            .add(jsonObj.getInstant("effectivestartdate"))
                            .add(jsonObj.getInstant("effectiveenddate"))
                            .add(jsonObj.getValue("picture"))
                            .add(((Number) jsonObj.getValue("subjectdirectoryid")).longValue())
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
        return deleteAll(SQL_DELETE_ALL_USERS);
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

    private static final String SQL_INSERT = "insert into subject (organizationid, crudsubjectid, subjecttypeid, subjectname, firstname, lastname, displayname, email, secondaryemail, effectivestartdate, effectiveenddate, password, passwordsalt, picture, subjectdirectoryid)\n" +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, coalesce(?, now()), ?, ?, ?, ?, ?)";

    private static final String SQL_DELETE = "update subject\n" +
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
            "  isactivated,\n" +
            "  subjecttypeid,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
            "  secondaryemail,\n" +
            "  effectivestartdate,\n" +
            "  effectiveenddate,\n" +
            "  picture,\n" +
            "  totallogincount,\n" +
            "  failedlogincount,\n" +
            "  invalidpasswordattemptcount,\n" +
            "  ispasswordchangerequired,\n" +
            "  passwordexpiresat,\n" +
            "  lastloginat,\n" +
            "  lastpasswordchangeat,\n" +
            "  lastauthenticatedat,\n" +
            "  lastfailedloginat,\n" +
            "  subjectdirectoryid\n" +
            "from subject\n";

    private static final String SQL_UPDATE = "UPDATE subject\n" +
            "SET\n" +
            "  organizationid      = ?\n" +
            "  , crudsubjectid      = ?\n" +
            "  , updated               = now()\n" +
            "  , subjecttypeid      = ?\n" +
            "  , subjectname       = ?\n" +
            "  , firstname            = ?\n" +
            "  , lastname            = ?\n" +
            "  , displayname      = ?\n" +
            "  , email                 = ?\n" +
            "  , secondaryemail    = ?\n" +
            "  , effectivestartdate = ?\n" +
            "  , effectiveenddate  = ?\n" +
            "  , picture             = ?\n" +
            "  , subjectdirectoryid = ?\n";

    private static final String SQL_WHERE = "where id = ?\n";

    private static final String SQL_WHERE_UUID_IS = "where uuid = CAST(? AS uuid)\n";

    private static final String SQL_WHERE_NAME_IS = "where lower(subjectname) = lower(?)\n";

    private static final String SQL_WHERE_NAME_LIKE = "where lower(subjectname) like lower(?)\n";

    private static final String SQL_WHERE_ONLY_USERS = "where subjecttypeid=" + Constants.SUBJECT_TYPE_USER + "\n";

    private static final String SQL_ORDERBY_NAME = "order by subjectname\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE_NAME_IS;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE_NAME_LIKE;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE + SQL_WHERE_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE_UUID_IS;

    private static final String SQL_DELETE_ALL_USERS = SQL_DELETE + SQL_WHERE_ONLY_USERS;

}