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

package com.verapi.portal.entity.idam;


import java.sql.Timestamp;
import java.util.UUID;

public class SubjectGroup {

    private long id;
    private java.util.UUID uuid;
    private long organizationId;
    private java.sql.Timestamp created;
    private java.sql.Timestamp updated;
    private java.sql.Timestamp deleted;
    private long isDeleted;
    private long crudSubjectId;
    private long isEnabled;
    private String groupName;
    private String description;
    private java.sql.Timestamp effectiveStartDate;
    private java.sql.Timestamp effectiveEndDate;

    public SubjectGroup() {
    }

    public SubjectGroup(SubjectGroup subjectGroup) {
        this.id = subjectGroup.id;
        this.uuid = subjectGroup.uuid;
        this.organizationId = subjectGroup.organizationId;
        this.created = subjectGroup.created;
        this.updated = subjectGroup.updated;
        this.deleted = subjectGroup.deleted;
        this.isDeleted = subjectGroup.isDeleted;
        this.crudSubjectId = subjectGroup.crudSubjectId;
        this.isEnabled = subjectGroup.isEnabled;
        this.groupName = subjectGroup.groupName;
        this.description = subjectGroup.description;
        this.effectiveStartDate = subjectGroup.effectiveStartDate;
        this.effectiveEndDate = subjectGroup.effectiveEndDate;
    }

    public SubjectGroup(long id, UUID uuid, long organizationId, Timestamp created, Timestamp updated, Timestamp deleted, long isDeleted, long crudSubjectId, long isEnabled, String groupName, String description, Timestamp effectiveStartDate, Timestamp effectiveEndDate) {
        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.isDeleted = isDeleted;
        this.crudSubjectId = crudSubjectId;
        this.isEnabled = isEnabled;
        this.groupName = groupName;
        this.description = description;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }


    public java.util.UUID getUuid() {
        return uuid;
    }

    public void setUuid(java.util.UUID uuid) {
        this.uuid = uuid;
    }


    public long getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(long organizationId) {
        this.organizationId = organizationId;
    }


    public java.sql.Timestamp getCreated() {
        return created;
    }

    public void setCreated(java.sql.Timestamp created) {
        this.created = created;
    }


    public java.sql.Timestamp getUpdated() {
        return updated;
    }

    public void setUpdated(java.sql.Timestamp updated) {
        this.updated = updated;
    }


    public java.sql.Timestamp getDeleted() {
        return deleted;
    }

    public void setDeleted(java.sql.Timestamp deleted) {
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


    public long getIsEnabled() {
        return isEnabled;
    }

    public void setIsEnabled(long isEnabled) {
        this.isEnabled = isEnabled;
    }


    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }


    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public java.sql.Timestamp getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public void setEffectiveStartDate(java.sql.Timestamp effectiveStartDate) {
        this.effectiveStartDate = effectiveStartDate;
    }


    public java.sql.Timestamp getEffectiveEndDate() {
        return effectiveEndDate;
    }

    public void setEffectiveEndDate(java.sql.Timestamp effectiveEndDate) {
        this.effectiveEndDate = effectiveEndDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectGroup)) return false;

        SubjectGroup that = (SubjectGroup) o;

        if (getId() != that.getId()) return false;
        if (getOrganizationId() != that.getOrganizationId()) return false;
        if (getIsDeleted() != that.getIsDeleted()) return false;
        if (getCrudSubjectId() != that.getCrudSubjectId()) return false;
        if (getIsEnabled() != that.getIsEnabled()) return false;
        if (!getUuid().equals(that.getUuid())) return false;
        if (!getCreated().equals(that.getCreated())) return false;
        if (!getUpdated().equals(that.getUpdated())) return false;
        if (getDeleted() != null ? !getDeleted().equals(that.getDeleted()) : that.getDeleted() != null) return false;
        if (!getGroupName().equals(that.getGroupName())) return false;
        if (!getDescription().equals(that.getDescription())) return false;
        if (!getEffectiveStartDate().equals(that.getEffectiveStartDate())) return false;
        return getEffectiveEndDate() != null ? getEffectiveEndDate().equals(that.getEffectiveEndDate()) : that.getEffectiveEndDate() == null;
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
        result = 31 * result + (int) (getIsEnabled() ^ (getIsEnabled() >>> 32));
        result = 31 * result + getGroupName().hashCode();
        result = 31 * result + getDescription().hashCode();
        result = 31 * result + getEffectiveStartDate().hashCode();
        result = 31 * result + (getEffectiveEndDate() != null ? getEffectiveEndDate().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "SubjectGroup{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", organizationId=" + organizationId +
                ", created=" + created +
                ", updated=" + updated +
                ", deleted=" + deleted +
                ", isDeleted=" + isDeleted +
                ", crudSubjectId=" + crudSubjectId +
                ", isEnabled=" + isEnabled +
                ", groupName='" + groupName + '\'' +
                ", description='" + description + '\'' +
                ", effectiveStartDate=" + effectiveStartDate +
                ", effectiveEndDate=" + effectiveEndDate +
                '}';
    }
}
