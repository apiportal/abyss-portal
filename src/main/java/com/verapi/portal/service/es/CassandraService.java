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

import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.AsyncResult;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.cassandra.CassandraClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public final class CassandraService {
    private static final Logger LOGGER = LoggerFactory.getLogger(CassandraService.class);
    private static final int LOCAL_CORE_NUM_OF_CONN = 4;
    private static final int LOCAL_MAX_NUM_OF_CONN = 10;
    private static final int REMOTE_CORE_NUM_OF_CONN = 2;
    private static final int REMOTE_MAX_NUM_OF_CONN = 4;

    private static CassandraService instance;
    private CassandraClient cassandraClient;
    private PreparedStatement preparedStatement;
    private RoutingContext routingContext;

    /**
     * Construct new singleton CassandraService instance
     *
     * @param routingContext Vertx instance is associated with this routing context, it is required for {@link io.vertx.cassandra.CassandraClient#createShared(Vertx, CassandraClientOptions)}
     */


    private CassandraService(RoutingContext routingContext) {
        LOGGER.info("initializing Cassandra Service");
        setRoutingContext(routingContext);
        String[] cassandraContactPoints = Config.getInstance().getConfigJsonObject().getString(Constants.CASSANDRA_CONTACT_POINTS).split(",");

        CassandraClientOptions cassandraClientOptions = new CassandraClientOptions();

        for (String contactPoint : cassandraContactPoints) {
            cassandraClientOptions.addContactPoint(contactPoint);
            LOGGER.info("Cassandra contact point[{}] added", contactPoint);
        }
        cassandraClientOptions.setPort(Config.getInstance().getConfigJsonObject().getInteger(Constants.CASSANDRA_PORT));

        PoolingOptions poolingOptions = new PoolingOptions();
        poolingOptions
                .setConnectionsPerHost(HostDistance.LOCAL, LOCAL_CORE_NUM_OF_CONN, LOCAL_MAX_NUM_OF_CONN)
                .setConnectionsPerHost(HostDistance.REMOTE, REMOTE_CORE_NUM_OF_CONN, REMOTE_MAX_NUM_OF_CONN);

        cassandraClientOptions.dataStaxClusterBuilder()
                .withCredentials(Config.getInstance().getConfigJsonObject().getString(Constants.CASSANDRA_DBUSER_NAME)
                        , Config.getInstance().getConfigJsonObject().getString(Constants.CASSANDRA_DBUSER_PASSWORD))
                .withLoadBalancingPolicy(new RoundRobinPolicy())
                .withoutJMXReporting()
                .withoutMetrics()
                .withCompression(ProtocolOptions.Compression.LZ4)
                //.withPoolingOptions(poolingOptions)
                .getConfiguration().getCodecRegistry().register(InstantCodec.instance);

        CassandraClient sharedCassandraClient = CassandraClient.createShared(getRoutingContext().vertx(), cassandraClientOptions);
        sharedCassandraClient.connect(Config.getInstance().getConfigJsonObject().getString(Constants.CASSANDRA_KEYSPACE), (AsyncResult<Void> event) -> {
            if (event.succeeded()) {
                LOGGER.info("Cassandra client connected");
                String insertStatement = "insert into platform_api_log (id, httpmethod, httppath, httpsession, \"index\", remoteaddress, source, timestamp,username)\n" +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                sharedCassandraClient.prepare(insertStatement, (AsyncResult<PreparedStatement> prepareResult) -> {
                    if (prepareResult.succeeded()) {
                        LOGGER.trace("Cassandra client prepared statement");
                        preparedStatement = prepareResult.result();
                        setCassandraClient(sharedCassandraClient);
                    } else {
                        LOGGER.error("Cassandra client is unable to prepare statement, error: {} \n stack trace:{}"
                                , event.cause().getLocalizedMessage(), event.cause().getStackTrace());
                    }
                });
            } else {
                LOGGER.error("Cassandra client is unable to connect, error: {} \n stack trace:{}"
                        , event.cause().getLocalizedMessage(), event.cause().getStackTrace());
            }
        });
    }

    /**
     * returns a singleton CassandraService instance
     *
     * @param routingContext Vertx instance is associated with this routing context, it is required for {@link io.vertx.cassandra.CassandraClient#createShared(Vertx, CassandraClientOptions)}
     */
    public static CassandraService getInstance(RoutingContext routingContext) {
        if ((instance == null) && (routingContext != null)) {
            instance = new CassandraService(routingContext);
        }
        return instance;
    }

    public void indexDocument(String index, UUID id, JsonObject source) {
        if (cassandraClient == null) {
            LOGGER.warn("Cassandra client initialization not completed yet");
            //TODO: fix to wait cassandraClient instance creation asynch block
            return;
        }

        cassandraClient
                .rxExecute(preparedStatement.bind(id
                        , getRoutingContext().request().method().toString()
                        , getRoutingContext().request().path()
                        , getRoutingContext().session().id()
                        , index
                        , getRoutingContext().request().remoteAddress().host()
                        , source.encode()
                        , Instant.now()
                        , getRoutingContext().user().principal().getString("username")))
                .doAfterTerminate(() -> cassandraClient.rxDisconnect())
                .subscribe(o -> LOGGER.info("successfully inserted into Cassandra")
                        , throwable -> LOGGER.error("unable to insert into Cassandra! error:{} \n stack trace: {}"
                                , throwable.getLocalizedMessage(), throwable.getStackTrace()));
    }

    private RoutingContext getRoutingContext() {
        return this.routingContext;
    }

    private void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    CassandraClient getCassandraClient() {
        return this.cassandraClient;
    }

    private void setCassandraClient(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }
}
