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

public class SubjectPermission {

    private long id;
    private java.util.UUID uuid;
    private long organizationId;
    private java.sql.Timestamp created;
    private java.sql.Timestamp updated;
    private java.sql.Timestamp deleted;
    private long isDeleted;
    private long crudSubjectId;
    private String permission;
    private String description;
    private java.sql.Timestamp effectiveStartDate;
    private java.sql.Timestamp effectiveEndDate;
    private long subjectId;

    public SubjectPermission() {
    }

    public SubjectPermission(SubjectPermission subjectPermission) {
        this.id = subjectPermission.id;
        this.uuid = subjectPermission.uuid;
        this.organizationId = subjectPermission.organizationId;
        this.created = subjectPermission.created;
        this.updated = subjectPermission.updated;
        this.deleted = subjectPermission.deleted;
        this.isDeleted = subjectPermission.isDeleted;
        this.crudSubjectId = subjectPermission.crudSubjectId;
        this.permission = subjectPermission.permission;
        this.description = subjectPermission.description;
        this.effectiveStartDate = subjectPermission.effectiveStartDate;
        this.effectiveEndDate = subjectPermission.effectiveEndDate;
        this.subjectId = subjectPermission.subjectId;
    }

    public SubjectPermission(long id, UUID uuid, long organizationId, Timestamp created, Timestamp updated, Timestamp deleted, long isDeleted, long crudSubjectId, String permission, String description, Timestamp effectiveStartDate, Timestamp effectiveEndDate, long subjectId) {
        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.isDeleted = isDeleted;
        this.crudSubjectId = crudSubjectId;
        this.permission = permission;
        this.description = description;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.subjectId = subjectId;
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


    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
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


    public long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(long subjectId) {
        this.subjectId = subjectId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectPermission)) return false;

        SubjectPermission that = (SubjectPermission) o;

        if (getId() != that.getId()) return false;
        if (getOrganizationId() != that.getOrganizationId()) return false;
        if (getIsDeleted() != that.getIsDeleted()) return false;
        if (getCrudSubjectId() != that.getCrudSubjectId()) return false;
        if (getSubjectId() != that.getSubjectId()) return false;
        if (!getUuid().equals(that.getUuid())) return false;
        if (!getCreated().equals(that.getCreated())) return false;
        if (!getUpdated().equals(that.getUpdated())) return false;
        if (getDeleted() != null ? !getDeleted().equals(that.getDeleted()) : that.getDeleted() != null) return false;
        if (!getPermission().equals(that.getPermission())) return false;
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
        result = 31 * result + getPermission().hashCode();
        result = 31 * result + getDescription().hashCode();
        result = 31 * result + getEffectiveStartDate().hashCode();
        result = 31 * result + (getEffectiveEndDate() != null ? getEffectiveEndDate().hashCode() : 0);
        result = 31 * result + (int) (getSubjectId() ^ (getSubjectId() >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "SubjectPermission{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", organizationId=" + organizationId +
                ", created=" + created +
                ", updated=" + updated +
                ", deleted=" + deleted +
                ", isDeleted=" + isDeleted +
                ", crudSubjectId=" + crudSubjectId +
                ", permission='" + permission + '\'' +
                ", description='" + description + '\'' +
                ", effectiveStartDate=" + effectiveStartDate +
                ", effectiveEndDate=" + effectiveEndDate +
                ", subjectId=" + subjectId +
                '}';
    }
}
