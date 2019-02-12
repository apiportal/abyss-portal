package com.verapi.portal.common;

import io.vertx.core.json.JsonObject;

/**
 * @deprecated  As of 12 Feb, replaced by {abyss-common}
 */
@Deprecated
public class Config {

    public static Config instance = null;
    public JsonObject config;

    private Config() {
    }

    public static Config getInstance() {
        if (instance == null)
            instance = new Config();
        return instance;
    }

    public JsonObject getConfigJsonObject() {
        if (config == null)
            setConfig(new JsonObject());
        return config;
    }

    public Config setConfig(JsonObject config) {
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
