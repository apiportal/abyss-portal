/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 7 2018
 *
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
    private static Logger logger = LoggerFactory.getLogger(AbstractElasticSearchService.class);
    private static RestHighLevelClient client;

    AbstractElasticSearchService() {
        logger.trace("AbstractElasticSearchService() invoked");
        client = new RestHighLevelClient(RestClient.builder(new HttpHost(Config.getInstance().getConfigJsonObject().getString(Constants.ES_SERVER_HOST),
                Config.getInstance().getConfigJsonObject().getInteger(Constants.ES_SERVER_PORT),
                Config.getInstance().getConfigJsonObject().getString(Constants.ES_SERVER_SCHEME))));
        logger.trace("RestHighLevelClient instance created : {}", client.toString());
    }

    public void close() {
        try {
            client.close();
        } catch (IOException e) {
            logger.error("close() : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
        }
    }

    private ActionListener<IndexResponse> listener = new ActionListener<IndexResponse>() {
        @Override
        public void onResponse(IndexResponse indexResponse) {
            logger.trace("listener onResponse : {}", indexResponse);
            //close();
        }

        @Override
        public void onFailure(Exception e) {
            logger.error("listener onFailure : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
            //close();
        }
    };

    void indexDocument(RoutingContext routingContext, String index, String type, String id, JsonObject source) {
        logger.trace("indexDocument() invoked");
        IndexRequest request = new IndexRequest(index, type, id);
        JsonObject sourceMap = source.copy();

        sourceMap.put("@timestamp", Instant.now());
        if (routingContext != null) {
            if (routingContext.user()==null || routingContext.user().principal()==null || !routingContext.user().principal().containsKey("username")) {
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
            //request.source(sourceMap.getMap(), XContentType.JSON);
            request.source(sourceMap.encode(), XContentType.JSON);
            client.indexAsync(request, RequestOptions.DEFAULT, listener);
        } catch (Exception e) {
            logger.error("indexDocument error : {} | {} | {}", e.getLocalizedMessage(), e.getStackTrace(), sourceMap);
        }

        //log into Cassandra
        //1. creating Rxified Cassandra client


    }
}

