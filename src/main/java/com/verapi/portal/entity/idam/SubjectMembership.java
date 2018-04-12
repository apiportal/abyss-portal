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

public class SubjectMembership {

    private long id;
    private java.util.UUID uuid;
    private long organizationId;
    private java.sql.Timestamp created;
    private java.sql.Timestamp updated;
    private java.sql.Timestamp deleted;
    private long isDeleted;
    private long crudSubjectId;
    private long subjectId;
    private long groupId;

    public SubjectMembership() {
    }

    public SubjectMembership(SubjectMembership subjectMembership) {
        this.id = subjectMembership.id;
        this.uuid = subjectMembership.uuid;
        this.organizationId = subjectMembership.organizationId;
        this.created = subjectMembership.created;
        this.updated = subjectMembership.updated;
        this.deleted = subjectMembership.deleted;
        this.isDeleted = subjectMembership.isDeleted;
        this.crudSubjectId = subjectMembership.crudSubjectId;
        this.subjectId = subjectMembership.subjectId;
        this.groupId = subjectMembership.groupId;
    }

    public SubjectMembership(long id, UUID uuid, long organizationId, Timestamp created, Timestamp updated, Timestamp deleted, long isDeleted, long crudSubjectId, long subjectId, long groupId) {
        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.isDeleted = isDeleted;
        this.crudSubjectId = crudSubjectId;
        this.subjectId = subjectId;
        this.groupId = groupId;
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


    public long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(long subjectId) {
        this.subjectId = subjectId;
    }


    public long getGroupId() {
        return groupId;
    }

    public void setGroupId(long groupId) {
        this.groupId = groupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectMembership)) return false;

        SubjectMembership that = (SubjectMembership) o;

        if (getId() != that.getId()) return false;
        if (getOrganizationId() != that.getOrganizationId()) return false;
        if (getIsDeleted() != that.getIsDeleted()) return false;
        if (getCrudSubjectId() != that.getCrudSubjectId()) return false;
        if (getSubjectId() != that.getSubjectId()) return false;
        if (getGroupId() != that.getGroupId()) return false;
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
        result = 31 * result + (int) (getSubjectId() ^ (getSubjectId() >>> 32));
        result = 31 * result + (int) (getGroupId() ^ (getGroupId() >>> 32));
        return result;
    }

    @Override
    public String toString() {
        return "SubjectMembership{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", organizationId=" + organizationId +
                ", created=" + created +
                ", updated=" + updated +
                ", deleted=" + deleted +
                ", isDeleted=" + isDeleted +
                ", crudSubjectId=" + crudSubjectId +
                ", subjectId=" + subjectId +
                ", groupId=" + groupId +
                '}';
    }
}
