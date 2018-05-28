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
import com.verapi.portal.common.Util;
import com.verapi.portal.oapi.schema.ApiSchemaError;
import com.verapi.portal.service.AbstractService;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class SubjectService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(SubjectService.class);

    private JsonObject requestJsonObject;

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

    /*
    public Single<UpdateResult> insert(JsonObject newRecord) {
        JsonArray insertParams = new JsonArray()
                .add(((Number) newRecord.getValue("organizationid")).longValue())
                .add(((Number) newRecord.getValue("crudsubjectid")).longValue())
                .add(((Number) newRecord.getValue("subjecttypeid")).longValue())
                .add(((String) newRecord.getValue("subjectname")))
                .add(((String) newRecord.getValue("firstname")))
                .add(((String) newRecord.getValue("lastname")))
                .add(((String) newRecord.getValue("displayname")))
                .add(((String) newRecord.getValue("email")))
                .add(((String) newRecord.getValue("secondaryemail")))
                .add((newRecord.getInstant("effectivestartdate")))
                .add((newRecord.getInstant("effectiveenddate")))
                .add(((String) newRecord.getValue("password")))
                .add(((String) newRecord.getValue("passwordsalt")))
                .add(((String) newRecord.getValue("picture")))
                .add(((Number) newRecord.getValue("subjectdirectoryid")).longValue());
        return insert(insertParams, SQL_INSERT);
    }
*/

    public Single<List<JsonObject>> insertAll(JsonArray insertParams) {
        //JsonArray result = new JsonArray();

        Observable<Object> insertParamsObservable = Observable.fromIterable(insertParams);

        return insertParamsObservable
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

                    return insert(insertParam, SQL_INSERT);

                })
                .flatMap(updateResult -> findById(updateResult.getKeys().getInteger(0), SQL_FIND_BY_ID).toObservable())
                .flatMap(resultSet -> {
                    JsonArray arr = new JsonArray();
                    resultSet.getRows().forEach(arr::add);
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", resultSet.getRows().get(0).getString("uuid"))
                            .put("status", HttpResponseStatus.CREATED.code())
                            .put("response", arr.getJsonObject(0))
                            .put("error", new ApiSchemaError().toJson());
                    //result.add(recordStatus);

                    return Observable.just(recordStatus);
                })

                .onErrorReturn(throwable -> new JsonObject().put("uuid", "0")
                        .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                        .put("response", new JsonObject()) //TODO: fill with empty Subject response json
                        .put("error", new ApiSchemaError()
                                .setUsermessage(throwable.getLocalizedMessage())
                                .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                .toJson()))
/*
                    -> {
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", "0")
                            .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put("response", new JsonObject()) //TODO: fill with empty Subject response json
                            .put("error", new ApiSchemaError()
                                    .setUsermessage(throwable.getLocalizedMessage())
                                    .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                    .toJson());
                    //result.add(recordStatus);

                    return Observable.just(recordStatus);

                })
*/

/*
                .onExceptionResumeNext(observer -> {

                })
*/

/*                .onErrorResumeNext(throwable -> {
                    JsonObject recordStatus = new JsonObject()
                            .put("uuid", "0")
                            .put("status", HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                            .put("response", new JsonObject()) //TODO: fill with empty Subject response json
                            .put("error", new ApiSchemaError()
                                    .setUsermessage(throwable.getLocalizedMessage())
                                    .setCode(HttpResponseStatus.INTERNAL_SERVER_ERROR.code())
                                    .setInternalmessage(Arrays.toString(throwable.getStackTrace()))
                                    .toJson());
                    //result.add(recordStatus);

                    return Observable.just(recordStatus);

                })*/
                .toList();
                //.collect(Collectors.toList());


/*
        Single<ResultSet> recordInsertResult = Single
                .fromObservable(insertParamsObservable)
                .flatMap(record -> {
                    JsonArray insertParam = new JsonArray()
                            .add(((Number) ((JsonObject) record).getValue("organizationid")).longValue())
                            .add(((Number) ((JsonObject) record).getValue("crudsubjectid")).longValue())
                            .add(((Number) ((JsonObject) record).getValue("subjecttypeid")).longValue())
                            .add(((String) ((JsonObject) record).getValue("subjectname")))
                            .add(((String) ((JsonObject) record).getValue("firstname")))
                            .add(((String) ((JsonObject) record).getValue("lastname")))
                            .add(((String) ((JsonObject) record).getValue("displayname")))
                            .add(((String) ((JsonObject) record).getValue("email")))
                            .add(((String) ((JsonObject) record).getValue("secondaryemail")))
                            .add(((JsonObject) record).getInstant("effectivestartdate"))
                            .add(((JsonObject) record).getInstant("effectiveenddate"))
                            .add(((String) ((JsonObject) record).getValue("password")))
                            .add(((String) ((JsonObject) record).getValue("passwordsalt")))
                            .add(((String) ((JsonObject) record).getValue("picture")))
                            .add(((Number) ((JsonObject) record).getValue("subjectdirectoryid")).longValue());

                    return insert(insertParam, SQL_INSERT);
                })
                .flatMap(updateResult -> findById(updateResult.getKeys().getInteger(0), SQL_FIND_BY_ID));

        subscribeAndProcess(result, recordInsertResult, HttpResponseStatus.CREATED.code());
*/

