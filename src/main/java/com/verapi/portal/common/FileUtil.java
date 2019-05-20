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

import io.vertx.core.json.JsonArray;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

class FileUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtil.class);
    private static final String OPENAPI = "openapi";

    JsonArray getYamlFileList() {
        JsonArray yamlFileList = new JsonArray();
        String s = new java.io.File(FilenameUtils.getName(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath())).getName();

        try (JarFile jf = new JarFile(s)) {
            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if (je.getName().startsWith(OPENAPI) && (je.getName().toLowerCase(Locale.ENGLISH).endsWith(".yaml"))) {
                    String fileName = je.getName();
                    yamlFileList.add(fileName.substring(fileName.lastIndexOf(OPENAPI) + OPENAPI.length() + 1));
                }
            }
        } catch (IOException e) {
            LOGGER.error("error while getting resource files readMyResources {} - {}"
                    , e.getLocalizedMessage(), e.getStackTrace());
        }

        return yamlFileList;
    }
}
