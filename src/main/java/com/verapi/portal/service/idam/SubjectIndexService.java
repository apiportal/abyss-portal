package com.verapi.portal.service.idam;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.service.AbstractService;
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

public class SubjectIndexService extends AbstractService<JsonObject> {

    private static Logger logger = LoggerFactory.getLogger(SubjectIndexService.class);

    public SubjectIndexService(Vertx vertx, AbyssJDBCService abyssJDBCService) throws Exception {
        super(vertx, abyssJDBCService);
        logger.info("SubjectIndexService() invoked " + vertx + abyssJDBCService);
    }

    public SubjectIndexService(Vertx vertx) throws Exception {
        super(vertx);
        logger.info("SubjectIndexService() invoked " + vertx);
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
        logger.info("SubjectIndexService findAll() invoked " + jdbcClient);
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
                                logger.info("SubjectIndexService findAll() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.info("SubjectIndexService findAll() # of records : 0");
                                return Single.just(resultSet);//return Single.error(new Exception("ApiService findAll() # of records : 0"));
                            }
                        })
                        // close the connection regardless succeeded or failed
                        .doAfterTerminate(conn::close)
                );
    }

    public Single<ResultSet> findBySubjectUuid(String subjectUuid) {
        logger.info("SubjectIndexService filterBySubjectName() invoked" + jdbcClient);
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
                                logger.info("SubjectIndexService filterBySubjectName() # of records :[" + resultSet.getNumRows() + "]");
                                return Single.just(resultSet);
                            } else {
                                logger.info("SubjectIndexService filterBySubjectName() # of records : 0");
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
            "\t(select st.id, st.uuid, st.\"name\", st.description,count(*) as \"count\" from api_state st, api a \n" +
            "\t  where st.id = a.api_state_id and a.subject_id = subj.id--:subjectid \n" +
            "\t  group by st.id, st.uuid, st.\"name\", st.description) as myApiStateList)\n" +
            "\t,\n" +
            "\t'myApiVisibilityList', (select json_agg(row_to_json(myApiVisibilityList)) from \n" +
            "\t(select vt.id, vt.uuid, vt.\"name\", count(*) as \"count\" from visibility_type vt, api a \n" +
            "\t  where vt.id = a.api_visibility_id and a.subject_id = subj.id \n" +
            "\t  group by vt.id, vt.uuid, vt.\"name\") as myApiVisibilityList)\n" +
            "\t,\n" +
            "\t'myApiTagList', (select json_agg(row_to_json(myApiTagList)) from \n" +
            "\t(select t.uuid, t.\"name\", count(*) as \"count\" from api_tag t, api a, api__api_tag axt \n" +
            "\t  where t.id = axt.api_tag_id and axt.api_id = a.id and a.subject_id = subj.id \n" +
            "\t  group by t.uuid, t.\"name\") as myApiTagList)\n" +
            "\t,  \n" +
            "\t'myApiGroupList', (select json_agg(row_to_json(myApiGroupList)) from \n" +
            "\t(select g.uuid, g.\"name\", count(*) as \"count\" from api_group g, api a, api__api_group axg \n" +
            "\t  where g.id = axg.api_group_id and axg.api_id = a.id and a.subject_id = subj.id \n" +
            "\t  group by g.uuid, g.\"name\") as myApiGroupList)\n" +
            "\t,  \n" +
            "\t'myApiCategoryList', (select json_agg(row_to_json(myApiCategoryList)) from \n" +
            "\t(select c.uuid, c.\"name\", count(*) as \"count\" from api_category c, api a, api__api_category axc \n" +
            "\t  where c.id = axc.api_category_id and axc.api_id = a.id and a.subject_id = subj.id \n" +
            "\t  group by c.uuid, c.\"name\") as myApiCategoryList)\n" +
            ") as result\n" +
            "from (\n" +
            "select id, \n" +
            "  uuid,\n" +
            "  organization_id,\n" +
            "  subject_name,\n" +
            "  first_name,\n" +
            "  last_name,\n" +
            "  display_name,\n" +
            "  email,\n" +
            "  picture,\n" +
            "  total_login_count,\n" +
            "  failed_login_count,\n" +
            "  invalid_password_attempt_count,\n" +
            "  is_password_change_required,\n" +
            "  password_expires_at,  \n" +
            "  last_login_at,\n" +
            "  last_failed_login_at,\n" +
            "  json_build_object('darkSidebar', false) as settings\n" +
            "  --json_array_elements('[]') as notifications \n" +
            "from subject\n" +
            "where lower(subject_name) = lower(?)\n" +
            "order by subject_name\n" +
            ") as subj;";

    private static final String SQL_FIND_BY_UUID = "select json_build_object(\n" +
            "\t'user', row_to_json(subj) \n" +
            "\t,\n" +
            "\t'myApiStateList', (select json_agg(row_to_json(myApiStateList)) from \n" +
            "\t(select st.uuid, st.\"name\", st.description,count(*) as \"count\" from api_state st, api a \n" +
            "\t  where st.id = a.api_state_id and a.subject_id = subj.id--:subjectid \n" +
            "\t  group by st.uuid, st.\"name\", st.description) as myApiStateList)\n" +
            "\t,\n" +
            "\t'myApiVisibilityList', (select json_agg(row_to_json(myApiVisibilityList)) from \n" +
            "\t(select vt.uuid, vt.\"name\", count(*) as \"count\" from visibility_type vt, api a \n" +
            "\t  where vt.id = a.api_visibility_id and a.subject_id = subj.id \n" +
            "\t  group by vt.uuid, vt.\"name\") as myApiVisibilityList)\n" +
            "\t,\n" +
            "\t'myApiTagList', (select json_agg(row_to_json(myApiTagList)) from \n" +
            "\t(select t.uuid, t.\"name\", count(*) as \"count\" from api_tag t, api a, api__api_tag axt \n" +
            "\t  where t.id = axt.api_tag_id and axt.api_id = a.id and a.subject_id = subj.id \n" +
            "\t  group by t.uuid, t.\"name\") as myApiTagList)\n" +
            "\t,  \n" +
            "\t'myApiGroupList', (select json_agg(row_to_json(myApiGroupList)) from \n" +
            "\t(select g.uuid, g.\"name\", count(*) as \"count\" from api_group g, api a, api__api_group axg \n" +
            "\t  where g.id = axg.api_group_id and axg.api_id = a.id and a.subject_id = subj.id \n" +
            "\t  group by g.uuid, g.\"name\") as myApiGroupList)\n" +
            "\t,  \n" +
            "\t'myApiCategoryList', (select json_agg(row_to_json(myApiCategoryList)) from \n" +
            "\t(select c.uuid, c.\"name\", count(*) as \"count\" from api_category c, api a, api__api_category axc \n" +
            "\t  where c.id = axc.api_category_id and axc.api_id = a.id and a.subject_id = subj.id \n" +
            "\t  group by c.uuid, c.\"name\") as myApiCategoryList)\n" +
            ") as result\n" +
            "from (\n" +
            "select id, \n" +
            "  uuid,\n" +
            "  organization_id,\n" +
            "  subject_name,\n" +
            "  first_name,\n" +
            "  last_name,\n" +
            "  display_name,\n" +
            "  email,\n" +
            "  picture,\n" +
            "  total_login_count,\n" +
            "  failed_login_count,\n" +
            "  invalid_password_attempt_count,\n" +
            "  is_password_change_required,\n" +
            "  password_expires_at,  \n" +
            "  last_login_at,\n" +
            "  last_failed_login_at,\n" +
            "  json_build_object('darkSidebar', false) as settings\n" +
            "  --json_array_elements('[]') as notifications \n" +
            "from subject\n" +
            //"where lower(subject_name) = lower(?)\n" +
            "where uuid = ?\n" +
            //"order by subject_name\n" +
            ") as subj;";


}