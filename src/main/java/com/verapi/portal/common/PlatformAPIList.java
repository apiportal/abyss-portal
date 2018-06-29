/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 6 2018
 *
 */

package com.verapi.portal.common;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlatformAPIList {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    public static PlatformAPIList instance = null;
    private JsonArray platformApiList = null;

    private PlatformAPIList() {
    }

    public static PlatformAPIList getInstance() {
        if (instance == null)
            instance = new PlatformAPIList();
        return instance;
    }

    public JsonArray getPlatformAPIList() {
        if (platformApiList == null)
            setPlatformAPIList(new FileUtil().getYamlFileList());
        return platformApiList;
    }

    public PlatformAPIList setPlatformAPIList(JsonArray platformApiList) {
        this.platformApiList = platformApiList;
        return this;
    }

    @Override
    public void finalize() {
        if (platformApiList != null) {
            platformApiList.clear();
        }
    }

}