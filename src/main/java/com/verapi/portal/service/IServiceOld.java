/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 4 2018
 *
 */

package com.verapi.portal.service;

import io.reactivex.Completable;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vertx.ext.sql.ResultSet;
import io.vertx.reactivex.ext.jdbc.JDBCClient;

import java.util.List;
import java.util.UUID;

@Deprecated
public interface IServiceOld<T> {

    Single<JDBCClient> initJDBCClient();

    Single<T> insert(T t);

    Maybe<T> findById(final long id);

    Maybe<T> findById(final UUID uuid);

    //Single<List<T>> findAll();
    Single<ResultSet> findAll();

    Maybe<T> update(final long id, T newT);

    Completable delete(final long id);

    Completable deleteAll();

}
