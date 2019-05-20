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

import com.verapi.abyss.common.Constants;
import com.verapi.abyss.exception.ApiSchemaError;
import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.oapi.CompositeResult;
import com.verapi.portal.service.AbstractService;
import com.verapi.portal.service.ApiFilterQuery;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.api.RequestParameters;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class LifecycleService extends AbstractService<UpdateResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleService.class);

    private static HashMap<String, String> stateToVisibilityMap = new HashMap<>();

    static {
        stateToVisibilityMap.put(Constants.API_STATE_DRAFT, Constants.API_VISIBILITY_PRIVATE);
        stateToVisibilityMap.put(Constants.API_STATE_STAGED, Constants.API_VISIBILITY_PRIVATE);
        stateToVisibilityMap.put(Constants.API_STATE_PUBLISHED, Constants.API_VISIBILITY_PUBLIC);
        stateToVisibilityMap.put(Constants.API_STATE_PROMOTED, Constants.API_VISIBILITY_PUBLIC);
        stateToVisibilityMap.put(Constants.API_STATE_DEMOTED, Constants.API_VISIBILITY_PUBLIC);
        stateToVisibilityMap.put(Constants.API_STATE_DEPRECATED, Constants.API_VISIBILITY_PUBLIC);
        stateToVisibilityMap.put(Constants.API_STATE_RETIRED, Constants.API_VISIBILITY_PRIVATE);
        stateToVisibilityMap.put(Constants.API_STATE_REMOVED, Constants.API_VISIBILITY_PRIVATE);
    }

    public LifecycleService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public LifecycleService(Vertx vertx) {
        super(vertx);
    }

    @Override
    protected String getInsertSql() { return ""; }

    @Override
    protected String getFindByIdSql() { return ""; }

    @Override
    protected JsonArray prepareInsertParameters(JsonObject insertRecord) {
        return new JsonArray();
    }



    public Single<JsonObject> updateLifecycle(RoutingContext routingContext) {
        LOGGER.trace("updateLifecycle invoked");

        String sessionOrganizationId = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_ORGANIZATION_UUID_COOKIE_NAME);
        String sessionUserId = routingContext.session().get(Constants.AUTH_ABYSS_PORTAL_USER_UUID_SESSION_VARIABLE_NAME);

        // Get the parsed parameters
        RequestParameters requestParameters = routingContext.get("parsedParameters");

        String apiId = routingContext.pathParam("uuid");

        // We get an user JSON object validated by Vert.x Open API validator
        JsonObject lifecycleChange = requestParameters.body().getJsonObject();
        String currentStateId  = lifecycleChange.getString("currentstateid");
        String nextStateId     = lifecycleChange.getString("nextstateid");
        String apiVisibilityId = stateToVisibilityMap.get(nextStateId);
        LOGGER.trace("Api: {} state transition from {} to {}. New Visibility Id: {}", apiId, currentStateId, nextStateId, apiVisibilityId);


        ApiService apiService = new ApiService(routingContext.vertx());

        return apiService.initJDBCClient(sessionOrganizationId)
                .flatMap(jdbcClient -> apiService.updateLifecycle(
                            UUID.fromString(apiId),
                            new JsonArray().add(sessionUserId)
                                    .add(nextStateId)
                                    .add(apiVisibilityId)
                            )
                            .flatMap(compositeResult -> {
                                if (compositeResult.getThrowable() == null) {
                                    if (compositeResult.getUpdateResult().getUpdated() == 1) {
                                        LOGGER.trace("updateLifecycle - api {} state changed from {} to {}", apiId, currentStateId, nextStateId);
                                        return Single.just(compositeResult.getUpdateResult());
                                    } else {
                                        LOGGER.error("updateLifecycle - api {} state update error from {} to {}", apiId, currentStateId, nextStateId);
                                        return Single.error(new Exception("Api Lifecycle Update Error Occurred"));
                                    }
                                } else {
                                    LOGGER.error("updateLifecycle - api {} state change error from {} to {}\n{}", apiId, currentStateId, nextStateId, compositeResult.getThrowable());
                                    return Single.error(compositeResult.getThrowable());
                                }
                            })
                )
                .flatMap(updateResult -> Single.just(new ApiSchemaError()
                                                    .setCode(HttpResponseStatus.OK.code())
                                                    .setUsermessage("Api State Changed Successfully!")
                                                    .setInternalmessage("")
                                                    .setDetails("Api State Changed Successfully!")
                                                    .setRecommendation("")
                                                    //.setMoreinfo(new URL(""))
                                                    .toJson())
                );
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
