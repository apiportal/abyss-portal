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

package com.verapi.portal.service.es;

import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Instant;

public abstract class AbstractElasticSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractElasticSearchService.class);
    private RestHighLevelClient client;
    private ActionListener<IndexResponse> listener;

    AbstractElasticSearchService() {
        LOGGER.trace("AbstractElasticSearchService() invoked");
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(Config.getInstance().getConfigJsonObject().getString(Constants.ES_SERVER_HOST),
                Config.getInstance().getConfigJsonObject().getInteger(Constants.ES_SERVER_PORT),
                Config.getInstance().getConfigJsonObject().getString(Constants.ES_SERVER_SCHEME))));
        LOGGER.trace("RestHighLevelClient instance created : {}", client);
        listener = new ActionListener<IndexResponse>() {
            @Override
            public void onResponse(IndexResponse indexResponse) {
                LOGGER.trace("listener onResponse : {}", indexResponse);
            }

            @Override
            public void onFailure(Exception e) {
                LOGGER.error("listener onFailure : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
            }
        };
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            LOGGER.error("close() : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
        }
    }

    void indexDocument(RoutingContext routingContext, String index, String type, String id, JsonObject source) {
        LOGGER.trace("indexDocument() invoked");
        Boolean isESLoggerEnabled = Config.getInstance().getConfigJsonObject().getBoolean(Constants.ES_LOGGER_ENABLED);
        if (!isESLoggerEnabled) {
            return;
        }

        IndexRequest request = new IndexRequest(index, type, id);
        JsonObject sourceMap = source.copy();

        sourceMap.put("@timestamp", Instant.now());
        if (routingContext != null) {
            if (routingContext.user() == null || routingContext.user().principal() == null || !routingContext.user().principal().containsKey("username")) {
                sourceMap.put("@username", "no_user");
            } else {
                sourceMap.put("@username", routingContext.user().principal().getString("username"));
            }
            sourceMap.put("@remoteaddress", routingContext.request().remoteAddress().host());
            sourceMap.put("@httpmethod", routingContext.request().method().toString());
            sourceMap.put("@httppath", routingContext.request().path());
            sourceMap.put("@httpsession", routingContext.session().id());
        }

        try {
            request.source(sourceMap.encode(), XContentType.JSON);
            client.indexAsync(request, RequestOptions.DEFAULT, listener);
        } catch (Exception e) {
            LOGGER.error("indexDocument error : {} | {} | {}", e.getLocalizedMessage(), e.getStackTrace(), sourceMap);
        }

    }
}

