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

package com.verapi.portal.service;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.reactivex.ext.jdbc.JDBCClient;

import java.util.UUID;

public interface IService<T> {

    Single<JDBCClient> initJDBCClient();

    Single<UpdateResult> insert(JsonObject newRecord);

    Single<UpdateResult> update(UUID uuid, JsonObject updateRecord);

    Single<JsonArray> updateAll(JsonObject updateRecord);

    Single<UpdateResult> delete(UUID uuid);

    Single<UpdateResult> deleteAll();

    Single<ResultSet> findById(long id);

    Single<ResultSet> findById(UUID uuid);

    Single<ResultSet> findByName(String name);

    Single<ResultSet> findAll();

}
