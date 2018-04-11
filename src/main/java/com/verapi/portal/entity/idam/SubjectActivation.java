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

public class SubjectActivation {

    private long id;
    private java.util.UUID uuid;
    private long organizationId;
    private java.sql.Timestamp created;
    private java.sql.Timestamp updated;
    private java.sql.Timestamp deleted;
    private long isDeleted;
    private long crudSubjectId;
    private long subjectId;
    private java.sql.Timestamp expireDate;
    private String token;

    public SubjectActivation() {
    }

    public SubjectActivation(SubjectActivation subjectActivation) {
        this.id = subjectActivation.id;
        this.uuid = subjectActivation.uuid;
        this.organizationId = subjectActivation.organizationId;
        this.created = subjectActivation.created;
        this.updated = subjectActivation.updated;
        this.deleted = subjectActivation.deleted;
        this.isDeleted = subjectActivation.isDeleted;
        this.crudSubjectId = subjectActivation.crudSubjectId;
        this.subjectId = subjectActivation.subjectId;
        this.expireDate = subjectActivation.expireDate;
        this.token = subjectActivation.token;
    }

    public SubjectActivation(long id, UUID uuid, long organizationId, Timestamp created, Timestamp updated, Timestamp deleted, long isDeleted, long crudSubjectId, long subjectId, Timestamp expireDate, String token) {
        this.id = id;
        this.uuid = uuid;
        this.organizationId = organizationId;
        this.created = created;
        this.updated = updated;
        this.deleted = deleted;
        this.isDeleted = isDeleted;
        this.crudSubjectId = crudSubjectId;
        this.subjectId = subjectId;
        this.expireDate = expireDate;
        this.token = token;
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


    public java.sql.Timestamp getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(java.sql.Timestamp expireDate) {
        this.expireDate = expireDate;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SubjectActivation)) return false;

        SubjectActivation that = (SubjectActivation) o;

        if (getId() != that.getId()) return false;
        if (getOrganizationId() != that.getOrganizationId()) return false;
        if (getIsDeleted() != that.getIsDeleted()) return false;
        if (getCrudSubjectId() != that.getCrudSubjectId()) return false;
        if (getSubjectId() != that.getSubjectId()) return false;
        if (!getUuid().equals(that.getUuid())) return false;
        if (!getCreated().equals(that.getCreated())) return false;
        if (!getUpdated().equals(that.getUpdated())) return false;
        if (getDeleted() != null ? !getDeleted().equals(that.getDeleted()) : that.getDeleted() != null) return false;
        if (!getExpireDate().equals(that.getExpireDate())) return false;
        return getToken().equals(that.getToken());
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
        result = 31 * result + getExpireDate().hashCode();
        result = 31 * result + getToken().hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "SubjectActivation{" +
                "id=" + id +
                ", uuid=" + uuid +
                ", organizationId=" + organizationId +
                ", created=" + created +
                ", updated=" + updated +
                ", deleted=" + deleted +
                ", isDeleted=" + isDeleted +
                ", crudSubjectId=" + crudSubjectId +
                ", subjectId=" + subjectId +
                ", expireDate=" + expireDate +
                ", token='" + token + '\'' +
                '}';
    }
}
