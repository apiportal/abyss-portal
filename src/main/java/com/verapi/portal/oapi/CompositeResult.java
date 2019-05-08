/*
 * Copyright 2019 Verapi Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
