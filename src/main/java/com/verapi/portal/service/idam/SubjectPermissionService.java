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

import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Constants;
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

public class SubjectPermissionService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(SubjectPermissionService.class);

    public SubjectPermissionService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public SubjectPermissionService(Vertx vertx) {
        super(vertx);
    }


    /**
     *
     * @param insertRecord
     * @return recordStatus
     */
    public Single<JsonObject> insert(JsonObject insertRecord, JsonObject parentRecordStatus) {
        logger.trace("---insert invoked");

        JsonArray insertParam = new JsonArray()
                .add(insertRecord.getString("organizationid"))
                .add(insertRecord.getString("crudsubjectid"))
                .add(insertRecord.getString("permission"))
                .add(insertRecord.getString("description"))
                .add(insertRecord.getInstant("effectivestartdate"))
                .add(insertRecord.getInstant("effectiveenddate"))
                .add(insertRecord.getString("subjectid"))
                .add(insertRecord.getString("resourceid"))
                .add(insertRecord.getString("resourceactionid"))
                .add(insertRecord.getString("accessmanagerid"))
                .add(insertRecord.getBoolean("isactive"));
        return insert(insertParam, SQL_INSERT)
                .flatMap(insertResult -> {
                    if (insertResult.getThrowable() == null) {
                        return findById(insertResult.getUpdateResult().getKeys().getInteger(0), SQL_FIND_BY_ID)
                                .flatMap(resultSet -> Single.just(insertResult.setResultSet(resultSet)));
                    } else {
                        return Single.just(insertResult);
                    }
                })
                .flatMap(result -> Single.just(evaluateCompositeResultAndReturnRecordStatus(result, parentRecordStatus)));
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        logger.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(o -> {
                    JsonObject jsonObj = (JsonObject) o;
                    JsonArray insertParam = new JsonArray()
                            .add(jsonObj.getString("organizationid"))
                            .add(jsonObj.getString("crudsubjectid"))
                            .add(jsonObj.getString("permission"))
                            .add(jsonObj.getString("description"))
                            .add(jsonObj.getInstant("effectivestartdate"))
                            .add(jsonObj.getInstant("effectiveenddate"))
                            .add(jsonObj.getString("subjectid"))
                            .add(jsonObj.getString("resourceid"))
                            .add(jsonObj.getString("resourceactionid"))
                            .add(jsonObj.getString("accessmanagerid"))
                            .add(jsonObj.getBoolean("isactive"));
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
                .flatMap(result -> Observable.just(evaluateCompositeResultAndReturnRecordStatus(result))
                )
                .toList();
    }

    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        JsonArray updateParams = new JsonArray()
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("crudsubjectid"))
                .add(updateRecord.getString("permission"))
                .add(updateRecord.getString("description"))
                .add(updateRecord.getInstant("effectivestartdate"))
                .add(updateRecord.getInstant("effectiveenddate"))
                .add(updateRecord.getString("subjectid"))
                .add(updateRecord.getString("resourceid"))
                .add(updateRecord.getString("resourceactionid"))
                .add(updateRecord.getString("accessmanagerid"))
                .add(updateRecord.getBoolean("isactive"))
                .add(uuid.toString());
        return update(updateParams, SQL_UPDATE_BY_UUID);
    }

    public Single<CompositeResult> updateByRef(JsonObject updateRecord) {
        JsonArray updateParams = new JsonArray()
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("crudsubjectid"))
                .add(updateRecord.getString("permission"))
                .add(updateRecord.getString("description"))
                .add(updateRecord.getInstant("effectivestartdate"))
                .add(updateRecord.getInstant("effectiveenddate"))
                .add(updateRecord.getString("subjectid"))
                .add(updateRecord.getString("resourceid"))
                .add(updateRecord.getString("resourceactionid"))
                .add(updateRecord.getString("accessmanagerid"))
                .add(updateRecord.getBoolean("isactive"))
                //Where Condition Args
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("subjectid"))
                .add(updateRecord.getString("resourceid"))
                .add(updateRecord.getString("resourceactionid"));
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
                    JsonArray updateParam = new JsonArray()
                            .add(jsonObj.getString("organizationid"))
                            .add(jsonObj.getString("crudsubjectid"))
                            .add(jsonObj.getString("permission"))
                            .add(jsonObj.getString("description"))
                            .add(jsonObj.getInstant("effectivestartdate"))
                            .add(jsonObj.getInstant("effectiveenddate"))
                            .add(jsonObj.getString("subjectid"))
                            .add(jsonObj.getString("resourceid"))
                            .add(jsonObj.getString("resourceactionid"))
                            .add(jsonObj.getString("accessmanagerid"))
                            .add(jsonObj.getBoolean("isactive"))
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

    private static final String SQL_INSERT = "insert into subject_permission (organizationid, crudsubjectid, permission, description, effectivestartdate, effectiveenddate, subjectid, resourceid, resourceactionid, accessmanagerid, isactive)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), ?, ?, ?, ?, CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?)";

    private static final String SQL_DELETE = "update subject_permission\n" +
            "set\n" +
            "  deleted     = now()\n" +
            "  , isdeleted = true\n";

    private static final String SQL_SELECT = "select\n" +
            "  subject_permission.uuid,\n" +
            "  subject_permission.organizationid,\n" +
            "  subject_permission.created,\n" +
            "  subject_permission.updated,\n" +
            "  subject_permission.deleted,\n" +
            "  subject_permission.isdeleted,\n" +
            "  subject_permission.crudsubjectid,\n" +
            "  subject_permission.permission,\n" +
            "  subject_permission.description,\n" +
            "  subject_permission.effectivestartdate,\n" +
            "  subject_permission.effectiveenddate,\n" +
            "  subject_permission.subjectid,\n" +
            "  subject_permission.resourceid,\n" +
            "  subject_permission.resourceactionid,\n" +
            "  subject_permission.accessmanagerid,\n" +
            "  subject_permission.isactive\n" +
            "from\n" +
            "subject_permission\n";

    private static final String SQL_UPDATE = "UPDATE subject_permission\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , permission      = ?\n" +
            "  , description       = ?\n" +
            "  , effectivestartdate      = ?\n" +
            "  , effectiveenddate      = ?\n" +
            "  , subjectid      = CAST(? AS uuid)\n" +
            "  , resourceid      = CAST(? AS uuid)\n" +
            "  , resourceactionid      = CAST(? AS uuid)\n" +
            "  , accessmanagerid      = CAST(? AS uuid)\n" +
            "  , isactive      = ?\n";


    private static final String SQL_CONDITION_NAME_IS = "lower(permission) = lower(?)\n";

    private static final String SQL_CONDITION_NAME_LIKE = "lower(permission) like lower(?)\n";

    private static final String SQL_ORDERBY_NAME = "order by permission\n";

    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";

    private static final String SQL_CONDITION_SUBJECT_IS = "subjectid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_RESOURCEID_IS = "resourceid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_RESOURCEACTIONID_IS = "resourceactionid = CAST(? AS uuid)\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;

    public static final String SQL_FIND_BY_SUBJECTID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_IS;

    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;

    public static final String SQL_DELETE_BY_SUBJECT = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_SUBJECT_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BY_REF_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_ORGANIZATION_IS +
                                                                        SQL_AND + SQL_CONDITION_SUBJECT_IS +
                                                                        SQL_AND + SQL_CONDITION_RESOURCEID_IS +
                                                                        SQL_AND + SQL_CONDITION_RESOURCEACTIONID_IS +
                                                                        SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    public static final String FILTER_BY_SUBJECT = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_IS;

    public static final String SQL_LIST_SUBJECT_API_SUBSCRIPTIONS = SQL_SELECT + ", resource\n" +
            SQL_WHERE + "subject_permission.subjectid = CAST(? AS uuid) and subject_permission.resourceid = resource.uuid and\n" +
            "resource.resourcetypeid = CAST('" + Constants.RESOURCE_TYPE_API + "' AS uuid)\n";
    
    public static final String SQL_LIST_SUBSCRIPTIONS_TO_MY_APIS = SQL_SELECT + ", resource, api\n" +
            SQL_WHERE + "api.subjectid = CAST(? AS uuid) and\n" +
            "api.uuid = resource.resourcerefid and\n" +
            "subject_permission.resourceid = resource.uuid and\n" +
            "resource.resourcetypeid = CAST('" + Constants.RESOURCE_TYPE_API + "' AS uuid)\n";

    public static final String SQL_CHECK_PERMISSION_OF_SUBJECT_IN_ORGANIZATION =
            "select s.displayname, s.subjectname, o.name, p.organizationid, p.permission, r.resourcename, rt.type, r.resourcerefid, ra.actionname\n" +
            "from\n" +
            "subject_permission\n p, resource r, resource_action ra, subject s, resource_type rt, organization o\n" +
            "where p.organizationid = o.uuid and p.organizationid = CAST(? AS uuid)\n" +
            "and p.subjectid = s.uuid and s.uuid = CAST(? AS uuid)\n" +
            "and r.resourcetypeid = rt.uuid and rt.uuid = '41bfd648-308e-401f-a1ce-9dbbf4e56eb6'\n" + // --OPERATION ID RESOURCE TYPE
            "and p.resourceid = r.uuid and (r.resourcename = ?)\n" +
            "and p.resourceactionid = ra.uuid and ra.uuid = 'cf52d8fc-591f-42dc-be1b-13983086f64d'\n"; // --INVOKE OPERATION ACTION

    public static final String SQL_CHECK_ROLE_BASED_PERMISSION_OF_SUBJECT_IN_ORGANIZATION = // Params: (:org, :user, :operationId)
            "select o.name, p.organizationid, p.permission, r.resourcename, rt.\"type\", r.resourcerefid, ra.actionname, s.subjectname\n" +
            "from\n" +
            "subject_permission\n p, resource r, resource_action ra, resource_type rt, organization o, subject_membership sm, subject s\n" +
            "where p.organizationid = o.uuid and p.organizationid in (CAST(? AS uuid), '3c65fafc-8f3a-4243-9c4e-2821aa32d293'::uuid) and o.isactive = true and p.isactive = true\n" +
            "and p.subjectid = sm.subjectgroupid and sm.subjectid = CAST(? AS uuid) and sm.subjecttypeid = '21371a15-04f8-445e-a899-006ee11c0e09'::uuid and sm.subjectgrouptypeid = 'bb76f638-632d-41f8-9511-9865091701f9'::uuid and sm.isactive = true\n" +
            "and r.resourcetypeid = rt.uuid and rt.uuid = '41bfd648-308e-401f-a1ce-9dbbf4e56eb6'::uuid and rt.isactive = true\n" + // --OPERATION ID RESOURCE TYPE
            "and p.resourceid = r.uuid and (r.resourcename = ?) and r.isactive = true\n" +
            "and p.resourceactionid = ra.uuid and ra.uuid = 'cf52d8fc-591f-42dc-be1b-13983086f64d'::uuid and ra.isactive = true\n" +// --INVOKE OPERATION ACTION
            "and s.subjecttypeid = 'bb76f638-632d-41f8-9511-9865091701f9'::uuid and s.uuid = sm.subjectgroupid and s.isactivated = true and s.islocked = false and s.isdeleted = false";
            //TODO: Check effective-date-start-end

}
