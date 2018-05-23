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

import java.util.ArrayList;
import java.util.UUID;

public interface IService {

    Single<JDBCClient> initJDBCClient();

    Single<UpdateResult> insert(final JsonArray insertParams, final String insertQuery);

    Single<UpdateResult> update(final UUID uuid, final JsonArray updateParams, final String updateQuery);

    Single<UpdateResult> updateAll(final ArrayList<UUID> uuid, JsonObject updateRecord);

    Single<UpdateResult> delete(final UUID uuid, final String deleteQuery);

    Single<UpdateResult> deleteAll(final String deleteAllQuery);

    Single<ResultSet> findById(final long id, final String findByIdQuery);

    Single<ResultSet> findById(final UUID uuid, final String findByIdQuery);

    Single<ResultSet> findByName(final String name, final String findByNameQuery);

    Single<ResultSet> findAll(final String findAllQuery);

}
