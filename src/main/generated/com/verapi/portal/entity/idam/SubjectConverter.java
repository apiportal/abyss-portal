/*
 * Copyright (c) 2014 Red Hat, Inc. and others
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.verapi.portal.entity.idam;

import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

/**
 * Converter for {@link com.verapi.portal.entity.idam.Subject}.
 *
 * NOTE: This class has been automatically generated from the {@link com.verapi.portal.entity.idam.Subject} original class using Vert.x codegen.
 */
public class SubjectConverter {

  public static void fromJson(JsonObject json, Subject obj) {
    if (json.getValue("crudSubjectId") instanceof Number) {
      obj.setCrudSubjectId(((Number)json.getValue("crudSubjectId")).longValue());
    }
    if (json.getValue("displayName") instanceof String) {
      obj.setDisplayName((String)json.getValue("displayName"));
    }
    if (json.getValue("email") instanceof String) {
      obj.setEmail((String)json.getValue("email"));
    }
    if (json.getValue("firstName") instanceof String) {
      obj.setFirstName((String)json.getValue("firstName"));
    }
    if (json.getValue("id") instanceof Long) {
      obj.setId((Long)json.getValue("id"));
    }
    if (json.getValue("isActivated") instanceof Number) {
      obj.setIsActivated(((Number)json.getValue("isActivated")).longValue());
    }
    if (json.getValue("isDeleted") instanceof Number) {
      obj.setIsDeleted(((Number)json.getValue("isDeleted")).longValue());
    }
    if (json.getValue("lastName") instanceof String) {
      obj.setLastName((String)json.getValue("lastName"));
    }
    if (json.getValue("organizationId") instanceof Number) {
      obj.setOrganizationId(((Number)json.getValue("organizationId")).longValue());
    }
    if (json.getValue("password") instanceof String) {
      obj.setPassword((String)json.getValue("password"));
    }
    if (json.getValue("passwordSalt") instanceof String) {
      obj.setPasswordSalt((String)json.getValue("passwordSalt"));
    }
    if (json.getValue("secondaryEmail") instanceof String) {
      obj.setSecondaryEmail((String)json.getValue("secondaryEmail"));
    }
    if (json.getValue("subjectName") instanceof String) {
      obj.setSubjectName((String)json.getValue("subjectName"));
    }
    if (json.getValue("subjectTypeId") instanceof Number) {
      obj.setSubjectTypeId(((Number)json.getValue("subjectTypeId")).longValue());
    }
    if (json.getValue("subjectTypeId") instanceof Number) {
      obj.setSubjectTypeId(((Number)json.getValue("subjectTypeId")).longValue());
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
  }
}