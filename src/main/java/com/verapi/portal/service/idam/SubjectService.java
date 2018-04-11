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

import com.verapi.portal.entity.idam.Subject;
import com.verapi.portal.service.AbstractService;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SubjectService extends AbstractService<Subject> {

    private static Logger logger = LoggerFactory.getLogger(SubjectService.class);

    @Override
    public Completable init() {
        return null;
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
                .doFinally(this::releaseJDBCServiceObject)
                .map(e -> subject);
    }

    @Override
    public Maybe<Subject> findById(long id) {
        return jdbcClient.rxQueryWithParams(SQL_FINB_BY_ID, new JsonArray().add(id))
                .doFinally(this::releaseJDBCServiceObject)
                .map(ResultSet::getRows)
                .toObservable()
                .flatMapIterable(e -> e)
                .singleElement()
                .map(Subject::new);
    }

    @Override
    public Maybe<Subject> findById(UUID uuid) {
        return jdbcClient.rxQueryWithParams(SQL_FINB_BY_UUID, new JsonArray().add(uuid))
                .doFinally(this::releaseJDBCServiceObject)
                .map(ResultSet::getRows)
                .toObservable()
                .flatMapIterable(e -> e)
                .singleElement()
                .map(Subject::new);
    }

    @Override
    public Single<List<Subject>> findAll() {
        return jdbcClient.rxQuery(SQL_FIND_ALL)
                .doFinally(this::releaseJDBCServiceObject)
                .map(ar -> ar.getRows().stream()
                        .map(Subject::new)
                        .collect(Collectors.toList())
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
                            .doFinally(this::releaseJDBCServiceObject)
                            .flatMapMaybe(v -> Maybe.just(subject));
                });
    }

    @Override
    public Completable delete(long id) {
        return jdbcClient.rxUpdateWithParams(SQL_DELETE, new JsonArray().add(id))
                .doFinally(this::releaseJDBCServiceObject)
                .toCompletable();
    }

    @Override
    public Completable deleteAll() {
        return jdbcClient.rxUpdate(SQL_DELETE_ALL)
                .doFinally(this::releaseJDBCServiceObject)
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
                            .doFinally(this::releaseJDBCServiceObject)
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
                            .doFinally(this::releaseJDBCServiceObject)
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

}
