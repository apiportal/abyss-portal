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
import com.verapi.portal.service.AbstractService;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SubjectService extends AbstractService<Subject> {

    private static Logger logger = LoggerFactory.getLogger(SubjectService.class);

    public SubjectService(Vertx vertx, AbyssJDBCService abyssJDBCService) throws Exception {
        super(vertx, abyssJDBCService);
        logger.info("SubjectService() invoked " + vertx + abyssJDBCService);
    }

    public SubjectService(Vertx vertx) throws Exception {
        super(vertx);
        logger.info("SubjectService() invoked " + vertx);
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
        logger.info("SubjectService findAll() invoked" + jdbcClient);
        return jdbcClient.rxQuery(SQL_FIND_ALL)
                .map(ar -> ar.getRows().stream()
                        .map(Subject::new)
                        .collect(Collectors.toList())
                );
    }

    @Override
    public Single<ResultSet> findAll() {
        logger.info("SubjectService findAll() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_COMPACT))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("SubjectService findAll() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.info("SubjectService findAll() # of records : 0");
                                return Single.just(resultSet);//return Single.error(new Exception("SubjectService findAll() # of records : 0"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterBySubjectName(String subjectName) {
        logger.info("SubjectService filterBySubjectName() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FILTER_BY_SUBJECTNAME, new JsonArray().add(subjectName + "%")))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("SubjectService filterBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.info("SubjectService filterBySubjectName() # of records : 0");
                                return Single.just(resultSet);
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

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

    /**
     * Entity specific methods
     */
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

    private static final String SQL_INSERT = "INSERT INTO Subject " +
            "(organization_id, created, updated, deleted, is_deleted, crud_subject_id, is_activated, subject_type_id, subject_name, first_name, last_name, display_name, email, secondary_email, effective_start_date, effective_end_date, password, password_salt) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
    private static final String SQL_FIND_ALL = "SELECT * FROM Subject";
    private static final String SQL_FINB_BY_ID = "SELECT * FROM todo WHERE id = ?";
    private static final String SQL_FINB_BY_UUID = "SELECT * FROM todo WHERE uuid = ?";
    private static final String SQL_DELETE = "DELETE FROM Subject WHERE id = ?";
    private static final String SQL_DELETE_ALL = "DELETE FROM Subject";
    private static final String SQL_UPDATE = "UPDATE Subject SET " +
            "organization_id = ?, created = ?, updated = ?, deleted = ?, is_deleted = ?, crud_subject_id = ?, is_activated = ?, " +
            "subject_type_id = ?, subject_name = ?, first_name = ?, last_name = ?, display_name = ?, email = ?, secondary_email = ?, " +
            "effective_start_date = ?, effective_end_date = ? " +
            "WHERE id = ?";
    private static final String SQL_UPDATE_IS_DELETED = "UPDATE Subject SET is_deleted = ? WHERE id = ?";
    private static final String SQL_UPDATE_EFFECTIVE_END_DATE = "UPDATE Subject SET effective_end_date = ? WHERE id = ?";
    private static final String SQL_FIND_ALL_COMPACT_OLD = "SELECT " +
            "uuid," +
            //"organization_id," +
            "created," +
            "updated," +
            "deleted," +
            "is_deleted," +
            //"crud_subject_id," +
            "is_activated," +
            //"subject_type_id," +
            "subject_name," +
            "first_name," +
            "last_name," +
            "display_name," +
            "email," +
            //"secondary_email," +
            "effective_start_date," +
            "effective_end_date " +
            "FROM portalschema.SUBJECT ORDER BY SUBJECT_NAME";

    private static final String SQL_FILTER_BY_SUBJECTNAME_OLD = "SELECT " +
            "uuid," +
            //"organization_id," +
            "created," +
            "updated," +
            "deleted," +
            "is_deleted," +
            //"crud_subject_id," +
            "is_activated," +
            //"subject_type_id," +
            "subject_name," +
            "first_name," +
            "last_name," +
            "display_name," +
            "email," +
            //"secondary_email," +
            "effective_start_date," +
            "effective_end_date " +
            "FROM portalschema.SUBJECT " +
            "WHERE lower(subject_name) like lower(?) " +
            "ORDER BY SUBJECT_NAME";

    private static final String SQL_FIND_ALL_COMPACT = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
            "  organization_id,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
            "  is_activated,\n" +
//            "  subject_type_id,\n" +
            "  subject_name,\n" +
            "  first_name,\n" +
            "  last_name,\n" +
            "  display_name,\n" +
            "  email,\n" +
//            "  secondary_email,\n" +
            "  effective_start_date,\n" +
            "  effective_end_date,\n" +
//            "  password,\n" +
//            "  password_salt,\n" +
            "  picture,\n" +
            "  total_login_count,\n" +
            "  failed_login_count,\n" +
            "  invalid_password_attempt_count,\n" +
            "  is_password_change_required,\n" +
            "  password_expires_at,\n" +
            "  last_login_at,\n" +
            "  last_password_change_at,\n" +
            "  last_authenticated_at\n" +
            "from subject order by subject_name";

    private static final String SQL_FILTER_BY_SUBJECTNAME = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
            "  organization_id,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
            "  is_activated,\n" +
//            "  subject_type_id,\n" +
            "  subject_name,\n" +
            "  first_name,\n" +
            "  last_name,\n" +
            "  display_name,\n" +
            "  email,\n" +
//            "  secondary_email,\n" +
            "  effective_start_date,\n" +
            "  effective_end_date,\n" +
//            "  password,\n" +
//            "  password_salt,\n" +
            "  picture,\n" +
            "  total_login_count,\n" +
            "  failed_login_count,\n" +
            "  invalid_password_attempt_count,\n" +
            "  is_password_change_required,\n" +
            "  password_expires_at,\n" +
            "  last_login_at,\n" +
            "  last_password_change_at,\n" +
            "  last_authenticated_at\n" +
            "from subject\n" +
            "where lower(subject_name) like lower(?)\n" +
            "order by subject_name";
}
