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

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.entity.idam.SubjectGroup;
import com.verapi.portal.service.AbstractServiceOld;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

@Deprecated
public class SubjectGroupServiceOld extends AbstractServiceOld<SubjectGroup> {

    private static Logger logger = LoggerFactory.getLogger(SubjectGroupServiceOld.class);

    public SubjectGroupServiceOld(Vertx vertx) throws Exception {
        super(vertx);
        logger.info("SubjectGroupServiceOld() invoked " + vertx);
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
        logger.info("SubjectGroupServiceOld findAll() invoked" + jdbcClient);
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
                            logger.trace("SubjectGroupServiceOld findAll() # of records :[" + resultSet.getNumRows() + "]");
                            return Single.just(resultSet);
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterByGroupName(String groupName) {
        logger.info("SubjectGroupServiceOld filterByGroupName() invoked" + jdbcClient);
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
                            logger.trace("SubjectGroupServiceOld filterByGroupName() # of records :[" + resultSet.getNumRows() + "]");
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
            "  organizationid,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  isdeleted,\n" +
//            "  crud_subject_id,\n" +
            "  isenabled,\n" +
            "  groupname,\n" +
            "  description,\n" +
            "  effectivestartdate,\n" +
            "  effectiveenddate\n" +
            "from subject_group order by groupname";

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
            "  groupname\n" +
//            "  description,\n" +
//            "  effective_start_date,\n" +
//            "  effective_end_date\n" +
            "from subject_group\n" +
            "where lower(groupname) like lower(?)\n" +
            "order by groupname";
}
