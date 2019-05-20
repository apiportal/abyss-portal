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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class ElasticSearchService extends AbstractElasticSearchService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ElasticSearchService.class);

    public void indexDocument(String type, JsonObject source) {
        LOGGER.trace("indexDocument() invoked");
        indexDocument((RoutingContext) null, type, source);
    }

    public void indexDocument(String index, String type, JsonObject source) {
        indexDocument(null, index, type, source);
    }

    public void indexDocument(RoutingContext routingContext, String index, String type, JsonObject source) {
        LOGGER.trace("indexDocument() invoked");
        UUID uuid = UUID.randomUUID();
        super.indexDocument(routingContext, index, type, uuid.toString(), source);
        Boolean isCassandraLoggerEnabled = Config.getInstance().getConfigJsonObject().getBoolean(Constants.CASSANDRA_LOGGER_ENABLED);
    }

    public void indexDocument(RoutingContext routingContext, String type, JsonObject source) {
        LOGGER.trace("indexDocument() invoked");
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM");
        String index = type + "-" + f.format(new Date());
        indexDocument(routingContext, index, type, source);
    }
}
