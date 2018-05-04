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
                        .flatMap(conn1 -> conn.rxQuery(SQL_FIND_ALL_COMPACT_JSON))
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


    private static final String SQL_FIND_ALL_COMPACT =
    "SELECT row_to_json(jayson) from (" +
            "SELECT " +
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
            "json_text," +
            "business_api_id," +
            "image," +
            "color," +
            "deployed," +
            "change_log, " +
            "(SELECT json_agg(json_build_object('uuid', t.uuid,'name', t.\"name\"))" +
            " FROM api_tag t join api__api_tag axt on t.id = axt.api_tag_id" +
            " WHERE axt.api_id = a.id) as tags," +
            "(SELECT json_agg(json_build_object('uuid', g.uuid,'name', g.\"name\"))" +
            " FROM api_group g join api__api_group axg on g.id = axg.api_group_id" +
            " WHERE axg.api_id = a.id) as groups," +
            "(SELECT json_agg(json_build_object('uuid', c.uuid,'name', c.\"name\"))" +
            " FROM api_category c join api__api_category axc on c.id = axc.api_category_id" +
            " WHERE axc.api_id = a.id) as categories " +
            "FROM portalschema.api a " +
            "WHERE json_text ?? 'servers' " +
            "ORDER BY json_text -> 'info' -> 'title'" +
            ") as jayson;";

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
            "json_text," +
            "business_api_id," +
            "image," +
            "color," +
            "deployed," +
            "change_log, " +
            "(SELECT json_agg(json_build_object('uuid', t.uuid,'name', t.\"name\"))" +
            " FROM api_tag t join api__api_tag axt on t.id = axt.api_tag_id" +
            " WHERE axt.api_id = a.id) as tags," +
            "(SELECT json_agg(json_build_object('uuid', g.uuid,'name', g.\"name\"))" +
            " FROM api_group g join api__api_group axg on g.id = axg.api_group_id" +
            " WHERE axg.api_id = a.id) as groups," +
            "(SELECT json_agg(json_build_object('uuid', c.uuid,'name', c.\"name\"))" +
            " FROM api_category c join api__api_category axc on c.id = axc.api_category_id" +
            " WHERE axc.api_id = a.id) as categories " +
            "FROM portalschema.api a " +
            "WHERE json_text ?? 'servers' " +
            "AND subject_id = (SELECT id FROM subject WHERE lower(subject_name) like lower(?)) " +
            "ORDER BY json_text -> 'info' -> 'title'" +
            ";";


    private static final String SQL_FIND_ALL_COMPACT_JSON = "select row_to_json(jayson) rowjson, to_json(json_text) openapi\n" +
            "from (\n" +
            "       select\n" +
            "         uuid,\n" +
            "         organization_id,\n" +
            "         created,\n" +
            "         updated,\n" +
            "         deleted,\n" +
            "         is_deleted,\n" +
            "         crud_subject_id,\n" +
            "         subject_id,\n" +
            "         is_proxy_api,\n" +
            "         api_state_id,\n" +
            "         api_visibility_id,\n" +
            "         language_name,\n" +
            "         language_version,\n" +
            "         data_format,\n" +
            "         raw_text,\n" +
            "         json_text,\n" +
            "         --          to_json(json_text) as jayson,\n" +
            "         --          jsonb_pretty(json_text),\n" +
            "         business_api_id,\n" +
            "         image,\n" +
            "         color,\n" +
            "         deployed,\n" +
            "         change_log,\n" +
            "         (\n" +
            "           select json_agg(\n" +
            "               json_build_object(\n" +
            "                   'uuid',\n" +
            "                   t.uuid,\n" +
            "                   'name',\n" +
            "                   t.\"name\"\n" +
            "               )\n" +
            "           )\n" +
            "           from\n" +
            "             api_tag t\n" +
            "             join api__api_tag axt on\n" +
            "                                     t.id = axt.api_tag_id\n" +
            "           where\n" +
            "             axt.api_id = a.id\n" +
            "         ) as tags,\n" +
            "         (\n" +
            "           select json_agg(json_build_object('uuid', g.uuid, 'name', g.\"name\"))\n" +
            "           from\n" +
            "             api_group g\n" +
            "             join api__api_group axg on g.id = axg.api_group_id\n" +
            "           where\n" +
            "             axg.api_id = a.id\n" +
            "         ) as groups,\n" +
            "         (\n" +
            "           select json_agg(json_build_object('uuid', c.uuid, 'name', c.\"name\"))\n" +
            "           from\n" +
            "             api_category c\n" +
            "             join api__api_category axc on c.id = axc.api_category_id\n" +
            "           where\n" +
            "             axc.api_id = a.id\n" +
            "         ) as categories\n" +
            "       from api a\n" +
            "       where json_text ?? 'servers'\n" +
            "       order by json_text -> 'info' -> 'title'\n" +
            "     ) as jayson;\n";


}