/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.service.idam;

import com.verapi.portal.common.AbyssJDBCService;
import com.verapi.portal.common.Config;
import com.verapi.portal.common.Constants;
import com.verapi.portal.service.AbstractService;
import io.reactivex.Single;
import io.reactivex.exceptions.CompositeException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class SubjectService extends AbstractService {

    private static Logger logger = LoggerFactory.getLogger(SubjectService.class);

    public SubjectService(Vertx vertx, AbyssJDBCService abyssJDBCService) {
        super(vertx, abyssJDBCService);
    }

    public SubjectService(Vertx vertx) {
        super(vertx);
    }

    public Single<UpdateResult> insert(JsonObject newRecord) {
        JsonArray insertParams = new JsonArray()
                .add(((Number) newRecord.getValue("organizationid")).longValue())
                .add(((Number) newRecord.getValue("crudsubjectid")).longValue())
                .add(((Number) newRecord.getValue("subjecttypeid")).longValue())
                .add(((String) newRecord.getValue("subjectname")))
                .add(((String) newRecord.getValue("firstname")))
                .add(((String) newRecord.getValue("lastname")))
                .add(((String) newRecord.getValue("displayname")))
                .add(((String) newRecord.getValue("email")))
                .add(((String) newRecord.getValue("secondaryemail")))
                .add((newRecord.getInstant("effectivestartdate")))
                .add((newRecord.getInstant("effectiveenddate")))
                .add(((String) newRecord.getValue("password")))
                .add(((String) newRecord.getValue("passwordsalt")))
                .add(((String) newRecord.getValue("picture")))
                .add(((Number) newRecord.getValue("subjectdirectoryid")).longValue());
        return insert(insertParams, SQL_INSERT);
    }

    public Single<UpdateResult> update(UUID uuid, JsonObject updateRecord) {
        JsonArray updateParams = new JsonArray()
                .add(((Number) updateRecord.getValue("organizationid")).longValue())
                .add(((Number) updateRecord.getValue("crudsubjectid")).longValue())
                .add(((Number) updateRecord.getValue("subjecttypeid")).longValue())
                .add(((String) updateRecord.getValue("subjectname")))
                .add(((String) updateRecord.getValue("firstname")))
                .add(((String) updateRecord.getValue("lastname")))
                .add(((String) updateRecord.getValue("displayname")))
                .add(((String) updateRecord.getValue("email")))
                .add(((String) updateRecord.getValue("secondaryemail")))
                .add((updateRecord.getInstant("effectivestartdate")))
                .add((updateRecord.getInstant("effectiveenddate")))
                .add(((String) updateRecord.getValue("picture")))
                .add(((Number) updateRecord.getValue("subjectdirectoryid")).longValue())
                .add(uuid);
        return update(uuid, updateParams, SQL_UPDATE_BY_UUID);
    }

    public Single<UpdateResult> updateAll(ArrayList<UUID> uuid, JsonObject updateRecord) {
        return updateAll(uuid, updateRecord);
    }

    public Single<UpdateResult> delete(UUID uuid) {
        return delete(uuid, SQL_DELETE_BY_UUID);
    }

    public Single<UpdateResult> deleteAll() {
        return deleteAll(SQL_DELETE);
    }

    public Single<ResultSet> findById(long id) {
        return findById(id, SQL_FIND_BY_ID);
    }

    public Single<ResultSet> findById(UUID uuid) {
        return findById(uuid, SQL_FIND_BY_UUID);
    }

    public Single<ResultSet> findByName(String name) {
        return findByName(name, SQL_FIND_BY_NAME);
    }

    public Single<ResultSet> findAll() {
        return findAll(SQL_SELECT);
    }

    private static final String SQL_INSERT = "insert into subject (organizationid, crudsubjectid, subjecttypeid, subjectname, firstname, lastname, displayname, email, secondaryemail, effectivestartdate, effectiveenddate, password, passwordsalt, picture, subjectdirectoryid)\n" +
            "values (?, ?, ?, ?, ?, ?, ?, ?, ?, coalesce(?, now()), ?, ?, ?, ?, ?)";

    private static final String SQL_DELETE = "update subject set isdeleted = true\n";

    private static final String SQL_SELECT = "select\n" +
            "  uuid,\n" +
            "  organizationid,\n" +
            "  created,\n" +
            "  updated,\n" +
            "  deleted,\n" +
            "  isdeleted,\n" +
            "  crudsubjectid,\n" +
            "  isactivated,\n" +
            "  subjecttypeid,\n" +
            "  subjectname,\n" +
            "  firstname,\n" +
            "  lastname,\n" +
            "  displayname,\n" +
            "  email,\n" +
            "  secondaryemail,\n" +
            "  effectivestartdate,\n" +
            "  effectiveenddate,\n" +
            "  picture,\n" +
            "  totallogincount,\n" +
            "  failedlogincount,\n" +
            "  invalidpasswordattemptcount,\n" +
            "  ispasswordchangerequired,\n" +
            "  passwordexpiresat,\n" +
            "  lastloginat,\n" +
            "  lastpasswordchangeat,\n" +
            "  lastauthenticatedat,\n" +
            "  lastfailedloginat,\n" +
            "  subjectdirectoryid\n" +
            "from subject\n";

    private static final String SQL_UPDATE = "UPDATE subject\n" +
            "SET\n" +
            "  organizationid       = ?\n" +
            "  , crudsubjectid      = ?\n" +
            "  , subjecttypeid      = ?\n" +
            "  , subjectname        = ?\n" +
            "  , firstname          = ?\n" +
            "  , lastname           = ?\n" +
            "  , displayname        = ?\n" +
            "  , email              = ?\n" +
            "  , secondaryemail     = ?\n" +
            "  , effectivestartdate = ?\n" +
            "  , effectiveenddate   = ?\n" +
            "  , picture            = ?\n" +
            "  , subjectdirectoryid = ?\n";

    private static final String SQL_WHERE = "where id = ?\n";

    private static final String SQL_WHERE_UUID_IS = "where uuid = ?\n";

    private static final String SQL_WHERE_NAME_IS = "where lower(subjectname) = lower(?)";

    private static final String SQL_ORDERBY_NAME = "order by subjectname\n";

    private static final String SQL_FIND_BY_ID = SQL_SELECT + SQL_WHERE;

    private static final String SQL_FIND_BY_UUID = SQL_SELECT + SQL_WHERE_UUID_IS;

    private static final String SQL_FIND_BY_NAME = SQL_SELECT + SQL_WHERE_NAME_IS;

    private static final String SQL_DELETE_BY_UUID = SQL_DELETE + SQL_WHERE_UUID_IS;

    private static final String SQL_UPDATE_BY_UUID = SQL_UPDATE + SQL_WHERE_UUID_IS;

}