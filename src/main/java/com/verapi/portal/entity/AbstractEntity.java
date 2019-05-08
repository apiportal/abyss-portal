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

package com.verapi.portal.entity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public abstract class AbstractEntity<T> implements IEntity<T> {

    private static Logger logger = LoggerFactory.getLogger(AbstractEntity.class);

    protected long id;
    protected String uuid;
    protected long organizationId;
    protected Instant created;
    protected Instant updated;
    protected Instant deleted;
    protected long isDeleted;
    protected long crudSubjectId;

    public AbstractEntity() {
    }

    public AbstractEntity(long id, String uuid, long organizationId, Instant created, Instant updated, Instant deleted, long isDeleted, long crudSubjectId) {
        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.isDeleted = isDeleted;
        this.crudSubjectId = crudSubjectId;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(long organizationId) {
        this.organizationId = organizationId;
    }

    public Instant getCreated() {
        return created;
    }

    public void setCreated(Instant created) {
        this.created = created;
    }

    public Instant getUpdated() {
        return updated;
    }

    public void setUpdated(Instant updated) {
        this.updated = updated;
    }

    public Instant getDeleted() {
        return deleted;
    }

    public void setDeleted(Instant deleted) {
        this.deleted = deleted;
    }

    public long getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(long isDeleted) {
        this.isDeleted = isDeleted;
    }

    public long getCrudSubjectId() {
        return crudSubjectId;
    }

    public void setCrudSubjectId(long crudSubjectId) {
        this.crudSubjectId = crudSubjectId;
    }

    public <TT> TT nvl(TT value, TT defaultValue) {
        return value == null ? defaultValue : value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AbstractEntity)) return false;

        AbstractEntity<?> that = (AbstractEntity<?>) o;

        if (getId() != that.getId()) return false;
        if (getOrganizationId() != that.getOrganizationId()) return false;
        if (getIsDeleted() != that.getIsDeleted()) return false;
        if (getCrudSubjectId() != that.getCrudSubjectId()) return false;
        if (!getUuid().equals(that.getUuid())) return false;
        if (!getCreated().equals(that.getCreated())) return false;
        if (!getUpdated().equals(that.getUpdated())) return false;
        return getDeleted() != null ? getDeleted().equals(that.getDeleted()) : that.getDeleted() == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (getId() ^ (getId() >>> 32));
        result = 31 * result + getUuid().hashCode();
        result = 31 * result + (int) (getOrganizationId() ^ (getOrganizationId() >>> 32));
        result = 31 * result + getCreated().hashCode();
        result = 31 * result + getUpdated().hashCode();
        result = 31 * result + (getDeleted() != null ? getDeleted().hashCode() : 0);
        result = 31 * result + (int) (getIsDeleted() ^ (getIsDeleted() >>> 32));
        result = 31 * result + (int) (getCrudSubjectId() ^ (getCrudSubjectId() >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "AbstractEntity{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", organizationId=" + organizationId +
                ", created=" + created +
                ", updated=" + updated +
                ", deleted=" + deleted +
                ", isDeleted=" + isDeleted +
                ", crudSubjectId=" + crudSubjectId +
                '}';
    }
}