/*
        insertParams.forEach(record -> {
            JsonArray insertParam = new JsonArray()
                    .add(((Number) ((JsonObject) record).getValue("organizationid")).longValue())
                    .add(((Number) ((JsonObject) record).getValue("crudsubjectid")).longValue())
                    .add(((Number) ((JsonObject) record).getValue("subjecttypeid")).longValue())
                    .add(((String) ((JsonObject) record).getValue("subjectname")))
                    .add(((String) ((JsonObject) record).getValue("firstname")))
                    .add(((String) ((JsonObject) record).getValue("lastname")))
                    .add(((String) ((JsonObject) record).getValue("displayname")))
                    .add(((String) ((JsonObject) record).getValue("email")))
                    .add(((String) ((JsonObject) record).getValue("secondaryemail")))
                    .add(((JsonObject) record).getInstant("effectivestartdate"))
                    .add(((JsonObject) record).getInstant("effectiveenddate"))
                    .add(((String) ((JsonObject) record).getValue("password")))
                    .add(((String) ((JsonObject) record).getValue("passwordsalt")))
                    .add(((String) ((JsonObject) record).getValue("picture")))
                    .add(((Number) ((JsonObject) record).getValue("subjectdirectoryid")).longValue());
            Single<ResultSet> recordInsertResult = initJDBCClient()
                    .flatMap(jdbcClient -> insert(insertParam, SQL_INSERT))
                    .flatMap(insertResult -> findById(insertResult.getKeys().getInteger(0), SQL_FIND_BY_ID))
                    .flatMap(Single::just);
            subscribeAndProcess(result, recordInsertResult, HttpResponseStatus.CREATED.code());
        });
*/
        //return Single.just(result);
    }

    public Single<UpdateResult> update(UUID uuid, JsonObject updateRecord) {
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
                .add(uuid);
        return update(updateParams, SQL_UPDATE_BY_UUID).toList()
                .flatMap(updateResults -> Single.just(updateResults.get(0)));
    }

    public Single<JsonArray> updateAll(JsonObject updateRecord) {
        JsonArray result = new JsonArray();
        updateRecord.forEach(record -> {
            JsonArray updateParams = new JsonArray()
                    .add(((Number) ((JsonObject) record.getValue()).getValue("organizationid")).longValue())
                    .add(((Number) ((JsonObject) record.getValue()).getValue("crudsubjectid")).longValue())
                    .add(((Number) ((JsonObject) record.getValue()).getValue("subjecttypeid")).longValue())
                    .add(((String) ((JsonObject) record.getValue()).getValue("subjectname")))
                    .add(((String) ((JsonObject) record.getValue()).getValue("firstname")))
                    .add(((String) ((JsonObject) record.getValue()).getValue("lastname")))
                    .add(((String) ((JsonObject) record.getValue()).getValue("displayname")))
                    .add(((String) ((JsonObject) record.getValue()).getValue("email")))
                    .add(((String) ((JsonObject) record.getValue()).getValue("secondaryemail")))
                    .add(((Instant) ((JsonObject) record.getValue()).getValue("effectivestartdate")))
                    .add(((Instant) ((JsonObject) record.getValue()).getValue("effectiveenddate")))
                    .add(((String) ((JsonObject) record.getValue()).getValue("picture")))
                    .add(((Number) ((JsonObject) record.getValue()).getValue("subjectdirectoryid")).longValue())
                    .add(record.getKey());
            Single<ResultSet> recordUpdateResult = initJDBCClient()
                    .flatMap(jdbcClient -> update(updateParams, SQL_UPDATE_BY_UUID))
                    .flatMap(updateResult -> findById(updateResult.getKeys().getInteger(0), SQL_FIND_BY_ID))
                    .flatMap(Single::just);
            subscribeAndProcess(result, recordUpdateResult, HttpResponseStatus.OK.code());
        });
        return Single.just(result);
    }

    public Single<UpdateResult> delete(UUID uuid) {
        JsonArray deleteParams = new JsonArray().add(uuid);
        return delete(deleteParams, SQL_DELETE_BY_UUID).toList()
                .flatMap(updateResults -> Single.just(updateResults.get(0)));
    }

    public Single<List<UpdateResult>> deleteAll() {
        return deleteAll(SQL_DELETE).toList();
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

    public Single<ResultSet> findAll() {
        return findAll(SQL_SELECT);
    }

    private static final String SQL_INSERT = "insert into subject (organizationid, crudsubjectid, subjecttypeid, subjectname, firstname, lastname, displayname, email, secondaryemail, effectivestartdate, effectiveenddate, password, passwordsalt, picture, subjectdirectoryid)\n" +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, coalesce(?, now()), ?, ?, ?, ?, ?)";

    private static final String SQL_DELETE = "update subject set isdeleted = true\n";

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
            "  organizationid       = ?\n" +
            "  , crudsubjectid      = ?\n" +
            "  , subjecttypeid      = ?\n" +
            "  , subjectname        = ?\n" +
            "  , firstname          = ?\n" +
            "  , lastname           = ?\n" +
            "  , displayname        = ?\n" +
            "  , email              = ?\n" +
            "  , secondaryemail     = ?\n" +
            "  , effectivestartdate = ?\n" +
            "  , effectiveenddate   = ?\n" +
            "  , picture            = ?\n" +
            "  , subjectdirectoryid = ?\n";

    private static final String SQL_WHERE = "where id = ?\n";

    private static final String SQL_WHERE_UUID_IS = "where uuid = ?\n";

    private static final String SQL_WHERE_NAME_IS = "where lower(subjectname) = lower(?)";

    private static final String SQL_ORDERBY_NAME = "order by subjectname\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE_NAME_IS;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE + SQL_WHERE_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE_UUID_IS;

}