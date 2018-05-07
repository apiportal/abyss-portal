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

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.entity.idam.SubjectGroup;
import com.verapi.portal.service.AbstractService;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class SubjectGroupService extends AbstractService<SubjectGroup> {

    private static Logger logger = LoggerFactory.getLogger(SubjectGroupService.class);

    public SubjectGroupService(Vertx vertx) throws Exception {
        super(vertx);
        logger.info("SubjectGroupService() invoked " + vertx);
    }


    @Override
    public Single<SubjectGroup> insert(SubjectGroup subjectGroup) {
        return null;
    }

    @Override
    public Maybe<SubjectGroup> findById(long id) {
        return null;
    }

    @Override
    public Maybe<SubjectGroup> findById(UUID uuid) {
        return null;
    }

    @Override
    public Single<ResultSet> findAll() {
        logger.info("SubjectGroupService findAll() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_COMPACT))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectGroupService findAll() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterByGroupName(String groupName) {
        logger.info("SubjectGroupService filterByGroupName() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.API_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FILTER_BY_GROUPNAME, new JsonArray().add(groupName + "%")))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectGroupService filterByGroupName() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    @Override
    public Maybe<SubjectGroup> update(long id, SubjectGroup newT) {
        return null;
    }

    @Override
    public Completable delete(long id) {
        return null;
    }

    @Override
    public Completable deleteAll() {
        return null;
    }

    private static final String SQL_FIND_ALL_COMPACT = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
            "  organization_id,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
            "  is_enabled,\n" +
            "  group_name,\n" +
            "  description,\n" +
            "  effective_start_date,\n" +
            "  effective_end_date\n" +
            "from subject_group order by group_name";

    private static final String SQL_FILTER_BY_GROUPNAME = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
//            "  organization_id,\n" +
//            "  created,\n" +
//            "  updated,\n" +
//            "  deleted,\n" +
//            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
//            "  is_enabled,\n" +
            "  group_name\n" +
//            "  description,\n" +
//            "  effective_start_date,\n" +
//            "  effective_end_date\n" +
            "from subject_group\n" +
            "where lower(group_name) like lower(?)\n" +
            "order by group_name";
}
