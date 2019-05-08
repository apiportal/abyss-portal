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


import com.verapi.portal.entity.AbstractEntity;
import com.verapi.portal.entity.idam.converter.SubjectConverter;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

//@DataObject(generateConverter = true)
public class Subject extends AbstractEntity<Subject> {

    private static Logger logger = LoggerFactory.getLogger(Subject.class);

    private long isActivated;
    private long subjectTypeId;
    private String subjectName;
    private String firstName;
    private String lastName;
    private String displayName;
    private String email;
    private String secondaryEmail;
    private Instant effectiveStartDate;
    private Instant effectiveEndDate;
    private String password;
    private String passwordSalt;

    public Subject() {
    }

    public Subject(Subject subject) {
        super(subject.id, subject.uuid, subject.organizationId, subject.created, subject.updated, subject.deleted, subject.isDeleted, subject.crudSubjectId);
        this.isActivated = subject.isActivated;
        this.subjectTypeId = subject.subjectTypeId;
        this.subjectName = subject.subjectName;
        this.firstName = subject.firstName;
        this.lastName = subject.lastName;
        this.displayName = subject.displayName;
        this.email = subject.email;
        this.secondaryEmail = subject.secondaryEmail;
        this.effectiveStartDate = subject.effectiveStartDate;
        this.effectiveEndDate = subject.effectiveEndDate;
        this.password = subject.password;
        this.passwordSalt = subject.passwordSalt;
    }

    public Subject(long id, String uuid, long organizationId, Instant created, Instant updated, Instant deleted, long isDeleted, long crudSubjectId, long isActivated, long subjectTypeId, String subjectName, String firstName, String lastName, String displayName, String email, String secondaryEmail, Instant effectiveStartDate, Instant effectiveEndDate, String password, String passwordSalt) {
        super(id, uuid, organizationId, created, updated, deleted, isDeleted, crudSubjectId);
        this.isActivated = isActivated;
        this.subjectTypeId = subjectTypeId;
        this.subjectName = subjectName;
        this.firstName = firstName;
        this.lastName = lastName;
        this.displayName = displayName;
        this.email = email;
        this.secondaryEmail = secondaryEmail;
        this.effectiveStartDate = effectiveStartDate;
        this.effectiveEndDate = effectiveEndDate;
        this.password = password;
        this.passwordSalt = passwordSalt;
    }

    public Subject(JsonObject obj) {
        SubjectConverter.fromJson(obj, this);
    }

    public Subject(String jsonStr) {
        SubjectConverter.fromJson(new JsonObject(jsonStr), this);
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        SubjectConverter.toJson(this, json);
        return json;
    }

    public long getIsActivated() {
        return isActivated;
    }

    public void setIsActivated(long isActivated) {
        this.isActivated = isActivated;
    }


    public long getSubjectTypeId() {
        return subjectTypeId;
    }

    public void setSubjectTypeId(long subjectTypeId) {
        this.subjectTypeId = subjectTypeId;
    }


    public String getSubjectName() {
        return subjectName;
    }

    public void setSubjectName(String subjectName) {
        this.subjectName = subjectName;
    }


    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }


    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }


    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }


    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }


    public String getSecondaryEmail() {
        return secondaryEmail;
    }

    public void setSecondaryEmail(String secondaryEmail) {
        this.secondaryEmail = secondaryEmail;
    }


    public Instant getEffectiveStartDate() {
        return effectiveStartDate;
    }

    public void setEffectiveStartDate(Instant effectiveStartDate) {
        this.effectiveStartDate = effectiveStartDate;
    }


    public Instant getEffectiveEndDate() {
        return effectiveEndDate;
    }

    public void setEffectiveEndDate(Instant effectiveEndDate) {
        this.effectiveEndDate = effectiveEndDate;
    }


    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }


    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Subject)) return false;

        Subject subject = (Subject) o;

        if (getIsActivated() != subject.getIsActivated()) return false;
        if (getSubjectTypeId() != subject.getSubjectTypeId()) return false;
        if (!getSubjectName().equals(subject.getSubjectName())) return false;
        if (!getFirstName().equals(subject.getFirstName())) return false;
        if (!getLastName().equals(subject.getLastName())) return false;
        if (!getDisplayName().equals(subject.getDisplayName())) return false;
        if (!getEmail().equals(subject.getEmail())) return false;
        if (getSecondaryEmail() != null ? !getSecondaryEmail().equals(subject.getSecondaryEmail()) : subject.getSecondaryEmail() != null)
            return false;
        if (!getEffectiveStartDate().equals(subject.getEffectiveStartDate())) return false;
        if (getEffectiveEndDate() != null ? !getEffectiveEndDate().equals(subject.getEffectiveEndDate()) : subject.getEffectiveEndDate() != null)
            return false;
        if (!getPassword().equals(subject.getPassword())) return false;
        return getPasswordSalt().equals(subject.getPasswordSalt());
    }

    @Override
    public int hashCode() {
        int result = (int) (getIsActivated() ^ (getIsActivated() >>> 32));
        result = 31 * result + (int) (getSubjectTypeId() ^ (getSubjectTypeId() >>> 32));
        result = 31 * result + getSubjectName().hashCode();
        result = 31 * result + getFirstName().hashCode();
        result = 31 * result + getLastName().hashCode();
        result = 31 * result + getDisplayName().hashCode();
        result = 31 * result + getEmail().hashCode();
        result = 31 * result + (getSecondaryEmail() != null ? getSecondaryEmail().hashCode() : 0);
        result = 31 * result + getEffectiveStartDate().hashCode();
        result = 31 * result + (getEffectiveEndDate() != null ? getEffectiveEndDate().hashCode() : 0);
        result = 31 * result + getPassword().hashCode();
        result = 31 * result + getPasswordSalt().hashCode();
        result = 31 * result + super.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return super.toString() +
                "Subject{" +
                "isActivated=" + isActivated +
                ", subjectTypeId=" + subjectTypeId +
                ", subjectName='" + subjectName + '\'' +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", displayName='" + displayName + '\'' +
                ", email='" + email + '\'' +
                ", secondaryEmail='" + secondaryEmail + '\'' +
                ", effectiveStartDate=" + effectiveStartDate +
                ", effectiveEndDate=" + effectiveEndDate +
                ", password='" + password + '\'' +
                ", passwordSalt='" + passwordSalt + '\'' +
                '}';
    }

    public Subject merge(Subject newSubject) {
        return new Subject(id, uuid, nvl(newSubject.organizationId, organizationId), nvl(newSubject.created, created), nvl(newSubject.updated, updated),
                nvl(newSubject.deleted, deleted), nvl(newSubject.isDeleted, isDeleted), nvl(newSubject.crudSubjectId, crudSubjectId), nvl(newSubject.isActivated, getIsActivated()),
                nvl(newSubject.subjectTypeId, getSubjectTypeId()), nvl(newSubject.subjectName, getSubjectName()), nvl(newSubject.firstName, getFirstName()), nvl(newSubject.lastName, getLastName()),
                nvl(newSubject.displayName, getDisplayName()), nvl(newSubject.email, getEmail()), nvl(newSubject.secondaryEmail, getSecondaryEmail()), nvl(newSubject.effectiveStartDate,
                getEffectiveStartDate()), nvl(newSubject.effectiveEndDate, getEffectiveEndDate()), this.password, this.passwordSalt);
    }


}
