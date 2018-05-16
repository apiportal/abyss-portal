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

package com.verapi.portal.oapi.schema;

import io.vertx.core.json.JsonObject;

import java.net.MalformedURLException;
import java.net.URL;

public class ApiSchemaError {
    private int code;
    private String usermessage;
    private String internalmessage;
    private String details;
    private String recommendation;
    private URL moreinfo;
    private String timestamp;
    private String path;

    public ApiSchemaError setCode(int code) {
        this.code = code;
        return this;
    }

    public ApiSchemaError setUsermessage(String usermessage) {
        this.usermessage = usermessage;
        return this;
    }

    public ApiSchemaError setInternalmessage(String internalmessage) {
        this.internalmessage = internalmessage;
        return this;
    }

    public ApiSchemaError setDetails(String details) {
        this.details = details;
        return this;
    }

    public ApiSchemaError setRecommendation(String recommendation) {
        this.recommendation = recommendation;
        return this;
    }

    public ApiSchemaError setMoreinfo(URL moreinfo) {
        this.moreinfo = moreinfo;
        return this;
    }

    public ApiSchemaError setMoreinfoURLasString(String moreinfo) {
        try {
            this.moreinfo = new URL(moreinfo);
        } catch (MalformedURLException e) {
            this.moreinfo = null;
        }
        return this;
    }


    public ApiSchemaError setTimestamp(String timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public ApiSchemaError setPath(String path) {
        this.path = path;
        return this;
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    public int getCode() {
        return code;
    }

    public String getUsermessage() {
        return usermessage;
    }

    public String getInternalmessage() {
        return internalmessage;
    }

    public String getDetails() {
        return details;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public URL getMoreinfo() {
        return moreinfo;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPath() {
        return path;
    }

}
