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

import io.vertx.core.json.JsonObject;

public class BuildProperties {

    public static BuildProperties instance = null;
    public JsonObject config;

    private BuildProperties() {
    }

    public static BuildProperties getInstance() {
        if (instance == null)
            instance = new BuildProperties();
        return instance;
    }

    public JsonObject getConfigJsonObject() {
        if (config == null)
            setBuildProperties(new JsonObject());
        return config;
    }

    public BuildProperties setBuildProperties(JsonObject config) {
        this.config = config;
        return this;
    }

    @Override
    public void finalize() {
        if (config != null) {
            config.clear();
        }
    }

}
