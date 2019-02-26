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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ElasticSearchService extends AbstractElasticSearchService {
    private static Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);

    public void indexDocument(String type, JsonObject source) {
        logger.trace("indexDocument() invoked");
        indexDocument((RoutingContext) null, type, source);
    }

    public void indexDocument(String index, String type, JsonObject source) {
        indexDocument(null, index, type, source);
    }

    public void indexDocument(RoutingContext routingContext, String index, String type, JsonObject source) {
        logger.trace("indexDocument() invoked");
        UUID uuid = UUID.randomUUID();
        super.indexDocument(routingContext, index, type, uuid.toString(), source);
//        Boolean isCassandraLoggerEnabled = Config.getInstance().getConfigJsonObject().getBoolean(Constants.CASSANDRA_LOGGER_ENABLED);
//        if (isCassandraLoggerEnabled) {
//            try {
//                if (CassandraService.getInstance(routingContext) != null)
//                    CassandraService.getInstance(routingContext).indexDocument(type, uuid, source);
//            } catch (Exception e) {
//                logger.error("Cassandra indexDocument error : {} | {} | {}", e.getLocalizedMessage(), e.getStackTrace(), source);
//            }
//        }
    }

    public void indexDocument(RoutingContext routingContext, String type, JsonObject source) {
        logger.trace("indexDocument() invoked");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM");
        String index = type + "-" + f.format(new Date());
        indexDocument(routingContext, index, type, source);
    }
}
