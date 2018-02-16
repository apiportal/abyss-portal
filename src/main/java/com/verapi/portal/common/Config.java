package com.verapi.portal.common;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

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
