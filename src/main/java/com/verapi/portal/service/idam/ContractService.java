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

import static java.time.temporal.ChronoUnit.DAYS;

@AbyssTableName(tableName = "contract")
public class ContractService extends AbstractService<UpdateResult> {

    private static final String NO_DATA_FOUND = "no_data_found";

    private static final String SQL_UNSUBSCRIBE = "select\tc.uuid, \n" +
            "\t\tr.uuid as resource_uuid,\n" +
            "\t\tsp.uuid as subject_permission_uuid,\n" +
            "\t\tCOALESCE((select json_agg(rat.uuid\n" +
            "\t\t\t\t\t) from resource_access_token rat\n" +
            "\t\t\t\t\t\twhere rat.subjectpermissionid = sp.uuid\n" +
            "\t\t\t\t), '[]') as resource_access_token_uuid\n" +
            "FROM\ncontract\nc\n" +
            "\tleft outer join resource r on r.resourcerefid = c.uuid\n" +
            "\tleft outer join subject_permission sp on sp.uuid = c.subjectpermissionid\n" +
            "where c.uuid = CAST(? AS uuid)";

    public static final String SQL_SUBSCRIPTIONS_OF_API = "select\tc.uuid, c.organizationid, c.created, c.updated, c.deleted, c.isdeleted, c.crudsubjectid, c.\"name\", c.description, c.apiid, c.subjectid, c.environment, \n" +
            "\t\tc.contractstateid, c.status, c.isrestrictedtosubsetofapi, c.licenseid, c.subjectpermissionid,\n" +
            "\t\tapp.displayname as appdisplayname, \n" +
            "\t\tCOALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', s.uuid, 'displayname', s.displayname)\n" +
            "\t\t\t\t\t) FROM subject s\n" +
            "\t\t\t\t\t\twhere sm.subjectid = s.uuid and s.subjecttypeid = '21371a15-04f8-445e-a899-006ee11c0e09'\n" +
            "\t\t\t), '[]') as appowners,\n" +
            "\t\tCOALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', r.uuid, 'organizationid', r.organizationid, 'created', r.created, 'updated', r.updated, 'deleted', r.deleted, 'isdeleted', r.isdeleted, \n" +
            "\t\t\t\t\t\t\t\t\t'crudsubjectid', r.crudsubjectid, 'resourcetypeid', r.resourcetypeid, 'resourcename', r.resourcename, 'description', r.description, \n" +
            "\t\t\t\t\t\t\t\t\t'resourcerefid', r.resourcerefid, 'isactive', r.isactive, 'subresourcename', r.subresourcename)\n" +
            "\t\t\t\t\t) FROM resource r\n" +
            "\t\t\t\t\t\twhere c.uuid = r.resourcerefid\n" +
            "\t\t\t), '[]') as resources,\n" +
            "\t\tCOALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', sp.uuid, 'organizationid', sp.organizationid, 'created', sp.created, 'updated', sp.updated, 'deleted', sp.deleted, \n" +
            "\t\t\t\t\t\t\t\t\t'isdeleted', sp.isdeleted, 'crudsubjectid', sp.crudsubjectid, \n" +
            "\t\t\t\t\t\t\t\t\t'permission', sp.permission, 'description', sp.description, 'effectivestartdate', sp.effectivestartdate, 'effectiveenddate', sp.effectiveenddate, \n" +
            "\t\t\t\t\t\t\t\t\t'subjectid', sp.subjectid, 'resourceid', sp.resourceid, 'resourceactionid', sp.resourceactionid, 'accessmanagerid', sp.accessmanagerid, 'isactive', sp.isactive,\n" +
            "\t\t\t\t\t\t\t\t\t'accesstokens', COALESCE((select json_agg(\n" +
            "\t\t\t\t\t\t\t\t\t\t\tjson_build_object('uuid', rat.uuid, 'organizationid', rat.organizationid, 'created', rat.created, 'updated', rat.updated, 'deleted', rat.deleted, 'isdeleted', rat.isdeleted, \n" +
            "\t\t\t\t\t\t\t\t\t\t\t'crudsubjectid', rat.crudsubjectid, 'subjectpermissionid', rat.subjectpermissionid, 'resourcetypeid', rat.resourcetypeid, 'resourcerefid', rat.resourcerefid, \n" +
            "\t\t\t\t\t\t\t\t\t\t\t'token', rat.token, 'expiredate', rat.expiredate, 'isactive', rat.isactive)\n" +
            "\t\t\t\t\t\t\t\t\t\t\t) from resource_access_token rat\n" +
            "\t\t\t\t\t\t\t\t\t\t\t\twhere rat.subjectpermissionid = sp.uuid\n" +
            "\t\t\t\t\t\t\t\t\t\t), '[]')\t\t\t\t\t\t\t\t\t\n" +
            "\t\t\t\t\t\t\t\t)\n" +
            "\t\t\t\t\t) FROM subject_permission sp\n" +
            "\t\t\t\t\t\twhere sp.uuid = c.subjectpermissionid\n" +
            "\t\t\t), '[]') as permissions,\n" +
            "\t\tCOALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', l.uuid, 'organizationid', l.organizationid, 'created', l.created, 'updated', l.updated, 'deleted', l.deleted, 'isdeleted', l.isdeleted, \n" +
            "\t\t\t\t\t\t\t\t\t'crudsubjectid', l.crudsubjectid, 'name', l.\"name\", 'version', l.\"version\", 'subjectid', l.subjectid, 'licensedocument', l.licensedocument, 'isactive', l.isactive)\n" +
            "\t\t\t\t\t) from license l\n" +
            "\t\t\t\t\t\twhere c.licenseid = l.uuid\n" +
            "\t\t\t), '[]') as licenses\n" +
            "FROM\ncontract\nc, subject app, subject_membership sm\n" +
            "where c.subjectid = app.uuid\n" +
            "and c.subjectid = sm.subjectgroupid and sm.subjecttypeid = '21371a15-04f8-445e-a899-006ee11c0e09' and sm.subjectgrouptypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6'\n" +
            "and c.apiid = CAST(? AS uuid)";
    private static final Logger LOGGER = LoggerFactory.getLogger(ContractService.class);
    private static final String SQL_INSERT = "insert into contract (organizationid, crudsubjectid, name, description, apiid, subjectid, environment, contractstateid, status, isrestrictedtosubsetofapi, licenseid, subjectpermissionid)\n" +
            "values (CAST(? AS uuid), CAST(? AS uuid), ?, ?, CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS uuid), CAST(? AS e_contract_status), ?, CAST(? AS uuid), CAST(? AS uuid))";
    private static final String SQL_DELETE = "update contract\n" +
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
            "  name,\n" +
            "  description,\n" +
            "  apiid,\n" +
            "  subjectid,\n" +
            "  environment,\n" +
            "  contractstateid,\n" +
            "  status,\n" +
            "  isrestrictedtosubsetofapi,\n" +
            "  licenseid,\n" +
            "  subjectpermissionid\n" +
            "from\n" +
            "contract\n";
    public static final String FILTER_BY_POLICY = SQL_SELECT + SQL_WHERE + "licenseid in (\n" +
            "    select uuid\n" +
            "    from license\n" +
            "    where licensedocument -> 'termsOfService' -> 'policyKey' @> ?::jsonb)";
    private static final String SQL_UPDATE = "UPDATE contract\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , updated               = now()\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , name      = ?\n" +
            "  , description      = ?\n" +
            "  , apiid      = CAST(? AS uuid)\n" +
            "  , subjectid      = CAST(? AS uuid)\n" +
            "  , environment      = ?\n" +
            "  , contractstateid      = CAST(? AS uuid)\n" +
            "  , status      = CAST(? AS e_contract_status)\n" +
            "  , isrestrictedtosubsetofapi      = ?\n" +
            "  , licenseid      = CAST(? AS uuid)\n" +
            "  , subjectpermissionid      = CAST(? AS uuid)\n";
    private static final String SQL_CONDITION_NAME_IS = "lower(name) = lower(?)\n";
    private static final String SQL_CONDITION_NAME_LIKE = "lower(name) like lower(?)\n";
    private static final String SQL_CONDITION_APIID_IS = "apiid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_APIID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APIID_IS;
    private static final String SQL_CONDITION_APPID_IS = "subjectid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_APPID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APPID_IS;
    private static final String SQL_CONDITION_APIS_OF_USERID_IS = "apiid in (select distinct uuid from api where subjectid = CAST(? AS uuid) and isdeleted = false and isproxyapi = true)\n";
    public static final String FILTER_BY_APIS_OF_USERID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APIS_OF_USERID_IS;
    private static final String SQL_CONDITION_APPS_OF_USERID_IS = "subjectid in (select distinct s.uuid from subject s, subject_membership sm where sm.subjectid = CAST(? AS uuid) and sm.subjectgroupid = s.uuid and s.subjecttypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6' and sm.subjectgrouptypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6')\n";
    public static final String FILTER_BY_APPS_OF_USERID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APPS_OF_USERID_IS;
    private static final String SQL_CONDITION_LICENSEID_IS = "licenseid = CAST(? AS uuid)\n";
    public static final String FILTER_BY_LICENSEID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_LICENSEID_IS;
    private static final String SQL_CONDITION_LICENSEID_IN = "licenseid in (select distinct uuid from license where subjectid = CAST(? AS uuid) and isdeleted = false)\n";
    public static final String FILTER_BY_LICENSES_OF_USER = SQL_SELECT + SQL_WHERE + SQL_CONDITION_LICENSEID_IN;
    private static final String SQL_CONDITION_SUBJECTPERMISSIONID_IS = "subjectpermissionid = CAST(? AS uuid)\n";
    static final String FILTER_BY_SUBJECTPERMISSIONID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECTPERMISSIONID_IS;
    private static final String SQL_ORDERBY_NAME = "order by name\n";
    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";
    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;
    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;
    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;
    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;
    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;
    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;
    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    public ContractService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public ContractService(Vertx vertx) {
        super(vertx);
    }

