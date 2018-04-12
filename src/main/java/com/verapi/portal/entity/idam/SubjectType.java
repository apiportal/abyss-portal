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

package com.verapi.portal.entity.idam;


import java.sql.Timestamp;
import java.util.UUID;

public class SubjectType {

    private String id;
    private java.util.UUID uuid;
    private long organizationId;
    private java.sql.Timestamp created;
    private java.sql.Timestamp updated;
    private java.sql.Timestamp deleted;
    private long isDeleted;
    private long crudSubjectId;
    private String typeName;
    private String typeDescription;

    public SubjectType() {
    }

    public SubjectType(SubjectType subjectType) {
        this.id = subjectType.id;
        this.uuid = subjectType.uuid;
        this.organizationId = subjectType.organizationId;
        this.created = subjectType.created;
        this.updated = subjectType.updated;
        this.deleted = subjectType.deleted;
        this.isDeleted = subjectType.isDeleted;
        this.crudSubjectId = subjectType.crudSubjectId;
        this.typeName = subjectType.typeName;
        this.typeDescription = subjectType.typeDescription;
    }

    public SubjectType(String id, UUID uuid, long organizationId, Timestamp created, Timestamp updated, Timestamp deleted, long isDeleted, long crudSubjectId, String typeName, String typeDescription) {
        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.isDeleted = isDeleted;
        this.crudSubjectId = crudSubjectId;
        this.typeName = typeName;
        this.typeDescription = typeDescription;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
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


    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }


    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectType)) return false;

        SubjectType that = (SubjectType) o;

        if (getOrganizationId() != that.getOrganizationId()) return false;
        if (getIsDeleted() != that.getIsDeleted()) return false;
        if (getCrudSubjectId() != that.getCrudSubjectId()) return false;
        if (!getId().equals(that.getId())) return false;
        if (!getUuid().equals(that.getUuid())) return false;
        if (!getCreated().equals(that.getCreated())) return false;
        if (!getUpdated().equals(that.getUpdated())) return false;
        if (getDeleted() != null ? !getDeleted().equals(that.getDeleted()) : that.getDeleted() != null) return false;
        if (!getTypeName().equals(that.getTypeName())) return false;
        return getTypeDescription().equals(that.getTypeDescription());
    }

    @Override
    public int hashCode() {
        int result = getId().hashCode();
        result = 31 * result + getUuid().hashCode();
        result = 31 * result + (int) (getOrganizationId() ^ (getOrganizationId() >>> 32));
        result = 31 * result + getCreated().hashCode();
        result = 31 * result + getUpdated().hashCode();
        result = 31 * result + (getDeleted() != null ? getDeleted().hashCode() : 0);
        result = 31 * result + (int) (getIsDeleted() ^ (getIsDeleted() >>> 32));
        result = 31 * result + (int) (getCrudSubjectId() ^ (getCrudSubjectId() >>> 32));
        result = 31 * result + getTypeName().hashCode();
        result = 31 * result + getTypeDescription().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubjectType{" +
                "id='" + id + '\'' +
                ", uuid=" + uuid +
                ", organizationId=" + organizationId +
                ", created=" + created +
                ", updated=" + updated +
                ", deleted=" + deleted +
                ", isDeleted=" + isDeleted +
                ", crudSubjectId=" + crudSubjectId +
                ", typeName='" + typeName + '\'' +
                ", typeDescription='" + typeDescription + '\'' +
                '}';
    }
}
