package com.verapi.portal.service.idam;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.abyss.common.Config;
import com.verapi.abyss.common.Constants;
import com.verapi.portal.service.AbstractServiceOld;
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
public class SubjectIndexService extends AbstractServiceOld<JsonObject> {

    private static Logger logger = LoggerFactory.getLogger(SubjectIndexService.class);

    public SubjectIndexService(Vertx vertx, AbyssJDBCService abyssJDBCService) throws Exception {
        super(vertx, abyssJDBCService);
        logger.trace("SubjectIndexService() invoked " + vertx + abyssJDBCService);
    }

    public SubjectIndexService(Vertx vertx) throws Exception {
        super(vertx);
        logger.trace("SubjectIndexService() invoked " + vertx);
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
        logger.trace("SubjectIndexService findAll() invoked " + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FIND_BY_SUBJECTNAME, new JsonArray().add("faik"))) //TODO: GET USER
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.trace("SubjectIndexService findAll() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.trace("SubjectIndexService findAll() # of records : 0");
                                return Single.just(resultSet);//return Single.error(new Exception("ApiService findAll() # of records : 0"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> findBySubjectUuid(String subjectUuid) {
        logger.trace("SubjectIndexService findBySubjectUuid() invoked " + jdbcClient);
        return jdbcClient
                .rxGetConnection().flatMap(conn -> conn
                        .setQueryTimeout(Config.getInstance().getConfigJsonObject().getInteger(Constants.PORTAL_DBQUERY_TIMEOUT))
                        // Disable auto commit to handle transaction manually
                        .rxSetAutoCommit(false)
                        // Switch from Completable to default Single value
                        .toSingleDefault(false)
                        //Check if user already exists
                        .flatMap(conn1 -> conn.rxQueryWithParams(SQL_FIND_BY_UUID, new JsonArray().add(subjectUuid)))
                        .flatMap(resultSet -> {
                            if (resultSet.getNumRows() > 0) {
                                logger.trace("SubjectIndexService filterBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.trace("SubjectIndexService filterBySubjectName() # of records : 0");
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


    private static final String SQL_FIND_ALL_COMPACT_JSON = "";

    private static final String SQL_FIND_BY_SUBJECTNAME = "select json_build_object(\n" +
            "\t'user', row_to_json(subj) \n" +
            "\t,\n" +
            "\t'myApiStateList', (select json_agg(row_to_json(myApiStateList)) from \n" +
            "\t(select st.id, st.uuid, st.\"name\", st.description, count(my_apis.subjectid) as \"count\" \n" +
            "\t  from api_state st left outer join \n" +
            "\t       (select a.id, a.subjectid, a.apistateid from api a where a.subjectid = subj.id) as my_apis \n" + //TODO: remove a.subject_id
            "\t       on (st.id = my_apis.api_state_id)  \n" +
            "\t  group by st.id, st.uuid, st.\"name\", st.description) as myApiStateList)\n" +
            "\t,\n" +
            "\t'myApiVisibilityList', (select json_agg(row_to_json(myApiVisibilityList)) from \n" +
            "\t(select vt.id, vt.uuid, vt.\"name\", count(my_apis.subjectid) as \"count\" \n" +
            "\t  from api_visibility_type vt left outer join \n" +
            "\t       (select a.id, a.subjectid, a.apivisibilityid from api a where a.subjectid = subj.id) as my_apis\n" + //TODO: remove a.subject_id
            "\t       on (vt.id = my_apis.apivisibilityid)\n" +
            "\t  group by vt.id, vt.uuid, vt.\"name\") as myApiVisibilityList)\n" +
            "\t,\n" +
            "\t'myApiTagList', COALESCE((select json_agg(row_to_json(myApiTagList)) from \n" +
            "\t(select t.uuid, t.\"name\", count(*) as \"count\" from api_tag t, api a, api__api_tag axt \n" +
            "\t  where t.uuid = axt.apitagid and axt.apiid = a.uuid and a.subjectid = subj.id \n" +
            "\t  group by t.uuid, t.\"name\") as myApiTagList), '[]')\n" +
            "\t,  \n" +
            "\t'myApiGroupList', COALESCE((select json_agg(row_to_json(myApiGroupList)) from \n" +
            "\t(select g.uuid, g.\"name\", count(*) as \"count\" from api_group g, api a, api__api_group axg \n" +
            "\t  where g.uuid = axg.apigroupid and axg.apiid = a.uuid and a.subjectid = subj.id \n" +
            "\t  group by g.uuid, g.\"name\") as myApiGroupList), '[]')\n" +
            "\t,  \n" +
            "\t'myApiCategoryList', COALESCE((select json_agg(row_to_json(myApiCategoryList)) from \n" +
            "\t(select c.uuid, c.\"name\", count(*) as \"count\" from api_category c, api a, api__api_category axc \n" +
            "\t  where c.uuid = axc.apicategoryid and axc.apiid = a.uuid and a.subjectid = subj.id \n" +
            "\t  group by c.uuid, c.\"name\") as myApiCategoryList), '[]')\n" +
            ") as result\n" +
            "from (\n" +
            "select id, \n" +
            "  uuid,\n" +
            "  organizationid,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
            "  picture,\n" +
            "  totallogincount,\n" +
            "  failedlogincount,\n" +
            "  invalidpasswordattemptcount,\n" +
            "  ispasswordchangerequired,\n" +
            "  passwordexpires_at,  \n" +
            "  lastloginat,\n" +
            "  lastfailedlogin_at,\n" +
            "  json_build_object('darkSidebar', false) as settings\n" +
            "  --json_array_elements('[]') as notifications \n" +
            "from subject\n" +
            "where lower(subjectname) = lower(?)\n" +
            "order by subjectname\n" +
            ") as subj;";

    private static final String SQL_FIND_BY_UUID = "select json_build_object(\n" +
            "\t'user', row_to_json(subj) \n" +
            "\t,\n" +
            "\t'myApiStateList', (select json_agg(row_to_json(myApiStateList)) from \n" +
            "\t(select st.id, st.uuid, st.\"name\", st.description, count(my_apis.subjectid) as \"count\" \n" +
            "\t  from api_state st left outer join \n" +
            "\t       (select a.id, a.subjectid, a.apistateid from api a where a.subjectid = subj.id and a.isproxyapi = false and openapidocument ?? 'servers') as my_apis \n" + //TODO: remove a.subject_id
            "\t       on (st.id = my_apis.apistateid)  \n" +
            "\t  group by st.id, st.uuid, st.\"name\", st.description) as myApiStateList)\n" +
            "\t,\n" +
            "\t'myApiVisibilityList', (select json_agg(row_to_json(myApiVisibilityList)) from \n" +
            "\t(select vt.id, vt.uuid, vt.\"name\", count(my_apis.subjectid) as \"count\" \n" +
            "\t  from api_visibility_type vt left outer join \n" +
            "\t       (select a.id, a.subjectid, a.apivisibilityid from api a where a.subjectid = subj.id and a.isproxyapi = false and openapidocument ?? 'servers') as my_apis\n" + //TODO: remove a.subject_id
            "\t       on (vt.id = my_apis.apivisibilityid)\n" +
            "\t  group by vt.id, vt.uuid, vt.\"name\") as myApiVisibilityList)\n" +
            "\t,\n" +
            "\t'myApiTagList', COALESCE((select json_agg(row_to_json(myApiTagList)) from \n" +
            "\t(select t.uuid, t.\"name\", count(*) as \"count\" from api_tag t, api a, api__api_tag axt \n" +
            "\t  where t.uuid = axt.apitagid and axt.apiid = a.uuid and a.subjectid = subj.id and a.isproxyapi = false and openapidocument ?? 'servers'\n" +
            "\t  group by t.uuid, t.\"name\") as myApiTagList), '[]')\n" +
            "\t,  \n" +
            "\t'myApiGroupList', COALESCE((select json_agg(row_to_json(myApiGroupList)) from \n" +
            "\t(select g.uuid, g.\"name\", count(*) as \"count\" from api_group g, api a, api__api_group axg \n" +
            "\t  where g.uuid = axg.apigroupid and axg.apiid = a.uuid and a.subjectid = subj.id and a.isproxyapi = false and openapidocument ?? 'servers'\n" +
            "\t  group by g.uuid, g.\"name\") as myApiGroupList), '[]')\n" +
            "\t,  \n" +
            "\t'myApiCategoryList', COALESCE((select json_agg(row_to_json(myApiCategoryList)) from \n" +
            "\t(select c.uuid, c.\"name\", count(*) as \"count\" from api_category c, api a, api__api_category axc \n" +
            "\t  where c.uuid = axc.apicategoryid and axc.apiid = a.uuid and a.subjectid = subj.id and a.isproxyapi = false and openapidocument ?? 'servers'\n" +
            "\t  group by c.uuid, c.\"name\") as myApiCategoryList), '[]')\n" +
            ") as result\n" +
            "from (\n" +
            "select id, \n" +
            "  uuid,\n" +
            "  organizationid,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
            "  picture,\n" +
            "  totallogincount,\n" +
            "  failedlogincount,\n" +
            "  invalidpasswordattemptcount,\n" +
            "  ispasswordchangerequired,\n" +
            "  passwordexpiresat,  \n" +
            "  lastloginat,\n" +
            "  lastfailedloginat,\n" +
            "  json_build_object('darkSidebar', false) as settings\n" +
            "  --json_array_elements('[]') as notifications \n" +
            "from subject\n" +
            //"where lower(subjectname) = lower(?)\n" +
            "where uuid = CAST(? as uuid)\n" +
            //"order by subjectname\n" +
            ") as subj;";


}