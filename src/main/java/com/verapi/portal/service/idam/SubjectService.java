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
import com.verapi.abyss.exception.Forbidden403Exception;
import com.verapi.abyss.exception.NoDataFoundException;
import com.verapi.abyss.exception.NotFound404Exception;
import com.verapi.abyss.exception.UnAuthorized401Exception;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Config;
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
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.auth.jdbc.JDBCAuth;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static com.verapi.portal.common.Util.encodeFileToBase64Binary;
import static java.time.temporal.ChronoUnit.DAYS;

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



    //TODO: Process Bulk Insert
    public Single<List<JsonObject>> insertAllCascaded(RoutingContext routingContext, JsonArray insertRecords) {
        logger.trace("SubjectService --- insertAllCascaded invoked");

        String sessionOrganizationId = (String)routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);
        String sessionUserId = (String)routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        Observable<JsonObject> insertParamsObservable = Observable.fromIterable(insertRecords).map(o -> (JsonObject) o);

        //daisy chaining record status
        return insertParamsObservable
            .flatMap(insertRecord -> insert(insertRecord, null).toObservable()) //insert app
            .flatMap(recordStatus -> {
                //Convert recordStatus to insertRecord Json Object
                if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                    JsonObject appInsertResult = recordStatus.getJsonObject("response");
                    JsonObject insertRecord = new JsonObject()
                            .put("organizationid", sessionOrganizationId)
                            .put("crudsubjectid", sessionUserId)
                            .put("resourcetypeid", Constants.RESOURCE_TYPE_APP)
                            .put("resourcename", appInsertResult.getString("displayname") + " APP")
                            .put("description", appInsertResult.getString("description"))
                            .put("resourcerefid", appInsertResult.getString("uuid"))
                            .put("isactive", true);

                    ResourceService resourceService = new ResourceService(routingContext.vertx());
                    return resourceService.initJDBCClient(sessionOrganizationId)
                            .flatMap(jdbcClient -> resourceService.insert(insertRecord, recordStatus)).toObservable();
                } else {
                    return Observable.just(recordStatus);
                }

            })
            .flatMap(recordStatus -> {
                //Convert recordStatus to insertRecord Json Object
                if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                    JsonObject resourceInsertResult = recordStatus.getJsonObject("response");
                    String description = "Ownership of " + resourceInsertResult.getString("resourcename") + " by " + routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_DISPLAY_NAME_SESSION_VARIABLE_NAME);
                    JsonObject insertRecord = new JsonObject()
                            .put("organizationid", sessionOrganizationId)
                            .put("crudsubjectid", sessionUserId)
                            .put("permission", "Ownership permission")
                            .put("description", description)
                            .put("effectivestartdate", Instant.now())
                            .put("effectiveenddate", Instant.now().plus(180, DAYS)) //TODO: Null mı bıraksak?
                            .put("subjectid", sessionUserId)
                            .put("resourceid", resourceInsertResult.getString("uuid"))
                            .put("resourceactionid", Constants.RESOURCE_ACTION_ALL_APP_ACTION)
                            .put("accessmanagerid", Constants.DEFAULT_ACCESS_MANAGER_UUID)
                            .put("isactive", true);

                    SubjectPermissionService subjectPermissionService = new SubjectPermissionService(routingContext.vertx());
                    return subjectPermissionService.initJDBCClient(sessionOrganizationId)
                            .flatMap(jdbcClient -> subjectPermissionService.insert(insertRecord, recordStatus)).toObservable();
                } else {
                    return Observable.just(recordStatus);
                }

            })
            .flatMap(recordStatus -> {
                //Convert recordStatus to insertRecord Json Object
                if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                    JsonObject permissionInsertResult = recordStatus.getJsonObject("response");

                    String appUuid = recordStatus.getJsonObject("parentRecordStatus")
                            .getJsonObject("parentRecordStatus").getString("uuid");

                    JsonObject insertRecord = new JsonObject()
                            .put("organizationid", sessionOrganizationId)
                            .put("crudsubjectid", sessionUserId)
                            .put("subjectpermissionid", permissionInsertResult.getString("uuid"))
                            .put("resourcetypeid", Constants.RESOURCE_TYPE_APP)
                            .put("resourcerefid", appUuid)
                            .put("isactive", true);

                    ResourceAccessTokenService resourceAccessTokenService = new ResourceAccessTokenService(routingContext.vertx());
                    return resourceAccessTokenService.initJDBCClient(sessionOrganizationId)
                            .flatMap(jdbcClient -> resourceAccessTokenService.insert(insertRecord, recordStatus)).toObservable();
                } else {
                    return Observable.just(recordStatus);
                }
            })
            .flatMap(recordStatus -> {
                //Convert recordStatus to insertRecord Json Object
                if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                    //JsonObject resourceAccessTokenInsertResult = recordStatus.getJsonObject("response");

                    JsonObject appRecord = recordStatus.getJsonObject("parentRecordStatus")
                            .getJsonObject("parentRecordStatus")
                            .getJsonObject("parentRecordStatus")
                            .getJsonObject("response");

                    JsonObject insertRecord = new JsonObject()
                            .put("organizationid", sessionOrganizationId)
                            .put("crudsubjectid", sessionUserId)
                            .put("subjectid", sessionUserId)
                            .put("subjectgroupid", appRecord.getString("uuid"))
                            .put("subjectdirectoryid", appRecord.getString("subjectdirectoryid"))
                            .put("subjecttypeid", Constants.SUBJECT_TYPE_USER)
                            .put("subjectgrouptypeid", Constants.SUBJECT_TYPE_APP)
                            .put("isactive", true);

                    SubjectMembershipService subjectMembershipService = new SubjectMembershipService(routingContext.vertx());
                    return  subjectMembershipService.initJDBCClient(sessionOrganizationId)
                            .flatMap(jdbcClient -> subjectMembershipService.insert(insertRecord, recordStatus)).toObservable();
                } else {
                    return Observable.just(recordStatus);
                }
            })
                .flatMap(recordStatus -> {
                    if (recordStatus.getInteger("status") == HttpResponseStatus.CREATED.code()) {
                        //JsonObject subjectMembershipInsertResult = recordStatus.getJsonObject("response");

                        JsonObject resourceAccessTokenRecordStatus = recordStatus.getJsonObject("parentRecordStatus");
                        JsonObject resourceAccessTokenInsertResult = resourceAccessTokenRecordStatus.getJsonObject("response");

                        JsonObject subjectPermissionRecordStatus = resourceAccessTokenRecordStatus.getJsonObject("parentRecordStatus");
                        JsonObject subjectPermissionInsertResult = subjectPermissionRecordStatus.getJsonObject("response")
                                .put("tokens", resourceAccessTokenInsertResult);

                        JsonObject resourceRecordStatus = subjectPermissionRecordStatus.getJsonObject("parentRecordStatus");
                        JsonObject resourceInsertResult = resourceRecordStatus.getJsonObject("response")
                                .put("permissions", subjectPermissionInsertResult);

                        JsonObject appRecordStatus = resourceRecordStatus.getJsonObject("parentRecordStatus");
                        appRecordStatus.getJsonObject("response").put("resources", resourceInsertResult);
                        appRecordStatus.getJsonObject("response").put("contracts", new JsonArray()); //Empty contracts

                        return Observable.just(appRecordStatus);
                    } else {
                        return Observable.just(recordStatus);
                    }

                }).toList();
    }

    @Override
    protected String getInsertSql() { return SQL_INSERT; }

    @Override
    protected String getFindByIdSql() { return SQL_FIND_BY_ID; }

    protected JsonArray prepareInsertParameters(JsonObject insertRecord) {
        return new JsonArray()
                .add(insertRecord.getString("organizationid"))
                .add(insertRecord.getString("crudsubjectid")) //TODO: Make it readOnly in Yaml
                .add(insertRecord.containsKey("isactivated") ? insertRecord.getBoolean("isactivated") : false)
                .add(insertRecord.getString("subjecttypeid"))
                .add(insertRecord.getString("subjectname"))
                .add(insertRecord.getString("firstname"))
                .add(insertRecord.getString("lastname"))
                .add(insertRecord.getString("displayname"))
                .add(insertRecord.getString("email"))
                .add(insertRecord.containsKey("secondaryemail") ? insertRecord.getString("secondaryemail") : "")
                .add(insertRecord.getInstant("effectivestartdate"))
                .add(insertRecord.containsKey("effectiveenddate") ? insertRecord.getInstant("effectiveenddate") : Instant.now().plus(90, DAYS))
                .add(insertRecord.getString("password"))
                .add(insertRecord.getString("passwordsalt"))
                .add(insertRecord.getValue("picture"))
                .add(insertRecord.getString("subjectdirectoryid"))
                .add(insertRecord.getBoolean("islocked"))
                .add(insertRecord.getBoolean("issandbox"))
                .add(insertRecord.getString("url"))
                .add(insertRecord.containsKey("isrestrictedtoprocessing") ? insertRecord.getBoolean("isrestrictedtoprocessing") : false)
                .add(insertRecord.containsKey("description") ? insertRecord.getString("description") : "")
                .add(insertRecord.containsKey("distinguishedname") ? insertRecord.getString("distinguishedname") : "")
                .add(insertRecord.containsKey("uniqueid") ? insertRecord.getString("uniqueid") : "")
                .add(insertRecord.containsKey("phonebusiness") ? insertRecord.getString("phonebusiness") : "")
                .add(insertRecord.containsKey("phonehome") ? insertRecord.getString("phonehome") : "")
                .add(insertRecord.containsKey("phonemobile") ? insertRecord.getString("phonemobile") : "")
                .add(insertRecord.containsKey("phoneextension") ? insertRecord.getString("phoneextension") : "")
                .add(insertRecord.containsKey("jobtitle") ? insertRecord.getString("jobtitle") : "")
                .add(insertRecord.containsKey("department") ? insertRecord.getString("department") : "")
                .add(insertRecord.containsKey("company") ? insertRecord.getString("company") : "");
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
                .flatMap(result -> Observable.just(evaluateCompositeResultAndReturnRecordStatus(result))
                )
                .toList();
    }

    protected JsonArray prepareUpdateParameters(JsonObject updateRecord) {
        return new JsonArray()
                .add(updateRecord.getString("organizationid"))
                .add(updateRecord.getString("crudsubjectid"))
                .add(updateRecord.containsKey("isactivated") ? updateRecord.getBoolean("isactivated") : false)
                .add(updateRecord.getString("subjecttypeid"))
                .add(updateRecord.getString("subjectname"))
                .add(updateRecord.getString("firstname"))
                .add(updateRecord.getString("lastname"))
                .add(updateRecord.getString("displayname"))
                .add(updateRecord.getString("email"))
                .add(updateRecord.containsKey("secondaryemail") ? updateRecord.getString("secondaryemail") : "")
                .add(updateRecord.getInstant("effectivestartdate"))
                .add(updateRecord.containsKey("effectiveenddate") ? updateRecord.getInstant("effectiveenddate") : Instant.now().plus(Config.getInstance().getConfigJsonObject().getInteger(Constants.PASSWORD_EXPIRATION_DAYS), DAYS))
/*
                .add(updateRecord.getString("password"))
                .add(updateRecord.getString("passwordsalt"))
*/
                .add(updateRecord.getValue("picture"))
                .add(updateRecord.getString("subjectdirectoryid"))
                .add(updateRecord.getBoolean("islocked"))
                .add(updateRecord.getBoolean("issandbox"))
                .add(updateRecord.getString("url"))
                .add(updateRecord.containsKey("isrestrictedtoprocessing") ? updateRecord.getBoolean("isrestrictedtoprocessing") : false)
                .add(updateRecord.containsKey("description") ? updateRecord.getString("description") : "")
                .add(updateRecord.containsKey("distinguishedname") ? updateRecord.getString("distinguishedname") : "")
                .add(updateRecord.containsKey("uniqueid") ? updateRecord.getString("uniqueid") : "")
                .add(updateRecord.containsKey("phonebusiness") ? updateRecord.getString("phonebusiness") : "")
                .add(updateRecord.containsKey("phonehome") ? updateRecord.getString("phonehome") : "")
                .add(updateRecord.containsKey("phonemobile") ? updateRecord.getString("phonemobile") : "")
                .add(updateRecord.containsKey("phoneextension") ? updateRecord.getString("phoneextension") : "")
                .add(updateRecord.containsKey("jobtitle") ? updateRecord.getString("jobtitle") : "")
                .add(updateRecord.containsKey("department") ? updateRecord.getString("department") : "")
                .add(updateRecord.containsKey("company") ? updateRecord.getString("company") : "");
    }

    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        if ((!updateRecord.containsKey("picture")) || (updateRecord.getValue("picture") == null)) {
            try {
                logger.trace("Updating Subject with default avatar");
                //update default avatar image TODO: later use picture using request message
                ClassLoader classLoader = getClass().getClassLoader();
                File file = new File(Objects.requireNonNull(classLoader.getResource(Constants.RESOURCE_DEFAULT_SUBJECT_AVATAR)).getFile());
                updateRecord.put("picture", encodeFileToBase64Binary(file));
            } catch (IOException e) {
                logger.error(e.getLocalizedMessage());
                logger.error(Arrays.toString(e.getStackTrace()));
            }
        }
        JsonArray updateParams = prepareUpdateParameters(updateRecord)
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
                            File file = new File(Objects.requireNonNull(classLoader.getResource(Constants.RESOURCE_DEFAULT_SUBJECT_AVATAR)).getFile());
                            jsonObj.put("picture", encodeFileToBase64Binary(file));
                        } catch (IOException e) {
                            logger.error(e.getLocalizedMessage());
                            logger.error(Arrays.toString(e.getStackTrace()));
                        }
                    }
                    JsonArray updateParam = prepareUpdateParameters(jsonObj)
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

    public Single<ResultSet> changePassword(RoutingContext routingContext, JDBCAuth jdbcAuth) {

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject requestBody = requestParameters.body().getJsonObject();

        UUID subjectUUID = UUID.fromString(routingContext.pathParam("uuid"));
        String oldPassword = requestBody.getString("oldpassword");
        String newPassword = requestBody.getString("newpassword");
        String confirmPassword = requestBody.getString("confirmpassword");
        String crudSubjectId = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        if (oldPassword == null || oldPassword.isEmpty()) {
            logger.error("oldPassword is null or empty.!");
            return Single.error(new UnAuthorized401Exception("Please enter Old Password field"));
        }

        if (newPassword == null || newPassword.isEmpty()) {
            logger.error("newPassword is null or empty");
            return Single.error(new UnAuthorized401Exception("Please enter New Password field"));
        }

        if (confirmPassword == null || confirmPassword.isEmpty()) {
            logger.warn("confirmPassword is null or empty");
            return Single.error(new UnAuthorized401Exception("Please enter Confirm Password field"));
        }

        if (!(newPassword.equals(confirmPassword))) {
            logger.warn("newPassword and confirmPassword does not match");
            return Single.error(new UnAuthorized401Exception("New Password and Confirm Password does not match"));
        }

        return findById(subjectUUID)
                .flatMap(findByIdResultSet -> {
                    if (findByIdResultSet.getNumRows() == 0)
                        return Single.error(new NoDataFoundException("The specified subject does not exist"));
                    else
                        if (findByIdResultSet.getRows().get(0).getString("organizationid").equals(organizationUuid)) {
                            return Single.just(findByIdResultSet);
                        } else {
                            return Single.error(new Forbidden403Exception("Organization incorrect. Please use your organization."));
                        }

                })
                .flatMap(resultSet -> {
                    return jdbcAuth.rxAuthenticate(new JsonObject()
                            .put("username", resultSet.getRows().get(0).getString("subjectname"))
                            .put("password", oldPassword));
                })
                .flatMap(user -> {
                    String salt = jdbcAuth.generateSalt();
                    return update(new JsonArray()
                                    .add(crudSubjectId)
                                    .add(jdbcAuth.computeHash(newPassword, salt))
                                    .add(salt)
                                    .add(Config.getInstance().getConfigJsonObject().getInteger(Constants.PASSWORD_EXPIRATION_DAYS))
                                    .add(subjectUUID.toString())
                            , SQL_CHANGE_PASSWORD_BY_UUID);
                })
                .flatMap(updateResult -> findById(subjectUUID));
    }


    public Single<CompositeResult> updateSubjectOrganization(JsonArray jsonArray) {
        return update(jsonArray, SubjectService.SQL_CHANGE_ORGANIZATION_BY_UUID);
    }

    public Single<CompositeResult> updateSubjectActivated(JsonArray jsonArray) {
        return update(jsonArray, SubjectService.SQL_CHANGE_ACTIVATED_BY_UUID);
    }

    public ApiFilterQuery.APIFilter getAPIFilter() {
        return apiFilter;
    }

    private static final String SQL_INSERT = "insert into subject (organizationid, crudsubjectid, isactivated, subjecttypeid, subjectname, firstname, lastname, displayname, email,\n" +
            "                     secondaryemail, effectivestartdate, effectiveenddate, password, passwordsalt, picture,\n" +
            "                     subjectdirectoryid, islocked, issandbox, url, isrestrictedtoprocessing, description,\n" +
            "                     distinguishedname, uniqueid, phonebusiness, phonehome, phonemobile, phoneextension,\n" +
            "                     jobtitle, department, company)\n" +
            "values\n" +
            "  (CAST(? AS uuid), CAST(? AS uuid), ?, CAST(? AS uuid), ?, ?, ?, ?, ?,\n" +
            "                    ?, coalesce(?, now()), ?, ?, ?, ?,\n" +
            "   CAST(? AS uuid), ?, ?, ?, ?, ?,\n" +
            "   ?, ?, ?, ?, ?, ?,\n" +
            "   ?, ?, ?)";


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
            "  subjectdirectoryid,\n" +
            "  islocked,\n" +
            "  issandbox,\n" +
            "  url,\n" +
            "  isrestrictedtoprocessing,\n" +
            "  description,\n" +
            "  distinguishedname,\n" +
            "  uniqueid,\n" +
            "  phonehome,\n" +
            "  phonebusiness,\n" +
            "  phonemobile,\n" +
            "  phoneextension\n" +
            "  jobtitle,\n" +
            "  department,\n" +
            "  company\n" +
            "from\n" +
            "subject\n";

    private static final String SQL_UPDATE = "UPDATE subject\n" +
            "SET\n" +
            "  organizationid      = CAST(? AS uuid)\n" +
            "  , crudsubjectid      = CAST(? AS uuid)\n" +
            "  , isactivated      = ?\n" +
            "  , updated               = now()\n" +
            "  , subjecttypeid      = CAST(? AS uuid)\n" +
            "  , subjectname       = ?\n" +
            "  , firstname            = ?\n" +
            "  , lastname            = ?\n" +
            "  , displayname      = ?\n" +
            "  , email                 = ?\n" +
            "  , secondaryemail    = ?\n" +
            "  , effectivestartdate = ?\n" +
            "  , effectiveenddate  = ?\n" +
            "  , picture             = ?\n" +
            "  , subjectdirectoryid = CAST(? AS uuid)\n" +
            "  , islocked = ?\n" +
            "  , issandbox = ?\n" +
            "  , url = ?\n" +
            "  , isrestrictedtoprocessing = ?\n" +
            "  , description = ?\n" +
            "  , distinguishedname = ?\n" +
            "  , uniqueid = ?\n" +
            "  , phonehome = ?\n" +
            "  , phonebusiness = ?\n" +
            "  , phonemobile = ?\n" +
            "  , phoneextension = ?\n" +
            "  , jobtitle = ?\n" +
            "  , department = ?\n" +
            "  , company = ?\n";

    private static final String SQL_CHANGE_PASSWORD = "update subject\n" +
            "set updated              = now()\n" +
            "  , crudsubjectid        = CAST(? AS uuid)\n" +
            "  , password             = ?\n" +
            "  , passwordsalt         = ?\n" +
            "  , passwordexpiresat    =  now() + ? * interval '1 DAY'\n" +
            "  , lastpasswordchangeat = now()\n";

    private static final String SQL_CHANGE_ACTIVATED = "update subject\n" +
            "set updated              = now()\n" +
            "  , crudsubjectid        = CAST(? AS uuid)\n" +
            "  , isactivated             = ?\n";

    private static final String SQL_CHANGE_ORGANIZATION = "update subject\n" +
            "set updated              = now()\n" +
            "  , organizationid       = CAST(? AS uuid)\n" +
            "  , crudsubjectid        = CAST(? AS uuid)\n";


    private static final String SQL_CONDITION_NAME_IS = "lower(subjectname) = lower(?)\n";

    private static final String SQL_CONDITION_NAME_LIKE = "lower(subjectname) like lower(?)\n";

    public static final String SQL_CONDITION_IS_USER = "subjecttypeid=CAST('" + Constants.SUBJECT_TYPE_USER + "' AS uuid)\n";

    public static final String SQL_CONDITION_IS_GROUP = "subjecttypeid=CAST('" + Constants.SUBJECT_TYPE_GROUP + "' AS uuid)\n";

    public static final String SQL_CONDITION_IS_ROLE = "subjecttypeid=CAST('" + Constants.SUBJECT_TYPE_ROLE + "' AS uuid)\n";

    public static final String SQL_CONDITION_DIRECTORY = "subjectdirectoryid=CAST(? AS uuid)\n";

    public static final String SQL_CONDITION_IS_NOT_SYSTEM = "subjecttypeid!=CAST('" + Constants.SUBJECT_TYPE_SYSTEM + "' AS uuid)\n";

    private static final String SQL_ORDERBY_NAME = "order by subjectname\n";

    private static final String SQL_CONDITION_ONLY_NOTDELETED = "isdeleted=false\n";

    public static final String SQL_CONDITION_IS_APP = "subjecttypeid=CAST('" + Constants.SUBJECT_TYPE_APP + "' AS uuid)\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_ID_IS;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS;

    public static final String SQL_FIND_BY_NAME_ONLY_NOTDELETED = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_IS + SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_FIND_LIKE_NAME = SQL_SELECT + SQL_WHERE + SQL_CONDITION_NAME_LIKE;

    private static final String SQL_DELETE_ALL = SQL_DELETE + SQL_WHERE + SQL_CONDITION_IS_NOT_SYSTEM + SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE_ALL + SQL_AND + SQL_CONDITION_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_CHANGE_PASSWORD_BY_UUID = SQL_CHANGE_PASSWORD + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_CHANGE_ACTIVATED_BY_UUID = SQL_CHANGE_ACTIVATED + SQL_WHERE + SQL_CONDITION_UUID_IS;

    private static final String SQL_CHANGE_ORGANIZATION_BY_UUID = SQL_CHANGE_ORGANIZATION + SQL_WHERE + SQL_CONDITION_UUID_IS;

    public static String FILTER_APPS = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_APP;

    public static String FILTER_USERS = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_USER;

    public static String FILTER_GROUPS = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_GROUP;

    public static String FILTER_ROLES = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_ROLE;

    public static String FILTER_USERS_UNDER_DIRECTORY = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_USER + SQL_AND + SQL_CONDITION_DIRECTORY + SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    public static String FILTER_GROUPS_UNDER_DIRECTORY = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_GROUP + SQL_AND + SQL_CONDITION_DIRECTORY + SQL_AND + SQL_CONDITION_ONLY_NOTDELETED;

    private static final ApiFilterQuery.APIFilter apiFilter = new ApiFilterQuery.APIFilter(SQL_CONDITION_NAME_IS, SQL_CONDITION_NAME_LIKE);

    //TODO: from+CRLF
    public static final String FILTER_USERS_WITH_GROUPS = "SELECT s.uuid, s.organizationid, o.name, s.created, s.updated, s.deleted, s.isdeleted, s.isactivated, s.subjecttypeid,\n" +
            "       s.subjectname, s.firstname, s.lastname, s.displayname, s.email, s.secondaryemail, s.effectivestartdate, s.effectiveenddate,\n" +
            "       s.picture, s.totallogincount, s.failedlogincount, s.invalidpasswordattemptcount, s.ispasswordchangerequired, s.passwordexpiresat,\n" +
            "       s.lastloginat, s.lastpasswordchangeat, s.lastauthenticatedat, s.lastfailedloginat, s.subjectdirectoryid, d.directoryname,\n" +
            "       s.islocked, s.issandbox, s.url, s.isrestrictedtoprocessing, s.description,\n" +
            "       s.distinguishedname, s.uniqueid, s.phonebusiness, s.phonehome, s.phonemobile, s.phoneextension,\n" +
            "       s.jobtitle, s.department, s.company,\n" +
            "       COALESCE((select json_agg(\n" +
            "                    json_build_object('uuid', g.uuid, 'isdeleted', g.isdeleted, 'displayname', g.displayname, 'description', g.description)\n" +
            "                   )\n" +
            "        from subject g\n" +
            "                  join subject_membership m on m.subjectgroupid = g.uuid\n" +
            "        where s.uuid = m.subjectid), '[]') as groups\n" +
            "from subject s\n" +
            "       inner join organization o on (s.organizationid = o.uuid)\n" +
            "       inner join subject_directory d on (s.subjectdirectoryid = d.uuid)\n" + SQL_WHERE + SQL_CONDITION_IS_USER;
    //TODO: from+CRLF
    public static final String FILTER_USER_WITH_GROUPS_AND_PERMISSIONS = "SELECT s.uuid, s.organizationid, o.name, s.created, s.updated, s.deleted, s.isdeleted, s.isactivated, s.subjecttypeid,\n" +
            "       s.subjectname, s.firstname, s.lastname, s.displayname, s.email, s.secondaryemail, s.effectivestartdate, s.effectiveenddate,\n" +
            "       s.picture, s.totallogincount, s.failedlogincount, s.invalidpasswordattemptcount, s.ispasswordchangerequired, s.passwordexpiresat,\n" +
            "       s.lastloginat, s.lastpasswordchangeat, s.lastauthenticatedat, s.lastfailedloginat, s.subjectdirectoryid, d.directoryname,\n" +
            "       s.islocked, s.issandbox, s.url, s.isrestrictedtoprocessing, s.description,\n" +
            "       s.distinguishedname, s.uniqueid, s.phonebusiness, s.phonehome, s.phonemobile, s.phoneextension,\n" +
            "       s.jobtitle, s.department, s.company,\n" +
            "       COALESCE((select json_agg(\n" +
            "                    json_build_object('uuid', g.uuid, 'isdeleted', g.isdeleted, 'displayname', g.displayname, 'description', g.description)\n" +
            "                   )\n" +
            "        from subject g\n" +
            "                  join subject_membership m on m.subjectgroupid = g.uuid\n" +
            "        where s.uuid = m.subjectid), '[]') as groups,\n" +
            "       COALESCE((select json_agg(\n" +
            "                 (SELECT x FROM (SELECT p.uuid, p.isdeleted, p.permission, p.description, p.subjectid, p.resourceid, p.resourceactionid, p.isactive) AS x)\n" +
            "               )\n" +
            "           from subject_permission p\n" +
            "       join subject_permission p2 on p2.subjectid = s.uuid\n" +
            "          ), '[]') as permissions\n" +
            "from subject s\n" +
            "inner join organization o on (s.organizationid = o.uuid)\n" +
            "inner join subject_directory d on (s.subjectdirectoryid = d.uuid)\n" +
            SQL_WHERE +
            "s." + SQL_CONDITION_UUID_IS;

    public static final String FILTER_USER_WITH_ORGANIZATIONS = "SELECT s.uuid, s.organizationid, s.created, s.updated, s.deleted, s.isdeleted, s.isactivated, s.subjecttypeid,\n" +
            "       s.subjectname, s.firstname, s.lastname, s.displayname, s.email, s.secondaryemail, s.effectivestartdate, s.effectiveenddate,\n" +
            "       s.picture, s.totallogincount, s.failedlogincount, s.invalidpasswordattemptcount, s.ispasswordchangerequired, s.passwordexpiresat,\n" +
            "       s.lastloginat, s.lastpasswordchangeat, s.lastauthenticatedat, s.lastfailedloginat, s.subjectdirectoryid, --d.directoryname,\n" +
            "       s.islocked, s.issandbox, s.url, s.isrestrictedtoprocessing, s.description,\n" +
            "       s.distinguishedname, s.uniqueid, s.phonebusiness, s.phonehome, s.phonemobile, s.phoneextension,\n" +
            "       s.jobtitle, s.department, s.company,\n" +
            "COALESCE((select json_agg(\n" +
            "\t\tjson_build_object('uuid', o.uuid, 'organizationid', o.organizationid, 'created', o.created, 'updated', o.updated, 'deleted', o.deleted, 'isdeleted', o.isdeleted, 'name', o.name, 'description', o.description, 'url', o.url, 'isactive', o.isactive, 'picture', o.picture \n" +
            "\t\t\t)\n" +
            "\t\t) from organization o\n" +
            "     \t\tjoin subject_organization so on so.organizationrefid = o.uuid\n" +
            "\t  \t\twhere s.uuid = so.subjectid\n" +
            "\t), '[]') as organizations\n" +
            "from\n" +
            "subject\ns\n" +
            "where s.subjecttypeid = '21371a15-04f8-445e-a899-006ee11c0e09'::uuid\n" +  // --User
            SQL_AND + "s." + SQL_CONDITION_UUID_IS;

    public static final String FILTER_APP_WITH_CONTRACTS_AND_ACCESS_TOKENS = "SELECT app.uuid, app.organizationid, app.created, app.updated, app.deleted, app.isdeleted, app.crudsubjectid, app.isactivated, app.subjecttypeid, app.subjectname, \n" +
            "\t\tapp.firstname, app.lastname, app.displayname, app.email, app.effectivestartdate, app.effectiveenddate, app.picture, app.subjectdirectoryid, \n" +
            "\t\tapp.islocked, app.issandbox, app.url, app.description,\n" +

            "COALESCE((select json_agg(\n" +
            "\t\tjson_build_object('uuid', c.uuid, 'organizationid', c.organizationid, 'created', c.created, 'updated', c.updated, 'deleted', c.deleted, 'isdeleted', c.isdeleted, 'crudsubjectid', c.crudsubjectid, \n" +
            "\t\t\t'name', c.\"name\", 'description', c.description, 'apiid', c.apiid, 'subjectid', c.subjectid, 'environment', c.environment, 'contractstateid', c.contractstateid, \n" +
            "\t\t\t'status', c.status, 'isrestrictedtosubsetofapi', c.isrestrictedtosubsetofapi, 'licenseid', c.licenseid, 'subjectpermissionid', c.subjectpermissionid, \n" +

            "\t\t\t'tokens', COALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', rat.uuid, 'organizationid', rat.organizationid, 'created', rat.created, 'updated', rat.updated, 'deleted', rat.deleted, 'isdeleted', rat.isdeleted, \n" +
            "\t\t\t\t\t'crudsubjectid', rat.crudsubjectid, 'subjectpermissionid', rat.subjectpermissionid, 'resourcetypeid', rat.resourcetypeid, 'resourcerefid', rat.resourcerefid, \n" +
            "\t\t\t\t\t'token', rat.token, 'expiredate', rat.expiredate, 'isactive', rat.isactive)\n" +
            "\t\t\t\t\t) from resource_access_token rat\n" +
            "\t\t\t\t\t\twhere rat.subjectpermissionid = sp.uuid\n" +
            "\t\t\t\t), '[]'),\n" +

            "\t\t\t'licenses', COALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', l.uuid, 'organizationid', l.organizationid, 'created', l.created, 'updated', l.updated, 'deleted', l.deleted, 'isdeleted', l.isdeleted, \n" +
            "\t\t\t\t\t'crudsubjectid', l.crudsubjectid, 'name', l.\"name\", 'version', l.\"version\", 'subjectid', l.subjectid, 'licensedocument', l.licensedocument, 'isactive', l.isactive)\n" +
            "\t\t\t\t\t) from license l\n" +
            "\t\t\t\t\t\twhere c.licenseid = l.uuid\n" +
            "\t\t\t\t), '[]'),\n" +

            "\t\t\t'apis', COALESCE((select json_agg(\n" +
            "\t\t\t\t\tjson_build_object('uuid', a.uuid, 'organizationid', a.organizationid, 'created', a.created, 'updated', a.updated, 'deleted', a.deleted, 'isdeleted', a.isdeleted, \n" +
            "\t\t\t\t\t'crudsubjectid', a.crudsubjectid, 'subjectid', a.subjectid, 'isproxyapi', a.isproxyapi, 'apistateid', a.apistateid, 'apivisibilityid', a.apivisibilityid, \n" +
            "\t\t\t\t\t'languagename', a.languagename, 'languageversion', a.languageversion, 'languageformat', a.languageformat, 'businessapiid', a.businessapiid, \n" +
            "\t\t\t\t\t'image', a.image, 'color', a.color, 'deployed', a.deployed, \n" +
            "\t\t\t\t\t'changelog', a.changelog, 'version', a.\"version\", 'issandbox', a.issandbox, 'islive', a.islive, 'isdefaultversion', a.isdefaultversion, 'islatestversion', a.islatestversion, \n" +
            "\t\t\t\t\t'apioriginid', a.apioriginid, 'apiparentid', a.apiparentid, \n" +
            "\t\t\t\t\t'apititle', a.openapidocument->'info'->>'title', 'apidescription', a.openapidocument->'info'->>'description', 'apiversion', a.openapidocument->'info'->>'version',\n" +
            "\t\t\t\t\t'apilicense', a.openapidocument->'info'->'license'->>'name', 'apiservers', a.openapidocument->'servers', \n" +
            "\t\t\t\t\t'apiowner', s.displayname)\n" +
            "\t\t\t\t\t) from api a\n" +
            "\t\t\t\t\t\tjoin subject s on s.uuid = a.subjectid\n" +
            "\t\t\t\t\t\twhere c.apiid = a.uuid\n" +
            "\t\t\t\t), '[]')\n" +

            "\t\t\t)\n" +
            "\t\t) from contract c\n" +
            "     \t\tjoin subject_permission sp on sp.uuid = c.subjectpermissionid\n" +
            "\t  \t\twhere app.uuid = c.subjectid\n" +
            "\t), '[]') as contracts\n" +
            "from\n" +
            "subject\napp\n" +
            ", subject usr, subject_membership sm\n" +
            "where app.subjecttypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6' --APP\n" +
            "and app.uuid = sm.subjectgroupid and sm.subjectid = usr.uuid \n" +
            "and sm.subjecttypeid = '21371a15-04f8-445e-a899-006ee11c0e09'::uuid and sm.subjectgrouptypeid = 'ca80dd37-7484-46d3-b4a1-a8af93b2d3c6'::uuid --USER + APP MEMBERSHIP\n" +
            "and usr.subjecttypeid = '21371a15-04f8-445e-a899-006ee11c0e09'::uuid \n" +
            "and usr." + SQL_CONDITION_UUID_IS;


    public static final String FILTER_APP_UUID = "app." + SQL_CONDITION_UUID_IS;

}
