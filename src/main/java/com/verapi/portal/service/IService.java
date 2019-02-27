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

package com.verapi.portal.service;

import com.verapi.portal.oapi.CompositeResult;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.ext.jdbc.JDBCClient;

import java.util.List;
import java.util.UUID;

public interface IService<T> {

    Single<JDBCClient> initJDBCClient();

    Single<JDBCClient> initJDBCClient(String organizationUuid);

    Single<List<JsonObject>> insertAll(JsonArray insertRecords);

    Single<CompositeResult> update(UUID uuid, JsonObject updateRecord);

    Single<List<JsonObject>> updateAll(JsonObject updateRecords);

    Single<CompositeResult> delete(UUID uuid);

    Single<CompositeResult> deleteAll();

    Single<CompositeResult> deleteAll(ApiFilterQuery apiFilterQuery);

    Single<ResultSet> findById(long id);

    Single<ResultSet> findById(UUID uuid);

    Single<ResultSet> findByName(String name);

    Single<ResultSet> findLikeName(String name);

    Single<ResultSet> findAll();

    Single<ResultSet> findAll(ApiFilterQuery apiFilterQuery);

    ApiFilterQuery.APIFilter getAPIFilter();

}
