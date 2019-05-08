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

import java.time.Instant;

public interface IEntity<T> {

    public long getId();

    public void setId(long id);

    public String getUuid();

    public void setUuid(String uuid);

    public long getOrganizationId();

    public void setOrganizationId(long organizationId);

    public Instant getCreated();

    public void setCreated(Instant created);

    public Instant getUpdated();

    public void setUpdated(Instant updated);

    public Instant getDeleted();

    public void setDeleted(Instant deleted);

    public long getIsDeleted();

    public void setIsDeleted(long isDeleted);

    public long getCrudSubjectId();

    public void setCrudSubjectId(long crudSubjectId);

    public <TT> TT nvl(TT value, TT defaultValue);

}
