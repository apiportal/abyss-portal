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
import com.verapi.abyss.sql.builder.Select;
import com.verapi.abyss.sql.builder.impl.Table;
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

public class ApiService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(ApiService.class);

    public ApiService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public ApiService(Vertx vertx) {
        super(vertx);
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
                            .add(jsonObj.getString("subjectid"))
                            .add(jsonObj.getBoolean("isproxyapi"))
                            .add(jsonObj.getString("apistateid"))
                            .add(jsonObj.getString("apivisibilityid"))
                            .add(jsonObj.getString("languagename"))
                            .add(jsonObj.getString("languageversion"))
                            .add(((Number) jsonObj.getValue("languageformat")).longValue())
                            .add(jsonObj.getString("originaldocument"))
                            .add(jsonObj.getJsonObject("openapidocument").encode())
                            .add(jsonObj.getJsonObject("extendeddocument").encode());
                    if (jsonObj.getBoolean("isproxyapi"))
                        insertParam.add(jsonObj.getString("businessapiid"));
                    insertParam.add(jsonObj.getString("image"))
                            .add(jsonObj.getString("color"))
                            .add(jsonObj.getInstant("deployed"))
                            .add(jsonObj.getString("changelog"))
                            .add(jsonObj.getString("version"))
                            .add(jsonObj.getBoolean("issandbox"))
                            .add(jsonObj.getBoolean("islive"))
                            .add(jsonObj.getBoolean("isdefaultversion"))
                            .add(jsonObj.getBoolean("islatestversion"))
                            .add(jsonObj.getString("apioriginid"))
                            .add(jsonObj.getString("apiparentid"));

                    return insert(insertParam, (jsonObj.getBoolean("isproxyapi")) ? SQL_INSERT : SQL_INSERT_BUSINESS_API).toObservable();
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
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("crudsubjectid"))
                .add(updateRecord.getString("subjectid"))
                .add(updateRecord.getBoolean("isproxyapi"))
                .add(updateRecord.getString("apistateid"))
                .add(updateRecord.getString("apivisibilityid"))
                .add(updateRecord.getString("languagename"))
                .add(updateRecord.getString("languageversion"))
                .add(((Number) updateRecord.getValue("languageformat")).longValue())
                .add(updateRecord.getString("originaldocument"))
                .add(updateRecord.getJsonObject("openapidocument").encode())
                .add(updateRecord.getJsonObject("extendeddocument").encode());
        if (updateRecord.getBoolean("isproxyapi"))
            updateParams.add(updateRecord.getString("businessapiid"));
        updateParams.add(updateRecord.getValue("image"))
                .add(updateRecord.getString("color"))
                .add(updateRecord.getInstant("deployed"))
                .add(updateRecord.getString("changelog"))
                .add(updateRecord.getString("version"))
                .add(updateRecord.getBoolean("issandbox"))
                .add(updateRecord.getBoolean("islive"))
                .add(updateRecord.getBoolean("isdefaultversion"))
                .add(updateRecord.getBoolean("islatestversion"))
                .add(updateRecord.getString("apioriginid"))
                .add(updateRecord.getString("apiparentid"))
                .add(uuid.toString());
        return update(updateParams, (updateRecord.getBoolean("isproxyapi")) ? SQL_UPDATE_BY_UUID : SQL_UPDATE_BUSINESS_API_BY_UUID);
    }

    public Single<List<JsonObject>> updateAll(JsonObject updateRecords) {
        JsonArray jsonArray = new JsonArray();
        updateRecords.forEach(updateRow -> jsonArray.add(new JsonObject(updateRow.getValue().toString()).put("uuid", updateRow.getKey())));
        Observable<Object> updateParamsObservable = Observable.fromIterable(jsonArray);
        return updateParamsObservable
                .flatMap(o -> {
                    JsonObject jsonObj = (JsonObject) o;
                    JsonArray updateParam = new JsonArray()
                            .add(jsonObj.getString("organizationid"))
                            .add(jsonObj.getString("crudsubjectid"))
                            .add(jsonObj.getString("subjectid"))
                            .add(jsonObj.getBoolean("isproxyapi"))
                            .add(jsonObj.getString("apistateid"))
                            .add(jsonObj.getString("apivisibilityid"))
                            .add(jsonObj.getString("languagename"))
                            .add(jsonObj.getString("languageversion"))
                            .add(((Number) jsonObj.getValue("languageformat")).longValue())
                            .add(jsonObj.getString("originaldocument"))
                            .add(jsonObj.getJsonObject("openapidocument").encode())
                            .add(jsonObj.getJsonObject("extendeddocument").encode());
                    if (jsonObj.getBoolean("isproxyapi"))
                        updateParam.add(jsonObj.getString("businessapiid"));
                    updateParam.add(jsonObj.getString("image"))
                            .add(jsonObj.getString("color"))
                            .add(jsonObj.getInstant("deployed"))
                            .add(jsonObj.getString("changelog"))
                            .add(jsonObj.getString("version"))
                            .add(jsonObj.getBoolean("issandbox"))
                            .add(jsonObj.getBoolean("islive"))
                            .add(jsonObj.getBoolean("isdefaultversion"))
                            .add(jsonObj.getBoolean("islatestversion"))
                            .add(jsonObj.getString("apioriginid"))
                            .add(jsonObj.getString("apiparentid"))
                            .add(jsonObj.getString("uuid"));
                    return update(updateParam, (jsonObj.getBoolean("isproxyapi")) ? SQL_UPDATE_BY_UUID : SQL_UPDATE_BUSINESS_API_BY_UUID).toObservable();
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

        if (apiFilterQuery.getFilterQueryParams().isEmpty()) {
            ApiFilterQuery sqlDeleteAllQuery = new ApiFilterQuery().setFilterQuery(SQL_DELETE_ALL).addFilterQuery(apiFilterQuery.getFilterQuery());
            return deleteAll(sqlDeleteAllQuery.getFilterQuery());
        } else {
            return deleteAll(apiFilterQuery.getFilterQuery(), apiFilterQuery.getFilterQueryParams());
        }
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
        return findAll(SQL_SELECT2);
    }

    public Single<ResultSet> findAll(ApiFilterQuery apiFilterQuery) {
        return filter(apiFilterQuery);
    }

    public ApiFilterQuery.APIFilter getAPIFilter() {
        return apiFilter;
    }

    public Single<ResultSet> findAllProxies() {
        return findAll(SQL_FIND_ALL_PROXIES);
    }


    private static final String SQL_INSERT = "insert into api (organizationid, crudsubjectid, subjectid, isproxyapi, apistateid, apivisibilityid, languagename, languageversion,\n" +
            "                 languageformat, originaldocument, openapidocument, extendeddocument, businessapiid, image, color, deployed, changelog,\n" +
            "                 version, issandbox, islive, isdefaultversion, islatestversion, apioriginid, apiparentid)\n" +
            "values\n" +
            "  (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS uuid), CAST(? AS uuid), ?, ?,\n" +
            "                    ?, ?, ?::JSON, ?::JSON, CAST(? AS uuid), ?, ?, ?, ?,\n" +
            "                             ?, ?, ?, ?, ?, CAST(? AS uuid), CAST(? AS uuid));";

    private static final String SQL_INSERT_BUSINESS_API = "insert into api (organizationid, crudsubjectid, subjectid, isproxyapi, apistateid, apivisibilityid, languagename, languageversion,\n" +
            "                 languageformat, originaldocument, openapidocument, extendeddocument, image, color, deployed, changelog,\n" +
            "                 version, issandbox, islive, isdefaultversion, islatestversion, apioriginid, apiparentid)\n" +
            "values\n" +
            "  (CAST(? AS uuid), CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS uuid), CAST(? AS uuid), ?, ?,\n" +
            "                    ?, ?, ?::JSON, ?::JSON, ?, ?, ?, ?,\n" +
            "                             ?, ?, ?, ?, ?, CAST(? AS uuid), CAST(? AS uuid));";

    private static final String SQL_DELETE = "update api\n" +
            "set\n" +
            "  deleted     = now()\n" +
            "  , isdeleted = true\n";

    private static final String SQL_SELECT2 = Select.select()
            .selectAll()
            .from(new Table("API"))
            .toQueryString();

    private static final String SQL_SELECT = "select\n" +
            "  api.uuid,\n" +
            "  api.organizationid,\n" +
            "  api.created,\n" +
            "  api.updated,\n" +
            "  api.deleted,\n" +
            "  api.isdeleted,\n" +
            "  api.crudsubjectid,\n" +
            "  api.isproxyapi,\n" +
            "  api.subjectid,\n" +
            "  api.apistateid,\n" +
            "  api.apivisibilityid,\n" +
            "  api.languagename,\n" +
            "  api.languageversion,\n" +
            "  api.languageformat,\n" +
            "  api.originaldocument,\n" +
            "  api.openapidocument::JSON,\n" +
            "  api.extendeddocument::JSON,\n" +
            "  api.businessapiid,\n" +
            "  api.image,\n" +
            "  api.color,\n" +
            "  api.deployed,\n" +
            "  api.changelog,\n" +
            "  api.version,\n" +
            "  api.issandbox,\n" +
            "  api.islive,\n" +
            "  api.isdefaultversion,\n" +
            "  api.islatestversion,\n" +
            "  api.apioriginid,\n" +
            "  api.apiparentid,\n" +
            "from\n" +
            "api\n";

    private static final String SQL_UPDATE = "UPDATE api\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , subjectid      = CAST(? AS uuid)\n" +
            "  , isproxyapi      = ?\n" +
            "  , apistateid      = CAST(? AS uuid)\n" +
            "  , apivisibilityid      = CAST(? AS uuid)\n" +
            "  , languagename      = ?\n" +
            "  , languageversion      = ?\n" +
            "  , languageformat      = ?\n" +
            "  , originaldocument      = ?\n" +
            "  , openapidocument      = ?::JSON\n" +
            "  , extendeddocument      = ?::JSON\n" +
            "  , businessapiid      = CAST(? AS uuid)\n" +
            "  , image      = ?\n" +
            "  , color      = ?\n" +
            "  , deployed      = ?\n" +
            "  , changelog      = ?\n" +
            "  , version      = ?\n" +
            "  , issandbox      = ?\n" +
            "  , islive      = ?\n" +
            "  , isdefaultversion      = ?\n" +
            "  , islatestversion      = ?\n" +
            "  , apioriginid      = CAST(? AS uuid)\n" +
            "  , apiparentid      = CAST(? AS uuid)\n";

    private static final String SQL_UPDATE_BUSINESS_API = "UPDATE api\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , subjectid      = CAST(? AS uuid)\n" +
            "  , isproxyapi      = ?\n" +
            "  , apistateid      = CAST(? AS uuid)\n" +
            "  , apivisibilityid      = CAST(? AS uuid)\n" +
            "  , languagename      = ?\n" +
            "  , languageversion      = ?\n" +
            "  , languageformat      = ?\n" +
            "  , originaldocument      = ?\n" +
            "  , openapidocument      = ?::JSON\n" +
            "  , extendeddocument      = ?::JSON\n" +
            //"  , businessapiid      = CAST(? AS uuid)\n" +
            "  , image      = ?\n" +
            "  , color      = ?\n" +
            "  , deployed      = ?\n" +
            "  , changelog      = ?\n" +
            "  , version      = ?\n" +
            "  , issandbox      = ?\n" +
            "  , islive      = ?\n" +
            "  , isdefaultversion      = ?\n" +
            "  , islatestversion      = ?\n" +
            "  , apioriginid      = CAST(? AS uuid)\n" +
            "  , apiparentid      = CAST(? AS uuid)\n";


    public static final String SQL_CONDITION_NAME_IS = "lower(openapidocument -> 'info' ->> 'title') = lower(?)\n";

    public static final String SQL_CONDITION_NAME_LIKE = "lower(openapidocument -> 'info' ->> 'title') like lower(?)\n";

    private static final String SQL_CONDITION_SUBJECT_IS = "subjectid = CAST(? AS uuid)\n";

    public static final String SQL_CONDITION_IS_PROXYAPI = "isproxyapi = true\n";

    public static final String SQL_CONDITION_IS_BUSINESSAPI = "isproxyapi = false\n";

    private static final String SQL_ORDERBY_NAME = "order by id\n";

    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";

    private static final String SQL_CONDITION_TAG_IS = "apitagid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_CATEGORY_IS = "apicategoryid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_GROUP_IS = "apigroupid = CAST(? AS uuid)\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_ID_IS;

    public static final String SQL_FIND_BY_UUID = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_NAME_IS;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_NAME_LIKE;

    private static final String SQL_FIND_ALL_PROXIES = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_IS_PROXYAPI + SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;

    public static final String SQL_DELETE_BY_SUBJECT = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_SUBJECT_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BUSINESS_API_BY_UUID = SQL_UPDATE_BUSINESS_API + SQL_WHERE + SQL_CONDITION_UUID_IS;

    public static final String FILTER_BY_SUBJECT = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_SUBJECT_IS;

    public static final String FILTER_BY_BUSINESS_API = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_IS_BUSINESSAPI;

    public static final String FILTER_BY_PROXY_API = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_IS_PROXYAPI;

    public static final String FILTER_BY_SUBJECT_AND_TAG = SQL_SELECT2 + ", api__api_tag\n" +
            SQL_WHERE + "api.uuid = apiid\n" + SQL_AND + SQL_CONDITION_SUBJECT_IS +
            SQL_AND + SQL_CONDITION_TAG_IS;

    public static final String FILTER_BY_SUBJECT_AND_CATEGORY = SQL_SELECT2 + ", api__api_category\n" +
            SQL_WHERE + "api.uuid = apiid\n" + SQL_AND + SQL_CONDITION_SUBJECT_IS +
            SQL_AND + SQL_CONDITION_CATEGORY_IS;

    public static final String FILTER_BY_SUBJECT_AND_GROUP = SQL_SELECT2 + ", api__api_group\n" +
            SQL_WHERE + "api.uuid = apiid\n" + SQL_AND + SQL_CONDITION_SUBJECT_IS +
            SQL_AND + SQL_CONDITION_GROUP_IS;

    public static final String FILTER_APIS_SHARED_WITH_SUBJECT = SQL_SELECT2 + ", subject_permission, resource\n" +
            SQL_WHERE + "subject_permission.subjectid = CAST(? AS uuid) and\n" +
            "subject_permission.resourceid = resource.uuid and\n" +
            "resource.resourcerefid = api.uuid and\n" +
            "resource.resourcetypeid = CAST('" + Constants.RESOURCE_TYPE_API + "' AS uuid) and\n" +
            "(subject_permission.resourceactionid = CAST('" + Constants.RESOURCE_ACTION_VIEW_API + "' AS uuid) OR\n" +
            "subject_permission.resourceactionid = CAST('" + Constants.RESOURCE_ACTION_EDIT_API + "' AS uuid))";

    public static final String FILTER_APIS_SHARED_BY_SUBJECT = SQL_SELECT2 + ", subject_permission, resource\n" +
            SQL_WHERE + "api.subjectid = CAST(? AS uuid) and\n" +
            "api.uuid = resource.resourcerefid and\n" +
            "resource.uuid = subject_permission.resourceid and\n" +
            "(subject_permission.resourceactionid = CAST('" + Constants.RESOURCE_ACTION_VIEW_API + "' AS uuid) OR\n" +
            "subject_permission.resourceactionid = CAST('" + Constants.RESOURCE_ACTION_EDIT_API + "' AS uuid))";

    public static final String FILTER_BY_LICENSE = SQL_SELECT2 + ", api_license\n" +
            SQL_WHERE + "api.uuid = api_license.apiid\n" +
            SQL_AND + "api_license.licenseid = CAST(? AS uuid)\n";

    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    /*static {
        FILTER_BY_SUBJECT.setFilterQuery(SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_IS);
        FILTER_BY_BUSINESS_API.setFilterQuery(SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_BUSINESSAPI);
    }
*/
}