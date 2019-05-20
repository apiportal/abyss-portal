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

package com.verapi.key.model;

import java.io.Serializable;
import java.time.Instant;

//import java.time.LocalDateTime;
//import java.time.ZonedDateTime;
//import org.joda.time.DateTime;


/**
 * @author faik.saglar
 * @version 1.0
 */
public class AuthenticationInfo implements Serializable {

    private static final long serialVersionUID = -5705493131142227985L;

    private String token;
    private String nonce;
    //private ZonedDateTime expireDate;
    //private DateTime expireDate;
    private Instant expireDate;
    private String userData;

    private boolean isValid;
    private String resultText;

    /**
     * @param token      token
     * @param nonce      nonce
     * @param expireDate expire date
     * @param userData   user data
     * @author faik.saglar
     */
    //public AuthenticationInfo(String token, String nonce, ZonedDateTime expireDate, String userData) {
    //public AuthenticationInfo(String token, String nonce, DateTime expireDate, String userData) {
    public AuthenticationInfo(String token, String nonce, Instant expireDate, String userData) {
        this.token = token;
        this.nonce = nonce;
        this.expireDate = expireDate;
        this.userData = userData;
        this.isValid = false;
        this.resultText = "Token Generation SUCCESSFUL";
    }

    /**
     * @param token      token
     * @param nonce      nonce
     * @param expireDate expire date
     * @param userData   user data
     * @param isValid    is valid
     * @param resultText result text
     * @author faik.saglar
     */
    //public AuthenticationInfo(String token, String nonce, ZonedDateTime expireDate, String userData,
    //public AuthenticationInfo(String token, String nonce, DateTime expireDate, String userData,
    public AuthenticationInfo(String token, String nonce, Instant expireDate, String userData,
                              boolean isValid, String resultText) {
        this.token = token;
        this.nonce = nonce;
        this.expireDate = expireDate;
        this.userData = userData;
        this.isValid = isValid;
        this.resultText = resultText;
    }

    /**
     * @param resultText result text
     * @author faik.saglar
     */
    public AuthenticationInfo(String resultText) {
        this.token = "";
        this.nonce = "";
        this.expireDate = null;
        this.userData = "";

        this.isValid = false;
        this.resultText = resultText;
    }

    /**
     * @return the token
     */
    public String getToken() {
        return token;
    }

    /**
     * @return the nonce
     */
    public String getNonce() {
        return nonce;
    }

    /**
     * @return the expireDate
     */
    //public ZonedDateTime getExpireDate() {
    //public DateTime getExpireDate() {
    public Instant getExpireDate() {
        return expireDate;
    }

    /**
     * @return the userData
     */
    public String getUserData() {
        return userData;
    }

    /**
     * @return the isValid
     */
    public boolean isValid() {
        return isValid;
    }

    /**
     * @param isValid the isValid to set
     */
    public void setValid(boolean isValid) {
        this.isValid = isValid;
    }

    /**
     * @return the resultText
     */
    public String getResultText() {
        return resultText;
    }

    /**
     * @param resultText the resultText to set
     */
    public void setResultText(String resultText) {
        this.resultText = resultText;
    }

}
