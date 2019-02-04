/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 1 2019
 *
 */

package com.verapi.portal.service.es;

import com.datastax.driver.core.HostDistance;
import com.datastax.driver.core.PoolingOptions;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.cassandra.CassandraClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class CassandraService2 {
    private static Logger logger = LoggerFactory.getLogger(CassandraService2.class);

    private static CassandraService2 instance = null;
    private CassandraClient cassandraClient;
    private PreparedStatement preparedStatement;
    private RoutingContext routingContext;

    public static CassandraService2 getInstance(RoutingContext routingContext) {
        if ((instance == null) && (routingContext != null))
            instance = new CassandraService2(routingContext);
        return instance;
    }

    private CassandraService2(RoutingContext routingContext) {
        setRoutingContext(routingContext);

        CassandraClientOptions cassandraClientOptions;
        cassandraClientOptions = new CassandraClientOptions()
                .addContactPoint("192.168.10.41")
                .addContactPoint("192.168.10.42");

        PoolingOptions poolingOptions = new PoolingOptions();
        poolingOptions
                .setConnectionsPerHost(HostDistance.LOCAL, 4, 10)
                .setConnectionsPerHost(HostDistance.REMOTE, 2, 4);

        cassandraClientOptions.dataStaxClusterBuilder()
                .withLoadBalancingPolicy(new RoundRobinPolicy())
                .withoutJMXReporting()
                .withoutMetrics()
                .withCompression(ProtocolOptions.Compression.LZ4)
                //.withPoolingOptions(poolingOptions)
                .getConfiguration().getCodecRegistry().register(InstantCodec.instance);

        CassandraClient cassandraClient = CassandraClient.createShared(getRoutingContext().vertx(), cassandraClientOptions);
        cassandraClient.connect("verapi_analytics_dev", event -> {
            if (event.succeeded()) {
                logger.info("Cassandra client connected");
                String insertStatement = "insert into platform_api_log (id, httpmethod, httppath, httpsession, \"index\", remoteaddress, source, timestamp,username)\n" +
                        "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
                cassandraClient.prepare(insertStatement, prepareResult -> {
                    if (prepareResult.succeeded()) {
                        logger.trace("Cassandra client prepared statement");
                        preparedStatement = prepareResult.result();
                        setCassandraClient(cassandraClient);
                    } else {
                        logger.error("Cassandra client is unable to prepare statement, error: {} \n stack trace:{}", event.cause().getLocalizedMessage());
                    }
                });
            } else {
                logger.error("Cassandra client is unable to connect, error: {} \n stack trace:{}", event.cause().getLocalizedMessage());
            }
        });
    }

    public void indexDocument(String index, UUID id, JsonObject source) {
        if (cassandraClient == null) {
            logger.warn("Cassandra client initialization not completed yet");
            return; //TODO: fix to wait cassandraClient instance creation asynch block
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
                .subscribe(o -> {
                            logger.info("successfully inserted into Cassandra");
                        }
                        , throwable -> {
                            logger.error("unable to insert into Cassandra! error:{} \n stack trace: {}", throwable.getLocalizedMessage(), throwable.getStackTrace());
                        });
    }

    private RoutingContext getRoutingContext() {
        return this.routingContext;
    }

    private void setRoutingContext(RoutingContext routingContext) {
        this.routingContext = routingContext;
    }

    private CassandraClient getCassandraClient() {
        return this.cassandraClient;
    }

    private void setCassandraClient(CassandraClient cassandraClient) {
        this.cassandraClient = cassandraClient;
    }
}
