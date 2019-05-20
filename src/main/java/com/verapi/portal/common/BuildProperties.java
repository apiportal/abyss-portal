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

package com.verapi.portal.common;

import io.vertx.core.json.JsonObject;

public final class BuildProperties {

    private static BuildProperties instance;
    private JsonObject config;

    private BuildProperties() {
    }

    public static BuildProperties getInstance() {
        if (instance == null) {
            instance = new BuildProperties();
        }
        return instance;
    }

    public JsonObject getConfigJsonObject() {
        if (config == null) {
            setBuildProperties(new JsonObject());
        }
        return config;
    }

    public BuildProperties setBuildProperties(JsonObject config) {
        this.config = config;
        return this;
    }
}
