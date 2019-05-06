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

    Single<JDBCClient> initJDBCClient(String organizationUuid, String operationId);

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
