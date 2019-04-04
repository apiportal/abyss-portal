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
import com.verapi.abyss.exception.Forbidden403Exception;
import com.verapi.abyss.exception.NoDataFoundException;
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
                            .add(jsonObj.containsKey("isactivated") ? jsonObj.getBoolean("isactivated") : false)
                            .add(jsonObj.getString("subjecttypeid"))
                            .add(jsonObj.getString("subjectname"))
                            .add(jsonObj.getString("firstname"))
                            .add(jsonObj.getString("lastname"))
                            .add(jsonObj.getString("displayname"))
                            .add(jsonObj.getString("email"))
                            .add(jsonObj.containsKey("secondaryemail") ? jsonObj.getString("secondaryemail") : "")
                            .add(jsonObj.getInstant("effectivestartdate"))
                            .add(jsonObj.containsKey("effectiveenddate") ? jsonObj.getInstant("effectiveenddate") : Instant.now().plus(90, DAYS))
                            .add(jsonObj.getString("password"))
                            .add(jsonObj.getString("passwordsalt"))
                            .add(jsonObj.getValue("picture"))
                            .add(jsonObj.getString("subjectdirectoryid"))
                            .add(jsonObj.getBoolean("islocked"))
                            .add(jsonObj.getBoolean("issandbox"))
                            .add(jsonObj.getString("url"))
                            .add(jsonObj.containsKey("isrestrictedtoprocessing") ? jsonObj.getBoolean("isrestrictedtoprocessing") : false)
                            .add(jsonObj.containsKey("description") ? jsonObj.getString("description") : "")
                            .add(jsonObj.containsKey("distinguishedname") ? jsonObj.getString("distinguishedname") : "")
                            .add(jsonObj.containsKey("uniqueid") ? jsonObj.getString("uniqueid") : "")
                            .add(jsonObj.containsKey("phonebusiness") ? jsonObj.getString("phonebusiness") : "")
                            .add(jsonObj.containsKey("phonehome") ? jsonObj.getString("phonehome") : "")
                            .add(jsonObj.containsKey("phonemobile") ? jsonObj.getString("phonemobile") : "")
                            .add(jsonObj.containsKey("phoneextension") ? jsonObj.getString("phoneextension") : "")
                            .add(jsonObj.containsKey("jobtitle") ? jsonObj.getString("jobtitle") : "")
                            .add(jsonObj.containsKey("department") ? jsonObj.getString("department") : "")
                            .add(jsonObj.containsKey("company") ? jsonObj.getString("company") : "");
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
        JsonArray updateParams = new JsonArray()
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
                .add(updateRecord.containsKey("company") ? updateRecord.getString("company") : "")
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
                    JsonArray updateParam = new JsonArray()
                            .add(jsonObj.getString("organizationid"))
                            .add(jsonObj.getString("crudsubjectid"))
                            .add(jsonObj.containsKey("isactivated") ? jsonObj.getBoolean("isactivated") : false)
                            .add(jsonObj.getString("subjecttypeid"))
                            .add(jsonObj.getString("subjectname"))
                            .add(jsonObj.getString("firstname"))
                            .add(jsonObj.getString("lastname"))
                            .add(jsonObj.getString("displayname"))
                            .add(jsonObj.getString("email"))
                            .add(jsonObj.containsKey("secondaryemail") ? jsonObj.getString("secondaryemail") : "")
                            .add(jsonObj.getInstant("effectivestartdate"))
                            .add(jsonObj.containsKey("effectiveenddate") ? jsonObj.getInstant("effectiveenddate") : Instant.now().plus(90, DAYS))
/*
                            .add(jsonObj.getString("password"))
                            .add(jsonObj.getString("passwordsalt"))
*/
                            .add(jsonObj.getValue("picture"))
                            .add(jsonObj.getString("subjectdirectoryid"))
                            .add(jsonObj.getBoolean("islocked"))
                            .add(jsonObj.getBoolean("issandbox"))
                            .add(jsonObj.getString("url"))
                            .add(jsonObj.containsKey("isrestrictedtoprocessing") ? jsonObj.getBoolean("isrestrictedtoprocessing") : false)
                            .add(jsonObj.containsKey("description") ? jsonObj.getString("description") : "")
                            .add(jsonObj.containsKey("distinguishedname") ? jsonObj.getString("distinguishedname") : "")
                            .add(jsonObj.containsKey("uniqueid") ? jsonObj.getString("uniqueid") : "")
                            .add(jsonObj.containsKey("phonebusiness") ? jsonObj.getString("phonebusiness") : "")
                            .add(jsonObj.containsKey("phonehome") ? jsonObj.getString("phonehome") : "")
                            .add(jsonObj.containsKey("phonemobile") ? jsonObj.getString("phonemobile") : "")
                            .add(jsonObj.containsKey("phoneextension") ? jsonObj.getString("phoneextension") : "")
                            .add(jsonObj.containsKey("jobtitle") ? jsonObj.getString("jobtitle") : "")
                            .add(jsonObj.containsKey("department") ? jsonObj.getString("department") : "")
                            .add(jsonObj.containsKey("company") ? jsonObj.getString("company") : "")
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

    public static String FILTER_USERS_UNDER_DIRECTORY = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_USER + SQL_AND + SQL_CONDITION_DIRECTORY;

    public static String FILTER_GROUPS_UNDER_DIRECTORY = SQL_SELECT + SQL_WHERE + SQL_CONDITION_IS_GROUP + SQL_AND + SQL_CONDITION_DIRECTORY;

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

}
