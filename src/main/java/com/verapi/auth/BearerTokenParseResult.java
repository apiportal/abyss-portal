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

package com.verapi.auth;

/**
 * @author faik.saglar
 */
public class BearerTokenParseResult {

    private Boolean isFailed;

    private String token;

    /**
     * Http Basic Auth Bearer Token Parse Result
     *
     * @param isFailed is parse failed
     * @param token    token
     */
    BearerTokenParseResult(Boolean isFailed, String token) {
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
