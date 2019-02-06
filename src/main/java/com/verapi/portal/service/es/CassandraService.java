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
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.policies.RoundRobinPolicy;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import io.reactivex.Single;
import io.vertx.cassandra.CassandraClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.cassandra.CassandraClient;
import io.vertx.reactivex.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

public class CassandraService {
    private static Logger logger = LoggerFactory.getLogger(CassandraService.class);

    private static CassandraService instance = null;
    private CassandraClient cassandraClient;
    private RoutingContext routingContext;

    public static CassandraService getInstance(RoutingContext routingContext) {
        if ((instance == null) && (routingContext != null))
            instance = new CassandraService(routingContext);
        return instance;
    }

    private CassandraService(RoutingContext routingContext) {
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

        setCassandraClient(cassandraClient);
    }

    public void indexDocument(String index, UUID id, JsonObject source) {
        String insertStatement = "insert into platform_api_log (id, httpmethod, httppath, httpsession, \"index\", remoteaddress, source, timestamp,username)\n" +
                "values (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        //getCassandraClient()
        cassandraClient
                .rxConnect("verapi_analytics_dev")
                .andThen(Single.just(getCassandraClient()))
                .flatMap(cassandraClient1 -> cassandraClient1.rxPrepare(insertStatement))
                //.andThen(getCassandraClient().rxPrepare(insertStatement))
                .flatMap(preparedStatement -> getCassandraClient()
                        .rxExecute(preparedStatement.bind(
                                id
                                , getRoutingContext().request().method().toString()
                                , getRoutingContext().request().path()
                                , getRoutingContext().session().id()
                                , index
                                , getRoutingContext().request().remoteAddress().host()
                                , source.encode()
                                , Instant.now()
                                , getRoutingContext().user().principal().getString("username"))))
                .doFinally(() -> getCassandraClient().rxDisconnect())
                .subscribe(o -> {
                            logger.info("successfully inserted into Cassandra");
                        }
                        , throwable -> {
                            logger.error("unable to insert into Cassandra: {}", throwable.getLocalizedMessage());
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
