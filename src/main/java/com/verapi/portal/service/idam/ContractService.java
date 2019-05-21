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
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static java.time.temporal.ChronoUnit.DAYS;

public class ContractService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    public ContractService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public ContractService(Vertx vertx) {
        super(vertx);
    }


    /** Subscribe APP to API
     *
     * @param routingContext
     * @param insertRecords
     * @return
     */
    public Single<List<JsonObject>> insertAllCascaded(RoutingContext routingContext, JsonArray insertRecords) {
        logger.trace("ContractService --- insertAllCascaded invoked");

        String sessionOrganizationId = (String)routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);
        String sessionUserId = (String)routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

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
                    if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                        //JsonObject permissionInsertResult = recordStatus.getJsonObject("response");
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
                                .put("status", Constants.CONTRACT_STATUS_IS_INFORCE)
                                .put("isrestrictedtosubsetofapi", Boolean.FALSE)
                                .put("licenseid", insertRequest.getString("licenseid"))
                                .put("subjectpermissionid", recordStatus.getString("uuid"));

                        return insert(insertRecord, recordStatus).toObservable(); //insert contract
                    } else {
                        return Observable.just(recordStatus);
                    }
                })

                //RESOURCE OF CONTRACT
                .flatMap(recordStatus -> {
                    //Convert recordStatus to insertRecord Json Object
                    if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                        JsonObject contractInsertResult = recordStatus.getJsonObject("response");
                        JsonObject insertRecord = new JsonObject()
                                .put("organizationid", sessionOrganizationId)
                                .put("crudsubjectid", sessionUserId)
                                .put("resourcetypeid", Constants.RESOURCE_TYPE_CONTRACT)
                                .put("resourcename", contractInsertResult.getString("name") + " CONTRACT RESOURCE " + contractInsertResult.getString("uuid"))
                                .put("description", contractInsertResult.getString("description") + " CONTRACT RESOURCE")
                                .put("resourcerefid", contractInsertResult.getString("uuid"))
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
                    if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                        String subjectPermissionId = recordStatus
                                .getJsonObject("parentRecordStatus")
                                .getJsonObject("parentRecordStatus")
                                .getString("uuid");

                        String apiId = recordStatus.getJsonObject("parentRecordStatus")
                                .getJsonObject("response").getString("apiid");

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
                    if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                        JsonObject accessTokenInsertResult = recordStatus.getJsonObject("response");
                        JsonObject resourceRecordStatus = recordStatus.getJsonObject("parentRecordStatus");
                        JsonObject contractRecordStatus = resourceRecordStatus.getJsonObject("parentRecordStatus");
                        JsonObject permissionRecordStatus = contractRecordStatus.getJsonObject("parentRecordStatus");

                        permissionRecordStatus.getJsonObject("response").put("accesstokens", accessTokenInsertResult);
                        resourceRecordStatus.getJsonObject("response").put("permissions", permissionRecordStatus.getJsonObject("response"));
                        contractRecordStatus.getJsonObject("response").put("resources", resourceRecordStatus.getJsonObject("response"));
                        contractRecordStatus.remove("parentRecordStatus");

                        return Observable.just(contractRecordStatus);
                    } else {
                        return Observable.just(recordStatus);
                    }

                }).toList();
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
                .add(insertRecord.getString("name"))
                .add(insertRecord.getString("description"))
                .add(insertRecord.getString("apiid"))
                .add(insertRecord.getString("subjectid"))
                .add(insertRecord.getString("environment"))
                .add(insertRecord.getString("contractstateid"))
                .add(insertRecord.getString("status"))
                .add(insertRecord.getBoolean("isrestrictedtosubsetofapi"))
                .add(insertRecord.getString("licenseid"))
                .add(insertRecord.getString("subjectpermissionid"));
    }

    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        logger.trace("---insertAll invoked");
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
        JsonArray updateParams = prepareInsertParameters(updateRecord)
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

    private static final String SQL_CONDITION_APPID_IS = "subjectid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_APIS_OF_USERID_IS = "apiid in (select distinct uuid from api where subjectid = CAST(? AS uuid) and isdeleted = false and isproxyapi = true)\n";

    private static final String SQL_CONDITION_APPS_OF_USERID_IS = "subjectid in (select distinct s.uuid from subject s, subject_membership sm where sm.subjectid = CAST(? AS uuid) and sm.subjectgroupid = s.uuid and s.subjecttypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6' and sm.subjectgrouptypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6')\n";

    private static final String SQL_CONDITION_LICENSEID_IS = "licenseid = CAST(? AS uuid)\n";

    private static final String SQL_CONDITION_LICENSEID_IN = "licenseid in (select distinct uuid from license where subjectid = CAST(? AS uuid) and isdeleted = false)\n";

    private static final String SQL_CONDITION_SUBJECTPERMISSIONID_IS = "subjectpermissionid = CAST(? AS uuid)\n";

    private static final String SQL_ORDERBY_NAME = "order by name\n";

    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;

    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;

    public static final String FILTER_BY_APIID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APIID_IS;

    public static final String FILTER_BY_APPID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APPID_IS;

    public static final String FILTER_BY_APIS_OF_USERID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APIS_OF_USERID_IS;

    public static final String FILTER_BY_APPS_OF_USERID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_APPS_OF_USERID_IS;

    static final String FILTER_BY_SUBJECTPERMISSIONID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_SUBJECTPERMISSIONID_IS;

    public static final String FILTER_BY_LICENSEID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_LICENSEID_IS;

    public static final String FILTER_BY_LICENSES_OF_USER = SQL_SELECT + SQL_WHERE + SQL_CONDITION_LICENSEID_IN;

    public static final String FILTER_BY_POLICY = SQL_SELECT + SQL_WHERE + "licenseid in (\n" +
            "    select uuid\n" +
            "    from license\n" +
            "    where licensedocument -> 'termsOfService' -> 'policyKey' @> ?::jsonb)";

    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

}
