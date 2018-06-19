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

@Deprecated
public class ApiService extends AbstractServiceOld<JsonObject> {

    private static Logger logger = LoggerFactory.getLogger(ApiService.class);

    public ApiService(Vertx vertx, AbyssJDBCService abyssJDBCService) throws Exception {
        super(vertx, abyssJDBCService);
        logger.trace("ApiService() invoked " + vertx + abyssJDBCService);
    }

    public ApiService(Vertx vertx) throws Exception {
        super(vertx);
        logger.trace("ApiService() invoked " + vertx);
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
        logger.trace("ApiService findAll() invoked " + jdbcClient);
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
                                logger.trace("ApiService findAll() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.trace("ApiService findAll() # of records : 0");
                                return Single.just(resultSet);//return Single.error(new Exception("ApiService findAll() # of records : 0"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> filterBySubjectName(String subjectName) {
        logger.trace("ApiService filterBySubjectName() invoked" + jdbcClient);
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
                                logger.trace("ApiService filterBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.trace("ApiService filterBySubjectName() # of records : 0");
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


/*    private static final String SQL_FIND_ALL_COMPACT =
    "SELECT row_to_json(jayson) from (" +
            "SELECT " +
            "uuid," +
            //"organization_id," +
            "created," +
            "updated," +
            "deleted," +
            "isdeleted," +
            //"crud_subject_id," +
            //"subject_id," +
            "is_proxy_api," +
            "api_state_id," +
            "api_visibility_id," +
            "language_name," +
            "language_version," +
            "data_format," +
            "openapi_document," +
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
            "WHERE openapi_document ?? 'servers' " +
            "ORDER BY openapi_document -> 'info' -> 'title'" +
            ") as jayson;";*/

    private static final String SQL_FILTER_BY_SUBJECTNAME = "SELECT " +
            "uuid," +
            //"organizationid," +
            "created," +
            "updated," +
            "deleted," +
            "isdeleted," +
            //"crudsubjectid," +
            //"subjectid," +
            "isproxyapi," +
            "apistateid," +
            "apivisibilityid," +
            "languagename," +
            "languageversion," +
            "dataformat," +
            "openapidocument," +
            "businessapiid," +
            "image," +
            "color," +
            "deployed," +
            "changelog, " +
            "(SELECT json_agg(json_build_object('uuid', t.uuid,'name', t.\"name\"))" +
            " FROM api_tag t join api__api_tag axt on t.uuid = axt.apitagid" +
            " WHERE axt.apiid = a.uuid) as tags," +
            "(SELECT json_agg(json_build_object('uuid', g.uuid,'name', g.\"name\"))" +
            " FROM api_group g join api__api_group axg on g.uuid = axg.apigroupid" +
            " WHERE axg.apiid = a.uuid) as groups," +
            "(SELECT json_agg(json_build_object('uuid', c.uuid,'name', c.\"name\"))" +
            " FROM api_category c join api__api_category axc on c.uuid = axc.apicategoryid" +
            " WHERE axc.apiid = a.uuid) as categories " +
            "FROM portalschema.api a " +
            "WHERE openapidocument ?? 'servers' " +
            "  AND isproxyapi = false\n" +
            "AND subjectid = (SELECT id FROM subject WHERE lower(subjectname) like lower(?)) " +
            "ORDER BY openapidocument -> 'info' -> 'title'" +
            ";";


    private static final String SQL_FIND_ALL_COMPACT_JSON = "select row_to_json(jayson) rowjson, to_json(openapidocument) openapi\n" +
            "from (\n" +
            "       select\n" +
            "         uuid,\n" +
            "         organizationid,\n" +
            "         created,\n" +
            "         updated,\n" +
            "         deleted,\n" +
            "         isdeleted,\n" +
            "         crudsubjectid,\n" +
            "         subjectid,\n" +
            "         isproxyapi,\n" +
            "         apistateid,\n" +
            "         apivisibilityid,\n" +
            "         languagename,\n" +
            "         languageversion,\n" +
            "         dataformat,\n" +
            "         originaldocument,\n" +
            "         openapidocument,\n" +
            "         businessapiid,\n" +
            "         image,\n" +
            "         color,\n" +
            "         deployed,\n" +
            "         changelog,\n" +
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
            "                                     t.uuid = axt.apitagid\n" +
            "           where\n" +
            "             axt.apiid = a.uuid\n" +
            "         ) as tags,\n" +
            "         (\n" +
            "           select json_agg(json_build_object('uuid', g.uuid, 'name', g.\"name\"))\n" +
            "           from\n" +
            "             api_group g\n" +
            "             join api__api_group axg on g.uuid = axg.apigroupid\n" +
            "           where\n" +
            "             axg.apiid = a.uuid\n" +
            "         ) as groups,\n" +
            "         (\n" +
            "           select json_agg(json_build_object('uuid', c.uuid, 'name', c.\"name\"))\n" +
            "           from\n" +
            "             api_category c\n" +
            "             join api__api_category axc on c.uuid = axc.apicategoryid\n" +
            "           where\n" +
            "             axc.apiid = a.uuid\n" +
            "         ) as categories\n" +
            "       from api a\n" +
            "       where openapidocument ?? 'servers'\n" +
            "         and isproxyapi = false\n" +
            "       order by openapidocument -> 'info' -> 'title'\n" +
            "     ) as jayson;\n";


}