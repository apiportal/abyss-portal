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
public class BasicTokenParseResult {

    private Boolean isFailed;

    private String username;

    private String password;

    /**
     * @param isFailed
     * @param username
     * @param password
     */
    public BasicTokenParseResult(Boolean isFailed, String username, String password) {
        this.isFailed = isFailed;
        this.username = username;
        this.password = password;
    }

    /**
     * @return the isFailed
     */
    public Boolean getIsFailed() {
        return isFailed;
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }


}