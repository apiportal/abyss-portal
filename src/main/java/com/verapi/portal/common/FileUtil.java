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
import io.vertx.reactivex.core.Vertx;

import java.io.File;
import java.io.FileFilter;
import java.net.URL;

public class FileUtil {
    public JsonArray getYamlFileList() {
        JsonArray yamlFileList = new JsonArray();
        //ClassLoader loader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = getClass().getClassLoader();
        URL url = classLoader.getResource("openapi");
        String path = null;
        if (url != null) {
            path = url.getPath();
            File[] listOfFiles = new File(path).listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getName().toLowerCase().endsWith(".yaml")
                            || pathname.isFile();
                }
            });
            if (listOfFiles != null) {
                for (File f : listOfFiles)
                    yamlFileList.add(f.getName());
            }
        }
        return yamlFileList;
    }
}
