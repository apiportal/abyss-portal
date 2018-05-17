/*
 *  Copyright (C) Verapi Yazilim Teknolojileri A.S. - All Rights Reserved
 *
 *  Unauthorized copying of this file, via any medium is strictly prohibited
 *  Proprietary and confidential
 *
 *  Written by Ismet Faik SAGLAR <faik.saglar@verapi.com>, December 2017
 */
package com.verapi.auth;

/**
 * @author faik.saglar
 *
 */
public class BearerTokenParseResult {

    private Boolean isFailed;

    private String token;

    /**
     * @param isFailed
     * @param token
     */
    public BearerTokenParseResult(Boolean isFailed, String token) {
        this.isFailed = isFailed;
        this.token = token;
    }

    /**
     * @return the isFailed
     */
    public Boolean getIsFailed() {
        return isFailed;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }
}