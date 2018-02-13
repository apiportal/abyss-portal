package com.verapi.starter.common;

import io.vertx.core.json.JsonObject;

public class Config {

    public static Config instance = null;
    public JsonObject config;

    protected Config() {
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


}
