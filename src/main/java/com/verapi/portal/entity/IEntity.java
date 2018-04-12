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
