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

package com.verapi.portal.oapi;

import io.vertx.ext.sql.ResultSet;
import io.vertx.ext.sql.UpdateResult;

public class CompositeResult {

    private ResultSet resultSet;
    private UpdateResult updateResult;
    private Throwable throwable;

    public CompositeResult(ResultSet resultSet) {
        this.resultSet = resultSet;
    }

    public CompositeResult(UpdateResult updateResult) {
        this.updateResult = updateResult;
    }

    public CompositeResult(Throwable throwable) {
        this.throwable = throwable;
    }

    public ResultSet getResultSet() {
        return resultSet;
    }

    public CompositeResult setResultSet(ResultSet resultSet) {
        this.resultSet = resultSet;
        return this;
    }

    public UpdateResult getUpdateResult() {
        return updateResult;
    }

    public CompositeResult setUpdateResult(UpdateResult updateResult) {
        this.updateResult = updateResult;
        return this;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public CompositeResult setThrowable(Throwable throwable) {
        this.throwable = throwable;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CompositeResult)) return false;

        CompositeResult that = (CompositeResult) o;

        if (getUpdateResult() != null ? !getUpdateResult().equals(that.getUpdateResult()) : that.getUpdateResult() != null)
            return false;
        return getThrowable() != null ? getThrowable().equals(that.getThrowable()) : that.getThrowable() == null;
    }

    @Override
    public int hashCode() {
        int result = getUpdateResult() != null ? getUpdateResult().hashCode() : 0;
        result = 31 * result + (getThrowable() != null ? getThrowable().hashCode() : 0);
        return result;
    }
}
