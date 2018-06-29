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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class FileUtil {
    private static final Logger logger = LoggerFactory.getLogger(FileUtil.class);

    JsonArray getYamlFileList() {
        JsonArray yamlFileList = new JsonArray();
        JarFile jf = null;
        try {
            String s = new java.io.File(this.getClass().getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
            jf = new JarFile(s);

            Enumeration<JarEntry> entries = jf.entries();
            while (entries.hasMoreElements()) {
                JarEntry je = entries.nextElement();
                if (je.getName().startsWith("openapi") && (je.getName().toLowerCase().endsWith(".yaml"))) {
                    String fileName = je.getName();
                    yamlFileList.add(fileName.substring(fileName.lastIndexOf("openapi") + "openapi".length() + 1, fileName.length()));
                }
            }
        } catch (IOException e) {
            logger.error("error while getting resource files readMyResources {} - {}", e.getLocalizedMessage(), e.getStackTrace());
        } finally {
            try {
                if (jf != null) {
                    jf.close();
                }
            } catch (Exception e) {
                logger.error("error while getting resource files readMyResources {} - {}", e.getLocalizedMessage(), e.getStackTrace());
            }
        }
        return yamlFileList;
    }

}
