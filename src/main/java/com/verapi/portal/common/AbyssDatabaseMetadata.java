/*
 *
 *  *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *  *
 *  *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  *  Proprietary and confidential
 *  *
 *  *  Written by Halil Özkan <halil.ozkan@verapi.com>, 10 2018
 *
 */

package com.verapi.portal.common;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbyssDatabaseMetadata {

    private static Logger logger = LoggerFactory.getLogger(AbyssDatabaseMetadata.class);

    String COLUMN_NAME;
    private String DATA_TYPE;
    public String TYPE_NAME;
    private String COLUMN_SIZE;
    private String DECIMAL_DIGITS;
    private String NULLABLE;
    private String REMARKS;
    private String COLUMN_DEF;
    private String IS_NULLABLE;

    private String IS_AUTOINCREMENT;

    Boolean isPrivate = false;
    Boolean isReadOnly = false;
    Boolean isWriteOnly = false;
    Boolean isEmailAddress = false;
    Boolean isBase64Encoded = false;
    public Boolean isJsonb = false;
    public Boolean isUuid = false;

    static final String FIELD_ID = "id";
    static final String FIELD_UUID = "uuid";

    static final String TYPE_JSONB = "jsonb";
    static final String TYPE_UUID = "uuid";

    static final String TAG_READONLY = "#readOnly#";
    static final String TAG_EMAIL = "#email#";
    static final String TAG_BASE64 = "#base64#";
    static final String TAG_LEVEL0 = "#Level:0#";
    static final String TAG_LEVEL1 = "#Level:1#";

    public AbyssDatabaseMetadata() {
    }

    public AbyssDatabaseMetadata(String COLUMN_NAME, String DATA_TYPE, String TYPE_NAME, String COLUMN_SIZE, String DECIMAL_DIGITS, String NULLABLE, String REMARKS, String COLUMN_DEF, String IS_NULLABLE, String IS_AUTOINCREMENT) {
        //default initializations
        this.COLUMN_NAME = COLUMN_NAME;
        this.DATA_TYPE = DATA_TYPE;
        this.TYPE_NAME = TYPE_NAME;
        this.COLUMN_SIZE = COLUMN_SIZE;
        this.DECIMAL_DIGITS = DECIMAL_DIGITS;
        this.NULLABLE = NULLABLE;
        this.REMARKS = REMARKS;
        this.COLUMN_DEF = COLUMN_DEF;
        this.IS_NULLABLE = IS_NULLABLE;
        this.IS_AUTOINCREMENT = IS_AUTOINCREMENT;

        //custom initializations by using inputs
        if (COLUMN_NAME.equals(FIELD_ID)) {
            this.isReadOnly = true;
            this.isPrivate = true;
        }

        if (COLUMN_NAME.equals(FIELD_UUID))
            this.isReadOnly = true;

        if (TYPE_NAME.equals(TYPE_JSONB))
            this.isJsonb = true;

        if (TYPE_NAME.equals(TYPE_UUID))
            this.isUuid = true;


        if (!REMARKS.isEmpty() && REMARKS.contains(TAG_LEVEL0)) {
            this.isWriteOnly = true;
            this.isPrivate = true;
        }

        if (!REMARKS.isEmpty() && REMARKS.contains(TAG_LEVEL1)) {
            this.isWriteOnly = true;
            this.isPrivate = true;
        }

        if (!REMARKS.isEmpty() && REMARKS.contains(TAG_READONLY))
            this.isReadOnly = true;

        if (!REMARKS.isEmpty() && REMARKS.contains(TAG_EMAIL))
            this.isEmailAddress = true;

        if (!REMARKS.isEmpty() && REMARKS.contains(TAG_BASE64))
            this.isBase64Encoded = true;

        logger.trace("{}", this.toString());
    }

    public JsonObject toJson() {
        return JsonObject.mapFrom(this);
    }

    @Override
    public String toString() {
        return "AbyssDatabaseMetadata{" +
                "COLUMN_NAME='" + COLUMN_NAME + '\'' +
                ", DATA_TYPE='" + DATA_TYPE + '\'' +
                ", TYPE_NAME='" + TYPE_NAME + '\'' +
                ", COLUMN_SIZE='" + COLUMN_SIZE + '\'' +
                ", DECIMAL_DIGITS='" + DECIMAL_DIGITS + '\'' +
                ", NULLABLE='" + NULLABLE + '\'' +
                ", REMARKS='" + REMARKS + '\'' +
                ", COLUMN_DEF='" + COLUMN_DEF + '\'' +
                ", IS_NULLABLE='" + IS_NULLABLE + '\'' +
                ", IS_AUTOINCREMENT='" + IS_AUTOINCREMENT + '\'' +
                ", isPrivate=" + isPrivate +
                ", isReadOnly=" + isReadOnly +
                ", isWriteOnly=" + isWriteOnly +
                ", isEmailAddress=" + isEmailAddress +
                ", isBase64Encoded=" + isBase64Encoded +
                ", isJsonb=" + isJsonb +
                ", isUuid=" + isUuid +
                '}';
    }

}