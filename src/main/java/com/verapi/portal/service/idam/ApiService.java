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
import com.verapi.abyss.sql.builder.Select;
import com.verapi.abyss.sql.builder.impl.Table;
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

@AbyssTableName(tableName = "api")
public class ApiService extends AbstractService<UpdateResult> {
    public static final String SQL_CONDITION_NAME_IS = "lower(openapidocument -> 'info' ->> 'title') = lower(?)\n";
    public static final String SQL_CONDITION_NAME_LIKE = "lower(openapidocument -> 'info' ->> 'title') like lower(?)\n";
    public static final String SQL_CONDITION_IS_PROXYAPI = "isproxyapi = true\n";
    public static final String SQL_CONDITION_IS_BUSINESSAPI = "isproxyapi = false\n";
    public static final String SQL_GET_IMAGE_BY_UUID = "select image\nfrom\napi\n" + SQL_WHERE + SQL_CONDITION_UUID_IS;
    public static final String FILTER_PROXIES_WITH_RESOURCES_FOR_EXPLORE = "SELECT \n" +
            "a.uuid, a.organizationid, a.created, a.updated, a.deleted, a.isdeleted, a.crudsubjectid, a.subjectid, \n" +
            "a.apistateid, a.apivisibilityid, a.languagename, a.languageversion, a.languageformat, a.color, a.deployed, \n" +
            "a.changelog, a.\"version\", a.issandbox, a.islive, a.isdefaultversion, a.islatestversion, a.apioriginid, a.apiparentid,\n" +
            "a.openapidocument->'info'->>'title' as apititle, \n" +
            "a.openapidocument->'info'->>'description' as apidescription,\n" +
            "a.openapidocument->'info'->>'version' as apiversion,\n" +
            "a.openapidocument->'info'->'license'->>'name' as apilicense,\n" +
            "a.openapidocument->'servers' as apiservers,\n" +
            "s.displayname as apiowner,\n" +
            "COALESCE((select json_agg(\n" +
            "json_build_object('uuid', r.uuid, 'organizationid', r.organizationid, 'created', r.created, 'updated', r.updated, 'deleted', r.deleted, 'isdeleted', r.isdeleted, \n" +
            "\t\t\t\t\t'crudsubjectid', r.crudsubjectid, 'resourcetypeid', r.resourcetypeid, 'resourcename', r.resourcename, 'description', r.description, \n" +
            "\t\t\t\t\t'resourcerefid', r.resourcerefid, 'isactive', r.isactive, 'subresourcename', r.subresourcename,\n" +
            "\t\t\t\t\t'permissions', COALESCE((select json_agg(\n" +
            "\t\t\t\t\t\t\t\tjson_build_object('uuid', sp.uuid, 'organizationid', sp.organizationid, 'created', sp.created, 'updated', sp.updated, 'deleted', sp.deleted, \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t'isdeleted', sp.isdeleted, 'crudsubjectid', sp.crudsubjectid, \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t'permission', sp.permission, 'description', sp.description, 'effectivestartdate', sp.effectivestartdate, 'effectiveenddate', sp.effectiveenddate, \n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t'subjectid', sp.subjectid, 'resourceid', sp.resourceid, 'resourceactionid', sp.resourceactionid, 'accessmanagerid', sp.accessmanagerid, 'isactive', sp.isactive)\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t) FROM subject_permission sp\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\t\twhere sp.resourceid = r.uuid\n" +
            "\t\t\t\t\t\t\t\t\t), '[]')\n" +
            "\t\t\t\t)\n" +
            "\t\t) FROM resource r\n" +
            "\t\t\twhere a.uuid = r.resourcerefid\n" +
            "\t\t\t), '[]') as resources,\n" +
            "COALESCE((select json_agg(\n" +
            "json_build_object('uuid', l.uuid, 'organizationid', l.organizationid, 'created', l.created, 'updated', l.updated, 'deleted', l.deleted, 'isdeleted', l.isdeleted, \n" +
            "\t\t\t\t'crudsubjectid', l.crudsubjectid, 'name', l.\"name\", 'version', l.\"version\", 'subjectid', l.subjectid, 'licensedocument', l.licensedocument, 'isactive', l.isactive)\n" +
            "\t\t) from license l\n" +
            "\t\t\t\tjoin api_license al on al.apiid = a.uuid\n" +
            "\t\t\twhere al.licenseid = l.uuid and al.isdeleted = false and al.isactive = true\n" +
            "\t\t\t), '[]') as availablelicenses\n" +
            "FROM\napi\na\n" +
            "\tjoin subject s on s.uuid = a.subjectid\n" +
            "where a.isproxyapi = true\n" + // --Only Proxies
            "and a.isdeleted = false\n" + // --Not Deleted
            "and a.apivisibilityid = 'e63c2874-aa12-433c-9dcf-65c1e8738a14'::uuid\n" + // --Public
            "and a.apistateid = '1425993f-f6be-4ca0-84fe-8a83e983ffd9'::uuid"; // --Promoted
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiService.class);
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
    public static final String SQL_FIND_BY_UUID = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_UUID_IS;
    public static final String FILTER_BY_BUSINESS_API = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_IS_BUSINESSAPI;
    public static final String FILTER_BY_PROXY_API = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_IS_PROXYAPI;
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
            SQL_AND + "api_license.licenseid = CAST(? AS uuid)\n" +
            SQL_AND + "api_license.isdeleted = false\n" +
            SQL_AND + "api_license.isactive = true";
    public static final String FILTER_BY_POLICY = SQL_SELECT2 + SQL_WHERE + "uuid in (\n" +
            "    select apiid\n" +
            "    from api_license\n" +
            "    where isdeleted = false\n" +
            "    and isactive = true\n" +
            "    and licenseid in (\n" +
            "        select uuid\n" +
            "        from license\n" +
            "        where licensedocument -> 'termsOfService' -> 'policyKey' @> ?::jsonb))";
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
    private static final String SQL_CONDITION_SUBJECT_IS = "subjectid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_SUBJECT = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_SUBJECT_IS;
    private static final String SQL_ORDERBY_NAME = "order by id\n";
    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";
    private static final String SQL_CONDITION_TAG_IS = "apitagid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_SUBJECT_AND_TAG = SQL_SELECT2 + ", api__api_tag\n" +
            SQL_WHERE + "api.uuid = apiid\n" + SQL_AND + SQL_CONDITION_SUBJECT_IS +
            SQL_AND + SQL_CONDITION_TAG_IS;
    private static final String SQL_CONDITION_CATEGORY_IS = "apicategoryid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_SUBJECT_AND_CATEGORY = SQL_SELECT2 + ", api__api_category\n" +
            SQL_WHERE + "api.uuid = apiid\n" + SQL_AND + SQL_CONDITION_SUBJECT_IS +
            SQL_AND + SQL_CONDITION_CATEGORY_IS;
    private static final String SQL_CONDITION_GROUP_IS = "apigroupid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_SUBJECT_AND_GROUP = SQL_SELECT2 + ", api__api_group\n" +
            SQL_WHERE + "api.uuid = apiid\n" + SQL_AND + SQL_CONDITION_SUBJECT_IS +
            SQL_AND + SQL_CONDITION_GROUP_IS;
    private static final String SQL_FIND_BY_ID = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_ID_IS;
    private static final String SQL_FIND_BY_NAME = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_NAME_IS;
    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_NAME_LIKE;
    private static final String SQL_FIND_ALL_PROXIES = SQL_SELECT2 + SQL_WHERE + SQL_CONDITION_IS_PROXYAPI + SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;
    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;
    public static final String SQL_DELETE_BY_SUBJECT = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_SUBJECT_IS;
    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_BUSINESS_API_BY_UUID = SQL_UPDATE_BUSINESS_API + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_API_LIFECYCLE = "UPDATE\napi\n" +
            "SET\n" +
            "    updated         = now()\n" +
            "  , crudsubjectid   = CAST(? AS uuid)\n" +
            "  , apistateid      = CAST(? AS uuid)\n" +
            "  , apivisibilityid = CAST(? AS uuid)\n" +
            SQL_WHERE + SQL_CONDITION_UUID_IS;// +
    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    public ApiService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public ApiService(Vertx vertx) {
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
                .add(insertRecord.getString("subjectid"))
                .add(insertRecord.getBoolean("isproxyapi"))
                .add(insertRecord.getString("apistateid"))
                .add(insertRecord.getString("apivisibilityid"))
                .add(insertRecord.getString("languagename"))
                .add(insertRecord.getString("languageversion"))
                .add(((Number) insertRecord.getValue("languageformat")).longValue())
                .add(insertRecord.getString("originaldocument"))
                .add(insertRecord.getJsonObject("openapidocument").encode())
                .add(insertRecord.getJsonObject("extendeddocument").encode());
        if (insertRecord.getBoolean("isproxyapi")) {
            insertParam.add(insertRecord.getString("businessapiid"));
        }
        insertParam.add(insertRecord.getString("image"))
                .add(insertRecord.getString("color"))
                .add(insertRecord.getInstant("deployed"))
                .add(insertRecord.getString("changelog"))
                .add(insertRecord.getString("version"))
                .add(insertRecord.getBoolean("issandbox"))
                .add(insertRecord.getBoolean("islive"))
                .add(insertRecord.getBoolean("isdefaultversion"))
                .add(insertRecord.getBoolean("islatestversion"))
                .add(insertRecord.getString("apioriginid"))
                .add(insertRecord.getString("apiparentid"));

        return insertParam;
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        LOGGER.trace("---insertAll invoked");
        Observable<Object> insertParamsObservable = Observable.fromIterable(insertRecords);
        return insertParamsObservable
                .flatMap(o -> Observable.just((JsonObject) o))
                .flatMap(jsonObj -> {
                    JsonArray insertParam = prepareInsertParameters(jsonObj);

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
        return update(updateParams, (updateRecord.getBoolean("isproxyapi")) ? SQL_UPDATE_BY_UUID : SQL_UPDATE_BUSINESS_API_BY_UUID);
    }

    public Single<CompositeResult> updateLifecycle(UUID uuid, JsonArray updateParams) {
        return update(updateParams.add(uuid.toString()), SQL_UPDATE_API_LIFECYCLE);
    }

    public Single<List<JsonObject>> updateAll(JsonObject updateRecords) {
        JsonArray jsonArray = new JsonArray();
        updateRecords.forEach(updateRow -> jsonArray.add(new JsonObject(updateRow.getValue().toString()).put("uuid", updateRow.getKey())));
        Observable<Object> updateParamsObservable = Observable.fromIterable(jsonArray);
        return updateParamsObservable
                .flatMap(o -> {
                    JsonObject jsonObj = (JsonObject) o;
                    JsonArray updateParam = prepareInsertParameters(jsonObj)
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
    //SQL_AND + SQL_CONDITION_IS_PROXYAPI;

    public Single<ResultSet> findAllProxies() {
        return findAll(SQL_FIND_ALL_PROXIES);
    }

    /*static {
        FILTER_BY_SUBJECT.setFilterQuery(SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECT_IS);
        FILTER_BY_BUSINESS_API.setFilterQuery(SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_BUSINESSAPI);
    }
*/
}
