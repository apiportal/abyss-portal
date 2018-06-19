/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.portal.service.idam;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.entity.idam.Subject;
import com.verapi.portal.service.AbstractServiceOld;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Deprecated
public class SubjectServiceOld extends AbstractServiceOld<Subject> {

    private static Logger logger = LoggerFactory.getLogger(SubjectServiceOld.class);

    public SubjectServiceOld(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
        logger.info("SubjectServiceOld() invoked " + vertx + abyssJDBCService);
    }

    public SubjectServiceOld(Vertx vertx) {
        super(vertx);
        logger.info("SubjectServiceOld() invoked " + vertx);
    }

    @Override
    public Single<Subject> insert(Subject subject) {
        JsonArray insertParams = new JsonArray().add(subject.getOrganizationId())
                .add(subject.getCreated())
                .add(subject.getUpdated())
                .add(subject.getDeleted())
                .add(subject.getIsDeleted())
                .add(subject.getCrudSubjectId())
                .add(subject.getIsActivated())
                .add(subject.getSubjectTypeId())
                .add(subject.getSubjectName())
                .add(subject.getFirstName())
                .add(subject.getLastName())
                .add(subject.getDisplayName())
                .add(subject.getEmail())
                .add(subject.getSecondaryEmail())
                .add(subject.getEffectiveStartDate())
                .add(subject.getEffectiveEndDate())
                .add(subject.getPassword())
                .add(subject.getPasswordSalt());
        return jdbcClient.rxUpdateWithParams(SQL_INSERT, insertParams)
                .map(e -> subject);
    }


    public Single<UpdateResult> insertJson(JsonObject subjectAsJson) {
        JsonArray insertParams = new JsonArray()
                .add(((Number) subjectAsJson.getValue("organizationid")).longValue())
                .add(((Number) subjectAsJson.getValue("crudsubjectid")).longValue())
                .add(((Number) subjectAsJson.getValue("subjecttypeid")).longValue())
                .add(((String) subjectAsJson.getValue("subjectname")))
                .add(((String) subjectAsJson.getValue("firstname")))
                .add(((String) subjectAsJson.getValue("lastname")))
                .add(((String) subjectAsJson.getValue("displayname")))
                .add(((String) subjectAsJson.getValue("email")))
                .add(((String) subjectAsJson.getValue("secondaryemail")))
                .add((subjectAsJson.getInstant("effectivestartdate")))
                .add((subjectAsJson.getInstant("effectiveenddate")))
                .add(((String) subjectAsJson.getValue("password")))
                .add(((String) subjectAsJson.getValue("passwordsalt")))
                .add(((String) subjectAsJson.getValue("picture")))
                .add(((Number) subjectAsJson.getValue("subjectdirectoryid")).longValue());
        return jdbcClient
                .rxGetConnection().flatMap(conn -> {
                            return conn
                                    .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                                    // Disable auto commit to handle transaction manually
                                    .rxSetAutoCommit(false)
                                    // Switch from Completable to default Single value
                                    .toSingleDefault(false)
                                    //Check if user already exists
                                    .flatMap(insertConn -> conn.rxUpdateWithParams(SQL_INSERT, insertParams))
                                    .flatMap(insertResult -> {
                                        if (insertResult.getUpdated() == 0)
                                            return Single.error(new Exception("unable to insert new record into database"));
                                        //conn.rxCommit();
                                        logger.trace("[" + insertResult.getUpdated() + "] subject created successfully: " + insertResult.getKeys().encodePrettily() + " | subject id: " + insertResult.getKeys().getInteger(0));
                                        return Single.just(insertResult);
                                    })

                                    // commit if all succeeded
/*
                                    .flatMap(updateResult -> {
                                        conn.rxCommit().subscribe(() -> logger.info("commit ok"));
                                        return Single.just(updateResult);
                                    })
*/
                                    .flatMap(updateResult -> conn.rxCommit().toSingleDefault(updateResult))

                                    // Rollback if any failed with exception propagation
                                    .onErrorResumeNext(ex -> conn.rxRollback().toSingleDefault(true)
                                            .onErrorResumeNext(ex2 -> Single.error(new CompositeException(ex, ex2)))
                                            .flatMap(ignore -> {
                                                logger.warn("rollback!!");
                                                logger.error(ex.getLocalizedMessage());
                                                logger.error(Arrays.toString(ex.getStackTrace()));
                                                return Single.error(ex);
                                            })
                                    )

                                    .doAfterSuccess(succ -> {
                                        logger.info("Subject record created successfully");
                                    })

                                    // close the connection regardless succeeded or failed
                                    .doAfterTerminate(conn::close);
                        }
                );

    }

