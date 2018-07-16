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

import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.apache.http.HttpHost;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

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
        }

        @Override
        public void onFailure(Exception e) {
            logger.error("listener onFailure : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
        }
    };

    void indexDocument(RoutingContext routingContext, String index, String type, String id, JsonObject source) {
        logger.trace("indexDocument() invoked");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
        IndexRequest request = new IndexRequest(index + "-" + f.format(new Date()), type, id);
        JsonObject sourceMap = source.copy();

        sourceMap.put("@timestamp", Instant.now());
        if (routingContext != null) {
            sourceMap.put("@username", routingContext.user().principal().getString("username"));
            sourceMap.put("@remoteaddress", routingContext.request().remoteAddress().host());
            sourceMap.put("@httpmethod", routingContext.request().method().toString());
            sourceMap.put("@httppath", routingContext.request().path());
            sourceMap.put("@httpsession", routingContext.session().id());
        }

        try {
            request.source(sourceMap.getMap(), XContentType.JSON);
            client.indexAsync(request, listener);
        } catch (Exception e) {
            logger.error("indexDocument error : {} | {}", e.getLocalizedMessage(), e.getStackTrace());
        }

    }
}
