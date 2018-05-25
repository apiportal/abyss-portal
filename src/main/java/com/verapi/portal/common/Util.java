/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 5 2018
 *
 */

package com.verapi.portal.common;


import io.vertx.core.json.JsonObject;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;

/**
 *
 */
public class Util {

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
        return (a == null) ? b : a;
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
        if (a != null)
            return b;
        else
            return null;
    }

    public static String encodeFileToBase64Binary(File file) throws IOException {
        FileInputStream fileInputStreamReader = new FileInputStream(file);
        byte[] bytes = new byte[(int) file.length()];
        fileInputStreamReader.read(bytes);
        return new String(Base64.getEncoder().encode(bytes), StandardCharsets.UTF_8);
    }

    private static JsonObject convertYamlToJson(String yamlString) {
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(yamlString);

        JsonObject jsonObject = new JsonObject(map);
        return jsonObject;
    }

    public static JsonObject loadYamlFile(File yamlFileName) throws FileNotFoundException {
//        ClassLoader classLoader = getClass().getClassLoader();
//        File file = new File(Objects.requireNonNull(classLoader.getResource(yamlFileName)).getFile());
        InputStream inputStream = new FileInputStream(yamlFileName);
        Yaml yaml = new Yaml();
        Map<String, Object> map = (Map<String, Object>) yaml.load(inputStream);
        return new JsonObject(map);
    }
}
