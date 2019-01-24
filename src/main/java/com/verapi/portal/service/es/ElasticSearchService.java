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
        super.indexDocument(routingContext, index, type, UUID.randomUUID().toString(), source);
    }

    public void indexDocument(RoutingContext routingContext, String type, JsonObject source) {
        logger.trace("indexDocument() invoked");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM");
        String index = type + "-" + f.format(new Date());
        indexDocument(routingContext, index, type, source);
    }
}
