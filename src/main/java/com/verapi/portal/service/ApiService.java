package com.verapi.portal.service;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class ApiService extends AbstractService<JsonObject> {

    private static Logger logger = LoggerFactory.getLogger(ApiService.class);

    public ApiService(Vertx vertx, AbyssJDBCService abyssJDBCService) throws Exception {
        super(vertx, abyssJDBCService);
        logger.info("ApiService() invoked " + vertx + abyssJDBCService);
    }

    public ApiService(Vertx vertx) throws Exception {
        super(vertx);
        logger.info("ApiService() invoked " + vertx);
    }


    @Override
    public Single insert(JsonObject o) {
        return null;
    }

    @Override
    public Maybe findById(long id) {
        return null;
    }

    @Override
    public Maybe findById(UUID uuid) {
        return null;
    }

    @Override
    public Single<ResultSet> findAll() {
        logger.info("ApiService findAll() invoked " + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_COMPACT))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("ApiService findAll() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.info("ApiService findAll() # of records : 0");
                                return Single.just(resultSet);//return Single.error(new Exception("ApiService findAll() # of records : 0"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterBySubjectName(String subjectName) {
        logger.info("ApiService filterBySubjectName() invoked" + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FILTER_BY_SUBJECTNAME, new JsonArray().add(subjectName+"%")))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.info("ApiService filterBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.info("ApiService filterBySubjectName() # of records : 0");
                                return Single.just(resultSet);
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }


    @Override
    public Maybe update(long id, JsonObject newT) {
        return null;
    }

    @Override
    public Completable delete(long id) {
        return null;
    }

    @Override
    public Completable deleteAll() {
        return null;
    }


    private static final String SQL_FIND_ALL_COMPACT = "SELECT " +
            "uuid," +
            //"organization_id," +
            "created," +
            "updated," +
            "deleted," +
            "is_deleted," +
            //"crud_subject_id," +
            //"subject_id," +
            "is_proxy_api," +
            "api_state_id," +
            "api_visibility_id," +
            "language_name," +
            "language_version," +
            "data_format," +
            //"raw_text," +
            "json_text," +
            "to_json(json_text) as jayson," +
            //"jsonb_pretty(json_text)," +
            "business_api_id," +
            "image," +
            "color," +
            "deployed," +
            "change_log " +
            "FROM portalschema.api " +
            "WHERE json_text ?? 'servers' " +
            "ORDER BY json_text -> 'info' -> 'title'" +
            ";";

    private static final String SQL_FILTER_BY_SUBJECTNAME = "SELECT " +
            "uuid," +
            //"organization_id," +
            "created," +
            "updated," +
            "deleted," +
            "is_deleted," +
            //"crud_subject_id," +
            //"subject_id," +
            "is_proxy_api," +
            "api_state_id," +
            "api_visibility_id," +
            "language_name," +
            "language_version," +
            "data_format," +
            //"raw_text," +
            "json_text," +
            "to_json(json_text) as jayson," +
            "business_api_id," +
            "image," +
            "color," +
            "deployed," +
            "change_log " +
            "FROM portalschema.api" +
            "WHERE json_text ?? 'servers' " +
            "AND subject_id = (SELECT id FROM subject WHERE lower(subject_name) like lower(?)) " +
            "ORDER BY json_text -> 'info' -> 'title'" +
            ";";

}