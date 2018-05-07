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
import com.verapi.portal.entity.idam.SubjectPermission;
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

public class SubjectPermissionService extends AbstractService<SubjectPermission> {

    private static Logger logger = LoggerFactory.getLogger(SubjectPermissionService.class);

    public SubjectPermissionService(Vertx vertx) throws Exception {
        super(vertx);
        logger.info("SubjectPermissionService() invoked " + vertx);
    }


    @Override
    public Single<SubjectPermission> insert(SubjectPermission subjectGroup) {
        return null;
    }

    @Override
    public Maybe<SubjectPermission> findById(long id) {
        return null;
    }

    @Override
    public Maybe<SubjectPermission> findById(UUID uuid) {
        return null;
    }

    @Override
    public Single<ResultSet> findAll() {
        logger.info("SubjectPermissionService findAll() invoked" + jdbcClient);
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
                            logger.trace("SubjectPermissionService findAll() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterByPermissionName(String permissionName) {
        logger.info("SubjectPermissionService filterByPermissionName() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FILTER_BY_PERMISSIONNAME, new JsonArray().add(permissionName + "%")))
                        .flatMap(resultSet -> {
                            logger.trace("SubjectPermissionService filterByPermissionName() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }


    @Override
    public Maybe<SubjectPermission> update(long id, SubjectPermission newT) {
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
            "  permission,\n" +
            "  description,\n" +
            "  effective_start_date,\n" +
            "  effective_end_date,\n" +
            "  subject_id\n" +
            "from subject_permission\n" +
            "order by permission";

    private static final String SQL_FILTER_BY_PERMISSIONNAME = "select\n" +
//            "  id,\n" +
            "  uuid,\n" +
//            "  organization_id,\n" +
//            "  created,\n" +
//            "  updated,\n" +
//            "  deleted,\n" +
//            "  is_deleted,\n" +
//            "  crud_subject_id,\n" +
            "  permission\n" +
//            "  description,\n" +
//            "  effective_start_date,\n" +
//            "  effective_end_date,\n" +
//            "  subject_id\n" +
            "from subject_permission\n" +
            "where lower(permission) like lower(?)\n" +
            "order by permission";

}