    @Override
    public Maybe<Subject> findById(long id) {
        return jdbcClient.rxQueryWithParams(SQL_FINB_BY_ID, new JsonArray().add(id))
                .map(ResultSet::getRows)
                .toObservable()
                .flatMapIterable(e -> e)
                .singleElement()
                .map(Subject::new);
    }

    @Override
    public Maybe<Subject> findById(UUID uuid) {
        return jdbcClient.rxQueryWithParams(SQL_FINB_BY_UUID, new JsonArray().add(uuid))
                .map(ResultSet::getRows)
                .toObservable()
                .flatMapIterable(e -> e)
                .singleElement()
                .map(Subject::new);
    }


    public Single<List<Subject>> findAllEntity() {
        logger.info("SubjectServiceOld findAll() invoked" + jdbcClient);
        return jdbcClient.rxQuery(SQL_FIND_ALL)
                .map(ar -> ar.getRows().stream()
                        .map(Subject::new)
                        .collect(Collectors.toList())
                );
    }

    @Override
    public Single<ResultSet> findAll() {
        logger.info("SubjectServiceOld findAll() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        //.flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_COMPACT))
                        .flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_WITH_GROUPS_PERMISSIONS))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectServiceOld findAll() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> findBySubjectId(long subjectId) {
        logger.info("SubjectServiceOld findBySubjectId() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FIND_BY_SUBJECTID, new JsonArray().add(subjectId)))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectServiceOld findBySubjectId() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterBySubjectName(String subjectName) {
        logger.info("SubjectServiceOld filterBySubjectName() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FILTER_BY_SUBJECTNAME, new JsonArray().add(subjectName + "%")))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectServiceOld filterBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> findBySubjectName(String subjectName) {
        logger.info("SubjectServiceOld findBySubjectName() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FIND_BY_SUBJECTNAME, new JsonArray().add(subjectName)))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectServiceOld findBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

/*
    public Single<List<JsonObject>> findAllJ() {
        logger.info("SubjectServiceOld findAllJ() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        //.flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_COMPACT))
                        .flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_WITH_GROUPS_PERMISSIONS))
                        .map(ar -> ar.getRows().stream()
                                .map(JsonObject::stream)
                                .collect(Collectors.toList()))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectServiceOld findAll() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }
*/

    @Override
    public Maybe<Subject> update(long id, Subject newT) {
        return findById(id)
                .flatMap(old -> {
                    Subject subject = old.merge(newT);
                    JsonArray updateParams = new JsonArray()
                            .add(subject.getOrganizationId())
                            .add(subject.getCreated())
                            .add(subject.getUpdated())
                            .add(subject.getDeleted())
                            .add(subject.getIsDeleted())
                            .add(subject.getCrudSubjectId())
                            .add(subject.getIsActivated())
                            .add(subject.getSubjectTypeId())
                            .add(subject.getSubjectName())
                            .add(subject.getFirstName())
                            .add(subject.getLastName())
                            .add(subject.getDisplayName())
                            .add(subject.getEmail())
                            .add(subject.getSecondaryEmail())
                            .add(subject.getEffectiveStartDate())
                            .add(subject.getEffectiveEndDate())
                            .add(subject.getPassword())
                            .add(subject.getPasswordSalt())
                            .add(subject.getId());
                    return jdbcClient.rxUpdateWithParams(SQL_UPDATE, updateParams)
                            .flatMapMaybe(v -> Maybe.just(subject));
                });
    }

    @Override
    public Completable delete(long id) {
        return jdbcClient.rxUpdateWithParams(SQL_DELETE, new JsonArray().add(id))
                .toCompletable();
    }

    @Override
    public Completable deleteAll() {
        return jdbcClient.rxUpdate(SQL_DELETE_ALL)
                .toCompletable();
    }

    public Maybe<Subject> updateIsDeleted(long id) {
        return findById(id)
                .flatMap(old -> {
                    Subject subject = old;
                    subject.setIsDeleted(1);
                    JsonArray updateParams = new JsonArray()
                            .add(subject.getIsDeleted())
                            .add(subject.getId());
                    return jdbcClient.rxUpdateWithParams(SQL_UPDATE_IS_DELETED, updateParams)
                            .flatMapMaybe(v -> Maybe.just(subject));
                });
    }

    public Maybe<Subject> updateEffectiveEndDate(long id, Instant effectiveEndDate) {
        return findById(id)
                .flatMap(old -> {
                    Subject subject = old;
                    subject.setEffectiveEndDate(effectiveEndDate);
                    JsonArray updateParams = new JsonArray()
                            .add(subject.getEffectiveEndDate())
                            .add(subject.getId());
                    return jdbcClient.rxUpdateWithParams(SQL_UPDATE_EFFECTIVE_END_DATE, updateParams)
                            .flatMapMaybe(v -> Maybe.just(subject));
                });
    }

    private static final String SQL_INSERT_OLD = "INSERT INTO Subject " +
            "(organizationid, created, updated, deleted, isdeleted, crudsubjectid, isactivated, subjecttypeid, subjectname, firstname, lastname, displayname, email, secondaryemail, effectivestartdate, effectiveenddate, password, passwordsalt) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String SQL_FIND_ALL = "SELECT * FROM Subject";
    private static final String SQL_FINB_BY_ID = "SELECT * FROM todo WHERE id = ?";
    private static final String SQL_FINB_BY_UUID = "SELECT * FROM todo WHERE uuid = ?";
    private static final String SQL_DELETE = "DELETE FROM Subject WHERE id = ?";
    private static final String SQL_DELETE_ALL = "DELETE FROM Subject";
    private static final String SQL_UPDATE = "UPDATE Subject SET " +
            "organization_id = ?, created = ?, updated = ?, deleted = ?, isdeleted = ?, crudsubjectid = ?, isactivated = ?, " +
            "subjecttypeid = ?, subjectname = ?, firstname = ?, lastname = ?, displayname = ?, email = ?, secondaryemail = ?, " +
            "effectivestartdate = ?, effectiveenddate = ? " +
            "WHERE id = ?";
    private static final String SQL_UPDATE_IS_DELETED = "UPDATE Subject SET isdeleted = ? WHERE id = ?";
    private static final String SQL_UPDATE_EFFECTIVE_END_DATE = "UPDATE Subject SET effectiveenddate = ? WHERE id = ?";

    //#########################################################
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

    private static final String SQL_WHERE = "where id = ?\n";

    private static final String SQL_WHERE_SUBJECTNAME_IS = "where lower(subjectname) = lower(?)";

    private static final String SQL_ORDERBY_NAME = "order by subjectname\n";

    private static final String SQL_FIND_BY_SUBJECTID = SQL_SELECT + SQL_WHERE;

    private static final String SQL_FIND_BY_SUBJECTNAME = SQL_SELECT + SQL_WHERE_SUBJECTNAME_IS;

    //#########################################################

    private static final String SQL_FIND_ALL_COMPACT = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
            "  organization_id,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  isdeleted,\n" +
//            "  crud_subject_id,\n" +
            "  isactivated,\n" +
//            "  subject_type_id,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
//            "  secondary_email,\n" +
            "  effectivestartdate,\n" +
            "  effectiveenddate,\n" +
//            "  password,\n" +
//            "  password_salt,\n" +
            "  picture,\n" +
            "  totallogincount,\n" +
            "  failedlogincount,\n" +
            "  invalidpasswordattempt_count,\n" +
            "  ispasswordchangerequired,\n" +
            "  passwordexpiresat,\n" +
            "  lastloginat,\n" +
            "  lastpasswordchangeat,\n" +
            "  lastauthenticatedat\n" +
            "from subject order by subjectname";

    private static final String SQL_FILTER_BY_SUBJECTNAME = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
//            "  organization_id,\n" +
//            "  created,\n" +
//            "  updated,\n" +
//            "  deleted,\n" +
//            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
//            "  is_activated,\n" +
//            "  subject_type_id,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
//            "  secondary_email,\n" +
//            "  effective_start_date,\n" +
//            "  effective_end_date,\n" +
//            "  password,\n" +
//            "  password_salt,\n" +
            "  picture\n" +
//            "  total_login_count,\n" +
//            "  failed_login_count,\n" +
//            "  invalid_password_attempt_count,\n" +
//            "  is_password_change_required,\n" +
//            "  password_expires_at,\n" +
//            "  last_login_at,\n" +
//            "  last_password_change_at,\n" +
//            "  last_authenticated_at\n" +
            "from subject\n" +
            "where lower(subjectname) like lower(?)\n" +
            "order by subjectname";

    private static final String SQL_FIND_BY_SUBJECTNAME_OLD = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
//            "  organization_id,\n" +
//            "  created,\n" +
//            "  updated,\n" +
//            "  deleted,\n" +
//            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
//            "  is_activated,\n" +
//            "  subject_type_id,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
//            "  secondary_email,\n" +
//            "  effective_start_date,\n" +
//            "  effective_end_date,\n" +
//            "  password,\n" +
//            "  password_salt,\n" +
            "  picture\n" +
//            "  total_login_count,\n" +
//            "  failed_login_count,\n" +
//            "  invalid_password_attempt_count,\n" +
//            "  is_password_change_required,\n" +
//            "  password_expires_at,\n" +
//            "  last_login_at,\n" +
//            "  last_password_change_at,\n" +
//            "  last_authenticated_at\n" +
            "from subject\n" +
            "where lower(subjectname) = lower(?)";

    private static final String SQL_INSERT = "insert into subject (organizationid, crudsubjectid, subjecttypeid, subjectname, firstname, lastname, displayname, email, secondaryemail, effectivestartdate, effectiveenddate, password, passwordsalt, picture, subjectdirectoryid)\n" +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, coalesce(?, now()), ?, ?, ?, ?, ?)";
//            "values (:organizationid, :crudsubjectid, :subjecttypeid, :subjectname, :firstname, :lastname, :displayname, :email, :secondaryemail, coalesce(:effectivestartdate, now()), :effectiveenddate, :password, :passwordsalt, :picture, :subjectdirectoryid)";

    private static final String SQL_FIND_ALL_WITH_GROUPS_PERMISSIONS = "select row_to_json(t)  rowjson\n" +
            "from (\n" +
            "       select\n" +
//            "         id,\n" +
            "         uuid,\n" +
            "         organizationid,\n" +
            "         created,\n" +
            "         updated,\n" +
            "         deleted,\n" +
            "         isdeleted,\n" +
            "         crudsubjectid,\n" +
            "         isactivated,\n" +
            "         subjecttypeid,\n" +
            "         subjectname,\n" +
            "         firstname,\n" +
            "         lastname,\n" +
            "         displayname,\n" +
            "         email,\n" +
            "         secondaryemail,\n" +
            "         effectivestartdate,\n" +
            "         effectiveenddate,\n" +
//            "         password,\n" +
//            "         passwordsalt,\n" +
            "         picture,\n" +
            "         totallogincount,\n" +
            "         failedlogincount,\n" +
            "         invalidpasswordattemptcount,\n" +
            "         ispasswordchangerequired,\n" +
            "         passwordexpiresat,\n" +
            "         lastloginat,\n" +
            "         lastpasswordchangeat,\n" +
            "         lastauthenticatedat,\n" +
            "         lastfailedloginat,\n" +
            "         (\n" +
            "           select json_agg(row_to_json(sp))\n" +
            "           from (\n" +
            "                  select\n" +
            "                    sg.uuid,\n" +
            "                    sg.groupname\n" +
            "                  from subject_group sg, subject_membership sm\n" +
            "                  where sm.subjectid = subject.id and sg.id = sm.subjectgroupid\n" +
            "                ) sp\n" +
            "         ) as groups,\n" +
            "         (\n" +
            "           select json_agg(row_to_json(rsp))\n" +
            "           from (\n" +
            "                  select\n" +
            "                    sp.uuid,\n" +
            "                    sp.permission\n" +
            "                  from subject_permission sp\n" +
            "                  where sp.subjectid = subject.id) rsp\n" +
            "         ) as permissions\n" +
            "       from subject\n" +
            "       order by subjectname\n" +
            "     ) as t";

}
