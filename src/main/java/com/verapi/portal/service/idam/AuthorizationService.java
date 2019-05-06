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

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.ApiFilterQuery;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

public class AuthorizationService extends AbstractService<UpdateResult> {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationService.class);

    public AuthorizationService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public AuthorizationService(Vertx vertx) {
        super(vertx);
    }

    @Override
    protected String getInsertSql() { return ""; }

    @Override
    protected String getFindByIdSql() { return ""; }

    @Override
    protected JsonArray prepareInsertParameters(JsonObject insertRecord) {
        return null;
    }

    /**
     *
     * @param resourceApi the resource API protected by API key
     * @param requestedApi the requested API captured from gateway uri as path parameter
     * @return <b>true</b> if the both are <b>same</b>, otherwise <b>false</b>
     */
    public Boolean authorize(UUID resourceApi, UUID requestedApi) {
        logger.trace("authorize invoked, resourceApi: [{}] | requestedApi: [{}]", resourceApi, requestedApi);
        return (resourceApi.compareTo(requestedApi) == 0);
    }

    @Override
    public Single<List<JsonObject>> insertAll(JsonArray insertRecords) {
        return null;
    }

    @Override
    public Single<CompositeResult> update(UUID uuid, JsonObject updateRecord) {
        return null;
    }

    @Override
    public Single<List<JsonObject>> updateAll(JsonObject updateRecords) {
        return null;
    }

    @Override
    public Single<CompositeResult> delete(UUID uuid) {
        return null;
    }

    @Override
    public Single<CompositeResult> deleteAll() {
        return null;
    }

    @Override
    public Single<CompositeResult> deleteAll(ApiFilterQuery apiFilterQuery) {
        return null;
    }

    @Override
    public Single<ResultSet> findById(long id) {
        return null;
    }

    @Override
    public Single<ResultSet> findById(UUID uuid) {
        return null;
    }

    @Override
    public Single<ResultSet> findByName(String name) {
        return null;
    }

    @Override
    public Single<ResultSet> findLikeName(String name) {
        return null;
    }

    @Override
    public Single<ResultSet> findAll() {
        return null;
    }

    @Override
    public Single<ResultSet> findAll(ApiFilterQuery apiFilterQuery) {
        return null;
    }

    @Override
    public ApiFilterQuery.APIFilter getAPIFilter() {
        return null;
    }
}