    /**
     * Subscribe APP to API
     *
     * @param routingContext
     * @param insertRecords
     * @return
     */
    public Single<List<JsonObject>> insertAllCascaded(RoutingContext routingContext, JsonArray insertRecords) {
        LOGGER.trace("ContractService --- insertAllCascaded invoked");

        String sessionOrganizationId = (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);
        String sessionUserId = (String) routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        Observable<JsonObject> insertParamsObservable = Observable.fromIterable(insertRecords).map(o -> (JsonObject) o);

        //daisy chaining record status
        return insertParamsObservable

                //SUBJECT PERMISSION FOR SUBSCRIPTION
                .flatMap(insertRequest -> {
                    JsonObject insertRecord = new JsonObject()
                            .put("organizationid", sessionOrganizationId)
                            .put("crudsubjectid", sessionUserId)
                            .put("permission", "Subscription Permission")
                            .put("description", "Subscription Permission of " + insertRequest.getString("contractdescription"))
                            .put("effectivestartdate", insertRequest.containsKey("effectivestartdate") ? insertRequest.getInstant("effectivestartdate") : Instant.now())
                            .put("effectiveenddate", insertRequest.containsKey("effectiveenddate") ? insertRequest.getInstant("effectiveenddate") : Instant.now().plus(365, DAYS)) //TODO: Get from License or subscription
                            .put("subjectid", insertRequest.getString("appid"))
                            .put("resourceid", insertRequest.getString("resourceidofapi"))
                            .put("resourceactionid", Constants.RESOURCE_ACTION_INVOKE_API)
                            .put("accessmanagerid", Constants.DEFAULT_ACCESS_MANAGER_UUID)
                            .put("isactive", Boolean.TRUE);

                    SubjectPermissionService subjectPermissionService = new SubjectPermissionService(routingContext.vertx());
                    return subjectPermissionService.initJDBCClient(sessionOrganizationId)
                            .flatMap(jdbcClient -> subjectPermissionService.insert(insertRecord, insertRequest)).toObservable();
                })

                //CONTRACT
                .flatMap(recordStatus -> {
                    //Convert recordStatus to insertRecord Json Object
                    if (recordStatus.getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                        //JsonObject permissionInsertResult = recordStatus.getJsonObject(STR_RESPONSE);
                        JsonObject insertRequest = recordStatus.getJsonObject("parentRecordStatus");

                        JsonObject insertRecord = new JsonObject()
                                .put("organizationid", sessionOrganizationId)
                                .put("crudsubjectid", sessionUserId)
                                .put("name", insertRequest.getString("contractname"))
                                .put("description", insertRequest.getString("contractdescription"))
                                .put("apiid", insertRequest.getString("apiid"))
                                .put("subjectid", insertRequest.getString("appid"))
                                .put("environment", insertRequest.getString("environment"))
                                .put("contractstateid", Constants.CONTRACT_STATE_IS_ACTIVATED)
                                .put(STR_STATUS, Constants.CONTRACT_STATUS_IS_INFORCE)
                                .put("isrestrictedtosubsetofapi", Boolean.FALSE)
                                .put("licenseid", insertRequest.getString("licenseid"))
                                .put("subjectpermissionid", recordStatus.getString(STR_UUID));

                        return insert(insertRecord, recordStatus).toObservable(); //insert contract
                    } else {
                        return Observable.just(recordStatus);
                    }
                })

                //RESOURCE OF CONTRACT
                .flatMap(recordStatus -> {
                    //Convert recordStatus to insertRecord Json Object
                    if (recordStatus.getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                        JsonObject contractInsertResult = recordStatus.getJsonObject(STR_RESPONSE);
                        JsonObject insertRecord = new JsonObject()
                                .put("organizationid", sessionOrganizationId)
                                .put("crudsubjectid", sessionUserId)
                                .put("resourcetypeid", Constants.RESOURCE_TYPE_CONTRACT)
                                .put("resourcename", contractInsertResult.getString("name") + " CONTRACT RESOURCE " + contractInsertResult.getString(STR_UUID))
                                .put("description", contractInsertResult.getString("description") + " CONTRACT RESOURCE")
                                .put("resourcerefid", contractInsertResult.getString(STR_UUID))
                                .put("isactive", Boolean.TRUE);

                        ResourceService resourceService = new ResourceService(routingContext.vertx());
                        return resourceService.initJDBCClient(sessionOrganizationId)
                                .flatMap(jdbcClient -> resourceService.insert(insertRecord, recordStatus)).toObservable();
                    } else {
                        return Observable.just(recordStatus);
                    }
                })

                //RESOURCE ACCESS TOKEN
                .flatMap(recordStatus -> {
                    //Convert recordStatus to insertRecord Json Object
                    if (recordStatus.getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                        String subjectPermissionId = recordStatus
                                .getJsonObject("parentRecordStatus")
                                .getJsonObject("parentRecordStatus")
                                .getString(STR_UUID);

                        String apiId = recordStatus.getJsonObject("parentRecordStatus")
                                .getJsonObject(STR_RESPONSE).getString("apiid");

                        JsonObject insertRecord = new JsonObject()
                                .put("organizationid", sessionOrganizationId)
                                .put("crudsubjectid", sessionUserId)
                                .put("subjectpermissionid", subjectPermissionId)
                                .put("resourcetypeid", Constants.RESOURCE_TYPE_API_PROXY)
                                .put("resourcerefid", apiId)
                                .put("isactive", Boolean.TRUE);

                        ResourceAccessTokenService resourceAccessTokenService = new ResourceAccessTokenService(routingContext.vertx());
                        return resourceAccessTokenService.initJDBCClient(sessionOrganizationId)
                                .flatMap(jdbcClient -> resourceAccessTokenService.insert(insertRecord, recordStatus)).toObservable();
                    } else {
                        return Observable.just(recordStatus);
                    }
                })

                //PREPARE RESULT
                .flatMap(recordStatus -> {
                    if (recordStatus.getInteger(STR_STATUS) == HttpResponseStatus.CREATED.code()) {
                        JsonObject accessTokenInsertResult = recordStatus.getJsonObject(STR_RESPONSE);
                        JsonObject resourceRecordStatus = recordStatus.getJsonObject("parentRecordStatus");
                        JsonObject contractRecordStatus = resourceRecordStatus.getJsonObject("parentRecordStatus");
                        JsonObject permissionRecordStatus = contractRecordStatus.getJsonObject("parentRecordStatus");

                        permissionRecordStatus.getJsonObject(STR_RESPONSE).put("accesstokens", accessTokenInsertResult);
                        resourceRecordStatus.getJsonObject(STR_RESPONSE).put("permissions", permissionRecordStatus.getJsonObject(STR_RESPONSE));
                        contractRecordStatus.getJsonObject(STR_RESPONSE).put("resources", resourceRecordStatus.getJsonObject(STR_RESPONSE));
                        contractRecordStatus.remove("parentRecordStatus");

                        return Observable.just(contractRecordStatus);
                    } else {
                        return Observable.just(recordStatus);
                    }

                }).toList();
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
                .add(insertRecord.getString("name"))
                .add(insertRecord.getString("description"))
                .add(insertRecord.getString("apiid"))
                .add(insertRecord.getString("subjectid"))
                .add(insertRecord.getString("environment"))
                .add(insertRecord.getString("contractstateid"))
                .add(insertRecord.getString(STR_STATUS))
                .add(insertRecord.getBoolean("isrestrictedtosubsetofapi"))
                .add(insertRecord.getString("licenseid"))
                .add(insertRecord.getString("subjectpermissionid"));
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
        JsonArray updateParams = prepareInsertParameters(updateRecord)
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
                    JsonArray updateParam = prepareInsertParameters(jsonObj)
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

    public Single<ResultSet> deleteCascaded(RoutingContext routingContext) {
        LOGGER.trace("ContractService --- deleteCascaded invoked");

        String sessionOrganizationId = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);

        ApiFilterQuery subscriptionEntitiesQuery = new ApiFilterQuery()
                .setFilterQuery(ContractService.SQL_UNSUBSCRIBE)
                .setFilterQueryParams(new JsonArray().add(routingContext.pathParam(STR_UUID)));

        ResultSet subscriptionEntitiesResult = new ResultSet();
        ResourceAccessTokenService resourceAccessTokenService = new ResourceAccessTokenService(routingContext.vertx());
        SubjectPermissionService subjectPermissionService = new SubjectPermissionService(routingContext.vertx());
        ResourceService resourceService = new ResourceService(routingContext.vertx());

        return findAll(subscriptionEntitiesQuery)
            .flatMap(resultSet -> {
                if (resultSet.getNumRows() > 0) {
                    LOGGER.trace("Subscription Entities Uuidies:{}",resultSet.getRows().get(0).encodePrettily());
                    subscriptionEntitiesResult.setResults(resultSet.getResults());
                    subscriptionEntitiesResult.setColumnNames(resultSet.getColumnNames());

                    return resourceAccessTokenService.initJDBCClient(sessionOrganizationId);
                } else {
                    return Single.error(new NotFound404Exception(NO_DATA_FOUND));
                }
            })
            .flatMap(jdbcClient -> {
                LOGGER.trace("subscriptionEntitiesResult:{}",subscriptionEntitiesResult.getRows().get(0).encodePrettily());
                JsonArray resourceAccessTokensArray = new JsonArray(subscriptionEntitiesResult.getRows().get(0).getString("resource_access_token_uuid"));
                LOGGER.trace("resourceAccessTokens:{}", resourceAccessTokensArray.encodePrettily());
                return Observable.fromIterable(resourceAccessTokensArray)
                    .map(o -> (String) o)
                    .flatMap(s -> resourceAccessTokenService.delete(UUID.fromString(s)).toObservable())
                    .flatMap(compositeResult -> {
                        if (compositeResult.getThrowable() == null) {
                            LOGGER.trace("Deleted Resource Access Token: {}", compositeResult.getUpdateResult().getKeys().getString(1));
                        } else {
                            LOGGER.trace("Error during deletion of Resource Access Token: Error:{}\n{}",
                                    compositeResult.getThrowable().getLocalizedMessage(),
                                    compositeResult.getThrowable().getStackTrace());
                        }
                        return Observable.just(compositeResult);
                    }).toList();
            })
            .flatMap(objects -> {
                LOGGER.trace("# of processed Resource Access Tokens:{}", objects.size());
                //TODO: Print Update results and error codes.

                return subjectPermissionService.initJDBCClient(sessionOrganizationId);
            })
            .flatMap(jdbcClient -> subjectPermissionService.delete(UUID.fromString(subscriptionEntitiesResult.getRows().get(0).getString("subject_permission_uuid")))
            )
            .flatMap(compositeResult -> {
                if (compositeResult.getThrowable() == null) {
                    LOGGER.trace("Deleted Subject Permission: {}", compositeResult.getUpdateResult().getKeys().getString(1));
                } else {
                    LOGGER.trace("Error during deletion of Subject Permission: Error:{}\n{}",
                            compositeResult.getThrowable().getLocalizedMessage(),
                            compositeResult.getThrowable().getStackTrace());
                }
                return resourceService.initJDBCClient(sessionOrganizationId);
            })
            .flatMap(jdbcClient -> {
                if (subscriptionEntitiesResult.getRows().get(0).getString("resource_uuid") != null) {
                    return resourceService.delete(UUID.fromString(subscriptionEntitiesResult.getRows().get(0).getString("resource_uuid")));
                } else {
                    return Single.just(new CompositeResult(new NotFound404Exception(NO_DATA_FOUND)));
                }
            })
            .flatMap(compositeResult -> {
                if (compositeResult.getThrowable() == null) {
                    LOGGER.trace("Deleted Resource: {}", compositeResult.getUpdateResult().getKeys().getString(1));
                } else {
                    LOGGER.trace("Error during deletion of Resource: Error:{}\n{}",
                            compositeResult.getThrowable().getLocalizedMessage(),
                            compositeResult.getThrowable().getStackTrace());
                }
                return this.initJDBCClient(sessionOrganizationId);
            })
            .flatMap(jdbcClient -> this.delete(UUID.fromString(subscriptionEntitiesResult.getRows().get(0).getString("uuid")))
            )
            .flatMap(compositeResult -> this.findById(UUID.fromString(subscriptionEntitiesResult.getRows().get(0).getString("uuid")))
            )
            .flatMap((ResultSet resultSet) -> {
                if (resultSet.getNumRows() == 0) {
                    return Single.error(new NotFound404Exception(NO_DATA_FOUND));
                } else {
                    return Single.just(resultSet);
                }
            });
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
