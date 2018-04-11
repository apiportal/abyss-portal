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

package com.verapi.portal.entity.idam.converter;

import com.verapi.portal.entity.idam.Subject;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class SubjectConverter {

    private static Logger logger = LoggerFactory.getLogger(SubjectConverter.class);

    public static void fromJson(JsonObject json, Subject obj) {
        if (json.getValue("crudSubjectId") instanceof Number) {
            obj.setCrudSubjectId(((Number) json.getValue("crudSubjectId")).longValue());
        }
        if (json.getValue("displayName") instanceof String) {
            obj.setDisplayName((String) json.getValue("displayName"));
        }
        if (json.getValue("email") instanceof String) {
            obj.setEmail((String) json.getValue("email"));
        }
        if (json.getValue("firstName") instanceof String) {
            obj.setFirstName((String) json.getValue("firstName"));
        }
        if (json.getValue("id") instanceof Number) {
            obj.setId(((Number) json.getValue("id")).longValue());
        }
        if (json.getValue("isActivated") instanceof Number) {
            obj.setIsActivated(((Number) json.getValue("isActivated")).longValue());
        }
        if (json.getValue("isDeleted") instanceof Number) {
            obj.setIsDeleted(((Number) json.getValue("isDeleted")).longValue());
        }
        if (json.getValue("lastName") instanceof String) {
            obj.setLastName((String) json.getValue("lastName"));
        }
        if (json.getValue("organizationId") instanceof Number) {
            obj.setOrganizationId(((Number) json.getValue("organizationId")).longValue());
        }
        if (json.getValue("password") instanceof String) {
            obj.setPassword((String) json.getValue("password"));
        }
        if (json.getValue("passwordSalt") instanceof String) {
            obj.setPasswordSalt((String) json.getValue("passwordSalt"));
        }
        if (json.getValue("secondaryEmail") instanceof String) {
            obj.setSecondaryEmail((String) json.getValue("secondaryEmail"));
        }
        if (json.getValue("subjectName") instanceof String) {
            obj.setSubjectName((String) json.getValue("subjectName"));
        }
        if (json.getValue("subjectTypeId") instanceof Number) {
            obj.setSubjectTypeId(((Number) json.getValue("subjectTypeId")).longValue());
        }
        if (json.getValue("subjectTypeId") instanceof Number) {
            obj.setSubjectTypeId(((Number) json.getValue("subjectTypeId")).longValue());
        }
        //Vertx Codegen cannot convert Timestamp, Instant and UUID types
        if (json.getValue("created") instanceof Instant) {
            obj.setCreated(((Instant) json.getInstant("created")));
        }
        if (json.getValue("updated") instanceof Instant) {
            obj.setCreated(((Instant) json.getInstant("updated")));
        }
        if (json.getValue("deleted") instanceof Instant) {
            obj.setCreated(((Instant) json.getInstant("deleted")));
        }
        if (json.getValue("effectiveStartDate") instanceof Instant) {
            obj.setCreated(((Instant) json.getInstant("effectiveStartDate")));
        }
        if (json.getValue("effectiveEndDate") instanceof Instant) {
            obj.setCreated(((Instant) json.getInstant("effectiveEndDate")));
        }
        if (json.getValue("uuid") instanceof String) {
            obj.setUuid((String) json.getValue("uuid"));
        }

    }

    public static void toJson(Subject obj, JsonObject json) {
        json.put("crudSubjectId", obj.getCrudSubjectId());
        if (obj.getDisplayName() != null) {
            json.put("displayName", obj.getDisplayName());
        }
        if (obj.getEmail() != null) {
            json.put("email", obj.getEmail());
        }
        if (obj.getFirstName() != null) {
            json.put("firstName", obj.getFirstName());
        }
        json.put("id", obj.getId());
        json.put("isActivated", obj.getIsActivated());
        json.put("isDeleted", obj.getIsDeleted());
        if (obj.getLastName() != null) {
            json.put("lastName", obj.getLastName());
        }
        json.put("organizationId", obj.getOrganizationId());
        if (obj.getPassword() != null) {
            json.put("password", obj.getPassword());
        }
        if (obj.getPasswordSalt() != null) {
            json.put("passwordSalt", obj.getPasswordSalt());
        }
        if (obj.getSecondaryEmail() != null) {
            json.put("secondaryEmail", obj.getSecondaryEmail());
        }
        if (obj.getSubjectName() != null) {
            json.put("subjectName", obj.getSubjectName());
        }
        json.put("subjectTypeId", obj.getSubjectTypeId());
        //Vertx Codegen cannot convert Timestamp, Instant and UUID types
        json.put("created", obj.getCreated());
        json.put("updated", obj.getUpdated());
        json.put("deleted", obj.getDeleted());
        json.put("effectiveStartDate", obj.getEffectiveStartDate());
        json.put("effectiveEndDate", obj.getEffectiveEndDate());
        if (obj.getUuid() != null) {
            json.put("uuid", obj.getUuid());
        }

    }

}
