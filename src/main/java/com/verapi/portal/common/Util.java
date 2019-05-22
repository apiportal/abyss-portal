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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 *
 */
public final class Util {
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    private Util() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Null value<br>
     * Returns a value if tested value is null, like Oracle NVL function
     *
     * @param a   value to be checked if null
     * @param b   value to be returned if a is null
     * @param <T> any
     * @return returns a if a is <b>not</b> null <br>returns b if a is null
     */
    public static <T> T nvl(T a, T b) {
        if (a == null) {
            return b;
        } else {
            return a;
        }
    }

    /**
     * Not Null Value<br>
     * Returns its calculated value if it is not null
     *
     * @param a   value to be checked if null
     * @param b   value to be returned if a is <b>not</b> null
     * @param <T> any
     * @return returns b if a is <b>not</b> null <br> returns a if a is null
     */
    public static <T> T nnvl(T a, T b) {
        //TODO: replaced with functional interface to disable executing b parameter before initalization
        if (a != null) {
            return b;
        } else {
            return null;
        }
    }

    public static String encodeFileToBase64Binary(File file) throws IOException {
        byte[] bytes;

        try (InputStream is = java.nio.file.Files.newInputStream(file.toPath())) {
            bytes = new byte[(int) file.length()];
            int read = is.read(bytes);
            LOGGER.trace("encodeFileToBase64Binary read {} bytes", read);
        }
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    public static JsonObject convertYamlToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = yaml.load(yamlString);
        return new JsonObject(map);
    }

    public static JsonObject loadYamlFile(File yamlFileName) throws IOException {
        Map<String, Object> map;
        try (InputStream is = java.nio.file.Files.newInputStream(yamlFileName.toPath())) {
            Yaml yaml = new Yaml();
            map = yaml.load(is);
            return new JsonObject(map);
        }
    }
}
